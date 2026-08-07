package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

public class CartridgeFA<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private final byte[] ram = new byte[256];

    private int bankBits = 0b0__0000_0000_0000;

    public CartridgeFA(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address, int dataBus) {
        int readValue;
        if (address >= 0x1000 && address <= 0x10FF) {
            readValue = this.emulator.combineWithDataBus(0, 0x00);
        } else if (address >= 0x1100 && address <= 0x11FF) {
            readValue = (int) this.ram[address & 0xFF] & 0xFF;
        } else {
            readValue = super.readByte(address, dataBus);
        }
        this.checkBankswitch(address, readValue);
        return readValue;
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x1000 && address <= 0x10FF) {
            this.ram[address & 0xFF] = (byte) value;
        }
        this.checkBankswitch(address, value);
    }

    @Override
    protected int mapROMAddress(int address) {
        return this.bankBits | (address & 0xFFF);
    }

    private void checkBankswitch(int address, int value) {
        if ((value & 1) != 0) {
            switch (address) {
                case 0x1FF8 -> this.bankBits = 0;
                case 0x1FF9 -> this.bankBits = (1 << 12);
                case 0x1FFA -> this.bankBits = (2 << 12);
            }
        }
    }

}