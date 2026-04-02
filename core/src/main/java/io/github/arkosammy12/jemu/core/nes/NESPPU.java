package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

import static io.github.arkosammy12.jemu.core.nes.NESCPUMMIOBus.*;

public class NESPPU<E extends NESEmulator> extends VideoGenerator<E> implements Bus {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 240;

    private final int[] oam = new int[256];

    private int ppuControl;
    private int ppuMask;
    private int ppuStatus;

    private int currentVRAMAddress; // v
    private int temporaryVRAMAddress; // t
    private int fineXScroll; // x
    private boolean writeLatch; // w

    private int scrollXLSB;
    private int scrollYLSB;
    private int currentOamAddress;

    public NESPPU(E emulator) {
        super(emulator);
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
        return switch (address) {
            case PPUCTRL_ADDR, PPUMASK_ADDR, OAMADDR_ADDR, PPUADDR_ADDR, PPUSCROLL_ADDR -> 0xFF;
            case PPUSTATUS_ADDR -> {
                int value = this.ppuMask;
                this.setVblankFlag(false);
                this.setW(false);
                yield value;
            }
            case OAMDATA_ADDR -> this.oam[this.currentOamAddress];
            case PPUDATA_ADDR -> 0xFF;
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case PPUCTRL_ADDR -> this.ppuControl = value & 0xFF;
            case PPUMASK_ADDR -> this.ppuMask = value & 0xFF;
            case PPUSTATUS_ADDR -> {}
            case OAMADDR_ADDR -> this.currentOamAddress = value & 0xFF;
            case OAMDATA_ADDR -> {
                // TODO: Bus conflicts. Read OAMDATA register section in nesdev for more info.
                this.oam[this.currentOamAddress] = value & 0xFF;
                this.currentOamAddress = (this.currentOamAddress + 1) & 0xFF;
            }
            case PPUSCROLL_ADDR -> {
                this.toggleW();
            }
            case PPUADDR_ADDR -> {
                this.toggleW();
            }
            case PPUDATA_ADDR -> {}
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
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

    private void setW(boolean value) {
        this.writeLatch = value;
    }

    private boolean getW() {
        return this.writeLatch;
    }

    private void toggleW() {
        this.writeLatch = !this.writeLatch;
    }

    private int getScrollX() {
        return this.scrollXLSB | ((this.ppuControl & 1) << 8);
    }

    private int getScrollY() {
        return this.scrollYLSB | ((this.ppuControl & 0b10) << 7);
    }

    public void subCycle() {

    }

    private int getBaseNametableAddress() {
        return switch (this.ppuControl & 0b11) {
            case 0 -> 0x2000;
            case 1 -> 0x2400;
            case 2 -> 0x2800;
            case 3 -> 0x2C00;
            default -> throw new EmulatorException(new IllegalStateException("Invalid NES PPU base nametable address value!"));
        };
    }

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

    private boolean getVblankNMIEnable() {
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

    private void setVblankFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 1 << 7;
        } else {
            this.ppuStatus &= ~(1 << 7);
        }
    }

    private boolean getVblankFlag() {
        return (this.ppuStatus & (1 << 7)) != 0;
    }

    private enum VRAMAddressIncrement {
        ADD_1_ACROSS,
        ADD_32_DOWN
    }

    private enum SpriteSize {
        SIZE_8_8,
        SIZE_8_16
    }

}
