package io.github.arkosammy12.jemu.app.system.gameboy;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.util.FrameRequesterVideoEvent;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class GameBoyManager extends SystemManager {

    private final GameBoyHost.Model gameboyModel;

    @Nullable
    private volatile GameBoyMenuBarSettings gameBoyMenuBarSettings;

    @Nullable
    private volatile GameBoyPanelSettings gameBoyPanelSettings;

    public GameBoyManager(Jemu jemu, SystemRegistry systemRegistry, GameBoyHost.Model gameboyModel) {
        super(jemu, systemRegistry);
        this.gameboyModel = gameboyModel;
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

    @Override
    public SystemAdapter createSystem() throws LineUnavailableException {
        return new GameBoyAdapter(jemu, this, this.gameboyModel);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof GameBoyAdapter gameBoyAdapter && this.gameboyModel == gameBoyAdapter.getModel();
    }

    public GameBoySettings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getGameBoySettings();
    }

    private Optional<GameBoyMenuBarSettings> getMenuBarSettings() {
        return Optional.ofNullable(this.gameBoyMenuBarSettings);
    }

    private Optional<GameBoyPanelSettings> getPanelSettings() {
        return Optional.ofNullable(this.gameBoyPanelSettings);
    }

    @Override
    public Optional<? extends Function<? super MainWindow, ? extends JMenu>> getSettingsMenuBarContents() {
        if (this.gameboyModel == GameBoyHost.Model.DMG) {
            return Optional.of(mainWindow -> {
                GameBoyMenuBarSettings gameBoyMenuBarSettings = new GameBoyMenuBarSettings(this, mainWindow);
                this.gameBoyMenuBarSettings = gameBoyMenuBarSettings;
                return gameBoyMenuBarSettings;
            });
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<? extends Function<? super MainWindow, ? extends JPanel>> getSettingsWindowContents() {
        if (this.gameboyModel == GameBoyHost.Model.DMG) {
            return Optional.of(mainWindow -> {
                GameBoyPanelSettings gameBoyPanelSettings = new GameBoyPanelSettings(this, mainWindow);
                this.gameBoyPanelSettings = gameBoyPanelSettings;
                return gameBoyPanelSettings;
            });
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangeEvent);
        this.getMenuBarSettings().ifPresent(gameBoyMenuBarSettings -> gameBoyMenuBarSettings.onEvent(coreSettingChangeEvent));
        this.getPanelSettings().ifPresent(gameBoyPanelSettings -> gameBoyPanelSettings.onEvent(coreSettingChangeEvent));
        if (coreSettingChangeEvent instanceof DMGPaletteSettingChangedEvent(GameBoySettings.DMGPalette dmgPalette)) {
            this.getEmulationSettings().setDMGPalette(dmgPalette);
        }
    }

    public record DMGPaletteSettingChangedEvent(GameBoySettings.DMGPalette dmgPalette) implements CoreSettingChangeEvent, FrameRequesterVideoEvent, Supplier<GameBoySettings.DMGPalette> {

        @Override
        public GameBoySettings.DMGPalette get() {
            return this.dmgPalette();
        }

    }


}
