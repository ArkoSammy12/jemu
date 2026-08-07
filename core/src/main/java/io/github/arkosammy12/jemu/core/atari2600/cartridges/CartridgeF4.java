package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

public class CartridgeF4<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private int bankBits = 0b000__0000_0000_0000;

    public CartridgeF4(E emulator) {
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
        switch (address) {
            case 0x1FF4 -> this.bankBits = 0;
            case 0x1FF5 -> this.bankBits = (1 << 12);
            case 0x1FF6 -> this.bankBits = (2 << 12);
            case 0x1FF7 -> this.bankBits = (3 << 12);
            case 0x1FF8 -> this.bankBits = (4 << 12);
            case 0x1FF9 -> this.bankBits = (5 << 12);
            case 0x1FFA -> this.bankBits = (6 << 12);
            case 0x1FFB -> this.bankBits = (7 << 12);
        }
    }

}