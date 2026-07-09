package io.github.arkosammy12.jemu.core.nes.ines;

import io.github.arkosammy12.jemu.core.nes.NESEmulator;

public class ExtendedINESFile extends INESFile {

    public ExtendedINESFile(byte[] file) {
        super(file);
    }

    protected int getMapperNumber(byte[] file) {
        return ((int) file[7] & 0xF0) | (((int) file[6] >>> 4) & 0x0F);
    }

    protected int getProgramRamSize(byte[] file) {
        int flags8 = (int) file[8] & 0xFF;
        return flags8 == 0 ? KB_8 : flags8 * KB_8;
    }

    @Override
    protected NESEmulator.TVSystem getTVSystem(byte[] file) {
        return ((int) file[9] & 1) != 0 ? NESEmulator.TVSystem.PAL : NESEmulator.TVSystem.NTSC;
    }

}
