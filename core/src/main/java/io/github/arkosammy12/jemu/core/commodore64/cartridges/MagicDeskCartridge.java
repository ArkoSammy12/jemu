package io.github.arkosammy12.jemu.core.commodore64.cartridges;

import io.github.arkosammy12.jemu.core.commodore64.Commodore64Cartridge;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.commodore64.crt.CHIPPacket;
import io.github.arkosammy12.jemu.core.commodore64.crt.CRTFile;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.List;

import static io.github.arkosammy12.jemu.core.util.ByteSizes.KB_8;

public class MagicDeskCartridge<E extends Commodore64Emulator> extends Commodore64Cartridge<E> {

    private final byte[] rom;

    private int register;
    private boolean exromLine = true;
    private int bankBits;

    public MagicDeskCartridge(E emulator, CRTFile crtFile) {
        super(emulator, crtFile);

        List<CHIPPacket> chipPackets = crtFile.getChipPackets();
        if (chipPackets.size() < 4 || chipPackets.size() > 16) {
            throw new ROMInitializationException(".CRT file with cartridge type 19 must have between 4 and 16 CHIP packets, but found %d packets instead!".formatted(chipPackets.size()));
        }

        this.rom = new byte[KB_8 * chipPackets.size()];

        for (CHIPPacket chipPacket : chipPackets) {
            int bankNumber = chipPacket.bankNumber();
            if (chipPacket.romData().length != KB_8) {
                throw new ROMInitializationException("Encountered CHIP packet with non 8KB size for a .CRT file with cartridge type 19!");
            }
            if (bankNumber >= chipPackets.size()) {
                throw new ROMInitializationException("Encountered CHIP packet with bank number %d, which is more than the amount of available packets!".formatted(bankNumber));
            }
            System.arraycopy(chipPacket.romData(), 0, this.rom, bankNumber * KB_8, KB_8);
        }

    }

    @Override
    public boolean getEXROM() {
        return this.exromLine;
    }

    @Override
    protected int readROML(int address) {
        return this.rom[((address & 0x1FFF) | this.bankBits) % this.rom.length];
    }

    @Override
    protected int readIO1(int address) {
        if (address == 0xDE00) {
            return this.register;
        } else {
            return this.emulator.getBus().combineWithDataBus(0, 0x00);
        }
    }

    @Override
    protected void writeIO1(int address, int value) {
        if (address == 0xDE00) {
            this.register = value & 0xFF;
            this.bankBits = (value & 0x7F) << 13;
            this.exromLine = (value & (1 << 7)) == 0;
        }
    }

}
