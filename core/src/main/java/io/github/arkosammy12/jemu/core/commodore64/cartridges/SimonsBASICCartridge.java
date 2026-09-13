package io.github.arkosammy12.jemu.core.commodore64.cartridges;

import io.github.arkosammy12.jemu.core.commodore64.Commodore64Cartridge;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.commodore64.crt.CHIPPacket;
import io.github.arkosammy12.jemu.core.commodore64.crt.CRTFile;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.List;

import static io.github.arkosammy12.jemu.core.util.ByteSizes.KB_8;

public class SimonsBASICCartridge<E extends Commodore64Emulator> extends Commodore64Cartridge<E> {

    private final byte[] romL = new byte[KB_8];
    private final byte[] romH = new byte[KB_8];

    private boolean gameLine;

    public SimonsBASICCartridge(E emulator, CRTFile crtFile) {
        super(emulator, crtFile);

        List<CHIPPacket> chipPackets = crtFile.getChipPackets();
        if (chipPackets.size() != 2) {
            throw new ROMInitializationException("Expected 2 CHIP packets in Simons' BASIC .CRT file, but found %d instead!".formatted(chipPackets.size()));
        }

        CHIPPacket romLPacket = chipPackets.getFirst();
        if (romLPacket.romData().length != KB_8) {
            throw new ROMInitializationException("Expected first CHIP packet of Simons' BASIC to be %d in length, but was %d bytes instead!".formatted(KB_8, romLPacket.romData().length));
        }
        if (romLPacket.startingLoadAddress() != 0x8000) {
            throw new ROMInitializationException("Expected first CHIP packet of Simons' BASIC to have a load address of $8000, but was %04X instead!".formatted(romLPacket.startingLoadAddress()));
        }
        System.arraycopy(romLPacket.romData(), 0, this.romL, 0, KB_8);

        CHIPPacket romHPacket = chipPackets.get(1);
        if (romHPacket.romData().length != KB_8) {
            throw new ROMInitializationException("Expected first CHIP packet of Simons' BASIC to be %d in length, but was %d bytes instead!".formatted(KB_8, romHPacket.romData().length));
        }
        if (romHPacket.startingLoadAddress() != 0xA000) {
            throw new ROMInitializationException("Expected second CHIP packet of Simons' BASIC to have a load address of $A000, but was %04X instead!".formatted(romHPacket.startingLoadAddress()));
        }
        System.arraycopy(romHPacket.romData(), 0, this.romH, 0, KB_8);
    }

    @Override
    public boolean getEXROM() {
        return true;
    }

    @Override
    public boolean getGAME() {
        return this.gameLine;
    }

    @Override
    protected int readROML(int address) {
        return (int) this.romL[address & 0x1FFF] & 0xFF;
    }

    @Override
    protected int readROMH(int address) {
        return (int) this.romH[address & 0x1FFF] & 0xFF;
    }

    @Override
    protected int readIO1(int address) {
        this.gameLine = false;
        return this.emulator.getBus().combineWithDataBus(0, 0x00);
    }

    @Override
    protected void writeIO1(int address, int value) {
        this.gameLine = true;
    }

}
