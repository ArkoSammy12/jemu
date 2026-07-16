package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.SystemHost;

import java.util.Optional;

public interface Atari2600SystemHost extends SystemHost {

    Optional<CartridgeInfo> getCartridgeInfo();

    interface CartridgeInfo {

        Optional<Atari2600Cartridge.Type> getCartridgeType();

        Optional<Atari2600Emulator.TVFormat> getTVFormat();

    }

}
