package io.github.arkosammy12.jemu.app.io.initializers;

import io.github.arkosammy12.jemu.app.util.System;

import java.nio.file.Path;
import java.util.Optional;

public interface CoreInitializer {

    Optional<Path> getRomPath();

    Optional<byte[]> getRawRom();

    Optional<System> getSystem();

}
