package io.github.arkosammy12.jemu.core.nintendo.gameboy.mbcs;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyCartridge;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyEmulator;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.Optional;
import java.util.OptionalInt;

public class MBC1 extends GameBoyCartridge {

    private boolean ramGate;
    private int bank1 = 1;
    private int bank2;
    private boolean mode;

    public MBC1(GameBoyEmulator emulator, int cartridgeType, byte[] romImage) {
        super(emulator, cartridgeType, romImage);
    }

    @Override
    protected int getROMLength() {
        return switch (this.romSizeHeader) {
            case 0x00 -> 2 * 0x4000;
            case 0x01 -> 4 * 0x4000;
            case 0x02 -> 8 * 0x4000;
            case 0x03 -> 16 * 0x4000;
            case 0x04 -> 32 * 0x4000;
            case 0x05 -> 64 * 0x4000;
            case 0x06 -> 128 * 0x4000;
            default -> throw new EmulatorException("Incompatible ROM size header $%02X for MBC1 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };
    }

    @Override
    protected OptionalInt getSRAMLength() {
        if (cartridgeType == 0x02 || cartridgeType == 0x03) {
            return switch (this.ramSizeHeader) {
                case 0x00 -> OptionalInt.empty();
                case 0x01 -> OptionalInt.of(0x800);
                case 0x02 -> OptionalInt.of(0x2000);
                case 0x03 -> OptionalInt.of(4 * 0x2000);
                default -> throw new EmulatorException("Incompatible RAM size header $%02X for MBC1 GameBoy cartridge type!".formatted(this.ramSizeHeader));
            };
        } else {
            return OptionalInt.empty();
        }
    }

    @Override
    protected boolean hasBattery() {
        return this.cartridgeType == 0x03;
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            if (this.mode) {
                return (int) this.rom[((this.bank2 << 19) | (address & 0x3FFF)) & this.romAddressMask] & 0xFF;
            } else {
                return (int) this.rom[(address & 0x3FFF) & this.romAddressMask] & 0xFF;
            }
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return (int) this.rom[((this.bank2 << 19) | (this.bank1 << 14) | (address & 0x3FFF)) & this.romAddressMask] & 0xFF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate && this.sram != null) {
                address = ((this.mode ? (this.bank2 << 13) : 0) | (address & 0x1FFF)) & this.ramAddressMask;
                if (address < this.sram.length) {
                    return (int) this.sram[address] & 0xFF;
                } else {
                    return 0xFF;
                }
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC1 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            this.ramGate = (value & 0xF) == 0b1010;
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            this.bank1 = value & 0b11111;
            if (this.bank1 == 0) {
                this.bank1 = 1;
            }
        } else if (address >= 0x4000 && address <= 0x5FFF) {
            this.bank2 = value & 0b11;
        } else if (address >= 0x6000 && address <= 0x7FFF) {
            this.mode = (value & 1) != 0;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate && this.sram != null) {
                address = ((this.mode ? (this.bank2 << 13) : 0) | (address & 0x1FFF)) & this.ramAddressMask;
                if (address < this.sram.length) {
                    this.sram[address] = (byte) value;
                }
            }
        }
    }

}
