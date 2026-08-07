package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

public class CartridgeFE<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private int bankBits = 0b000__0000_0000_0000;
    private int bankSwitchLatchCounter;

    public CartridgeFE(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address, int dataBus) {
        int ret = super.readByte(address, dataBus);
        this.checkBankswitch(address);
        return ret;
    }

    @Override
    public void writeByte(int address, int value) {
        this.checkBankswitch(address);
    }

    @Override
    public void cycle() {
        if (this.bankSwitchLatchCounter > 0) {
            this.bankSwitchLatchCounter--;
            if (this.bankSwitchLatchCounter <= 0) {
                this.bankBits = (((this.emulator.getBus().getDataBus() >>> 5) & 0b111) ^ 0b111) << 12;
            }
        }
    }

    @Override
    protected int mapROMAddress(int address) {
        return this.bankBits | (address & 0xFFF);
    }

    private void checkBankswitch(int address) {
        if (address == 0x01FE) {
            this.bankSwitchLatchCounter = 2;
        }
    }

}