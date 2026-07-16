package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

import static io.github.arkosammy12.jemu.core.util.ByteSizes.KB_1;

public class Cartridge3E<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    private final byte[] ram = new byte[32 * KB_1];

    private int romBankBits = 0b0000_0000__0000_0000_0000;
    private int ramBankBits = 0b0_0000__00_0000_0000;
    private LowerSegmentMapping lowerSegmentMapping = LowerSegmentMapping.ROM;

    public Cartridge3E(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address) {
        if ((address & 0xFFF) >= 0x800 || this.lowerSegmentMapping == LowerSegmentMapping.ROM) {
            return super.readByte(address);
        } else {
            return (int) this.ram[(address & 0x3FF) | this.ramBankBits] & 0xFF;
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
        return address | (address >= 0x800 ? 0b1111_1111__0000_0000_0000 : this.romBankBits);
    }

    private void checkBankswitch(int address, int value) {
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

    private enum LowerSegmentMapping {
        ROM,
        RAM
    }

}