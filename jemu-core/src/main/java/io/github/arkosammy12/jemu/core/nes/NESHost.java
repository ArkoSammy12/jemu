package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.nes.ines.CartridgeInfo;

import java.nio.file.Path;
import java.util.Optional;

public interface NESHost extends SystemHost {

    Optional<Path> getSaveDataDirectory();

    Optional<CartridgeInfo> getExternalCartridgeInfo(int totalRomSize, boolean hasByteTrainer);

}
