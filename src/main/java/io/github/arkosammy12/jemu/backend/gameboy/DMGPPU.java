package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.Bus;
import io.github.arkosammy12.jemu.backend.common.VideoGenerator;
import io.github.arkosammy12.jemu.backend.common.Processor;
import io.github.arkosammy12.jemu.backend.cores.SM83;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import java.util.Arrays;
import java.util.LinkedList;

import static io.github.arkosammy12.jemu.backend.gameboy.GameBoyBus.*;
import static io.github.arkosammy12.jemu.backend.gameboy.GameBoyMMIOBus.*;

public class DMGPPU<E extends GameBoyEmulator> extends VideoGenerator<E> implements Bus {

    public static final int WIDTH = 160;
    public static final int HEIGHT = 144;

    private static final int OAM_SCAN_MODE = 2;
    private static final int DRAWING_MODE = 3;
    private static final int HBLANK_MODE = 0;
    private static final int VBLANK_MODE = 1;

    private static final int CYCLES_PER_SCANLINE = 456;
    private static final int SCANLINES_PER_FRAME = 154;

    private static final int DMG_LCD_OFF_COLOR = 0xFF9BBC0F;

    private static final int[] DMG_PALETTE = {
            0xFF9BBC0F,
            0xFF8BAC0F,
            0xFF306230,
            0xFF0F380F
    };

    private final int[] vRam = new int[0x2000];
    private final int[] oam = new int[0x00A0];

    private int lcdControl;
    private int ppuStatus;
    private int scrollY;
    private int scrollX;
    private int lcdY;
    private int lcdYCompare;
    private int backgroundPalette;
    private int objectPalette0;
    private int objectPalette1;
    private int windowY;
    private int windowX;

    private final int[][] lcd;

    private Mode currentMode = Mode.MODE_0_HBLANK;
    private int scanlineCycle;
    private int dotCycleIndex;
    private int scanlineNumber;

    private boolean enablePixelWrites;
    private int enablePixelWritesDelay;

    private boolean oldStatInterruptLine;

    private boolean windowPixelRendered;

    private int pixelX;
    private int discardedPixels;
    private int windowLine;
    private boolean windowYCondition;
    private boolean windowXCondition;

    private final Integer[] spriteBuffer = new Integer[10];
    private int scannedEntries = 0;

    private static final int TERMINATE_BG_FETCHER = -1;
    private final IntArrayFIFOQueue backgroundFifo = new IntArrayFIFOQueue(8);
    private int bgFifoStep = TERMINATE_BG_FETCHER;
    private int bgFifoFetcherX;
    private int bgFifoCurrentTileNumber;
    private int bgFifoTileDataEffectiveAddress;
    private int bgFifoTileDataLow;
    private int bgFifoTileDataHigh;

    private static final int TERMINATE_SPRITE_FETCHER = -1;
    private final LinkedList<Integer> spriteFifo = new LinkedList<>();
    private int spriteFifoCurrentEntryIndex;
    private int spriteFifoStep = TERMINATE_SPRITE_FETCHER;
    private int spriteFifoCurrentTileNumber;
    private int spriteFifoTileDataEffectiveAddress;
    private int spriteFifoTileDataLow;
    private int spriteFifoTileDataHigh;

    public DMGPPU(E emulator) {
        super(emulator);
        this.lcd = new int[this.getImageWidth()][this.getImageHeight()];
        for (int[] ints : this.lcd) {
            Arrays.fill(ints, DMG_LCD_OFF_COLOR);
        }
        Arrays.fill(this.spriteBuffer, null);
        for (int i = 0; i < 8; i++) {
            this.spriteFifo.offer(null);
        }
    }

    @Override
    public int getImageWidth() {
        return WIDTH;
    }

    @Override
    public int getImageHeight() {
        return HEIGHT;
    }

    @Override
    public int readByte(int address) {
        if (address >= OAM_START && address <= OAM_END) {
            int ppuMode = this.getPpuMode();
            if (ppuMode == OAM_SCAN_MODE || ppuMode == DRAWING_MODE) {
                return 0xFF;
            }
            return this.oam[address - OAM_START];

        } else if (address >= VRAM_START && address <= VRAM_END) {
            if (this.getPpuMode() != DRAWING_MODE || !this.getLcdPpuEnable()) {
                return this.vRam[address - VRAM_START];
            } else {
                return 0xFF;
            }
        } else {
            return switch (address) {
                case LCDC_ADDR -> this.lcdControl;
                case STAT_ADDR -> this.ppuStatus | 0b10000000;
                case SCY_ADDR -> this.scrollY;
                case SCX_ADDR -> this.scrollX;
                case LY_ADDR -> this.lcdY;
                case LYC_ADDR -> this.lcdYCompare;
                case BGP_ADDR -> this.backgroundPalette;
                case OBP0_ADDR -> this.objectPalette0;
                case OBP1_ADDR -> this.objectPalette1;
                case WY_ADDR -> this.windowY;
                case WX_ADDR -> this.windowX;
                default -> throw new EmulatorException("Invalid address $%04X for GameBoy PPU!".formatted(address));
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= OAM_START && address <= OAM_END) {
            int ppuMode = this.getPpuMode();
            if (ppuMode == HBLANK_MODE || ppuMode == VBLANK_MODE) {
              this.oam[address - OAM_START] = value & 0xFF;
            }
        } else if (address >= VRAM_START && address <= VRAM_END) {
            if (this.getPpuMode() != DRAWING_MODE) {
              this.vRam[address - VRAM_START] = value & 0xFF;
            }
        } else {
            switch (address) {
                case LCDC_ADDR -> {
                    boolean oldLcdEnable = this.getLcdPpuEnable();
                    this.lcdControl = value & 0xFF;
                    boolean newLcdEnable = this.getLcdPpuEnable();
                    if (oldLcdEnable != newLcdEnable) {
                        this.scanlineNumber = 0;
                        this.lcdY = 0;
                        this.scanlineCycle = 0;
                        this.currentMode = Mode.MODE_0_HBLANK;
                        this.setPpuMode(HBLANK_MODE);
                    }
                    if (!oldLcdEnable && newLcdEnable) {
                        this.onLcdOn();
                    } else if (oldLcdEnable && !newLcdEnable) {
                        this.onLcdOff();
                    }
                }
                case STAT_ADDR -> this.ppuStatus = (value & 0b11111000) | (this.ppuStatus & 0b111);
                case SCY_ADDR -> this.scrollY = value & 0xFF;
                case SCX_ADDR -> {
                    this.scrollX = value & 0xFF;
                }
                case LY_ADDR -> {}
                case LYC_ADDR -> this.lcdYCompare = value & 0xFF;
                case BGP_ADDR -> this.backgroundPalette = value & 0xFF;
                case OBP0_ADDR -> this.objectPalette0 = value & 0xFF;
                case OBP1_ADDR -> this.objectPalette1 = value & 0xFF;
                case WY_ADDR -> this.windowY = value & 0xFF;
                case WX_ADDR -> this.windowX = value & 0xFF;
                default -> throw new EmulatorException("Invalid address $%04X for GameBoy PPU!".formatted(address));
            }
        }
    }

    private void onLcdOn() {
        this.enablePixelWritesDelay = 2;
        this.scanlineCycle = 4;
    }

    private void onLcdOff() {
        this.enablePixelWrites = false;
        this.enablePixelWritesDelay = -1;
        for (int[] ints : this.lcd) {
            Arrays.fill(ints, DMG_LCD_OFF_COLOR);
        }
        this.emulator.getHost().getVideoDriver().ifPresent(driver -> driver.outputFrame(this.lcd));
    }

    public void cycle() {
        this.cycleDot();
        this.cycleDot();
        this.cycleDot();
        this.cycleDot();
    }

    private void cycleDot() {
        if (!this.getLcdPpuEnable()) {
            return;
        }

        boolean scanlineNumberIncremented = false;
        this.scanlineCycle++;
        if (this.scanlineCycle >= CYCLES_PER_SCANLINE) {
            scanlineNumberIncremented = true;
            this.scanlineCycle = 0;
        }

        switch (this.currentMode) {
            case MODE_0_HBLANK -> this.onHBlank();
            case MODE_1_VBLANK -> this.onVBlank();
            case MODE_2_OAM_SCAN -> this.onOamScan();
            case MODE_3_DRAWING -> this.onDrawing();
        }

        if (scanlineNumberIncremented) {
            int originalScanlineNumber = this.scanlineNumber;
            this.scanlineNumber = (this.scanlineNumber + 1) % SCANLINES_PER_FRAME;
            if (originalScanlineNumber != 153) {
                this.lcdY = (this.lcdY + 1) % SCANLINES_PER_FRAME;
            }
            this.dotCycleIndex = 0;
            this.clearLyEqualsLycFlag();
            if (this.windowPixelRendered) {
                this.windowPixelRendered = false;
                this.windowLine = (this.windowLine + 1) % SCANLINES_PER_FRAME;
            }
        }

        this.nextState(scanlineNumberIncremented);

        //if (this.scanlineCycle >= 3) {
            if (this.lcdY == this.lcdYCompare) {
                this.setLyEqualsLycFlag();
            } else {
                this.clearLyEqualsLycFlag();
            }
        //}

        boolean statInterruptLine = false;
        statInterruptLine |= this.getLycInterruptSelect() && this.getLyEqualsLycFlag();
        statInterruptLine |= this.getMode0InterruptSelect() && this.getPpuMode() == HBLANK_MODE;
        statInterruptLine |= this.getMode1InterruptSelect() && this.getPpuMode() == VBLANK_MODE;
        statInterruptLine |= this.getMode2InterruptSelect() && this.getPpuMode() == OAM_SCAN_MODE;
        if (!this.oldStatInterruptLine && statInterruptLine) {
            this.triggerStatInterrupt();
        }
        this.oldStatInterruptLine = statInterruptLine;

    }

    private void nextState(boolean lcdYIncremented) {
        if (lcdYIncremented) {
            this.dotCycleIndex = 0;
        }
        Mode oldMode = this.currentMode;
        if (this.scanlineCycle == 0) {
            if (this.scanlineNumber >= 144) {
                this.currentMode = Mode.MODE_1_VBLANK;
            } else {
                this.currentMode = Mode.MODE_2_OAM_SCAN;
            }
        } else if (this.scanlineCycle == 80) {
            if (this.scanlineNumber >= 144) {
                this.currentMode = Mode.MODE_1_VBLANK;
            } else {
                this.currentMode = Mode.MODE_3_DRAWING;
            }
        } else if (this.pixelX >= 168) {
            this.currentMode = Mode.MODE_0_HBLANK;
        }
        if (oldMode != this.currentMode) {
            this.dotCycleIndex = 0;
        }
    }

    private void onVBlank() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                this.dotCycleIndex = 1;
            }
            case 1 -> {
                this.dotCycleIndex = 2;
            }
            case 2 -> {
                this.setPpuMode(Mode.MODE_1_VBLANK.getValue());
                this.dotCycleIndex = 3;
            }
            case 3 -> {
                if (this.scanlineNumber == 144) {
                    this.triggerVBlankInterrupt();
                    this.windowYCondition = false;
                    this.windowLine = 0;

                    // For some reason, Mooneye test vblank_stat_intr-GS.gb expects this
                    if (this.getMode2InterruptSelect()) {
                        this.triggerStatInterrupt();
                    }

                    if (this.enablePixelWritesDelay > 0) {
                        this.enablePixelWritesDelay--;
                        if (this.enablePixelWritesDelay == 0) {
                            this.enablePixelWrites = true;
                        }
                    }

                    this.emulator.getHost().getVideoDriver().ifPresent(driver -> driver.outputFrame(this.lcd));
                } else if (this.scanlineNumber == 153) {
                    this.lcdY = 0;
                }

                this.dotCycleIndex = 4;
            }
            case 4 -> {}
        }
    }

    private void onHBlank() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                this.scannedEntries = 0;

                this.pixelX = 0;
                this.discardedPixels = 0;
                this.windowXCondition = false;

                this.backgroundFifo.clear();
                this.bgFifoStep = TERMINATE_BG_FETCHER;
                this.bgFifoFetcherX = 0;
                this.bgFifoCurrentTileNumber = 0;
                this.bgFifoTileDataEffectiveAddress = 0;
                this.bgFifoTileDataLow = 0;
                this.bgFifoTileDataHigh = 0;

                Arrays.fill(this.spriteBuffer, null);

                this.spriteFifo.clear();
                for (int i = 0; i < 8; i++) {
                    this.spriteFifo.add(null);
                }

                this.spriteFifoStep = TERMINATE_SPRITE_FETCHER;
                this.spriteFifoCurrentEntryIndex = -1;
                this.spriteFifoCurrentTileNumber = 0;
                this.spriteFifoTileDataEffectiveAddress = 0;
                this.spriteFifoTileDataLow = 0;
                this.spriteFifoTileDataHigh = 0;

                this.dotCycleIndex = 1;
            }
            case 1 -> {
                this.dotCycleIndex = 2;
            }
            case 2 -> {
                this.setPpuMode(Mode.MODE_0_HBLANK.getValue());
                this.dotCycleIndex = 3;
            }
            case 3 -> {}
        }
    }

    private void onOamScan() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                if (this.scanlineNumber == this.windowY) {
                    this.windowYCondition = true;
                }
                this.dotCycleIndex = 1;
            }
            case 1 -> {
                this.scanOamEntry();
                this.dotCycleIndex = 2;
            }
            case 2, 4 -> {
                this.dotCycleIndex = 3;
            }
            case 3 -> {
                this.scanOamEntry();
                this.setPpuMode(Mode.MODE_2_OAM_SCAN.getValue());
                this.dotCycleIndex = 4;
            }
        }
    }

    private void scanOamEntry() {
        int spriteY = this.getOamByte(0xFE00 + (this.scannedEntries * 4));
        int spriteX = this.getOamByte(0xFE00 + (this.scannedEntries * 4) + 1);
        int tileIndex = this.getOamByte(0xFE00 + (this.scannedEntries * 4) + 2);
        int spriteAttributes = this.getOamByte(0xFE00 + (this.scannedEntries * 4) + 3);

        for (int i = 0; i < 10; i++) {
            if (this.spriteBuffer[i] == null) {
                if ((this.scanlineNumber + 16 >= spriteY) && (this.scanlineNumber + 16 < spriteY + (this.getObjectSize() ? 16 : 8))) {
                    this.spriteBuffer[i] = createSpriteBufferEntry(spriteY, spriteX, tileIndex, spriteAttributes);
                }
                break;
            }
        }
        this.scannedEntries++;
    }

    private void onDrawing() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                this.tickDraw();
                this.dotCycleIndex = 1;
            }
            case 1 -> {
                this.tickDraw();
                this.dotCycleIndex = 2;
            }
            case 2 -> {
                this.tickDraw();
                this.dotCycleIndex = 3;
            }
            case 3 -> {
                this.setPpuMode(Mode.MODE_3_DRAWING.getValue());
                this.tickDraw();
                this.dotCycleIndex = 4;
            }
            case 4 -> {
                this.tickDraw();
            }
        }
    }

    private void tickDraw() {
        boolean originalWindowCondition = this.isRenderingWindow();
        if (this.pixelX == this.windowX + 1 && this.getWindowEnable()) {
            this.windowXCondition = true;
        }

        if (!originalWindowCondition && this.isRenderingWindow()) {
            this.bgFifoStep = TERMINATE_BG_FETCHER;
            this.bgFifoFetcherX = 0;
            this.windowPixelRendered = true;
            this.backgroundFifo.clear();
        }

        int currentSpriteEntryIndex = this.getSpriteEntryIndexMatchingX(this.pixelX);
        if ((currentSpriteEntryIndex >= 0 || this.spriteFifoStep >= 0) && this.getObjectEnable()) {
            if (this.bgFifoStep != 6) {
                if (this.bgFifoStep < 0) {
                    this.bgFifoStep = 0;
                }
                this.tickBackgroundFifo();
            } else {
                if (this.spriteFifoStep < 0) {
                    this.spriteFifoStep = 0;
                }
                if (this.spriteFifoCurrentEntryIndex < 0) {
                    this.spriteFifoCurrentEntryIndex = currentSpriteEntryIndex;
                }
                this.tickSpriteFifo();
            }
        } else {
            if (this.bgFifoStep < 0) {
                this.bgFifoStep = 0;
            }
            this.tickBackgroundFifo();
            this.tickPixelShifter();
        }
    }

    private void tickBackgroundFifo() {
        switch (bgFifoStep) {
            case 0 -> {
                this.bgFifoStep = 1;
            }
            case 1 -> {
                if (this.isRenderingWindow()) {
                    int tileMapBase = this.getWindowTileMap() ? 0x9C00 : 0x9800;
                    int tileX = this.bgFifoFetcherX & 0x1F;
                    int tileY = this.windowLine >>> 3;
                    int tileMapIndex = tileX + (tileY * 32);
                    tileMapIndex &= 0x3FF;
                    int address = tileMapBase + tileMapIndex;
                    this.bgFifoCurrentTileNumber = this.getVRamByte(address);
                } else {
                    int tileMapBase = this.getBackgroundTileMap() ? 0x9C00 : 0x9800;
                    int tileX = ((pixelX + scrollX) >> 3) & 0x1F;
                    int tileY = ((this.scanlineNumber + this.scrollY) & 0xFF) >>> 3;
                    int tileMapIndex = tileX + (tileY * 32);
                    tileMapIndex &= 0x3FF;
                    int address = tileMapBase + tileMapIndex;
                    this.bgFifoCurrentTileNumber = this.getVRamByte(address);
                }
                this.bgFifoStep = 2;
            }
            case 2 -> {
                this.bgFifoStep = 3;
            }
            case 3 -> {
                int effectiveAddress;
                if (this.getBackgroundAndWindowTiles()) {
                    effectiveAddress = 0x8000 + (this.bgFifoCurrentTileNumber * 16);
                } else {
                    byte signedTileNumber =  (byte) this.bgFifoCurrentTileNumber;
                    effectiveAddress = 0x9000 + (signedTileNumber * 16);
                }

                if (this.isRenderingWindow()) {
                    effectiveAddress = (effectiveAddress + (2 * (this.windowLine % 8))) & 0xFFFF;
                } else {
                    effectiveAddress = (effectiveAddress + (2 * ((this.scanlineNumber + this.scrollY) % 8))) & 0xFFFF;
                }

                this.bgFifoTileDataEffectiveAddress = effectiveAddress;
                this.bgFifoTileDataLow = this.getVRamByte(effectiveAddress);

                this.bgFifoStep = 4;
            }
            case 4 -> {
                this.bgFifoStep = 5;
            }
            case 5 -> {
                this.bgFifoTileDataHigh = this.getVRamByte((this.bgFifoTileDataEffectiveAddress + 1) & 0xFFFF);
                if (this.backgroundFifo.isEmpty()) {
                    this.pushPixelsToBgFifo();
                    this.bgFifoStep = TERMINATE_BG_FETCHER;
                } else {
                    this.bgFifoStep = 6;
                }
            }
            case 6 -> {
                if (!this.backgroundFifo.isEmpty()) {
                    this.bgFifoStep = 6;
                } else {
                    this.pushPixelsToBgFifo();
                    this.bgFifoStep = TERMINATE_BG_FETCHER;
                }
            }
        }
    }

    private void pushPixelsToBgFifo() {
        for (int i = 7; i >= 0; i--) {
            int low = (this.bgFifoTileDataLow >>> i) & 1;
            int high = (this.bgFifoTileDataHigh >>> i) & 1;
            this.backgroundFifo.enqueue((high << 1) | low);
        }
        this.bgFifoFetcherX++;
    }

    private void tickSpriteFifo() {
        switch (this.spriteFifoStep) {
            case 0 -> {
                this.spriteFifoStep = 1;
            }
            case 1 -> {
                this.spriteFifoCurrentTileNumber = getTileIndexFromEntry(this.spriteBuffer[this.spriteFifoCurrentEntryIndex]);
                this.spriteFifoStep = 2;
            }
            case 2 -> {
                this.spriteFifoStep = 3;
            }
            case 3 -> {
                boolean objSize = this.getObjectSize();
                int spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
                int spriteAttributes = getSpriteAttributesFromEntry(spriteEntry);
                boolean yFlip = getYFlipFromAttributes(spriteAttributes);
                int spriteY = getSpriteYFromEntry(spriteEntry);
                int tileIndex = this.spriteFifoCurrentTileNumber;

                int width = objSize ? 15 : 7;
                if (objSize) {
                    tileIndex &= ~1;
                }

                int row = ((scanlineNumber + 16) - spriteY) % (width + 1);
                if (row < 0) {
                    row += (width + 1);
                }

                int offset = yFlip ? (width - row) * 2 : row * 2;
                this.spriteFifoTileDataEffectiveAddress = (0x8000 + tileIndex * 16 + offset) & 0xFFFF;

                this.spriteFifoTileDataLow = this.getVRamByte(this.spriteFifoTileDataEffectiveAddress);
                this.spriteFifoStep = 4;
            }
            case 4 -> {
                this.spriteFifoStep = 5;
            }
            case 5 -> {
                this.spriteFifoTileDataHigh = this.getVRamByte((this.spriteFifoTileDataEffectiveAddress + 1) & 0xFFFF);

                int spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
                int spriteX = getSpriteXFromEntry(spriteEntry);
                int spriteAttributes = getSpriteAttributesFromEntry(spriteEntry);
                boolean xFlip = getXFlipFromAttributes(spriteAttributes);
                boolean priority = getPriorityFromAttributes(spriteAttributes);
                boolean palette = getDmgPaletteFromAttributes(spriteAttributes);

                for (int i = 0; i < 8; i++) {
                    if (spriteX + i < 8) {
                        continue;
                    }
                    Integer currentQueuedPixel = this.spriteFifo.get(i);
                    if (currentQueuedPixel == null || getColorNumberFromPixelEntry(currentQueuedPixel) == 0) {
                        int bit = xFlip ? 1 << i : 1 << (7 - i);
                        int low = (this.spriteFifoTileDataLow & bit) != 0 ? 1 : 0;
                        int high = (this.spriteFifoTileDataHigh & bit) != 0 ? 1 : 0;
                        int colorNumber = (low | (high << 1));
                        int pixelEntry = createPixelEntry(colorNumber, priority, palette);
                        this.spriteFifo.set(i, pixelEntry);
                    }
                }

                this.spriteBuffer[this.spriteFifoCurrentEntryIndex] = null;
                this.spriteFifoCurrentEntryIndex = -1;
                this.spriteFifoStep = TERMINATE_SPRITE_FETCHER;
            }
        }
    }

    private void tickPixelShifter() {
        if (this.backgroundFifo.isEmpty()) {
            return;
        }

        int bgPixel = this.backgroundFifo.dequeueInt();
        if (!this.getBackgroundAndWindowEnable()) {
            bgPixel = 0;
        }
        int bgPaletteIndex = (this.backgroundPalette >> (bgPixel * 2)) & 0b11;
        Integer finalPixel = DMG_PALETTE[bgPaletteIndex];

        int bgDiscardTarget = this.scrollX % 8;
        if (!this.isRenderingWindow() && this.discardedPixels < bgDiscardTarget) {
            this.discardedPixels++;
            finalPixel = null;
        }

        Integer spritePixel = this.spriteFifo.poll();
        this.spriteFifo.offer(null);
        if (spritePixel != null) {
            int spriteColorNumber = getColorNumberFromPixelEntry(spritePixel);
            boolean priority = getPriorityForPixelEntry(spritePixel);
            boolean palette = getPaletteForPixelEntry(spritePixel);
            if (spriteColorNumber != 0 && !(priority && bgPixel != 0)) {
                int colorPaletteIndex = ((palette ? this.objectPalette1 : this.objectPalette0) >>> (spriteColorNumber * 2)) & 0b11;
                finalPixel = DMG_PALETTE[colorPaletteIndex];
            }
        }

        if (finalPixel != null) {
            if (this.pixelX >= 8 && this.enablePixelWrites) {
                this.lcd[this.pixelX - 8][this.scanlineNumber] = finalPixel;
            }
            this.pixelX++;
        }
    }

    private boolean getLcdPpuEnable() {
        return (this.lcdControl & 0b10000000) != 0;
    }

    private boolean getWindowTileMap() {
        return (this.lcdControl & 0b01000000) != 0;
    }

    private boolean getWindowEnable() {
        return (this.lcdControl & 0b00100000) != 0;
    }

    private boolean getBackgroundAndWindowTiles() {
        return (this.lcdControl & 0b00010000) != 0;
    }

    private boolean getBackgroundTileMap() {
        return (this.lcdControl & 0b00001000) != 0;
    }

    private boolean getObjectSize() {
        return (this.lcdControl & 0b00000100) != 0;
    }

    private boolean getObjectEnable() {
        return (this.lcdControl & 0b00000010) != 0;
    }

    private boolean getBackgroundAndWindowEnable() {
        return (this.lcdControl & 0b00000001) != 0;
    }

    private boolean getLycInterruptSelect() {
        return (this.ppuStatus & 0b01000000) != 0;
    }

    private boolean getMode2InterruptSelect() {
        return (this.ppuStatus & 0b00100000) != 0;
    }

    private boolean getMode1InterruptSelect() {
        return (this.ppuStatus & 0b00010000) != 0;
    }

    private boolean getMode0InterruptSelect() {
        return (this.ppuStatus & 0b00001000) != 0;
    }

    public boolean getLyEqualsLycFlag() {
        return (this.ppuStatus & 0b00000100) != 0;
    }

    private int getPpuMode() {
        return this.ppuStatus & 0b11;
    }

    private void setPpuMode(int mode) {
        this.ppuStatus = (this.ppuStatus & 0b11111100) | (mode & 0b11);
    }

    private void setLcdY(int value) {
        this.scanlineNumber = value % SCANLINES_PER_FRAME;
    }

    private void setLyEqualsLycFlag() {
        this.ppuStatus = Processor.setBit(this.ppuStatus, 0b100);
    }

    private void clearLyEqualsLycFlag() {
        this.ppuStatus = Processor.clearBit(this.ppuStatus, 0b100);
    }

    public void writeOamDma(int address, int value) {
        if (address >= OAM_START && address <= OAM_END) {
            this.oam[address - OAM_START] = value & 0xFF;
        } else {
            throw new EmulatorException("Invalid GameBoy OAM address \"%04X\"!".formatted(address));
        }
    }

    private int getOamByte(int address) {
        if (address >= OAM_START && address <= OAM_END) {
            return this.oam[address - OAM_START];
        } else {
            throw new EmulatorException("Invalid GameBoy OAM address \"%04X\"!".formatted(address));
        }
    }

    private int getVRamByte(int address) {
        if (address >= VRAM_START && address <= VRAM_END) {
            return this.vRam[address - VRAM_START];
        } else {
            throw new EmulatorException("Invalid GameBoy VRAM address \"%04X\"!".formatted(address));
        }
    }

    private int getSpriteEntryIndexMatchingX(int x) {
        for (int i = 0; i < 10; i++) {
            Integer spriteEntry = this.spriteBuffer[i];
            if (spriteEntry != null && getSpriteXFromEntry(spriteEntry) == x) {
                return i;
            }
        }
        return -1;
    }

    private boolean isRenderingWindow() {
        return this.windowXCondition && this.windowYCondition;
    }

    private void triggerVBlankInterrupt() {
        int IF = this.emulator.getMMIOController().getIF();
        this.emulator.getMMIOController().setIF(Processor.setBit(IF, SM83.VBLANK_MASK));
    }

    private void triggerStatInterrupt() {
        int IF = this.emulator.getMMIOController().getIF();
        this.emulator.getMMIOController().setIF(Processor.setBit(IF, SM83.LCD_MASK));
    }

    private static int createSpriteBufferEntry(int spriteY, int spriteX, int tileIndex, int spriteAttributes) {
        return ((spriteAttributes & 0xFF) << 24) | ((tileIndex & 0xFF) << 16) | ((spriteX & 0xFF) << 8) | (spriteY & 0xFF);
    }

    private static int getSpriteAttributesFromEntry(int entry) {
        return (entry >>> 24) & 0xFF;
    }

    private static int getTileIndexFromEntry(int entry) {
        return (entry >>> 16) & 0xFF;
    }

    private static int getSpriteXFromEntry(int entry) {
        return (entry >>> 8) & 0xFF;
    }

    private static int getSpriteYFromEntry(int entry) {
        return (entry) & 0xFF;
    }

    private static boolean getPriorityFromAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b10000000) != 0;
    }

    private static boolean getYFlipFromAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b01000000) != 0;
    }

    private static boolean getXFlipFromAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b00100000) != 0;
    }

    private static boolean getDmgPaletteFromAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b00010000) != 0;
    }

    private static int createPixelEntry(int colorNumber, boolean priority, boolean palette) {
        return ((palette ? 1 : 0) << 16) | ((priority ? 1 : 0) << 8) | colorNumber;
    }

    private static int getColorNumberFromPixelEntry(int pixel) {
        return pixel & 0b11;
    }

    private static boolean getPriorityForPixelEntry(int pixel) {
        return ((pixel >>> 8) & 1) != 0;
    }

    private static boolean getPaletteForPixelEntry(int pixel) {
        return ((pixel >>> 16) & 1) != 0;
    }

    private enum Mode {
        MODE_0_HBLANK(HBLANK_MODE),
        MODE_1_VBLANK(VBLANK_MODE),
        MODE_2_OAM_SCAN(OAM_SCAN_MODE),
        MODE_3_DRAWING(DRAWING_MODE);

        private final int value;

        Mode(int value) {
            this.value = value;
        }

        private int getValue() {
            return this.value;
        }

        private boolean matchesValue(int value) {
            return value == this.value;
        }

    }

}
