package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.gameboy.DMGPPU;

import static io.github.arkosammy12.jemu.core.gameboy.DMGBus.VRAM_END;
import static io.github.arkosammy12.jemu.core.gameboy.DMGBus.VRAM_START;
import static io.github.arkosammy12.jemu.core.gameboycolor.CGBMMMIOBus.*;

public class CGBPPU<E extends GameBoyColorEmulator> extends DMGPPU<E> {

    private VRAMBank vramBank = VRAMBank.BANK_0;

    private final int[] vRamBank1 = new int[0x2000];

    private final int[] bgPaletteRam = new int[0x40];
    private final int[] objPaletteRam = new int[0x40];

    private int backgroundPaletteIndex;
    private int objectPaletteIndex;
    private boolean objectPriorityMode;

    private int bgFifoTileNumberPointer;
    private int bgFifoCurrentTileAttributes;

    public CGBPPU(E emulator) {
        super(emulator);
    }

    protected int getLcdOffColor() {
        return 0xFFFFFFFF;
    }

    public Mode getMode() {
        return this.currentMode;
    }

    @Override
    public int readByte(int address) {
        if (address >= VRAM_START && address <= VRAM_END) {
            if (!Mode.MODE_3_DRAWING.matchesValue(this.getPpuMode()) || !this.getLcdPpuEnable()) {
                return switch (this.vramBank) {
                    case BANK_0 -> this.vRam[address - VRAM_START];
                    case BANK_1 -> this.vRamBank1[address - VRAM_START];
                };
            } else {
                return 0xFF;
            }
        } else {
            return switch (address) {
                case VBK -> switch (vramBank) {
                    case BANK_0 -> 0xFE;
                    case BANK_1 -> 0xFF;
                };
                case BGPI -> this.backgroundPaletteIndex | 0b01000000;
                case BGPD -> this.bgPaletteRam[this.getBgPaletteAddress()];
                case OBPI -> this.objectPaletteIndex | 0b01000000;
                case OBPD -> this.objPaletteRam[this.getObjPaletteAddress()];
                case OPRI -> this.objectPriorityMode ? 0xFF : 0xFE;
                default -> super.readByte(address);
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= VRAM_START && address <= VRAM_END) {
            if (!Mode.MODE_3_DRAWING.matchesValue(this.getPpuMode()) || !this.getLcdPpuEnable()) {
                switch (this.vramBank) {
                    case BANK_0 -> this.vRam[address - VRAM_START] = value & 0xFF;
                    case BANK_1 -> this.vRamBank1[address - VRAM_START] = value & 0xFF;
                }
            }
        } else {
            switch (address) {
                case VBK -> this.vramBank = (value & 1) != 0 ? VRAMBank.BANK_1 : VRAMBank.BANK_0;
                case BGPI -> this.backgroundPaletteIndex = value & 0xFF;
                case BGPD -> {
                    //if (!Mode.MODE_3_DRAWING.matchesValue(this.getPpuMode()) || !this.getLcdPpuEnable()) {
                        this.bgPaletteRam[this.getBgPaletteAddress()] = value & 0xFF;
                    //}
                    if (this.getBgPaletteAddressAutoIncrement()) {
                        this.incrementBgPaletteAddress();
                    }
                }
                case OBPI -> this.objectPaletteIndex = value & 0xFF;
                case OBPD -> {
                    //if (!Mode.MODE_3_DRAWING.matchesValue(this.getPpuMode()) || !this.getLcdPpuEnable()) {
                        this.objPaletteRam[this.getObjPaletteAddress()] = value & 0xFF;
                    //}
                    if (this.getObjPaletteAddressAutoIncrement()) {
                        this.incrementObjPaletteAddress();
                    }
                }
                case OPRI -> this.objectPriorityMode = (value & 1) != 0;
                default -> super.writeByte(address, value);
            }
        }
    }

    @Override
    protected void onHBlankStart() {
        super.onHBlankStart();
        this.bgFifoTileNumberPointer = 0;
        this.bgFifoCurrentTileAttributes = 0;
    }

    @Override
    protected void tickBackgroundFifo() {
        if (this.emulator.isDmgCompatibilityMode()) {
            super.tickBackgroundFifo();
            return;
        }
        switch (this.bgFifoStep) {
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
                    this.bgFifoTileNumberPointer = address;
                    this.bgFifoCurrentTileNumber = this.getVRamByte(address);
                } else {
                    int tileMapBase = this.getBackgroundTileMap() ? 0x9C00 : 0x9800;
                    int tileX = ((this.pixelX + this.scrollX) >> 3) & 0x1F;
                    int tileY = ((this.scanlineNumber + this.scrollY) & 0xFF) >>> 3;
                    int tileMapIndex = tileX + (tileY * 32);
                    tileMapIndex &= 0x3FF;
                    int address = tileMapBase + tileMapIndex;
                    this.bgFifoTileNumberPointer = address;
                    this.bgFifoCurrentTileNumber = this.getVRamByte(address);
                }
                this.bgFifoStep = 2;
            }
            case 2 -> {
                this.bgFifoCurrentTileAttributes = this.getVRamByte(this.bgFifoTileNumberPointer, VRAMBank.BANK_1);
                this.bgFifoStep = 3;
            }
            case 3 -> {
                if (this.bgFifoFirstFetch) {
                    this.bgFifoFirstFetch = false;
                    for (int i = 0; i < 8; i++) {
                        this.backgroundFifo.enqueue(0);
                    }
                    this.bgFifoStep = 0;
                } else {
                    int tileAddress;
                    if (this.getBackgroundAndWindowTiles()) {
                        tileAddress = 0x8000 + (this.bgFifoCurrentTileNumber * 16);
                    } else {
                        byte signedTile = (byte) this.bgFifoCurrentTileNumber;
                        tileAddress = 0x9000 + signedTile * 16;
                    }

                    int row;
                    if (this.isRenderingWindow()) {
                        row = this.windowLine & 7;
                    } else {
                        row = (this.scanlineNumber + this.scrollY) & 7;
                    }

                    if (getCgbYFlipFromBgAttributes(this.bgFifoCurrentTileAttributes)) {
                        row = 7 - row;
                    }

                    int effectiveAddress = tileAddress + row * 2;
                    this.bgFifoTileDataEffectiveAddress = effectiveAddress;
                    this.bgFifoTileDataLow = this.getVRamByte(effectiveAddress, getCgbVRamBankFromBgAttributes(this.bgFifoCurrentTileAttributes));
                    this.bgFifoStep = 4;
                }
            }
            case 4 -> {
                this.bgFifoStep = 5;
            }
            case 5 -> {
                this.bgFifoTileDataHigh = this.getVRamByte(this.bgFifoTileDataEffectiveAddress + 1, getCgbVRamBankFromBgAttributes(this.bgFifoCurrentTileAttributes));
                if (this.backgroundFifo.isEmpty()) {
                    this.pushBgPixels();
                    this.bgFifoStep = 0;
                } else {
                    this.bgFifoStep = 6;
                }
            }
            case 6 -> {
                if (this.backgroundFifo.isEmpty()) {
                    this.pushBgPixels();
                    this.bgFifoStep = 0;
                } else {
                    this.bgFifoStep = 6;
                }
            }
        }
    }

    @Override
    protected void pushBgPixels() {
         if (this.emulator.isDmgCompatibilityMode()) {
             super.pushBgPixels();
             return;
         }
        for (int i = 0; i < 8; i++) {
            int bit = getCgbXFlipFromBgAttributes(this.bgFifoCurrentTileAttributes) ? i : (7 - i);
            int low = (this.bgFifoTileDataLow >> bit) & 1;
            int high = (this.bgFifoTileDataHigh >> bit) & 1;
            int color = (high << 1) | low;
            int pixelEntry = createCgbBgPixelEntry(color, getCgbPriorityFromBgAttributes(this.bgFifoCurrentTileAttributes), getCgbPaletteFromBgAttributes(this.bgFifoCurrentTileAttributes));
            this.backgroundFifo.enqueue(pixelEntry);
        }
        this.bgFifoFetcherX++;
    }

    protected boolean isFetchingSprites(int currentSpriteEntryIndex) {
        return currentSpriteEntryIndex >= 0;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void tickSpriteFifo() {
        if (this.emulator.isDmgCompatibilityMode()) {
            super.tickSpriteFifo();
            return;
        }
        switch (this.spriteFifoStep) {
            case 0 -> {
                this.spriteFifoStep = 1;
            }
            case 1 -> {
                this.spriteFifoCurrentTileNumber = getTileIndexFromSpriteEntry(this.spriteBuffer[this.spriteFifoCurrentEntryIndex]);
                this.spriteFifoStep = 2;
            }
            case 2 -> {
                this.spriteFifoStep = 3;
            }
            case 3 -> {
                boolean objSize = this.getObjectSize();
                int spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
                int spriteAttributes = getSpriteAttributesFromEntry(spriteEntry);
                boolean yFlip = getYFlipFromObjAttributes(spriteAttributes);
                int spriteY = getSpriteYFromSpriteEntry(spriteEntry);
                int tileIndex = this.spriteFifoCurrentTileNumber;

                int width = objSize ? 15 : 7;
                if (objSize) {
                    tileIndex &= ~1;
                }

                int row = ((this.scanlineNumber + 16) - spriteY) % (width + 1);
                if (row < 0) {
                    row += (width + 1);
                }

                int offset = yFlip ? (width - row) * 2 : row * 2;
                this.spriteFifoTileDataEffectiveAddress = (0x8000 + tileIndex * 16 + offset) & 0xFFFF;

                this.spriteFifoTileDataLow = this.getVRamByte(this.spriteFifoTileDataEffectiveAddress, getCgbVRamBankFromObjAttributes(spriteAttributes));
                this.spriteFifoStep = 4;
            }
            case 4 -> {
                this.spriteFifoStep = 5;
            }
            case 5 -> {
                int spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
                int spriteX = getSpriteXFromSpriteEntry(spriteEntry);
                int spriteAttributes = getSpriteAttributesFromEntry(spriteEntry);
                boolean xFlip = getXFlipFromObjAttributes(spriteAttributes);
                boolean priority = getPriorityFromObjAttributes(spriteAttributes);
                int palette = getCgbPaletteFromObjAttributes(spriteAttributes);

                this.spriteFifoTileDataHigh = this.getVRamByte((this.spriteFifoTileDataEffectiveAddress + 1) & 0xFFFF, getCgbVRamBankFromObjAttributes(spriteAttributes));

                for (int i = 0; i < 8; i++) {
                    if (spriteX + i < 8) {
                        continue;
                    }
                    int bit = xFlip ? 1 << i : 1 << (7 - i);
                    int low = (this.spriteFifoTileDataLow & bit) != 0 ? 1 : 0;
                    int high = (this.spriteFifoTileDataHigh & bit) != 0 ? 1 : 0;
                    int colorNumber = (low | (high << 1));
                    if (colorNumber == 0) {
                        continue;
                    }
                    Integer currentQueuedPixel = this.spriteFifo.get(i);
                    if (currentQueuedPixel == null || getCgbColorNumberFromObjPixelEntry(currentQueuedPixel) == 0 || (!this.objectPriorityMode && this.spriteFifoCurrentEntryIndex < getCgbOamIndexForObjPixelEntry(currentQueuedPixel))) {
                        this.spriteFifo.set(i, createCgbObjPixelEntry(colorNumber, priority, palette, this.spriteFifoCurrentEntryIndex));
                    }
                }

                this.spriteBuffer[this.spriteFifoCurrentEntryIndex] = null;
                this.spriteFifoCurrentEntryIndex = -1;
                this.spriteFifoStep = 0;

                this.spriteCount++;
            }
        }
    }

    @Override
    protected void tickPixelShifter() {
        if (this.backgroundFifo.isEmpty()) {
            return;
        }

        int bgPixel = this.backgroundFifo.dequeueInt();
        Integer objPixel = this.spriteFifo.poll();
        this.spriteFifo.offer(null);

        Integer finalPixel;
        if (this.emulator.isDmgCompatibilityMode()) {
            if (!this.getBackgroundAndWindowEnable()) {
                bgPixel = 0;
            }

            int bgPaletteIndex = (this.backgroundPalette >>> ((bgPixel & 0b11) * 2)) & 0b11;
            finalPixel = this.getARGBForBgPixelEntry(bgPaletteIndex, 0);

            int bgDiscardTarget = this.scrollX % 8;
            if (!this.isRenderingWindow() && this.discardedPixels < bgDiscardTarget) {
                this.discardedPixels++;
                finalPixel = null;
            }

            if (objPixel != null) {
                int objColorNumber = getDmgColorNumberFromObjPixelEntry(objPixel);
                if (!this.getObjectEnable()) {
                    objColorNumber = 0;
                }
                boolean objPriority = getDmgPriorityForObjPixelEntry(objPixel);
                boolean objPalette = getDmgPaletteForObjPixelEntry(objPixel);
                if (objColorNumber != 0 && !(objPriority && bgPixel != 0)) {
                    int objPaletteReg = objPalette ? this.objectPalette1 : this.objectPalette0;
                    int objPaletteIndex = (objPaletteReg >>> (objColorNumber * 2)) & 0b11;
                    finalPixel = this.getARGBForObjPixelEntry(objPaletteIndex, objPalette ? 1 : 0);
                }
            }
        } else {
            int bgColor = getCgbColorNumberFromBgPixelEntry(bgPixel);
            boolean bgPriority = getCgbPriorityFromBgPixelEntry(bgPixel);
            int bgPalette = getCgbPaletteFromBgPixelEntry(bgPixel);

            if (!this.getBackgroundAndWindowEnable()) {
                bgColor = 0;
            }

            finalPixel = this.getARGBForBgPixelEntry(getCgbColorNumberFromBgPixelEntry(bgPixel), bgPalette);

            int bgDiscardTarget = this.scrollX % 8;
            if (!this.isRenderingWindow() && this.discardedPixels < bgDiscardTarget) {
                this.discardedPixels++;
                finalPixel = null;
            }

            if (objPixel != null) {
                boolean objPriority = getDmgPriorityForObjPixelEntry(objPixel);
                int objColor = getCgbColorNumberFromObjPixelEntry(objPixel);
                if (!this.getObjectEnable()) {
                    objColor = 0;
                }
                int objPalette = getCgbPaletteFromObjPixelEntry(objPixel);
                if (objColor != 0 && (!this.getBackgroundAndWindowEnable() || bgColor == 0 || (!bgPriority && !objPriority))) {
                    finalPixel = this.getARGBForObjPixelEntry(objColor, objPalette);
                }
            }
        }

        if (finalPixel != null) {
            if (this.pixelX >= 8 && this.enablePixelWrites) {
                this.lcd[this.pixelX - 8][this.scanlineNumber] = finalPixel;
            }
            this.pixelX++;
        }

    }

    private int getARGBForBgPixelEntry(int colorNumber, int palette) {
        int colorRamIndex = (palette * 8) + (colorNumber * 2);

        int r5 = this.bgPaletteRam[colorRamIndex] & 0b11111;
        int g5 = ((this.bgPaletteRam[colorRamIndex + 1] & 0b11) << 3) | ((this.bgPaletteRam[colorRamIndex] & 0b11100000) >>> 5);
        int b5 = (this.bgPaletteRam[colorRamIndex + 1] & 0b01111100) >>> 2;

        return toARGB(r5, g5, b5);
    }

    private int getARGBForObjPixelEntry(int colorNumber, int palette) {
        int colorRamIndex = (palette * 8) + (colorNumber * 2);

        int r5 = this.objPaletteRam[colorRamIndex] & 0b11111;
        int g5 = ((this.objPaletteRam[colorRamIndex + 1] & 0b11) << 3) | ((this.objPaletteRam[colorRamIndex] & 0b11100000) >>> 5);
        int b5 = (this.objPaletteRam[colorRamIndex + 1] & 0b01111100) >>> 2;

        return toARGB(r5, g5, b5);
    }

    private static int toARGB(int r5, int g5, int b5) {
        int r8 = ((r5 << 3) | (r5 >>> 2)) & 0xFF;
        int g8 = ((g5 << 3) | (g5 >>> 2)) & 0xFF;
        int b8 = ((b5 << 3) | (b5 >>> 2)) & 0xFF;
        return (0xFF << 24) | (r8 << 16) | (g8 << 8) | b8;
    }

    private boolean getBgPaletteAddressAutoIncrement() {
        return (this.backgroundPaletteIndex & 0b10000000) != 0;
    }

    private boolean getObjPaletteAddressAutoIncrement() {
        return (this.objectPaletteIndex & 0b10000000) != 0;
    }

    private int getBgPaletteAddress() {
        return this.backgroundPaletteIndex & 0b111111;
    }

    private int getObjPaletteAddress() {
        return this.objectPaletteIndex & 0b111111;
    }

    private void incrementBgPaletteAddress() {
        int newAddress = (this.getBgPaletteAddress() + 1) & 0x3F;
        this.backgroundPaletteIndex = (this.backgroundPaletteIndex & 0b11000000) | newAddress;
    }

    private void incrementObjPaletteAddress() {
        int newAddress = (this.getObjPaletteAddress() + 1) & 0x3F;
        this.objectPaletteIndex = (this.objectPaletteIndex & 0b11000000) | newAddress;
    }

    private static int createCgbBgPixelEntry(int colorNumber, boolean priority, int palette) {
        return ((palette & 0b111) << 16) | ((priority ? 1 : 0) << 8) | colorNumber;
    }

    private static int getCgbPaletteFromBgPixelEntry(int pixel) {
        return (pixel >>> 16) & 0b111;
    }

    private static boolean getCgbPriorityFromBgPixelEntry(int pixel) {
        return (pixel >>> 8 & 1) != 0;
    }

    private static int getCgbColorNumberFromBgPixelEntry(int pixel) {
        return pixel & 0b11;
    }

    private static boolean getCgbPriorityFromBgAttributes(int bgAttributes) {
        return (bgAttributes & 0b10000000) != 0;
    }

    private static boolean getCgbYFlipFromBgAttributes(int bgAttributes) {
        return (bgAttributes & 0b01000000) != 0;
    }

    private static boolean getCgbXFlipFromBgAttributes(int bgAttributes) {
        return (bgAttributes & 0b00100000) != 0;
    }

    private static VRAMBank getCgbVRamBankFromBgAttributes(int bgAttributes) {
        return (bgAttributes & 0b1000) != 0 ? VRAMBank.BANK_1 : VRAMBank.BANK_0;
    }

    private static int getCgbPaletteFromBgAttributes(int bgAttributes) {
        return bgAttributes & 0b111;
    }

    private static int createCgbObjPixelEntry(int colorNumber, boolean priority, int palette, int oamIndex) {
        return (oamIndex << 24) | ((palette & 0b111) << 16) | ((priority ? 1 : 0) << 8) | colorNumber;
    }

    private static VRAMBank getCgbVRamBankFromObjAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b1000) != 0 ? VRAMBank.BANK_1 : VRAMBank.BANK_0;
    }

    private static int getCgbPaletteFromObjAttributes(int spriteAttributes) {
        return spriteAttributes & 0b111;
    }

    private static int getCgbOamIndexForObjPixelEntry(int pixel) {
        return pixel >>> 24;
    }

    private static int getCgbColorNumberFromObjPixelEntry(int pixel) {
        return pixel & 0b11;
    }

    private static int getCgbPaletteFromObjPixelEntry(int pixel) {
        return (pixel >>> 16) & 0b111;
    }

    private int getVRamByte(int address, VRAMBank bank) {
        if (address >= VRAM_START && address <= VRAM_END) {
            return switch (bank) {
                case BANK_0 -> super.getVRamByte(address);
                case BANK_1 -> this.vRamBank1[address - VRAM_START];
            };
        } else {
            throw new EmulatorException("Invalid GameBoy VRAM address \"%04X\"!".formatted(address));
        }
    }

    private enum VRAMBank {
        BANK_0,
        BANK_1;
    }

}
