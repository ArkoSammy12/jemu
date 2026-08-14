package io.github.arkosammy12.jemu.app.system.gameboy;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.util.FrameRequesterVideoEvent;
import io.github.arkosammy12.jemu.app.util.exceptions.SystemRedirectException;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.nio.file.Path;
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
    public SystemAdapter createSystem(boolean detectedAutomatically) throws LineUnavailableException {
        if (detectedAutomatically && this.getEmulationSettings().preferGameBoyColor() && this.gameboyModel != GameBoyHost.Model.CGB) {
            throw new SystemRedirectException(this.systemRegistry.getGameBoyColorManager());
        }
        return new GameBoyAdapter(jemu, this, this.gameboyModel);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof GameBoyAdapter gameBoyAdapter && this.gameboyModel == gameBoyAdapter.getModel();
    }

    GameBoySettings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getGameBoySettings();
    }

    private Optional<GameBoyMenuBarSettings> getMenuBarSettings() {
        return Optional.ofNullable(this.gameBoyMenuBarSettings);
    }

    private Optional<GameBoyPanelSettings> getPanelSettings() {
        return Optional.ofNullable(this.gameBoyPanelSettings);
    }

    @Override
    public Optional<? extends Function<? super EventPublisher, ? extends JMenu>> getSettingsMenuBarContents() {
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
    public Optional<? extends Function<? super EventPublisher, ? extends JPanel>> getSettingsWindowContents() {
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
    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangedEvent);
        this.getMenuBarSettings().ifPresent(gameBoyMenuBarSettings -> gameBoyMenuBarSettings.onEvent(coreSettingChangedEvent));
        this.getPanelSettings().ifPresent(gameBoyPanelSettings -> gameBoyPanelSettings.onEvent(coreSettingChangedEvent));
        switch (coreSettingChangedEvent) {
            case PreferGameBoyColorSettingChangedEvent(boolean preferGameBoyColor) -> this.getEmulationSettings().setPreferGameBoyColor(preferGameBoyColor);
            case DMGPaletteSettingChangedEvent(GameBoySettings.DMGPalette dmgPalette) -> this.getEmulationSettings().setDMGPalette(dmgPalette);
            case UseBuiltInBootRomSettingChangedEvent(boolean useBuiltInBootRom) -> this.getEmulationSettings().setUseBuiltInBootROM(useBuiltInBootRom);
            case GameBoyBootRomPathChangedEvent(@Nullable Path path) -> this.getEmulationSettings().setGameBoyBootRomPath(path);
            case GameBoyColorBootRomPathChangedEvent(@Nullable Path path) -> this.getEmulationSettings().setGameBoyColorBootROMPath(path);
            default -> {}
        }
    }

    record PreferGameBoyColorSettingChangedEvent(boolean preferGameBoyColor) implements CoreSettingChangedEvent, Supplier<Boolean> {

        @Override
        public Boolean get() {
            return this.preferGameBoyColor();
        }

    }

    record DMGPaletteSettingChangedEvent(GameBoySettings.DMGPalette dmgPalette) implements CoreSettingChangedEvent, FrameRequesterVideoEvent, Supplier<GameBoySettings.DMGPalette> {

        @Override
        public GameBoySettings.DMGPalette get() {
            return this.dmgPalette();
        }

    }

    record UseBuiltInBootRomSettingChangedEvent(boolean useBuiltInBootRom) implements CoreSettingChangedEvent, Supplier<Boolean> {

        @Override
        public Boolean get() {
            return this.useBuiltInBootRom();
        }

    }

    record GameBoyBootRomPathChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }

    record GameBoyColorBootRomPathChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }


}
