package io.github.arkosammy12.jemu.app.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.GameBoyAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;

import javax.sound.sampled.LineUnavailableException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class GameBoyManager implements SystemManager {

    private final GameBoyHost.Model gameboyModel;

    public GameBoyManager(GameBoyHost.Model gameboyModel) {
        this.gameboyModel = gameboyModel;
    }

    @Override
    public SystemAdapter createSystem(Jemu jemu, System system) throws LineUnavailableException {
        return new GameBoyAdapter(jemu, system, this.gameboyModel, this);
    }

    @Override
    public String getName() {
        return switch (this.gameboyModel) {
            case DMG -> "Game Boy";
            case CGB -> "Game Boy Color";
        };
    }

    @Override
    public String getId() {
        return switch (this.gameboyModel) {
            case DMG -> "gameboy";
            case CGB -> "gameboy-color";
        };
    }

    @Override
    public Collection<String> getFileExtensions() {
        return switch (this.gameboyModel) {
            case DMG -> List.of("gb");
            case CGB -> List.of("gbc");
        };
    }
}
