package io.github.arkosammy12.jemu.app.system.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.adapters.GameBoyAdapter;
import io.github.arkosammy12.jemu.app.system.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;

import javax.sound.sampled.LineUnavailableException;
import java.util.Collection;
import java.util.List;

public class GameBoyManager extends SystemManager {

    private final GameBoyHost.Model gameboyModel;

    public GameBoyManager(Jemu jemu, SystemRegistry systemRegistry, GameBoyHost.Model gameboyModel) {
        super(jemu, systemRegistry);
        this.gameboyModel = gameboyModel;
    }

    @Override
    public SystemAdapter createSystem() throws LineUnavailableException {
        return new GameBoyAdapter(jemu, this.gameboyModel, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof GameBoyAdapter gameBoyAdapter && this.gameboyModel == gameBoyAdapter.getModel();
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
