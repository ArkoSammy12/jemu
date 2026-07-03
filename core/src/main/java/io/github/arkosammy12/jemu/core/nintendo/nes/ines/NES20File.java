package io.github.arkosammy12.jemu.core.nintendo.nes.ines;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.nes.NESEmulator;

public class NES20File extends ExtendedINESFile {

    private final int nonVolatileProgramRamSize;
    private final int nonVolatileCharacterRamSize;

    public NES20File(byte[] file) {
        super(file);

        this.nonVolatileProgramRamSize = this.getNonVolatileProgramRamSize(file);
        this.nonVolatileCharacterRamSize = this.getNonVolatileCharacterRamSize(file);
    }

    public static int parseNes20ProgramRomSizeBytes(byte[] file) {
        int programRomLsb = (int) file[4] & 0xFF;
        int programRomMsb = (int) file[9] & 0x0F;
        if (programRomMsb == 0xF) {
            int multiplier = (programRomLsb & 0b11) * 2 + 1;
            int exponent = (programRomLsb >>> 2) & 0b111111;
            return (int) (Math.pow(2, exponent) * (double) multiplier);
        } else {
            return ((programRomMsb << 8) | programRomLsb) * KB_16;
        }
    }

    public static int parseNes20CharacterRomSizeBytes(byte[] file) {
        int characterRomLsb = (int) file[5] & 0xFF;
        int characterRomMsb = (((int) file[9] & 0xFF) >>> 4) & 0x0F;
        if (characterRomMsb == 0xF) {
            int multiplier = (characterRomLsb & 0b11) * 2 + 1;
            int exponent = (characterRomMsb >>> 2) & 0b111111;
            return (int) (Math.pow(2, exponent) * (double) multiplier);
        } else {
            return ((characterRomMsb << 8) | characterRomLsb) * KB_8;
        }
    }

    @Override
    protected int getProgramRomSizeBytes(byte[] file) {
        return parseNes20ProgramRomSizeBytes(file);
    }

    @Override
    protected int getCharacterRomSizeBytes(byte[] file) {
        return parseNes20CharacterRomSizeBytes(file);
    }

    @Override
    protected int getMapperNumber(byte[] file) {
        return (((int) file[8] & 0x0F) << 4) | ((int) file[7] & 0xF0) | (((int) file[6] >>> 4) & 0x0F);
    }

    @Override
    public int getSubmapperNumber(byte[] file) {
        return (((int) file[8] & 0xFF) >>> 4) & 0xF;
    }

    @Override
    protected int getProgramRamSize(byte[] file) {
        int flags10 = (int) file[10] & 0xFF;
        int volatileShiftCount = flags10 & 0x0F;
        return volatileShiftCount == 0 ? 0 : 64 << volatileShiftCount;
    }

    private int getNonVolatileProgramRamSize(byte[] file) {
        int flags10 = (int) file[10] & 0xFF;
        int nonVolatileShiftCount = (flags10 >>> 4) & 0x0F;
        return nonVolatileShiftCount == 0 ? 0 : 64 << nonVolatileShiftCount;
    }

    private int getNonVolatileCharacterRamSize(byte[] file) {
        int flags10 = (int) file[11] & 0xFF;
        int nonVolatileShiftCount = (flags10 >>> 4) & 0x0F;
        return nonVolatileShiftCount == 0 ? 0 : 64 << nonVolatileShiftCount;
    }

    @Override
    protected int getCharacterRamSize(byte[] file) {
        int flags10 = (int) file[11] & 0xFF;
        int volatileShiftCount = flags10 & 0x0F;
        return volatileShiftCount == 0 ? 0 : 64 << volatileShiftCount;
    }

    public int getNonVolatileProgramRamSizeBytes() {
        return this.nonVolatileProgramRamSize;
    }

    public int getNonVolatileCharacterRamSizeBytes() {
        return this.nonVolatileCharacterRamSize;
    }

    @Override
    protected NESEmulator.TVSystem getTVSystem(byte[] file) {
        return switch ((int) file[12] & 0b11) {
            case 0 -> NESEmulator.TVSystem.NTSC;
            case 1 -> NESEmulator.TVSystem.PAL;
            case 2 -> NESEmulator.TVSystem.MULTIPLE_REGION;
            case 3 -> NESEmulator.TVSystem.DENDY;
            default -> throw new EmulatorException("NES 2.0 CPU/PPU timing field value not in [0, 3]!");
        };
    }

}
