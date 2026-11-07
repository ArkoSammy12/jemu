package io.github.arkosammy12.jemu.core.gameboy.mbcs;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyCartridge;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;

import java.util.OptionalInt;

public class MBC2 extends GameBoyCartridge {

    private int romBank = 1;
    private boolean ramGate;

    public MBC2(GameBoyEmulator emulator, int cartridgeType, byte[] romImage) {
        super(emulator, cartridgeType, romImage);
    }

    @Override
    protected int getROMLength() {
        return switch (this.romSizeHeader) {
            case 0x00 -> 2 * 0x4000;
            case 0x01 -> 4 * 0x4000;
            case 0x02 -> 8 * 0x4000;
            case 0x03 -> 16 * 0x4000;
            default -> throw new ROMInitializationException("Incompatible ROM size header $%02X for MBC2 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };
    }

    @Override
    protected OptionalInt getSRAMLength() {
        return this.cartridgeType == 0x06 ? OptionalInt.of(512) : OptionalInt.empty();
    }

    @Override
    protected boolean hasBattery() {
        return this.cartridgeType == 0x06;
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            return (int) this.rom[(address & 0x3FFF) & this.romAddressMask] & 0xFF;
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return (int) this.rom[((this.romBank << 14) | (address & 0x3FFF)) & this.romAddressMask] & 0xFF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate && this.sram != null) {
                return ((int) this.sram[address & 0x1FF] & 0xFF) | 0xF0;
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC2 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            if ((address & (1 << 8)) == 0) {
                this.ramGate = (value & 0xF) == 0b1010;
            } else {
                this.romBank = value & 0xF;
                if (this.romBank == 0) {
                    this.romBank = 1;
                }
            }
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate && this.sram != null) {
                this.sram[address & 0x1FF] = (byte) value;
            }
        }
    }

}
