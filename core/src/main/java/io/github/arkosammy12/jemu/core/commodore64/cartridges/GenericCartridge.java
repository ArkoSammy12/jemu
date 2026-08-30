package io.github.arkosammy12.jemu.core.commodore64.cartridges;

import io.github.arkosammy12.jemu.core.commodore64.Commodore64Cartridge;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.commodore64.crt.CHIPPacket;
import io.github.arkosammy12.jemu.core.commodore64.crt.CRTFile;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static io.github.arkosammy12.jemu.core.util.ByteSizes.KB_8;

public class GenericCartridge<E extends Commodore64Emulator> extends Commodore64Cartridge<E> {

    private byte @Nullable [] roml;
    private byte @Nullable [] romh;

    private final boolean exromLine;
    private final boolean gameLine;

    public GenericCartridge(E emulator, CRTFile crtFile) {
        super(emulator, crtFile);
        this.exromLine = crtFile.getEXROMLineStatus();
        this.gameLine = crtFile.getGAMELineStatus();

        List<CHIPPacket> chipPackets = crtFile.getChipPackets();
        this.loadPacket(chipPackets.getFirst());
        if (chipPackets.size() >= 2) {
            this.loadPacket(chipPackets.get(1));
        }
    }

    private void loadPacket(CHIPPacket chipPacket) {
        int startingLoadAddress = chipPacket.startingLoadAddress();
        int offset = startingLoadAddress & 0x1FFF;
        byte[] data = chipPacket.romData();
        if (startingLoadAddress < 0xA000) {
            if (this.roml == null) {
                this.roml = new byte[KB_8];
                Arrays.fill(this.roml, (byte) 0xFF);
            }

            int copyLength = this.roml.length - offset;
            System.arraycopy(data, 0, this.roml, offset, Math.clamp(copyLength, 0, data.length));

            if (data.length > copyLength) {
                if (this.romh == null) {
                    this.romh = new byte[KB_8];
                    Arrays.fill(this.romh, (byte) 0xFF);
                }

                System.arraycopy(data, copyLength, this.romh, 0, Math.clamp(data.length - copyLength, 0, this.romh.length));
            }

        } else {
            if (this.romh == null) {
                this.romh = new byte[KB_8];
                Arrays.fill(this.romh, (byte) 0xFF);
            }
            System.arraycopy(data, 0, this.romh, offset, Math.clamp(this.romh.length - offset, 0, data.length));
        }
    }

    @Override
    public boolean getEXROM() {
        return this.exromLine;
    }

    @Override
    public boolean getGAME() {
        return this.gameLine;
    }

    @Override
    protected int readROML(int address) {
        if (this.roml == null) {
            return this.emulator.getBus().combineWithDataBus(0x00, 0x00);
        } else {
            return (int) this.roml[address & 0x1FFF] & 0xFF;
        }
    }

    @Override
    protected int readROMH(int address) {
        if (this.romh == null) {
            return this.emulator.getBus().combineWithDataBus(0x00, 0x00);
        } else {
            return (int) this.romh[address & 0x1FFF] & 0xFF;
        }
    }

}
