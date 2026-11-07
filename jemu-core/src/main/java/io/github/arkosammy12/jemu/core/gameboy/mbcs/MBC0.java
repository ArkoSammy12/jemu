package io.github.arkosammy12.jemu.core.gameboy.mbcs;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyCartridge;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;

import java.util.OptionalInt;

public class MBC0 extends GameBoyCartridge {

    public MBC0(GameBoyEmulator emulator, int cartridgeType, byte[] romImage) {
        super(emulator, cartridgeType, romImage);
    }

    @Override
    protected int getROMLength() {
        return 0x8000;
    }

    @Override
    protected OptionalInt getSRAMLength() {
        if (this.cartridgeType == 0x08 || this.cartridgeType == 0x09) {
            return switch (this.ramSizeHeader) {
                case 0x00 -> OptionalInt.empty();
                case 0x01 -> OptionalInt.of(0x800);
                case 0x02 -> OptionalInt.of(0x2000);
                default -> throw new ROMInitializationException("Incompatible RAM size header $%02X for MBC0 GameBoy cartridge type!".formatted(this.ramSizeHeader));
            };
        } else {
            return OptionalInt.empty();
        }
    }

    @Override
    protected boolean hasBattery() {
        return this.cartridgeType == 0x09;
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x7FFF) {
            return (int) this.rom[address] & 0xFF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            address &= 0x1FFF;
            if (this.sram != null && address < this.sram.length) {
                return (int) this.sram[address] & 0xFF;
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC0 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0xA000 && address <= 0xBFFF) {
            address &= 0x1FFF;
            if (this.sram != null && address < this.sram.length) {
                this.sram[address] = (byte) value;
            }
        }
    }

}

