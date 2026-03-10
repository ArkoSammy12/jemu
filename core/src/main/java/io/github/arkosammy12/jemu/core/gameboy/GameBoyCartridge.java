package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.apache.commons.io.FilenameUtils;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class GameBoyCartridge implements Bus {

    public static final int CARTRIDGE_TYPE_ADDRESS = 0x0147;
    public static final int ROM_SIZE_ADDRESS = 0x0148;
    public static final int RAM_SIZE_ADDRESS = 0x0149;

    private final GameBoyEmulator gameBoyEmulator;
    protected final int[] originalRom;

    protected final int cartridgeType;
    protected final int romSizeHeader;
    protected final int ramSizeHeader;

    public GameBoyCartridge(GameBoyEmulator emulator, int cartridgeType) {
        this.gameBoyEmulator = emulator;
        int[] rom = SystemHost.byteToIntArray(emulator.getHost().getRom());
        this.originalRom = Arrays.copyOf(rom, rom.length);
        this.cartridgeType = cartridgeType;
        this.romSizeHeader = rom[ROM_SIZE_ADDRESS];
        this.ramSizeHeader = rom[RAM_SIZE_ADDRESS];
    }

    public static GameBoyCartridge getCartridge(GameBoyEmulator emulator) {
        int cartridgeType = SystemHost.byteToIntArray(emulator.getHost().getRom())[CARTRIDGE_TYPE_ADDRESS];
        return switch (cartridgeType) {
            case 0x00, 0x08, 0x09 -> new MBC0(emulator, cartridgeType);
            case 0x01, 0x02, 0x03 -> new MBC1(emulator, cartridgeType);
            case 0x05, 0x06 -> new MBC2(emulator, cartridgeType);
            case 0x0F, 0x10 -> new RTCMBC3(emulator, cartridgeType);
            case 0x11, 0x12, 0x13 -> new MBC3(emulator, cartridgeType);
            case 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E -> new MBC5(emulator, cartridgeType);
            default -> throw new EmulatorException("Unimplemented GameBoy cartridge type %04X!".formatted(cartridgeType));
        };
    }

    public void cycle() {

    }

    protected final Optional<int[]> readSaveData() {
        Path saveDataDirectory = this.gameBoyEmulator.getHost().getSaveDataDirectory();
        String romName = FilenameUtils.getBaseName(this.gameBoyEmulator.getHost().getRomPath().toString());
        Path saveDataFilePath = saveDataDirectory.resolve("%s.sav".formatted(romName));
        try {
            byte[] bytes = Files.readAllBytes(saveDataFilePath);
            int[] saveData = SystemHost.byteToIntArray(bytes);
            return Optional.of(saveData);
        } catch (NoSuchFileException e) {
            Logger.warn("Save data for GameBoy ROM file %s not found!".formatted(saveDataFilePath));
            return Optional.empty();
        } catch (IOException e) {
            Logger.error("Error reading save data for GameBoy ROM file: {}", e);
            return Optional.empty();
        }
    }

    public void save() {
        Optional<int[]> saveDataOptional = this.getSaveData();
        if (saveDataOptional.isEmpty()) {
            return;
        }
        int[] saveData = saveDataOptional.get();

        Path saveDataDirectory = this.gameBoyEmulator.getHost().getSaveDataDirectory();
        String romName = FilenameUtils.getBaseName(this.gameBoyEmulator.getHost().getRomPath().toString());
        if (!Files.exists(saveDataDirectory)) {
            try {
                Files.createDirectory(saveDataDirectory);
            } catch (IOException e) {
                Logger.error("Error creating save data directory for GameBoy system cartridge: {}", e);
                return;
            }
        }
        Path saveDataFilePath = saveDataDirectory.resolve("%s.sav".formatted(romName));
        byte[] bytes = new byte[saveData.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (saveData[i] & 0xFF);
        }

        try {
            Files.write(saveDataFilePath, bytes);
        } catch (IOException e) {
            Logger.error("Error writing save data for GameBoy system cartridge: {}", e);
        }
    }

    protected Optional<int[]> getSaveData() {
        return Optional.empty();
    }

}
