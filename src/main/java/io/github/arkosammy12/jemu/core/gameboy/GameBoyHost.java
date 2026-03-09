package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.SystemHost;

public interface GameBoyHost extends SystemHost {

    Model getModel();

    enum Model {
        DMG,
        CGB,
    }

}
