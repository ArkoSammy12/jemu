package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.nes.ines.NES20File;
import io.github.arkosammy12.jemu.core.nes.mappers.*;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class NESCartridge<E extends NESEmulator> implements Bus {

    protected final E emulator;
    protected final INESFile iNESFile;
    protected final NametableArrangement iNESFileNametableArrangement;

    private final byte[] vram;

    public NESCartridge(E emulator, INESFile iNESFile) {
        this.emulator = emulator;
        this.iNESFile = iNESFile;
        this.iNESFileNametableArrangement = this.iNESFile.getNametableArrangement() ? NESCartridge.NametableArrangement.HORIZONTAL : NESCartridge.NametableArrangement.VERTICAL;
        this.vram = new byte[switch (this.getVRAMSize()) {
            case KB_2 -> 0x800;
            case KB_4 -> 0x1000;
        }];
        this.iNESFile.getTrainer().ifPresent(trainer -> {
            for (byte value : trainer) {
                this.writeByte(0x7000, value);
            }
        });
    }

    public static <E extends NESEmulator> NESCartridge<E> getCartridge(E emulator, INESFile iNESFile) {
        int mapperNumber = iNESFile.getMapperNumber();
        int subMapperNumber = iNESFile.getSubmapperNumber();
        return switch (mapperNumber) {
            case 0 -> new NROMCartridge<>(emulator, iNESFile);
            case 1 -> new MMC1Cartridge<>(emulator, iNESFile);
            case 2 -> new UxROMCartridge<>(emulator, iNESFile);
            case 3 -> new CNROMCartridge<>(emulator, iNESFile);
            case 4 -> {
                if (iNESFile.getSubmapperNumber() == 1) {
                    yield new MMC6Cartridge<>(emulator, iNESFile);
                } else {
                    yield new MMC3Cartridge<>(emulator, iNESFile);
                }
            }
            case 7 -> new AxROMCartridge<>(emulator, iNESFile);
            case 9 -> new MMC2Cartridge<>(emulator, iNESFile);
            case 10 -> new MMC4Cartridge<>(emulator, iNESFile);
            // TODO: For VRC2 and VRC4 iNES compatibility, place registers in two places to satisfy both submapper possibilities for a single iNES mapper
            case 21 -> switch (subMapperNumber) {
                case 1, 2 -> new VRC4Cartridge<>(emulator, iNESFile);
                default -> throw new ROMInitializationException("Invalid iNES mapper %d and submapper number %d combination!".formatted(mapperNumber, subMapperNumber));
            };
            case 22 -> {
                if (subMapperNumber == 0) {
                    yield new VRC2Cartridge<>(emulator, iNESFile);
                } else {
                    throw new ROMInitializationException("Invalid iNES mapper %d and submapper number %d combination!".formatted(mapperNumber, subMapperNumber));
                }
            }
            case 23, 25 -> switch (subMapperNumber) {
                case 1, 2 -> new VRC4Cartridge<>(emulator, iNESFile);
                case 3 -> new VRC2Cartridge<>(emulator, iNESFile);
                default -> throw new ROMInitializationException("Invalid iNES mapper %d and submapper number %d combination!".formatted(mapperNumber, subMapperNumber));
            };
            case 24, 26 -> new VRC6Cartridge<>(emulator, iNESFile);
            case 71 -> new INESMapper71Cartridge<>(emulator, iNESFile);
            case 218 -> new INESMapper218Cartridge<>(emulator, iNESFile);
            default -> throw new ROMInitializationException("Unimplemented iNES mapper number %d!".formatted(mapperNumber));
        };
    }

    public INESFile getINESFile() {
        return this.iNESFile;
    }

    abstract public int readBytePPU(int address);

    abstract public void writeBytePPU(int address, int value);

    protected VRAMSize getVRAMSize() {
        return VRAMSize.KB_2;
    }

    public void onPPUHalfDot() {

    }

    public void observePPUAddress(int address) {

    }

    public double mixAPUAudio(double apuOutput) {
        return apuOutput;
    }

    protected int readByteVRAM(int address) {
        return (int) this.vram[address] & 0xFF;
    }

    protected void writeByteVRAM(int address, int value) {
        this.vram[address] = (byte) value;
    }

    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, this.iNESFileNametableArrangement);
    }

    protected int mapNametableAddress(int address, NametableArrangement nametableArrangement) {
        return switch (nametableArrangement) {
            case HORIZONTAL -> (address & (1 << 10)) | (address & 0x3FF);
            case VERTICAL -> ((address & (1 << 11)) >>> 1) | (address & 0x3FF);
            case SINGLE_SCREEN_LOWER_BANK -> address & 0x3FF;
            case SINGLE_SCREEN_UPPER_BANK -> 0x400 | (address & 0x3FF);
            case FOUR_SCREEN -> address & 0xFFF;
        };
    }

    public void cycle() {

    }

    public boolean getIRQSignal() {
        return false;
    }

    public void save() {
        Optional<Path> optionalSaveDataDirectory = this.emulator.getHost().getSaveDataDirectory();
        if (optionalSaveDataDirectory.isEmpty()) {
            Logger.warn("Cannot save NES cartridge save data because no save data directory was provided!");
            return;
        }
        Path saveDataDirectory = optionalSaveDataDirectory.get();
        String romName = FilenameUtils.getBaseName(this.emulator.getHost().getRomPath().toString());
        if (!Files.exists(saveDataDirectory)) {
            try {
                Files.createDirectory(saveDataDirectory);
            } catch (IOException e) {
                Logger.error("Error creating save data directory for NES system cartridge: {}", e);
                return;
            }
        }

        if (this.savePrgRam()) {
            this.getNonVolatilePrgRam().ifPresent(programRAM -> {
                Path prgRamSaveDataFilePath = saveDataDirectory.resolve("%s.sav".formatted(romName));
                try {
                    Files.write(prgRamSaveDataFilePath, Arrays.copyOf(programRAM, programRAM.length));
                } catch (IOException e) {
                    Logger.error("Error writing save PRG-RAM for NES system cartridge: {}", e);
                }
            });
        }

        if (this.saveChrRam()) {
            this.getNonVolatileChrRam().ifPresent(characterRAM -> {
                Path chrRamSaveDataFilePath = saveDataDirectory.resolve("%s.chr.sav".formatted(romName));
                try {
                    Files.write(chrRamSaveDataFilePath, Arrays.copyOf(characterRAM, characterRAM.length));
                } catch (IOException e) {
                    Logger.error("Error writing save CHR-RAM for NES system cartridge: {}", e);
                }
            });
        }
    }

    protected Optional<SaveData> readSaveData() {
        boolean savePrgRam = this.savePrgRam();
        boolean saveChrRam = this.saveChrRam();

        if (!savePrgRam && !saveChrRam) {
            return Optional.empty();
        }

        Optional<Path> optionalSaveDataDirectory = this.emulator.getHost().getSaveDataDirectory();
        if (optionalSaveDataDirectory.isEmpty()) {
            Logger.warn("Cannot read NES cartridge save data because no save data directory was provided!");
            return Optional.empty();
        }
        Path saveDataDirectory = optionalSaveDataDirectory.get();
        String romName = FilenameUtils.getBaseName(this.emulator.getHost().getRomPath().toString());

        byte[] prgRam = null;
        byte[] chrRam = null;

        if (savePrgRam) {
            Path prgRamSaveDataFilePath = saveDataDirectory.resolve("%s.sav".formatted(romName));
            try {
                prgRam = Files.readAllBytes(prgRamSaveDataFilePath);
            } catch (NoSuchFileException e) {
                Logger.warn("PRG-RAM save data for NES ROM file %s not found!".formatted(prgRamSaveDataFilePath));
            } catch (IOException e) {
                Logger.error("Error reading PRG-RAM save data for NES ROM file: {}", e);
            }
        }

        if (saveChrRam) {
            Path chrRamSaveDataFilePath = saveDataDirectory.resolve("%s.chr.sav".formatted(romName));
            try {
                chrRam = Files.readAllBytes(chrRamSaveDataFilePath);
            } catch (NoSuchFileException e) {
                Logger.warn("CHR-RAM save data for NES ROM file %s not found!".formatted(chrRamSaveDataFilePath));
            } catch (IOException e) {
                Logger.error("Error reading CHR-RAM save data for NES ROM file: {}", e);
            }
        }

        if (prgRam == null && chrRam == null) {
            return Optional.empty();
        } else {
            return Optional.of(new SaveData(prgRam, chrRam));
        }
    }

    protected void restoreSaveData(byte @Nullable [] programRAM, byte @Nullable [] characterRAM) {
        this.readSaveData().ifPresent(saveData -> {
            if (saveData.prgRam() != null && programRAM != null) {
                try {
                    System.arraycopy(saveData.prgRam(), 0, programRAM, 0, programRAM.length);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Logger.error("Error attempting to restore saved PRG-RAM (expected %d bytes, but found %d bytes instead)!".formatted(programRAM.length, saveData.prgRam().length));
                } catch (Exception e) {
                    Logger.error("Error attempting to restore saved PRG-RAM: {}", e);
                }
            }
            if (saveData.chrRam() != null && characterRAM != null) {
                try {
                    System.arraycopy(saveData.chrRam(), 0, characterRAM, 0, characterRAM.length);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Logger.error("Error attempting to restore saved CHR-RAM (expected %d bytes, but found %d bytes instead)!".formatted(characterRAM.length, saveData.chrRam().length));
                } catch (Exception e) {
                    Logger.error("Error attempting to restore saved CHR-RAM: {}", e);
                }
            }
        });
    }

    // TODO: Take into account that on some mappers not all of PRG-RAM or CHR-RAM should be saved.
    // >There are only a few mappers where this is a thing, and those have a canonical ordering.
    // >Otherwise, you should expect that a mapper will have only one type and you should probably reject ROMs that specify both.
    //
    // Fiskbit

    protected Optional<byte[]> getNonVolatilePrgRam() {
        return Optional.empty();
    }

    protected Optional<byte[]> getNonVolatileChrRam() {
        return Optional.empty();
    }

    protected boolean savePrgRam() {
        if (this.iNESFile instanceof NES20File nes20File) {
            return nes20File.getNonVolatileProgramRamSizeBytes() > 0;
        } else {
            return this.iNESFile.hasBattery();
        }
    }

    protected boolean saveChrRam() {
        // TODO: Check if the battery bit is set in case this is the RacerMate mapper, as it does save its CHR-RAM if so
        return this.iNESFile instanceof NES20File nes20File && nes20File.getNonVolatileCharacterRamSizeBytes() > 0;
    }

    public enum NametableArrangement {
        HORIZONTAL,
        VERTICAL,
        SINGLE_SCREEN_LOWER_BANK,
        SINGLE_SCREEN_UPPER_BANK,
        FOUR_SCREEN
    }

    protected enum VRAMSize {
        KB_2,
        KB_4
    }

    protected record SaveData(byte @Nullable [] prgRam, byte @Nullable [] chrRam) {}

}
