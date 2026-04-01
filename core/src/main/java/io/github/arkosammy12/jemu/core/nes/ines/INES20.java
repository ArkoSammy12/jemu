package io.github.arkosammy12.jemu.core.nes.ines;

public class INES20 extends INES10 {

    private final int nonVolatileProgramRamSize;
    private final int nonVolatileCharacterRamSize;

    public INES20(int[] file) {
        super(file);

        this.nonVolatileProgramRamSize = this.getNonVolatileProgramRamSize(file);
        this.nonVolatileCharacterRamSize = this.getNonVolatileCharacterRamSize(file);
    }

    public static int parseNes20ProgramRomSizeBytes(int[] file) {
        int programRomLsb = file[4] & 0xFF;
        int programRomMsb = file[9] & 0x0F;
        if (programRomMsb == 0xF) {
            int multiplier = (programRomLsb & 0b11) * 2 + 1;
            int exponent = (programRomLsb >>> 2) & 0b111111;
            return (int) (Math.pow(2, exponent) * multiplier);
        } else {
            return ((programRomMsb << 8) | programRomLsb) * KB_16;
        }
    }

    public static int parseNes20CharacterRomSizeBytes(int[] file) {
        int characterRomLsb = file[5] & 0xFF;
        int characterRomMsb = file[9] & 0x0F;
        if (characterRomMsb == 0xF) {
            int multiplier = (characterRomLsb & 0b11) * 2 + 1;
            int exponent = (characterRomMsb >>> 2) & 0b111111;
            return (int) (Math.pow(2, exponent) * multiplier);
        } else {
            return ((characterRomMsb << 8) | characterRomLsb) * KB_8;
        }
    }

    @Override
    protected int getProgramRomSizeBytes(int[] file) {
        return parseNes20ProgramRomSizeBytes(file);
    }

    @Override
    protected int getCharacterRomSizeBytes(int[] file) {
        return parseNes20CharacterRomSizeBytes(file);
    }

    @Override
    protected int getMapperNumber(int[] file) {
        return ((file[8] & 0x0F) << 4) | (file[7] & 0xF0) | ((file[6] >>> 4) & 0x0F);
    }

    @Override
    protected int getProgramRamSize(int[] file) {
        int flags10 = file[10] & 0xFF;
        int volatileShiftCount = flags10 & 0x0F;
        return volatileShiftCount == 0 ? 0 : 64 << volatileShiftCount;
    }

    private int getNonVolatileProgramRamSize(int[] file) {
        int flags10 = file[10] & 0xFF;
        int nonVolatileShiftCount = (flags10 >>> 4) & 0x0F;
        return nonVolatileShiftCount == 0 ? 0 : 64 << nonVolatileShiftCount;
    }

    private int getNonVolatileCharacterRamSize(int[] file) {
        int flags10 = file[11] & 0xFF;
        int nonVolatileShiftCount = (flags10 >>> 4) & 0x0F;
        return nonVolatileShiftCount == 0 ? 0 : 64 << nonVolatileShiftCount;
    }


    protected int getCharacterRamSize(int[] file) {
        int flags10 = file[11] & 0xFF;
        int volatileShiftCount = flags10 & 0x0F;
        return volatileShiftCount == 0 ? 0 : 64 << volatileShiftCount;
    }

    public int getNonVolatileProgramRamSizeBytes() {
        return this.nonVolatileProgramRamSize;
    }

    public int getNonVolatileCharacterRamSizeBytes() {
        return this.nonVolatileCharacterRamSize;
    }

}
