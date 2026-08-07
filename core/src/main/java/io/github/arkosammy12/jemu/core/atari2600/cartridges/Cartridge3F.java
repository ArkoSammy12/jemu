package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

public class Cartridge3F<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private static final int ARM_MASK = 0x00C0;

    private int romBankBits = 0b0000_0000__0000_0000_0000;
    private boolean bankSwitchLatch = false;
    private boolean previousA12 = false;

    public Cartridge3F(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address, int dataBus) {
        this.checkBankSwitch(address, dataBus);
        return super.readByte(address, dataBus);
    }

    @Override
    public void writeByte(int address, int value) {
        this.checkBankSwitch(address, value);
    }

    @Override
    protected int mapROMAddress(int address) {
        address &= 0xFFF;
        return address | (address >= 0x800 ? 0b1111_1111__0000_0000_0000 : this.romBankBits);
    }

    private void checkBankSwitch(int address, int value) {
        boolean A12 = (address & 0x1000) != 0;
        if (this.bankSwitchLatch && !this.previousA12 && A12) {
            this.romBankBits = (value & 0xFF) << 11;
        }
        this.bankSwitchLatch = (address & ARM_MASK) == 0;
        this.previousA12 = A12;
    }

}
