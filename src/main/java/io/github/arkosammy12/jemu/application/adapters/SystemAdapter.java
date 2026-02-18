package io.github.arkosammy12.jemu.application.adapters;

import io.github.arkosammy12.jemu.application.util.System;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.backend.common.SystemHost;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;

public interface SystemAdapter extends SystemHost, Closeable {

    byte[] getRom();

    System getSystem();

    Emulator getEmulator();

    static byte[] readRawRom(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new EmulatorException("Failed to read ROM file from path: " + path, e);
        }
    }

}
