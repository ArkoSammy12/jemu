package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

import static io.github.arkosammy12.jemu.core.nes.NESCPUMMIOBus.*;

public class NESPPU<E extends NESEmulator> extends VideoGenerator<E> implements Bus {

    public static final int CHR_ROM_START = 0x0000;
    public static final int CHR_ROM_END = 0x1FFF;

    public static final int CIRAM_START = 0x2000;
    public static final int CIRAM_END = 0x2FFF;

    public static final int CIRAM_MIRROR_START = 0x3000;
    public static final int CIRAM_MIRROR_END = 0x3EFF;

    public static final int PALETTE_RAM_START = 0x3F00;
    public static final int PALETTE_RAM_END = 0x3F1F;

    public static final int PALETTE_RAM_MIRROR_START = 0x3F20;
    public static final int PALETTE_RAM_MIRROR_END = 0x3FFF;

    private static final int WIDTH = 256;

    private static final int DOTS_PER_SCANLINE = 341;

    private static final int NTSC_SCANLINES_PER_FRAME = 262;
    private static final int NTSC_VISIBLE_SCANLINES = 240;

    private static final int PAL_SCANLINES_PER_FRAME = 312;
    private static final int PAL_VISIBLE_SCANLINES = 239;

    private final int[][] video;
    private final int scanlinesPerFrame;
    private final int visibleScanlines;
    private final boolean oddFrameDotSkip;

    private final int[] oam = new int[256];
    private final int[] paletteRam = new int[0x20];

    private int ppuControl;
    private int ppuMask;
    private int ppuStatus;

    private int currentVRAMAddress; // v
    private int temporaryVRAMAddress; // t
    private int fineXScroll; // x
    private boolean writeLatch; // w

    private int currentOamAddress;

    private DotHalf currentDotHalf = DotHalf.FIRST;
    private int dotNumber;
    private int scanlineNumber;
    private boolean nmiSignal;

    private boolean oddFrame;
    private int ppuDataReadBuffer;
    private int copyTtoVCountdown;


    public NESPPU(E emulator) {
        super(emulator);
        this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
        this.visibleScanlines = NTSC_VISIBLE_SCANLINES;
        this.oddFrameDotSkip = true;
        this.video = new int[WIDTH][this.visibleScanlines];
    }

    @Override
    public int getImageWidth() {
        return WIDTH;
    }

    @Override
    public int getImageHeight() {
        return this.visibleScanlines;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case PPUCTRL_ADDR, PPUMASK_ADDR, OAMADDR_ADDR, PPUADDR_ADDR, PPUSCROLL_ADDR -> 0xFF;
            case PPUSTATUS_ADDR -> {
                int value = this.ppuStatus;
                this.setVBlankFlag(false);
                this.clearW();
                yield value | 0b11111;
            }
            case OAMDATA_ADDR -> this.oam[this.currentOamAddress];
            case PPUDATA_ADDR -> {
                int readAddress = this.getV() & 0x3FFF;

                int ret = this.ppuDataReadBuffer;

                if (readAddress <= CHR_ROM_END) {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                } else if (readAddress <= CIRAM_END) {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                } else if (readAddress <= CIRAM_MIRROR_END) {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                } else if (readAddress <= PALETTE_RAM_END) {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                    ret = this.paletteRam[readAddress - PALETTE_RAM_START];
                } else {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                    ret = this.paletteRam[readAddress - PALETTE_RAM_MIRROR_START];
                }

                // TODO: If the $2007 access happens to coincide with a standard VRAM address increment (either horizontal or vertical), it will presumably not double-increment the relevant counter.
                if (this.isRenderingEnabled() && (this.isVisibleScanline() || this.isPreRenderScanline())) {
                    this.incrementCoarseXPosition();
                    this.incrementYPosition();
                } else {
                    this.incrementV();
                }
                yield ret;
            }
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
    }

    // TODO: Bus conflicts for VRAM and OAM during rendering

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case PPUCTRL_ADDR -> {
                this.ppuControl = value & 0xFC;
                setT((getT() &  ~0xC00) | ((value & 0b11) << 10));
                this.setNMISignal(this.getVBlankFlag());
            }
            case PPUMASK_ADDR -> this.ppuMask = value & 0xFF;
            case PPUSTATUS_ADDR -> {}
            case OAMADDR_ADDR -> this.currentOamAddress = value & 0xFF;
            case OAMDATA_ADDR -> {
                // TODO: Bus conflicts. Read OAMDATA register section in nesdev for more info.
                this.oam[this.currentOamAddress] = value & 0xFF;
                this.currentOamAddress = (this.currentOamAddress + 1) & 0xFF;
            }
            case PPUSCROLL_ADDR -> {
                if (this.getW()) {
                    this.setT((this.getT() & ~0x73E0) | ((value & 0b00000111) << 12) | ((value & 0b00111000) << 2) | ((value & 0b11000000) << 2));
                    // (wait 1 to 1.5 dots after the write completes as per nesdev)
                    this.copyTtoVCountdown = 3;
                } else {
                    this.setT((this.getT() & ~0xF) | ((value >>> 3) & 0xF));
                    this.setX(value & 0b111);
                }
                this.toggleW();
            }
            case PPUADDR_ADDR -> {
                if (this.getW()) {
                    this.setT((this.getT() & ~0xFF) | (value & 0xFF));
                } else {
                    this.setT((this.getT() & ~0x7F00) | ((value & 0b00111111) << 8));
                }
                this.toggleW();
            }
            case PPUDATA_ADDR -> {
                this.writeBytePPU(this.getV(), value);

                // TODO: If the $2007 access happens to coincide with a standard VRAM address increment (either horizontal or vertical), it will presumably not double-increment the relevant counter.
                if (this.isRenderingEnabled() && (this.isVisibleScanline() || this.isPreRenderScanline())) {
                    this.incrementCoarseXPosition();
                    this.incrementYPosition();
                } else {
                    this.incrementV();
                }
            }
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
    }

    private void setNMISignal(boolean value) {
        this.nmiSignal = value;
    }

    public boolean getNMISignal() {
        return this.nmiSignal && this.getVBlankFlag();
    }

    // TODO: Toggling rendering takes effect approximately 3-4 dots after the write. This delay is required by Battletoads to avoid a crash.
    private boolean isRenderingEnabled() {
        return this.enableBackgroundRendering() || this.enableSpriteRendering();
    }

    private boolean isPreRenderScanline() {
        return this.scanlineNumber == this.scanlinesPerFrame - 1;
    }

    private boolean isVisibleScanline() {
        return this.scanlineNumber < this.visibleScanlines;
    }

    private void incrementV() {
        setV(getV() + switch (this.getVramAddressIncrement()) {
            case VRAMAddressIncrement.ADD_1_ACROSS -> 1;
            case ADD_32_DOWN -> 32;
        });
    }

    // Dot 256 of each scanline if rendering is enabled
    private void incrementYPosition() {
        if ((this.getV() & 0x7000) != 0x7000) {
            this.setV(this.getV() + 0x1000);
        } else {
            this.setV(this.getV() & ~0x7000);
            int y = (this.getV() & 0x03E0) >>> 5;
            if (y == 29) {
                y = 0;
                this.setV(this.getV() ^ 0x0800);
            } else if (y == 31) {
                y = 0;
            } else {
                y++;
            }
            this.setV((this.getV() & ~0x03E0) | ((y & 0x1F) << 5));
        }
    }

    // Between dot 328 of a scanline, and 256 of the next scanline
    private void incrementCoarseXPosition() {
        if ((this.getV() & 0x001F) == 31) {
            this.setV(this.getV() & ~0x001F);
            this.setV(this.getV() ^ 0x0400);
        } else {
            this.setV(getV() + 1);
        }
    }

    // Dot 257 of each scanline if rendering is enabled
    private void copyHorizontalPositionBitsToV() {
        this.setV((this.getV() & ~0x41F) | (this.getT() & 0x41F));
    }

    // During dots 280 to 304 of the pre-render scanline (end of vblank)
    private void copyVerticalPositionBitsToV() {
        this.setV((this.getV() & ~0x7BFF) | (this.getT() & 0x7BFF));
    }

    private void setV(int value) {
        this.currentVRAMAddress = value & 0x7FFF;
    }

    private int getV() {
        return this.currentVRAMAddress;
    }

    private void setT(int value) {
        this.temporaryVRAMAddress = value & 0x7FFF;
    }

    private int getT() {
        return this.temporaryVRAMAddress;
    }

    private void setX(int value) {
        this.fineXScroll = value & 0b111;
    }

    private int getX() {
        return this.fineXScroll;
    }

    private void clearW() {
        this.writeLatch = false;
    }

    private boolean getW() {
        return this.writeLatch;
    }

    private void toggleW() {
        this.writeLatch = !this.writeLatch;
    }

    public void cycleHalfDot() {

        // TODO: skip cycle on odd frame
        if (this.copyTtoVCountdown > 0) {
            this.copyTtoVCountdown--;
            if (this.copyTtoVCountdown <= 0) {
                this.setV(this.getT());
            }
        }

        switch (this.currentDotHalf) {
            case FIRST -> {

            }
            case SECOND -> {

                if (this.scanlineNumber == this.visibleScanlines + 1) {
                    if (this.dotNumber == 1) {
                        this.setVBlankFlag(true);
                    }
                } else if (this.isPreRenderScanline()) {
                    if (this.dotNumber == 1) {
                        this.setVBlankFlag(false);
                    }
                }

                this.dotNumber++;
                if (this.dotNumber >= DOTS_PER_SCANLINE) {
                    this.dotNumber = 0;
                    if (!this.oddFrame && this.oddFrameDotSkip) {
                        this.dotNumber = 1;
                    }
                    this.scanlineNumber++;
                    if (this.scanlineNumber >= this.scanlinesPerFrame) {
                        this.scanlineNumber = 0;
                        this.oddFrame = !this.oddFrame;
                    }
                }
            }
        }
        this.currentDotHalf = this.currentDotHalf.getOpposite();
    }

    /*
    private int getBaseNametableAddress() {
        return switch (this.ppuControl & 0b11) {
            case 0 -> 0x2000;
            case 1 -> 0x2400;
            case 2 -> 0x2800;
            case 3 -> 0x2C00;
            default -> throw new EmulatorException(new IllegalStateException("Invalid NES PPU base nametable address value!"));
        };
    }
     */

    private VRAMAddressIncrement getVramAddressIncrement() {
        return (this.ppuControl & (1 << 2)) != 0 ? VRAMAddressIncrement.ADD_32_DOWN : VRAMAddressIncrement.ADD_1_ACROSS;
    }

    private int get8x8SpritePatternTableAddress() {
        return (this.ppuControl & (1 << 3)) != 0 ? 0x1000 : 0x0000;
    }

    private int getBackgroundPatternTableAddress() {
        return (this.ppuControl & (1 << 4)) != 0 ? 0x1000 : 0x0000;
    }

    private SpriteSize getSpriteSize() {
        return (this.ppuControl & (1 << 5)) != 0 ? SpriteSize.SIZE_8_16 : SpriteSize.SIZE_8_8;
    }

    private boolean getMasterSlaveSelect() {
        return (this.ppuControl & (1 << 6)) != 0;
    }

    private boolean getVBlankNMIEnable() {
        return (this.ppuControl & (1 << 7)) != 0;
    }

    private boolean useGrayscaleColors() {
        return (this.ppuMask & 1) != 0;
    }

    private boolean showBackgroundInLeftmost8Pixels() {
        return (this.ppuMask & (1 << 1)) != 0;
    }

    private boolean showSpritesInLeftmost8Pixels() {
        return (this.ppuMask & (1 << 2)) != 0;
    }

    private boolean enableBackgroundRendering() {
        return (this.ppuMask & (1 << 3)) != 0;
    }

    private boolean enableSpriteRendering() {
        return (this.ppuMask & (1 << 4)) != 0;
    }

    // TODO: Account for PAL difference
    private boolean emphasizeRed() {
        return (this.ppuMask & (1 << 5)) != 0;
    }

    // TODO: Account for PAL difference
    private boolean emphasizeGreen() {
        return (this.ppuMask & (1 << 6)) != 0;
    }

    private boolean emphasizeBlue() {
        return (this.ppuMask & (1 << 7)) != 0;
    }

    private void setSpriteOverflowFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 1 << 5;
        } else {
            this.ppuStatus &= ~(1 << 5);
        }
    }

    private boolean getSpriteOverflowFlag() {
        return (this.ppuStatus & (1 << 5)) != 0;
    }

    private void setSprite0HitFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 1 << 6;
        } else {
            this.ppuStatus &= ~(1 << 6);
        }
    }

    private boolean getSprite0Flag() {
        return (this.ppuStatus & (1 << 6)) != 0;
    }

    private void setVBlankFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 1 << 7;
        } else {
            this.ppuStatus &= ~(1 << 7);
        }
    }

    private boolean getVBlankFlag() {
        return (this.ppuStatus & (1 << 7)) != 0;
    }

    private int readBytePPU(int address) {
        address &= 0x3FFF;
        if (address <= CHR_ROM_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else if (address <= CIRAM_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else if (address <= CIRAM_MIRROR_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else if (address <= PALETTE_RAM_END) {
            this.emulator.getCartridge().readBytePPU(address);
            return this.paletteRam[address - PALETTE_RAM_START];
        } else {
            this.emulator.getCartridge().readBytePPU(address);
            return this.paletteRam[address - PALETTE_RAM_MIRROR_START];
        }
    }

    private void writeBytePPU(int address, int value) {
        address &= 0x3FFF;
        if (address <= CHR_ROM_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else if (address <= CIRAM_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else if (address <= CIRAM_MIRROR_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else if (address <= PALETTE_RAM_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
            this.paletteRam[address - PALETTE_RAM_START] = value & 0xFF;
        } else {
            this.emulator.getCartridge().writeBytePPU(address, value);
            this.paletteRam[address - PALETTE_RAM_MIRROR_START] = value & 0xFF;
        }
    }

    private enum VRAMAddressIncrement {
        ADD_1_ACROSS,
        ADD_32_DOWN
    }

    private enum SpriteSize {
        SIZE_8_8,
        SIZE_8_16
    }

    private enum DotHalf {
        FIRST,
        SECOND;

        private DotHalf getOpposite() {
            return switch (this) {
                case FIRST -> SECOND;
                case SECOND -> FIRST;
            };
        }

    }

}
