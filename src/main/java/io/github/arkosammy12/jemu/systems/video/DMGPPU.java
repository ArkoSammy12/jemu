package io.github.arkosammy12.jemu.systems.video;

import io.github.arkosammy12.jemu.systems.GameBoyEmulator;
import io.github.arkosammy12.jemu.systems.bus.Bus;
import io.github.arkosammy12.jemu.systems.cpu.Processor;
import io.github.arkosammy12.jemu.systems.cpu.SM83;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import java.util.Arrays;

import static io.github.arkosammy12.jemu.systems.bus.GameBoyBus.*;
import static io.github.arkosammy12.jemu.systems.misc.gameboy.GameBoyMMIOBus.*;

public class DMGPPU<E extends GameBoyEmulator> extends Display<E> implements Bus {

    public static final int WIDTH = 160;
    public static final int HEIGHT = 144;

    private static final int OAM_SCAN_MODE = 2;
    private static final int DRAWING_MODE = 3;
    private static final int HBLANK_MODE = 0;
    private static final int VBLANK_MODE = 1;

    private static final int CYCLES_PER_SCANLINE = 456;
    private static final int SCANLINES_PER_FRAME = 154;

    int[] DMG_PALETTE = {
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

    private long cycles;
    private boolean oldStatInterruptLine;

    private boolean firstOamScanCycle;
    private boolean firstHBlankCycle;
    private boolean firstVBlankCycle;
    private boolean windowPixelRendered;

    private int pixelX;
    private int discardedPixels;
    private int windowLine;
    private boolean windowYCondition;
    private boolean windowXCondition;

    private final int[] oamEntryBuffer = new int[10 * 4];
    private int scannedEntries = 0;
    private int oamScanStep;

    private final IntArrayFIFOQueue backgroundFifo = new IntArrayFIFOQueue(8);
    private int bgFifoStep;
    private int bgFifoFetcherX;
    private int bgFifoCurrentTileNumber;
    private int bgFifoTileDataEffectiveAddress;
    private int bgFifoTileDataLow;
    private int bgFifoTileDataHigh;

    private final IntArrayFIFOQueue spriteFifo = new IntArrayFIFOQueue(8);
    private int spriteFifoStep;

    public DMGPPU(E emulator) {
        super(emulator);
        this.lcd = new int[getWidth()][getHeight()];
        for (int[] ints : this.lcd) {
            Arrays.fill(ints, 0xFF000000);
        }
        Arrays.fill(this.oamEntryBuffer, -1);
        this.setPpuMode(OAM_SCAN_MODE);
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
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
    public void writeByte(int address, int value) {
        if (address >= OAM_START && address <= OAM_END) {
            //int ppuMode = this.getPpuMode();
            //if (ppuMode == HBLANK_MODE || ppuMode == VBLANK_MODE) {
                this.oam[address - OAM_START] = value & 0xFF;
            //}
        } else if (address >= VRAM_START && address <= VRAM_END) {
            //if (this.getPpuMode() != DRAWING_MODE) {
                this.vRam[address - VRAM_START] = value & 0xFF;
            //}
        } else {
            switch (address) {
                case LCDC_ADDR -> this.lcdControl = value & 0xFF;
                case STAT_ADDR -> this.ppuStatus = (value & 0b11111000) | (this.ppuStatus & 0b111);
                case SCY_ADDR -> this.scrollY = value & 0xFF;
                case SCX_ADDR -> this.scrollX = value & 0xFF;
                case LY_ADDR -> {}
                case LYC_ADDR -> this.lcdYCompare = value & 0xFF;
                case BGP_ADDR -> this.backgroundPalette = value & 0xFF;
                case OBP0_ADDR -> this.objectPalette0 = value & 0xFF;
                case OBP1_ADDR -> this.objectPalette1 = value & 0xFF;
                case WY_ADDR -> this.windowY = value & 0xFF;
                case WX_ADDR -> this.windowX = value & 0xFF;
                default -> throw new IllegalArgumentException("Invalid address \"%04X\" for GameBoy PPU".formatted(address));
            }
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= OAM_START && address <= OAM_END) {
            //int ppuMode = this.getPpuMode();
            //if (ppuMode == HBLANK_MODE || ppuMode == VBLANK_MODE) {
                return this.oam[address - OAM_START];
            //} else {
                //return 0xFF;
            //}
        } else if (address >= VRAM_START && address <= VRAM_END) {
            //if (this.getPpuMode() != DRAWING_MODE) {
                return this.vRam[address - VRAM_START];
            //} else {
                //return 0xFF;
            //}
        } else {
            return switch (address) {
                case LCDC_ADDR -> this.lcdControl;
                case STAT_ADDR -> this.ppuStatus;
                case SCY_ADDR -> this.scrollY;
                case SCX_ADDR -> this.scrollX;
                case LY_ADDR -> this.lcdY;
                case LYC_ADDR -> this.lcdYCompare;
                case BGP_ADDR -> this.backgroundPalette;
                case OBP0_ADDR -> this.objectPalette0;
                case OBP1_ADDR -> this.objectPalette1;
                case WY_ADDR -> this.windowY;
                case WX_ADDR -> this.windowX;
                default -> throw new IllegalArgumentException("Invalid address \"%04X\" for GameBoy PPU".formatted(address));
            };
        }
    }

    public void cycle() {
        this.cycleDot();
        this.cycleDot();
        this.cycleDot();
        this.cycleDot();
    }

    private void cycleDot() {
        int currentPpuMode = this.getPpuMode();
        int nextPpuMode = switch (currentPpuMode) {
            case OAM_SCAN_MODE -> onOamScan();
            case DRAWING_MODE -> onDrawing();
            case HBLANK_MODE -> onHBlank();
            case VBLANK_MODE -> onVBlank();
            default -> throw new IllegalStateException("GameBoy PPU mode \"%d\" is not a valid value!".formatted(currentPpuMode));
        };
        this.cycles++;

        if (this.cycles % CYCLES_PER_SCANLINE == 0) {
            this.lcdY = (this.lcdY + 1) % SCANLINES_PER_FRAME;
            if (this.windowPixelRendered) {
                this.windowPixelRendered = false;
                this.windowLine = (this.windowLine + 1) % SCANLINES_PER_FRAME;
            }
        }

        if (currentPpuMode != VBLANK_MODE && nextPpuMode == VBLANK_MODE) {
            this.triggerVBlankInterrupt();
        }

        boolean lyEqualsLyc = this.lcdY == this.lcdYCompare;
        if (lyEqualsLyc) {
            this.setLyEqualsLycFlag();
        } else {
            this.clearLyEqualsLycFlag();
        }

        boolean statInterruptLine = false;
        statInterruptLine |= this.getLycInterruptSelect() && lyEqualsLyc;
        statInterruptLine |= this.getMode0InterruptSelect() && nextPpuMode == HBLANK_MODE;
        statInterruptLine |= this.getMode1InterruptSelect() && nextPpuMode == VBLANK_MODE;
        statInterruptLine |= this.getMode2InterruptSelect() && nextPpuMode == OAM_SCAN_MODE;
        if (!this.oldStatInterruptLine && statInterruptLine) {
            this.triggerStatInterrupt();
        }
        this.oldStatInterruptLine = statInterruptLine;

        this.setPpuMode(nextPpuMode);

    }

    private int onOamScan() {
        if (!this.firstOamScanCycle) {
            if (this.lcdY == this.windowY) {
                this.windowYCondition = true;
            }

            this.firstOamScanCycle = true;
        }


        return switch (this.oamScanStep) {
            case 0 -> {
                this.oamScanStep = 1;
                yield OAM_SCAN_MODE;
            }
            case 1 -> {
                Bus bus = this.emulator.getBus();
                int spriteY = bus.readByte(0xFE00 + (this.scannedEntries * 4));
                int spriteX = bus.readByte(0xFE00 + (this.scannedEntries * 4) + 1);
                int tileIndex = bus.readByte(0xFE00 + (this.scannedEntries * 4) + 2);
                int spriteAttributes = bus.readByte(0xFE00 + (this.scannedEntries * 4) + 3);

                for (int i = 0; i < 10; i++) {
                    int entryStartingIndex = i * 4;
                    int testedSpriteY = this.oamEntryBuffer[entryStartingIndex];
                    if (testedSpriteY < 0) {
                        if (((this.lcdY + 16) >= spriteY) && ((this.lcdY + 16) < spriteY + (this.getObjectSize() ? 16 : 8))) {
                            this.oamEntryBuffer[entryStartingIndex] = spriteY;
                            this.oamEntryBuffer[entryStartingIndex + 1] = spriteX;
                            this.oamEntryBuffer[entryStartingIndex + 2] = tileIndex;
                            this.oamEntryBuffer[entryStartingIndex + 3] = spriteAttributes;
                        }
                        break;
                    }
                }

                this.scannedEntries++;
                this.oamScanStep = 0;
                if (this.scannedEntries >= 40) {
                    this.scannedEntries = 0;
                    this.firstOamScanCycle = false;
                    yield DRAWING_MODE;
                } else {
                    yield OAM_SCAN_MODE;
                }
            }
            default -> throw new IllegalStateException("Invalid OAM scan step number \"%d\"".formatted(this.oamScanStep));
        };
    }

    private int onHBlank() {
        if (!this.firstHBlankCycle) {
            this.firstHBlankCycle = true;

            this.pixelX = 0;
            this.discardedPixels = 0;

            this.backgroundFifo.clear();
            Arrays.fill(this.oamEntryBuffer, -1);

            this.bgFifoStep = 0;
            this.bgFifoFetcherX = 0;

            this.bgFifoCurrentTileNumber = 0;
            this.bgFifoTileDataEffectiveAddress = 0;
            this.bgFifoTileDataLow = 0;
            this.bgFifoTileDataHigh = 0;

            this.windowXCondition = false;
        }

        if (this.getScanlineCycle() >= CYCLES_PER_SCANLINE - 1) {
            this.firstHBlankCycle = false;
            return (this.lcdY == 143) ? VBLANK_MODE : OAM_SCAN_MODE;
        } else {
            return HBLANK_MODE;
        }
    }

    private int onVBlank() {
        if (!this.firstVBlankCycle) {
            this.firstVBlankCycle = true;

            this.windowYCondition = false;
            this.windowLine = 0;
        }

        if (this.getScanlineCycle() == CYCLES_PER_SCANLINE - 1 && this.lcdY == SCANLINES_PER_FRAME - 1) {
            this.firstVBlankCycle = false;
            return OAM_SCAN_MODE;
        } else {
            return VBLANK_MODE;
        }
    }

    private int onDrawing() {

        boolean originalWindowCondition = this.isRenderingWindow();

        if (this.pixelX == this.windowX + 1 && this.getWindowEnable()) {
            this.windowXCondition = true;
        }

        if (!originalWindowCondition && this.isRenderingWindow()) {
            this.bgFifoStep = 0;
            this.bgFifoFetcherX = 0;
            this.windowPixelRendered = true;
            this.backgroundFifo.clear();
        }

        this.tickBackgroundFifo();

        if (!this.backgroundFifo.isEmpty()) {

            int discardAmount = this.scrollX % 8;

            int bgPixel = this.backgroundFifo.dequeueInt();
            if (!this.getBackgroundAndWindowEnable()) {
                bgPixel = 0;
            }
            int bgPaletteIndex = (this.backgroundPalette >> (bgPixel * 2)) & 0b11;

            int finalPixel = DMG_PALETTE[bgPaletteIndex];


            if (!this.isRenderingWindow() && this.discardedPixels < discardAmount) {
                this.discardedPixels++;
            } else {
                if (this.pixelX >= 8) {
                    this.lcd[this.pixelX - 8][this.lcdY] = finalPixel;
                }
                this.pixelX++;
            }


        }

        if (this.pixelX >= 168) {
            return HBLANK_MODE;
        } else {
            return DRAWING_MODE;
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
                    int tileY = ((this.lcdY + this.scrollY) & 0xFF) >> 3;
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
                    effectiveAddress = (effectiveAddress + (2 * ((this.lcdY + this.scrollY) % 8))) & 0xFFFF;
                }

                this.bgFifoTileDataEffectiveAddress = effectiveAddress;
                this.bgFifoTileDataLow = getVRamByte(effectiveAddress);

                this.bgFifoStep = 4;
            }
            case 4 -> {
                this.bgFifoStep = 5;
            }
            case 5 -> {
                this.bgFifoTileDataHigh = getVRamByte((this.bgFifoTileDataEffectiveAddress + 1) & 0xFFFF);
                this.bgFifoStep = 6;
            }
            case 6 -> {
                this.bgFifoStep = 7;
            }
            case 7 -> {
                if (!this.backgroundFifo.isEmpty()) {
                    this.bgFifoStep = 7;
                } else {
                    for (int i = 7; i >= 0; i--) {
                        int lo = (this.bgFifoTileDataLow  >> i) & 1;
                        int hi = (this.bgFifoTileDataHigh >> i) & 1;
                        this.backgroundFifo.enqueue((hi << 1) | lo);
                    }
                    this.bgFifoFetcherX++;
                    this.bgFifoStep = 0;
                }
            }
        }
    }


    private void tickSpriteFifo() {
        switch (this.spriteFifoStep) {
            case 0 -> {
                this.spriteFifoStep = 1;
            }
            case 1 -> {
                this.spriteFifoStep = 2;
            }
            case 2 -> {
                this.spriteFifoStep = 3;
            }
            case 3 -> {
                this.spriteFifoStep = 4;
            }
            case 4 -> {
                this.spriteFifoStep = 5;
            }
            case 5 -> {
                this.spriteFifoStep = 6;
            }
            case 6 -> {
                this.spriteFifoStep = 7;
            }
            case 7 -> {
                this.spriteFifoStep = 0;
            }
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

    private void setLyEqualsLycFlag() {
        this.ppuStatus = Processor.setBit(this.ppuStatus, 0b100);
    }

    private void clearLyEqualsLycFlag() {
        this.ppuStatus = Processor.clearBit(this.ppuStatus, 0b100);
    }

    private int getScanlineCycle() {
        return Math.toIntExact(this.cycles % CYCLES_PER_SCANLINE);
    }

    private int getOamByte(int address) {
        if (address >= OAM_START && address <= OAM_END) {
            return this.oam[address - OAM_START];
        } else {
            throw new IllegalArgumentException("Invalid GameBoy OAM address \"%04X\"!".formatted(address));
        }
    }

    private int getVRamByte(int address) {
        if (address >= VRAM_START && address <= VRAM_END) {
            return this.vRam[address - VRAM_START];
        } else {
            throw new IllegalArgumentException("Invalid GameBoy VRAM address \"%04X\"!".formatted(address));
        }
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

    @Override
    public void populateRenderBuffer(int[][] renderBuffer) {
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                renderBuffer[x][y] = this.lcd[x][y];
            }
        }
    }

}
