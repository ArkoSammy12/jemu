package io.github.arkosammy12.jemu.config.initializers;

import io.github.arkosammy12.jemu.util.DisplayAngle;
import io.github.arkosammy12.jemu.util.KeyboardLayout;
import io.github.arkosammy12.jemu.util.System;

import java.nio.file.Path;
import java.util.Optional;

public interface CommonInitializer {

    Optional<Path> getRomPath();

    Optional<byte[]> getRawRom();

    Optional<System> getSystem();

    Optional<DisplayAngle> getDisplayAngle();

    Optional<KeyboardLayout> getKeyboardLayout();

}
