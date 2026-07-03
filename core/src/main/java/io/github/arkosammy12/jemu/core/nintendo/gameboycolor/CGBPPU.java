package io.github.arkosammy12.jemu.core.nintendo.gameboycolor;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGPPU;

import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGBus.VRAM_END;
import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGBus.VRAM_START;

public class CGBPPU<E extends GameBoyColorEmulator> extends DMGPPU<E> {

    public static final int VBK_ADDR = 0xFF4F;
    public static final int BGPI_ADDR = 0xFF68;
    public static final int BGPD_ADDR = 0xFF69;
    public static final int OBPI_ADDR = 0xFF6A;
    public static final int OBPD_ADDR = 0xFF6B;
    public static final int OPRI_ADDR = 0xFF6C;

    private VRAMBank vramBank = VRAMBank.BANK_0;

    private final byte[] vramBank1 = new byte[0x2000];

    private final byte[] bgPaletteRAM = new byte[0x40];
    private final byte[] objPaletteRAM = new byte[0x40];

    private int backgroundPaletteIndex;
    private boolean bgPaletteAddressAutoIncrement;

    private int objectPaletteIndex;
    private boolean objPaletteAddressAutoIncrement;

    private boolean objectPriorityMode;

    private int bgFifoTileNumberPointer;
    private int bgFifoCurrentTileAttributes;

    public CGBPPU(E emulator) {
        super(emulator);
    }

    protected int getLCDOffColor() {
        return 0xFFFFFF;
    }

    public Mode getMode() {
        return this.currentMode;
    }

    @Override
    public int readByte(int address) {
        if (address >= VRAM_START && address <= VRAM_END) {
            if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                return switch (this.vramBank) {
                    case BANK_0 -> (int) this.vram[address - VRAM_START] & 0xFF;
                    case BANK_1 -> (int) this.vramBank1[address - VRAM_START] & 0xFF;
                };
            } else {
                return 0xFF;
            }
        } else {
            return switch (address) {
                case VBK_ADDR -> switch (vramBank) {
                    case BANK_0 -> 0xFE;
                    case BANK_1 -> 0xFF;
                };
                case BGPI_ADDR -> this.backgroundPaletteIndex | 0b01000000;
                case BGPD_ADDR -> {
                    if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                        yield (int) this.bgPaletteRAM[this.getBgPaletteAddress()] & 0xFF;
                    } else {
                        yield 0xFF;
                    }
                }
                case OBPI_ADDR -> this.objectPaletteIndex | 0b01000000;
                case OBPD_ADDR -> {
                    if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                        yield (int) this.objPaletteRAM[this.getObjPaletteAddress()] & 0xFF;
                    } else {
                        yield 0xFF;
                    }
                }
                case OPRI_ADDR -> this.objectPriorityMode ? 0xFF : 0xFE;
                default -> super.readByte(address);
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= VRAM_START && address <= VRAM_END) {
            if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                switch (this.vramBank) {
                    case BANK_0 -> this.vram[address - VRAM_START] = (byte) value;
                    case BANK_1 -> this.vramBank1[address - VRAM_START] = (byte) value;
                }
            }
        } else {
            switch (address) {
                case VBK_ADDR -> this.vramBank = (value & 1) != 0 ? VRAMBank.BANK_1 : VRAMBank.BANK_0;
                case BGPI_ADDR -> {
                    this.backgroundPaletteIndex = value & 0xFF;
                    this.bgPaletteAddressAutoIncrement = (value & 0b10000000) != 0;
                }
                case BGPD_ADDR -> {
                    if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                        this.bgPaletteRAM[this.getBgPaletteAddress()] = (byte) value;
                    }
                    if (this.getBgPaletteAddressAutoIncrement()) {
                        this.incrementBgPaletteAddress();
                    }
                }
                case OBPI_ADDR -> {
                    this.objectPaletteIndex = value & 0xFF;
                    this.objPaletteAddressAutoIncrement = (value & 0b10000000) != 0;
                }
                case OBPD_ADDR -> {
                    if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                        this.objPaletteRAM[this.getObjPaletteAddress()] = (byte) value;
                    }
                    if (this.getObjPaletteAddressAutoIncrement()) {
                        this.incrementObjPaletteAddress();
                    }
                }
                case OPRI_ADDR -> this.objectPriorityMode = (value & 1) != 0;
                default -> super.writeByte(address, value);
            }
        }
    }

    @Override
    protected void checkSTATWriteBug() {
        // No STAT write bug on CGB
    }

    @Override
    public void checkArmOAMBugRead(int address) {
        // No OAM bug on CGB
    }

    @Override
    public void checkArmOAMBugWrite(int address) {
        // No OAM bug on CGB
    }

    @Override
    protected void checkOAMBugTrigger(int tCycle) {
        // No OAM bug on CGB
    }

    @Override
    protected void doOAMBugRead() {
        // No OAM bug on CGB
    }

    @Override
    protected void doOAMBugWrite() {
        // No OAM bug on CGB
    }

    @Override
    protected void onHBlankStart() {
        super.onHBlankStart();
        this.bgFifoTileNumberPointer = 0;
        this.bgFifoCurrentTileAttributes = 0;
    }

    @Override
    protected void tickBackgroundFifo() {
        if (this.emulator.isDMGCompatibilityMode()) {
            super.tickBackgroundFifo();
            return;
        }
        switch (this.bgFifoStep) {
            case 0 -> {
                this.bgFifoStep = 1;
            }
            case 1 -> {
                if (this.isRenderingWindow()) {
                    int tileX = this.bgFifoFetcherX & 0x1F;
                    int tileY = this.windowLine >>> 3;
                    int tileMapIndex = tileX + (tileY * 32);
                    tileMapIndex &= 0x3FF;
                    int address = this.getWindowTileMap() + tileMapIndex;
                    this.bgFifoTileNumberPointer = address;
                    this.bgFifoCurrentTileNumber = this.getVRAMByte(address);
                } else {
                    int tileX = ((this.pixelX + this.scrollX) >> 3) & 0x1F;
                    int tileY = ((this.scanlineNumber + this.scrollY) & 0xFF) >>> 3;
                    int tileMapIndex = tileX + (tileY * 32);
                    tileMapIndex &= 0x3FF;
                    int address = this.getBackgroundTileMap() + tileMapIndex;
                    this.bgFifoTileNumberPointer = address;
                    this.bgFifoCurrentTileNumber = this.getVRAMByte(address);
                }
                this.bgFifoStep = 2;
            }
            case 2 -> {
                this.bgFifoCurrentTileAttributes = this.getVRAMByte(this.bgFifoTileNumberPointer, VRAMBank.BANK_1);
                this.bgFifoStep = 3;
            }
            case 3 -> {
                if (this.bgFifoFirstFetch) {
                    this.bgFifoFirstFetch = false;
                    for (int i = 0; i < 8; i++) {
                        this.backgroundFifo.set(0, 0);
                    }
                    this.backgroundFifo.setFull();
                    this.bgFifoStep = 0;
                } else {
                    int tileAddress;
                    if (this.getBackgroundAndWindowTiles()) {
                        tileAddress = 0x8000 + (this.bgFifoCurrentTileNumber * 16);
                    } else {
                        byte signedTile = (byte) this.bgFifoCurrentTileNumber;
                        tileAddress = 0x9000 + (int) signedTile * 16;
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
                    this.bgFifoTileDataLow = this.getVRAMByte(effectiveAddress, getCgbVRamBankFromBgAttributes(this.bgFifoCurrentTileAttributes));
                    this.bgFifoStep = 4;
                }
            }
            case 4 -> {
                this.bgFifoStep = 5;
            }
            case 5 -> {
                this.bgFifoTileDataHigh = this.getVRAMByte(this.bgFifoTileDataEffectiveAddress + 1, getCgbVRamBankFromBgAttributes(this.bgFifoCurrentTileAttributes));
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
                }
            }
        }
    }

    @Override
    protected void pushBgPixels() {
         if (this.emulator.isDMGCompatibilityMode()) {
             super.pushBgPixels();
             return;
         }
        for (int i = 0; i < 8; i++) {
            int bit = getCgbXFlipFromBgAttributes(this.bgFifoCurrentTileAttributes) ? i : (7 - i);
            int low = (this.bgFifoTileDataLow >> bit) & 1;
            int high = (this.bgFifoTileDataHigh >> bit) & 1;
            this.backgroundFifo.set(i, createCgbBgPixelEntry((high << 1) | low, getCgbPriorityFromBgAttributes(this.bgFifoCurrentTileAttributes), getCgbPaletteFromBgAttributes(this.bgFifoCurrentTileAttributes)));
        }
        this.backgroundFifo.setFull();
        this.bgFifoFetcherX++;
    }

    @Override
    protected boolean isFetchingSprites(int currentSpriteEntryIndex) {
        // The CGB's PPU still fetches regardless of the obj enable bit in LCDC
        return currentSpriteEntryIndex >= 0;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void tickSpriteFifo() {
        if (this.emulator.isDMGCompatibilityMode()) {
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
                long spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
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

                this.spriteFifoTileDataLow = this.getVRAMByte(this.spriteFifoTileDataEffectiveAddress, getCgbVRamBankFromObjAttributes(spriteAttributes));
                this.spriteFifoStep = 4;
            }
            case 4 -> {
                this.spriteFifoStep = 5;
            }
            case 5 -> {
                long spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
                int spriteX = getSpriteXFromSpriteEntry(spriteEntry);
                int spriteAttributes = getSpriteAttributesFromEntry(spriteEntry);
                boolean xFlip = getXFlipFromObjAttributes(spriteAttributes);
                boolean priority = getPriorityFromObjAttributes(spriteAttributes);
                int palette = getCgbPaletteFromObjAttributes(spriteAttributes);

                this.spriteFifoTileDataHigh = this.getVRAMByte((this.spriteFifoTileDataEffectiveAddress + 1) & 0xFFFF, getCgbVRamBankFromObjAttributes(spriteAttributes));

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
                    int currentQueuedPixel = this.spriteFifo.get(i);
                    if (getCgbColorNumberFromObjPixelEntry(currentQueuedPixel) == 0 || !this.objectPriorityMode && this.spriteFifoCurrentEntryIndex < getCgbOamIndexForObjPixelEntry(currentQueuedPixel)) {
                        this.spriteFifo.set(i, createCgbObjPixelEntry(colorNumber, priority, palette, this.spriteFifoCurrentEntryIndex));
                    }
                }

                this.spriteBuffer[this.spriteFifoCurrentEntryIndex] = -1;
                this.spriteFifoCurrentEntryIndex = -1;
                this.spriteFifoStep = 0;

            }
        }
    }

    @Override
    protected void tickPixelShifter() {
        if (this.backgroundFifo.isEmpty()) {
            return;
        }

        int bgPixel = this.backgroundFifo.shiftHead(0);
        int objPixel = this.spriteFifo.shiftHead(0);

        int finalPixel;
        if (this.emulator.isDMGCompatibilityMode()) {
            if (!this.getBackgroundAndWindowEnable()) {
                bgPixel = 0;
            }

            int bgPaletteIndex = (this.backgroundPalette >>> ((bgPixel & 0b11) * 2)) & 0b11;
            finalPixel = this.getRGBForBgPixelEntry(bgPaletteIndex, 0);

            int bgDiscardTarget = this.scrollX % 8;
            if (!this.isRenderingWindow() && this.discardedPixels < bgDiscardTarget) {
                this.discardedPixels++;
                finalPixel = -1;
            }

            int objColorNumber = getDmgColorNumberFromObjPixelEntry(objPixel);
            if (!this.getObjectEnable()) {
                objColorNumber = 0;
            }
            if (objColorNumber != 0 && !(getDmgPriorityForObjPixelEntry(objPixel) && bgPixel != 0)) {
                boolean objPalette = getDmgPaletteForObjPixelEntry(objPixel);
                int objPaletteReg = objPalette ? this.objectPalette1 : this.objectPalette0;
                int objPaletteIndex = (objPaletteReg >>> (objColorNumber * 2)) & 0b11;
                finalPixel = this.getRGBForObjPixelEntry(objPaletteIndex, objPalette ? 1 : 0);
            }
        } else {
            int bgColor = getCgbColorNumberFromBgPixelEntry(bgPixel);
            boolean bgPriority = getCgbPriorityFromBgPixelEntry(bgPixel);
            int bgPalette = getCgbPaletteFromBgPixelEntry(bgPixel);

            if (!this.getBackgroundAndWindowEnable()) {
                bgColor = 0;
            }
            finalPixel = this.getRGBForBgPixelEntry(getCgbColorNumberFromBgPixelEntry(bgPixel), bgPalette);

            int bgDiscardTarget = this.scrollX % 8;
            if (!this.isRenderingWindow() && this.discardedPixels < bgDiscardTarget) {
                this.discardedPixels++;
                finalPixel = -1;
            }

            int objColor = getCgbColorNumberFromObjPixelEntry(objPixel);
            if (!this.getObjectEnable()) {
                objColor = 0;
            }
            if (objColor != 0 && (!this.getBackgroundAndWindowEnable() || bgColor == 0 || (!bgPriority && !getDmgPriorityForObjPixelEntry(objPixel)))) {
                finalPixel = this.getRGBForObjPixelEntry(objColor, getCgbPaletteFromObjPixelEntry(objPixel));
            }
        }

        // TODO: Emulate color shown in the LCD during CPU STOP mode depending on which mode the STOP mode lands on. Same for DMG
        if (finalPixel >= 0) {
            if (this.pixelX >= 8 && this.enablePixelWrites) {
                this.lcd[(this.scanlineNumber * WIDTH) + (this.pixelX - 8)] = finalPixel;
            }
            this.pixelX++;
        }
    }

    private int getRGBForBgPixelEntry(int colorNumber, int palette) {
        int colorRamIndex = (palette * 8) + (colorNumber * 2);

        int r5 = ((int) this.bgPaletteRAM[colorRamIndex] & 0xFF) & 0b11111;
        int g5 = ((((int) this.bgPaletteRAM[colorRamIndex + 1] & 0xFF) & 0b11) << 3) | ((((int) this.bgPaletteRAM[colorRamIndex] & 0xFF) & 0b11100000) >>> 5);
        int b5 = (((int) this.bgPaletteRAM[colorRamIndex + 1] & 0xFF) & 0b01111100) >>> 2;

        return toRGB(r5, g5, b5);
    }

    private int getRGBForObjPixelEntry(int colorNumber, int palette) {
        int colorRamIndex = (palette * 8) + (colorNumber * 2);

        int r5 = ((int) this.objPaletteRAM[colorRamIndex] & 0xFF) & 0b11111;
        int g5 = ((((int) this.objPaletteRAM[colorRamIndex + 1] & 0xFF) & 0b11) << 3) | ((((int) this.objPaletteRAM[colorRamIndex] & 0xFF) & 0b11100000) >>> 5);
        int b5 = (((int) this.objPaletteRAM[colorRamIndex + 1] & 0xFF) & 0b01111100) >>> 2;

        return toRGB(r5, g5, b5);
    }

    private static int toRGB(int r5, int g5, int b5) {
        int r8 = ((r5 << 3) | (r5 >>> 2)) & 0xFF;
        int g8 = ((g5 << 3) | (g5 >>> 2)) & 0xFF;
        int b8 = ((b5 << 3) | (b5 >>> 2)) & 0xFF;
        return (r8 << 16) | (g8 << 8) | b8;
    }

    private boolean getBgPaletteAddressAutoIncrement() {
        return this.bgPaletteAddressAutoIncrement;
    }

    private boolean getObjPaletteAddressAutoIncrement() {
        return this.objPaletteAddressAutoIncrement;
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

    private int getVRAMByte(int address, VRAMBank bank) {
        if (address >= VRAM_START && address <= VRAM_END) {
            return switch (bank) {
                case BANK_0 -> super.getVRAMByte(address);
                case BANK_1 -> (int) this.vramBank1[address - VRAM_START] & 0xFF;
            };
        } else {
            throw new EmulatorException("Invalid GameBoy VRAM address \"%04X\"!".formatted(address));
        }
    }

    private enum VRAMBank {
        BANK_0,
        BANK_1
    }

}
