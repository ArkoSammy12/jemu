package io.github.arkosammy12.jemu.core.nes.ines;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class INESFile {

    public static final int KB_2 = 0x800;
    public static final int KB_4 = KB_2 * 2;
    public static final int KB_8 = KB_4 * 2;
    public static final int KB_16 = KB_8 * 2;
    public static final int KB_32 = KB_16 * 2;
    public static final int KB_64 = KB_32 * 2;
    public static final int KB_128 = KB_64 * 2;
    public static final int KB_256 = KB_128 * 2;
    public static final int KB_512 = KB_256 * 2;

    private final boolean nametableArrangement;
    private final boolean hasBattery;
    private final boolean hasAlternativeNametableLayout;
    private final byte[] programRomData;
    private final byte @Nullable [] characterRomData;
    private final byte @Nullable [] byteTrainer;

    private final int mapperNumber;
    private final int submapperNumber;
    private final int programRamSizeBytes;
    private final int nonVolatileProgramRamSizeBytes;
    private final int characterRamSizeBytes;
    private final int nonVolatileCharacterRamSizeBytes;
    private final NESEmulator.TVSystem tvSystem;

    public <E extends NESEmulator> INESFile(E emulator, byte[] file) {

        int flags6 = (int) file[6] & 0xFF;;
        boolean hasByeTrainer = (flags6 & (1 << 2)) != 0;

        int programRomDataBeginIndex = 16;
        if (hasByeTrainer) {
            programRomDataBeginIndex += 512;
        }

        int programRomSizeBytes = this.getProgramRomSizeBytes(file);
        if (programRomSizeBytes <= 0) {
            throw new EmulatorException("PRG-ROM size header cannot be 0!");
        }

        this.programRomData = new byte[programRomSizeBytes];
        System.arraycopy(file, programRomDataBeginIndex, this.programRomData, 0, this.programRomData.length);

        if (hasByeTrainer) {
            this.byteTrainer = new byte[512];
            System.arraycopy(file, 16, this.byteTrainer, 0, this.byteTrainer.length);
        } else {
            this.byteTrainer = null;
        }

        int characterRomSizeBytes = this.getCharacterRomSizeBytes(file);
        if (characterRomSizeBytes <= 0) {
            this.characterRomData = null;
        } else {
            int characterRomDataBeginIndex = programRomDataBeginIndex + this.programRomData.length;
            this.characterRomData = new byte[characterRomSizeBytes];
            System.arraycopy(file, characterRomDataBeginIndex, this.characterRomData, 0, this.characterRomData.length);
        }

        Optional<CartridgeInfo> externalCartridgeInfo = emulator.getHost().getExternalCartridgeInfo(programRomSizeBytes + characterRomSizeBytes, hasByeTrainer);
        if (externalCartridgeInfo.isPresent()) {
            CartridgeInfo cartridgeInfo = externalCartridgeInfo.get();
            this.mapperNumber = cartridgeInfo.getMapperNumber();
            this.submapperNumber = cartridgeInfo.getSubmapperNumber();
            this.programRamSizeBytes = cartridgeInfo.getProgramRamSize();
            this.characterRamSizeBytes = cartridgeInfo.getCharacterRamSize();
            this.nonVolatileProgramRamSizeBytes = cartridgeInfo.getNonVolatileProgramRamSize();
            this.nonVolatileCharacterRamSizeBytes = cartridgeInfo.getNonVolatileCharacterRamSize();
            this.tvSystem = cartridgeInfo.getTVSystem();
            this.nametableArrangement = cartridgeInfo.getNametableArrangement();
            this.hasBattery = cartridgeInfo.hasBattery();
            this.hasAlternativeNametableLayout = cartridgeInfo.hasAlternativeNametableLayout();
        } else {
            this.mapperNumber = this.getMapperNumber(file);
            this.submapperNumber = this.getSubmapperNumber(file);
            this.programRamSizeBytes = this.getProgramRamSize(file);
            this.characterRamSizeBytes = this.getCharacterRamSize(file);
            this.nonVolatileProgramRamSizeBytes = this.getNonVolatileProgramRamSize(file);
            this.nonVolatileCharacterRamSizeBytes = this.getNonVolatileCharacterRamSize(file);
            this.tvSystem = this.getTVSystem(file);
            this.nametableArrangement = (flags6 & 1) != 0;
            this.hasBattery = (flags6 & (1 << 1)) != 0;
            this.hasAlternativeNametableLayout = (flags6 & (1 << 3)) != 0;
        }
    }

    protected int getProgramRomSizeBytes(byte[] file) {
        return ((int) file[4] & 0xFF) * KB_16;
    }

    protected int getCharacterRomSizeBytes(byte[] file) {
        return ((int) file[5] & 0xFF) * KB_8;
    }

    protected int getMapperNumber(byte[] file) {
        return ((int) file[6] >>> 4) & 0xF;
    }

    protected int getSubmapperNumber(byte[] file) {
        return 0;
    }

    protected int getProgramRamSize(byte[] file) {
        return KB_8;
    }

    protected int getCharacterRamSize(byte[] file) {
        return ((int) file[5] & 0xFF) == 0 ? KB_8 : 0;
    }

    protected int getNonVolatileProgramRamSize(byte[] file) {
        return 0;
    }

    protected int getNonVolatileCharacterRamSize(byte[] file) {
        return 0;
    }

    protected NESEmulator.TVSystem getTVSystem(byte[] file) {
        return NESEmulator.TVSystem.NTSC;
    }

    public byte[] getProgramRom() {
        return Arrays.copyOf(this.programRomData, this.programRomData.length);
    }

    public Optional<byte[]> getCharacterRom() {
        return Optional.ofNullable(this.characterRomData == null ? null : Arrays.copyOf(this.characterRomData, this.characterRomData.length));
    }

    public Optional<byte[]> getTrainer() {
        return Optional.ofNullable(this.byteTrainer == null ? null : Arrays.copyOf(this.byteTrainer, this.byteTrainer.length));
    }

    public int getMapperNumber() {
        return this.mapperNumber;
    }

    public int getSubmapperNumber() {
        return this.submapperNumber;
    }

    public int getProgramRamSize() {
        return this.programRamSizeBytes;
    }

    public int getCharacterRamSize() {
        return this.characterRamSizeBytes;
    }

    public int getNonVolatileProgramRamSizeBytes() {
        return this.nonVolatileProgramRamSizeBytes;
    }

    public int getNonVolatileCharacterRamSizeBytes() {
        return this.nonVolatileCharacterRamSizeBytes;
    }

    public boolean getNametableArrangement() {
        return this.nametableArrangement;
    }

    public boolean hasBattery() {
        return this.hasBattery;
    }

    public boolean hasAlternativeNametableLayout() {
        return this.hasAlternativeNametableLayout;
    }

    public NESEmulator.TVSystem getTVSystem() {
        return this.tvSystem;
    }

    public static <E extends NESEmulator> INESFile getINESFile(E emulator, byte[] file) {
        try {
            int maskedByte7 = (int) file[7] & 0x0C;
            boolean bytes12To15AreZero = true;
            for (int i = 12; i <= 15; i++) {
                if (((int) file[i] & 0xFF) != 0) {
                    bytes12To15AreZero = false;
                    break;
                }
            }

            boolean hasByeTrainer = (((int) file[6] & 0xFF) & (1 << 2)) != 0;
            int programRomSizeBytes = NES20File.parseNes20ProgramRomSizeBytes(file);
            int characterRomSizeBytes = NES20File.parseNes20CharacterRomSizeBytes(file);

            int finalIndex = 16;
            if (hasByeTrainer) {
                finalIndex += 512;
            }

            finalIndex += programRomSizeBytes;
            finalIndex += characterRomSizeBytes;

            if (maskedByte7 == 0x08 && finalIndex <= file.length) {
                return new NES20File(emulator, file);
            } else if (maskedByte7 == 0x04) {
                return new INESFile(emulator, file);
            } else if (maskedByte7 == 0x00 && bytes12To15AreZero) {
                return new ExtendedINESFile(emulator, file);
            } else {
                return new INESFile(emulator, file);
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EmulatorException("Error initializing from iNES file!", e);
        }
    }

}
