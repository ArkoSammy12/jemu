package io.github.arkosammy12.jemu.config.settings;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.Emulator;
import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.util.DisplayAngle;
import io.github.arkosammy12.jemu.util.System;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public interface EmulatorSettings {

    Jemu getJchip();

    int[] getRom();

    Optional<String> getRomTitle();

    DisplayAngle getDisplayAngle();

    System getSystem();

    Emulator getEmulator();

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
