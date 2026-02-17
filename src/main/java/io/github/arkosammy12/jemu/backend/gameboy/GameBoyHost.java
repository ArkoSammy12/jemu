package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.SystemHost;

public interface GameBoyHost extends SystemHost {

    Model getModel();

    enum Model {
        DMG,
        CGB,
    }

}
