package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;
import org.tinylog.Logger;

import static io.github.arkosammy12.jemu.core.util.ByteSizes.*;

public class Cartridge3E<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private final byte[] ram = new byte[32 * KB_1];
    private final int lastRomBankBits;

    private int romBankBits = 0b0000_0000__0000_0000_0000;
    private int ramBankBits = 0b0_0000__00_0000_0000;
    private LowerSegmentMapping lowerSegmentMapping = LowerSegmentMapping.ROM;

    public Cartridge3E(E emulator) {
        super(emulator);
        this.lastRomBankBits = this.getLastROMBankNumber(KB_2, 0xFF) << 11;
    }

    @Override
    public int readByte(int address, int dataBus) {
        if ((address & 0xFFF) >= 0x800 || this.lowerSegmentMapping == LowerSegmentMapping.ROM) {
            return super.readByte(address, dataBus);
        } else if (address >= 0x1000 && address <= 0x13FF) {
            return (int) this.ram[(address & 0x3FF) | this.ramBankBits] & 0xFF;
        } else {
            if (address >= 0x1400 && address <= 0x17FF) {
                this.ram[(address & 0x3FF) | this.ramBankBits] = (byte) dataBus;
            }
            return this.emulator.combineWithDataBus(0, 0x00);
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (this.lowerSegmentMapping == LowerSegmentMapping.RAM && address >= 0x1400 && address <= 0x17FF) {
            this.ram[(address & 0x3FF) | this.ramBankBits] = (byte) value;
        }
        this.checkBankswitch(address, value);
    }

    @Override
    protected int mapROMAddress(int address) {
        address &= 0xFFF;
        return address | (address >= 0x800 ? this.lastRomBankBits  : this.romBankBits);
    }

    private void checkBankswitch(int address, int value) {
        if ((address & 0x1000) == 0) {
            switch (address & 0xFFF) {
                case 0x03F -> {
                    this.romBankBits = (value & 0xFF) << 11;
                    this.lowerSegmentMapping = LowerSegmentMapping.ROM;
                }
                case 0x03E -> {
                    this.ramBankBits = (value & 0b11111) << 10;
                    this.lowerSegmentMapping = LowerSegmentMapping.RAM;
                }
            }
        }
    }

    private enum LowerSegmentMapping {
        ROM,
        RAM
    }

}