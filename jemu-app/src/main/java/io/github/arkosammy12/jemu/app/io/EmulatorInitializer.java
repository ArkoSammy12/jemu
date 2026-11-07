package io.github.arkosammy12.jemu.app.io;

import java.nio.file.Path;
import java.util.Optional;

public interface EmulatorInitializer {

    Optional<Path> getRomPath();

    Optional<byte[]> getRomImage();

}
