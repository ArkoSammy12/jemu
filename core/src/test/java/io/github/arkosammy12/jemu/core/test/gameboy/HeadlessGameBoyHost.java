package io.github.arkosammy12.jemu.core.test.gameboy;

import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class HeadlessGameBoyHost implements GameBoyHost {

    private final byte[] rom;
    private final Path romPath;

    HeadlessGameBoyHost(Path romPath) throws IOException {
        this.romPath = romPath;
        this.rom = Files.readAllBytes(romPath);
    }

    @Override
    public Optional<byte[]> getRom() {
        return Optional.of(this.rom);
    }

    @Override
    public Optional<Path> getRomPath() {
        return Optional.of(this.romPath);
    }

    @Override
    public String getSystemName() {
        return "GameBoy";
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.empty();
    }

    @Override
    public Optional<? extends VideoDriver> getVideoDriver() {
        return Optional.empty();
    }

    @Override
    public Optional<? extends AudioDriver> getAudioDriver() {
        return Optional.empty();
    }

    @Override
    public boolean useBuiltInBootROM() {
        return true;
    }

    @Override
    public int getRGB8ForDMGPaletteIndex(int paletteIndex) {
        return 0;
    }

    @Override
    public Optional<Path> getBootROMPath() {
        return Optional.empty();
    }

    @Override
    public Optional<Path> getSaveDataDirectory() {
        return Optional.empty();
    }

}
