package io.github.arkosammy12.jemu.core.nes.ines;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class INESFile {

    public static final int KB_8 = 0x2000;
    public static final int KB_16 = 0x4000;

    private final NametableArrangement nametableArrangement;
    private final boolean hasBattery;
    private final boolean hasAlternativeNametableLayout;
    private final int[] programRomData;
    private final int @Nullable [] characterRomData;
    private final int @Nullable [] byteTrainer;

    private final int mapperNumber;
    private final int programRamSizeBytes;
    private final int characterRamSizeBytes;

    public INESFile(int[] file) {

        this.mapperNumber = this.getMapperNumber(file);
        this.programRamSizeBytes = this.getProgramRamSize(file);
        this.characterRamSizeBytes = this.getCharacterRamSize(file);

        int flags6 = file[6] & 0xFF;
        this.nametableArrangement = (flags6 & 1) != 0 ? NametableArrangement.HORIZONTAL : NametableArrangement.VERTICAL;
        this.hasBattery = (flags6 & (1 << 1)) != 0;
        this.hasAlternativeNametableLayout = (flags6 & (1 << 3)) != 0;

        boolean hasByeTrainer = (flags6 & (1 << 2)) != 0;

        int programRomDataBeginIndex = 16;
        if (hasByeTrainer) {
            programRomDataBeginIndex += 512;
        }

        int programRomSizeBytes = this.getProgramRomSizeBytes(file);
        if (programRomSizeBytes <= 0) {
            throw new EmulatorException("PRG-ROM size header cannot be 0!");
        }

        this.programRomData = new int[programRomSizeBytes];
        System.arraycopy(file, programRomDataBeginIndex, this.programRomData, 0, this.programRomData.length);

        if (hasByeTrainer) {
            this.byteTrainer = new int[512];
            System.arraycopy(file, 16, this.byteTrainer, 0, this.byteTrainer.length);
        } else {
            this.byteTrainer = null;
        }

        int characterRomSizeBytes = this.getCharacterRomSizeBytes(file);
        if (characterRomSizeBytes <= 0) {
            this.characterRomData = null;
        } else {
            int characterRomDataBeginIndex = programRomDataBeginIndex + this.programRomData.length;
            this.characterRomData = new int[characterRomSizeBytes];
            System.arraycopy(file, characterRomDataBeginIndex, this.characterRomData, 0, this.characterRomData.length);
        }

    }

    protected int getProgramRomSizeBytes(int[] file) {
        return (file[4] & 0xFF) * KB_16;
    }

    protected int getCharacterRomSizeBytes(int[] file) {
        return (file[5] & 0xFF) * KB_8;
    }

    protected int getMapperNumber(int[] file) {
        return (file[6] >>> 4) & 0xF;
    }

    protected int getProgramRamSize(int[] file) {
        return KB_8;
    }

    protected int getCharacterRamSize(int[] file) {
        return (file[5] & 0xFF) == 0 ? KB_8 : 0;
    }

    public int[] getProgramRom() {
        return Arrays.copyOf(this.programRomData, this.programRomData.length);
    }

    public Optional<int[]> getCharacterRom() {
        return Optional.ofNullable(this.characterRomData == null ? null : Arrays.copyOf(this.characterRomData, this.characterRomData.length));
    }

    public Optional<int[]> getByteTrainer() {
        return Optional.ofNullable(this.byteTrainer == null ? null : Arrays.copyOf(this.byteTrainer, this.byteTrainer.length));
    }

    public int getMapperNumber() {
        return this.mapperNumber;
    }

    public int getProgramRamSize() {
        return this.programRamSizeBytes;
    }

    public int getCharacterRamSize() {
        return this.characterRamSizeBytes;
    }

    public NametableArrangement getNametableMirroring() {
        return this.nametableArrangement;
    }

    public boolean hasBattery() {
        return this.hasBattery;
    }

    public boolean hasAlternativeNametableLayout() {
        return this.hasAlternativeNametableLayout;
    }

    public static INESFile getINESFile(int[] file) {
        try {
            int maskedByte7 = file[7] & 0x0C;
            boolean bytes12To15AreZero = true;
            for (int i = 12; i <= 15; i++) {
                if (file[i] != 0) {
                    bytes12To15AreZero = false;
                    break;
                }
            }

            boolean hasByeTrainer = (file[6] & (1 << 2)) != 0;
            int programRomSizeBytes = INES20.parseNes20ProgramRomSizeBytes(file);
            int characterRomSizeBytes = INES20.parseNes20CharacterRomSizeBytes(file);

            int finalIndex = 16;
            if (hasByeTrainer) {
                finalIndex += 512;
            }

            finalIndex += programRomSizeBytes;
            finalIndex += characterRomSizeBytes;

            if (maskedByte7 == 0x08 && finalIndex < file.length) {
                return new INES20(file);
            } else if (maskedByte7 == 0x04) {
                return new INESFile(file);
            } else if (maskedByte7 == 0x00 && bytes12To15AreZero) {
                return new INES10(file);
            } else {
                return new INESFile(file);
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EmulatorException("Error initializing from iNES file!", e);
        }
    }

    public enum NametableArrangement {
        HORIZONTAL,
        VERTICAL
    }

}
