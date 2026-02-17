package io.github.arkosammy12.jemu.application.config.settings;

import io.github.arkosammy12.jemu.application.audio.AudioRenderer;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.backend.common.SystemHost;
import io.github.arkosammy12.jemu.backend.drivers.VideoDriver;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.application.util.System;

import java.nio.file.Files;
import java.nio.file.Path;

public interface EmulatorSettings extends SystemHost {

    System getSystem();

    Emulator getEmulator();

    void setVideoDriver(VideoDriver videoDriver);

     void setAudioDriver(AudioRenderer audioDriver);

    static int[] loadRom(byte[] rawRom) {
        int[] rom = new int[rawRom.length];
        for (int i = 0; i < rom.length; i++) {
            rom[i] = rawRom[i] & 0xFF;
        }
        return rom;
    }

    static byte[] readRawRom(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new EmulatorException("Failed to read ROM file from path: " + path, e);
        }
    }

}
