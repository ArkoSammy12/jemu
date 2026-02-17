package io.github.arkosammy12.jemu.application.config.settings;

import io.github.arkosammy12.jemu.application.audio.AudioRenderer;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import io.github.arkosammy12.jemu.backend.drivers.VideoDriver;
import io.github.arkosammy12.jemu.application.config.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;

import java.util.Arrays;
import java.util.Optional;

public abstract class AbstractEmulatorSettings implements EmulatorSettings {

    private final byte[] rom;
    private VideoDriver videoDriver;
    private AudioDriver audioDriver;

    public AbstractEmulatorSettings(CoreInitializer initializer) {

        Optional<byte[]> rawRomOptional = initializer.getRawRom();

        if (rawRomOptional.isEmpty()) {
            throw new EmulatorException("Must select a ROM file before starting emulation!");
        }

        byte[] rom = rawRomOptional.get();
        this.rom = Arrays.copyOf(rom, rom.length);

    }

    @Override
    public byte[] getRom() {
        return Arrays.copyOf(this.rom, this.rom.length);
    }

    @Override
    public Optional<VideoDriver> getVideoDriver() {
        return Optional.ofNullable(this.videoDriver);
    }

    @Override
    public Optional<AudioDriver> getAudioDriver() {
        return Optional.ofNullable(this.audioDriver);
    }

    @Override
    public void setVideoDriver(VideoDriver videoDriver) {
        this.videoDriver = videoDriver;
    }

    @Override
    public void setAudioDriver(AudioRenderer audioDriver) {
        this.audioDriver = audioDriver;
    }

}
