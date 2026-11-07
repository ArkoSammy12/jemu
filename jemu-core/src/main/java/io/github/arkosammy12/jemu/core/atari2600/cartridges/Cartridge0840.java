package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

public class Cartridge0840<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private int bankBits = 0b0__0000_0000_0000;

    public Cartridge0840(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address, int dataBus) {
        this.checkBankswitch(address);
        return super.readByte(address, dataBus);
    }

    @Override
    public void writeByte(int address, int value) {
        this.checkBankswitch(address);
    }

    @Override
    protected int mapROMAddress(int address) {
        return this.bankBits | (address & 0xFFF);
    }

    private void checkBankswitch(int address) {
        switch (address & 0x1840) {
            case 0x0800 -> this.bankBits = 0;
            case 0x0840 -> this.bankBits = (1 << 12);
        }
    }

}