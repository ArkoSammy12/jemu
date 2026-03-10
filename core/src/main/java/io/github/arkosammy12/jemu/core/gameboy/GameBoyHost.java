package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.SystemHost;

import java.nio.file.Path;

public interface GameBoyHost extends SystemHost {

    Model getModel();

    Path getSaveDataDirectory();

    enum Model {
        DMG,
        CGB,
    }

}
