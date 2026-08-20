package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.SystemHost;

import java.nio.file.Path;
import java.util.Optional;

public interface Commodore64Host extends SystemHost {

    Optional<Path> getKernalROMPath();

    Optional<Path> getBASICRomPath();

    Optional<Path> getCharacterROMPath();

    int getRB8ForPaletteIndex(int paletteIndex);

}
