package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

public class CartridgeF8<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private int bankBits = 0b0__0000_0000_0000;

    public CartridgeF8(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address) {
        this.checkBankswitch(address);
        return super.readByte(address);
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
        switch (address) {
            case 0x1FF8 -> this.bankBits = 0;
            case 0x1FF9 -> this.bankBits = (1 << 12);
        }
    }

}