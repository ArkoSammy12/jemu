package io.github.arkosammy12.jemu.core.nes.ines;

public class ExtendedINESFile extends INESFile {

    public ExtendedINESFile(int[] file) {
        super(file);
    }

    protected int getMapperNumber(int[] file) {
        return (file[7] & 0xF0) | ((file[6] >>> 4) & 0x0F);
    }

    protected int getProgramRamSize(int[] file) {
        int flags8 = file[8];
        return flags8 == 0 ? KB_8 : flags8 * KB_8;
    }

}
