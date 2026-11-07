package io.github.arkosammy12.jemu.core.gameboy.mbcs;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyCartridge;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;

import java.util.OptionalInt;

public class MBC5 extends GameBoyCartridge {

    private boolean ramGate;
    private int romBankLower = 1;
    private int romBankUpper;
    private int ramBank;

    public MBC5(GameBoyEmulator emulator, int cartridgeType, byte[] romImage) {
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
            case 0x07 -> 256 * 0x4000;
            case 0x08 -> 512 * 0x4000;
            default -> throw new ROMInitializationException("Incompatible ROM size header $%02X for MBC5 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };
    }

    @Override
    protected OptionalInt getSRAMLength() {
        if (cartridgeType == 0x1A || cartridgeType == 0x1B || cartridgeType == 0x1D || cartridgeType == 0x1E) {
            return switch (this.ramSizeHeader) {
                case 0x00 -> OptionalInt.empty();
                case 0x01 -> OptionalInt.of(0x800);
                case 0x02 -> OptionalInt.of(0x2000);
                case 0x03 -> OptionalInt.of(4 * 0x2000);
                case 0x04 -> OptionalInt.of(16 * 0x2000);
                case 0x05 -> OptionalInt.of(8 * 0x2000);
                default -> throw new ROMInitializationException("Incompatible RAM size header $%02X for MBC5 GameBoy cartridge type!".formatted(this.ramSizeHeader));
            };
        } else {
            return OptionalInt.empty();
        }
    }

    @Override
    protected boolean hasBattery() {
        return this.cartridgeType == 0x1B || this.cartridgeType == 0x1E;
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            return (int) this.rom[(address & 0x3FFF) & this.romAddressMask] & 0xFF;
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return (int) this.rom[((this.romBankUpper << 22) | (this.romBankLower << 14) | (address & 0x3FFF)) & this.romAddressMask] & 0xFF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate && this.sram != null) {
                address = ((this.ramBank << 13) | (address & 0x1FFF)) & this.ramAddressMask;
                if (address < this.sram.length) {
                    return (int) this.sram[address] & 0xFF;
                } else {
                    return 0xFF;
                }
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC5 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            this.ramGate = (value & 0xFF) == 0x0A;
        } else if (address >= 0x2000 && address <= 0x2FFF) {
            this.romBankLower = value & 0xFF;
        } else if (address >= 0x3000 && address <= 0x3FFF) {
            this.romBankUpper = value & 1;
        } else if (address >= 0x4000 && address <= 0x5FFF) {
            this.ramBank = value & 0xF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate && this.sram != null) {
                address = ((this.ramBank << 13) | (address & 0x1FFF)) & this.ramAddressMask;
                if (address < this.sram.length) {
                    this.sram[address] = (byte) value;
                }
            }
        }
    }

}
