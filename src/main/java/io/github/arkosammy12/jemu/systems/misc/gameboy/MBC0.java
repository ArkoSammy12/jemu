package io.github.arkosammy12.jemu.systems.misc.gameboy;

import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.systems.GameBoyEmulator;

import static io.github.arkosammy12.jemu.systems.bus.GameBoyBus.*;

public class MBC0 extends GameBoyCartridge {

    private final int externalRamLength;
    protected int[] romX;
    protected int[] externalRam;

    public MBC0(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);
        this.romX = new int[0x4000];

        System.arraycopy(this.originalRom, this.rom0.length, this.romX, 0, this.romX.length);

        switch (this.ramBankAmount) {
            case 0x00 -> this.externalRamLength = 0x0;
            case 0x01 -> this.externalRamLength = 0x800;
            case 0x02 -> this.externalRamLength = 0x2000;
            default -> throw new EmulatorException("Illegal RAM size value " + String.format("%02X", this.ramBankAmount) + " for MBC0 cartridge!");
        }

        if (this.externalRamLength > 0) {
            this.externalRam = new int[this.externalRamLength];
        }

    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= SRAM_START && address <= SRAM_END) {
            address -= SRAM_START;
            if (address < this.externalRamLength) {
                this.externalRam[address] = value & 0xFF;
            }
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= ROM0_START && address <= ROM0_END) {
            return this.rom0[address - ROM0_START];
        } else if (address >= ROMX_START && address <= ROMX_END) {
            return this.romX[address - ROMX_START];
        } else if (address >= SRAM_START && address <= SRAM_END) {
            address -= SRAM_START;
            if (address < this.externalRamLength) {
                return this.externalRam[address];
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid address " + String.format("%04X", address) + " for MBC0 cartridge read!");
        }
    }

}
