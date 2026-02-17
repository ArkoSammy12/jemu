package io.github.arkosammy12.jemu.application.config.initializers;

import io.github.arkosammy12.jemu.application.util.KeyboardLayout;
import io.github.arkosammy12.jemu.application.util.System;

import java.nio.file.Path;
import java.util.Optional;

public interface CoreInitializer {

    Optional<Path> getRomPath();

    Optional<byte[]> getRawRom();

    Optional<System> getSystem();

    Optional<KeyboardLayout> getKeyboardLayout();

}
