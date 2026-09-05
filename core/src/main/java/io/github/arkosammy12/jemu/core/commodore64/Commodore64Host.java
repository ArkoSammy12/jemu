package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.SystemHost;

import java.nio.file.Path;
import java.util.Optional;

public interface Commodore64Host extends SystemHost {

    Optional<Path> getKernalROMPath();

    Optional<Path> getBASICRomPath();

    Optional<Path> getCharacterROMPath();

    int getRGB8ForPaletteIndex(int paletteIndex);

    default void onPrgFilePatched() {

    }

    default boolean isDebugCartEnabled() {
        return false;
    }

    default void onDebugCartWrite(int value) {

    }

}
