package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.SystemHost;

import java.nio.file.Path;
import java.util.Optional;

public interface NESHost extends SystemHost {

    Optional<Path> getSaveDataDirectory();

}
