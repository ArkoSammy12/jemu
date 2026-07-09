package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import io.github.arkosammy12.jemu.core.gameboy.mbcs.*;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

public abstract class GameBoyCartridge implements Bus {

    public static final int CARTRIDGE_TYPE_ADDRESS = 0x0147;
    public static final int ROM_SIZE_ADDRESS = 0x0148;
    public static final int RAM_SIZE_ADDRESS = 0x0149;

    private final GameBoyEmulator emulator;

    protected final int cartridgeType;
    protected final int romSizeHeader;
    protected final int ramSizeHeader;

    protected final byte[] rom;
    protected final byte @Nullable [] sram;

    protected final int romAddressMask;
    protected final int ramAddressMask;

    public GameBoyCartridge(GameBoyEmulator emulator, int cartridgeType, byte[] romImage) {
        this.emulator = emulator;
        Optional<byte[]> optionalROM = emulator.getHost().getRom();
        if (optionalROM.isEmpty()) {
            throw new MissingROMException(emulator.getHost().getSystemName());
        }
        this.cartridgeType = cartridgeType;
        this.romSizeHeader = romImage[ROM_SIZE_ADDRESS];
        this.ramSizeHeader = romImage[RAM_SIZE_ADDRESS];

        this.rom = new byte[this.getROMLength()];
        OptionalInt sramLength = this.getSRAMLength();
        this.sram = sramLength.isPresent() ? new byte[sramLength.getAsInt()] : null;

        this.romAddressMask = getBitMaskForLength(this.rom.length);
        this.ramAddressMask = this.sram == null ? 0 : getBitMaskForLength(this.sram.length);

        try {
            System.arraycopy(romImage, 0, rom, 0, rom.length);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ROMInitializationException("ROM file size mismatch: expected %d bytes, got %d bytes!".formatted(rom.length, romImage.length));
        } catch (Exception e) {
            throw new ROMInitializationException("Error initializing GameBoy cartridge ROM!", e);
        }

        if (this.hasBattery()) {
            this.readSaveData().ifPresent(this::restoreSaveData);
        }

    }

    public static GameBoyCartridge getCartridge(GameBoyEmulator emulator) {
        Optional<byte[]> optionalROM = emulator.getHost().getRom();
        if (optionalROM.isEmpty()) {
            throw new MissingROMException(emulator.getHost().getSystemName());
        }
        byte[] rom = optionalROM.get();
        int cartridgeType = SystemHost.byteToIntArray(rom)[CARTRIDGE_TYPE_ADDRESS];
        return switch (cartridgeType) {
            case 0x00, 0x08, 0x09 -> new MBC0(emulator, cartridgeType, rom);
            case 0x01, 0x02, 0x03 -> new MBC1(emulator, cartridgeType, rom);
            case 0x05, 0x06 -> new MBC2(emulator, cartridgeType, rom);
            case 0x0F, 0x10 -> new RTCMBC3(emulator, cartridgeType, rom);
            case 0x11, 0x12, 0x13 -> new MBC3(emulator, cartridgeType, rom);
            case 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E -> new MBC5(emulator, cartridgeType, rom);
            default -> throw new ROMInitializationException("Unimplemented GameBoy cartridge type %04X!".formatted(cartridgeType));
        };
    }

    protected abstract int getROMLength();

    protected abstract OptionalInt getSRAMLength();

    protected abstract boolean hasBattery();

    protected void restoreSaveData(byte[] saveData) {
        if (this.sram == null) {
            return;
        }
        try {
            System.arraycopy(saveData, 0, this.sram, 0, sram.length);
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.error("Save data size mismatch for GameBoy cartridge: expected {} bytes, got {} bytes", sram.length, saveData.length);
        } catch (Exception e) {
            Logger.error("Error reading save data for GameBoy cartridge: {}", e);
        }
    }

    public void cycle() {

    }

    public void save() {
        Optional<byte[]> saveDataOptional = this.getSaveData();
        if (saveDataOptional.isEmpty()) {
            return;
        }

        Optional<Path> optionalSaveDataDirectory = this.emulator.getHost().getSaveDataDirectory();
        if (optionalSaveDataDirectory.isEmpty()) {
            Logger.warn("Cannot save GameBoy cartridge save data because no save data directory was provided!");
            return;
        }
        byte[] saveData = saveDataOptional.get();

        Path saveDataDirectory = optionalSaveDataDirectory.get();
        String romName = FilenameUtils.getBaseName(this.emulator.getHost().getRomPath().toString());
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
            bytes[i] = (byte) ((int) saveData[i] & 0xFF);
        }

        try {
            Files.write(saveDataFilePath, bytes);
        } catch (IOException e) {
            Logger.error("Error writing save data for GameBoy system cartridge: {}", e);
        }
    }

    protected final Optional<byte[]> readSaveData() {
        Optional<Path> optionalSaveDataDirectory = this.emulator.getHost().getSaveDataDirectory();
        if (optionalSaveDataDirectory.isEmpty()) {
            Logger.warn("Cannot read GameBoy cartridge save data because no save data directory was provided!");
            return Optional.empty();
        }
        Path saveDataDirectory = optionalSaveDataDirectory.get();
        String romName = FilenameUtils.getBaseName(this.emulator.getHost().getRomPath().toString());
        Path saveDataFilePath = saveDataDirectory.resolve("%s.sav".formatted(romName));
        try {
            return Optional.of(Files.readAllBytes(saveDataFilePath));
        } catch (NoSuchFileException e) {
            Logger.warn("Save data for GameBoy ROM file %s not found!".formatted(saveDataFilePath));
            return Optional.empty();
        } catch (IOException e) {
            Logger.error("Error reading save data for GameBoy ROM file: {}", e);
            return Optional.empty();
        }
    }

    protected Optional<byte[]> getSaveData() {
        return Optional.ofNullable(this.hasBattery() ? this.sram : null);
    }

    protected int getBitMaskForLength(int length) {
        return ((1 << (32 - Integer.numberOfLeadingZeros(length))) - 1) >> 1;
    }

}
