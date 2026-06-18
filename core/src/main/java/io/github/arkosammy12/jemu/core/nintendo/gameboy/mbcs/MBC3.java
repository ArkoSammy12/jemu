package io.github.arkosammy12.jemu.core.nintendo.gameboy.mbcs;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyCartridge;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyEmulator;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.Optional;
import java.util.OptionalInt;

public class MBC3 extends GameBoyCartridge {

    private final boolean mbc30;

    private int romBank = 1;
    protected int ramBank;
    protected boolean ramRTCEnable;

    public MBC3(GameBoyEmulator emulator, int cartridgeType, byte[] romImage) {
        super(emulator, cartridgeType, romImage);
        this.mbc30 = this.romSizeHeader == 0x07 || (this.hasSRAM() && this.ramSizeHeader == 0x05);
    }

    private boolean hasSRAM() {
        return this.cartridgeType == 0x10 || this.cartridgeType == 0x12 || this.cartridgeType == 0x13;
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
            default -> throw new EmulatorException("Incompatible ROM size header $%02X for MBC3 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };
    }

    @Override
    protected OptionalInt getSRAMLength() {
        if (this.hasSRAM()) {
            return switch (this.ramSizeHeader) {
                case 0x00 -> OptionalInt.empty();
                case 0x01 -> OptionalInt.of(0x800);
                case 0x02 -> OptionalInt.of(0x2000);
                case 0x03 -> OptionalInt.of(4 * 0x2000);
                case 0x05 -> OptionalInt.of(8 * 0x2000);
                default -> throw new EmulatorException("Incompatible RAM size header $%02X for MBC3 GameBoy cartridge type!".formatted(this.ramSizeHeader));
            };
        } else {
            return OptionalInt.empty();
        }
    }

    @Override
    protected boolean hasBattery() {
        return this.cartridgeType == 0x0F || this.cartridgeType == 0x10 || this.cartridgeType == 0x13;
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            return (int) this.rom[(address & 0x3FFF) & this.romAddressMask] & 0xFF;
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return (int) this.rom[((this.romBank << 14) | (address & 0x3FFF)) & this.romAddressMask] & 0xFF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramBank <= 0x07 && this.ramRTCEnable && this.sram != null) {
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
            throw new EmulatorException("Invalid GameBoy MBC3 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            this.ramRTCEnable = (value & 0xF) == 0xA;
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            this.romBank = value & (this.mbc30 ? 0xFF : 0x7F);
            if (this.romBank == 0) {
                this.romBank = 1;
            }
        } else if (address >= 0x4000 && address <= 0x5FFF) {
            this.ramBank = value & 0xF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramBank <= 0x07 && this.ramRTCEnable && this.sram != null) {
                address = ((this.ramBank << 13) | (address & 0x1FFF)) & this.ramAddressMask;
                if (address < this.sram.length) {
                    this.sram[address] = (byte) value;
                }
            }
        }
    }

}
