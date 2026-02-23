package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;


public class MBC0 extends GameBoyCartridge {

    private final int[] rom = new int[0x8000];
    private final int[] sRam;

    public MBC0(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);

        this.sRam = switch (this.ramSizeHeader) {
            case 0x00 -> null;
            case 0x01 -> new int[0x800];
            case 0x02 -> new int[0x2000];
            default -> throw new EmulatorException("Incompatible RAM size header $%02X for MBC0 GameBoy cartridge type!".formatted(this.ramSizeHeader));
        };

        try {
            System.arraycopy(this.originalRom, 0, this.rom, 0, this.rom.length);
        } catch (Exception e) {
            throw new EmulatorException("Error initializing GameBoy cartridge ROM!", e);
        }

    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x7FFF) {
            return this.rom[address];
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            address -= 0xA000;
            if (this.sRam != null && address < this.sRam.length) {
                return this.sRam[address];
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
            address -= 0xA000;
            if (this.sRam != null && address < this.sRam.length) {
                this.sRam[address] = value & 0xFF;
            }
        }
    }

}

