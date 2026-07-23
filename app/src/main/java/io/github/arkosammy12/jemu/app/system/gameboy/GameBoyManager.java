package io.github.arkosammy12.jemu.app.system.gameboy;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.util.FrameRequesterVideoEvent;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.EmulationSettingsBuilder;

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

    @Override
    public EmulationSettingsBuilder buildSystemSettings(EmulationSettingsBuilder emulationSettingsBuilder) {
        EmulationSettingsBuilder builder = super.buildSystemSettings(emulationSettingsBuilder);
        if (this.gameboyModel != GameBoyHost.Model.DMG) {
            return builder;
        }
        return builder.addSection("Game Boy", section -> {
            section.addEnumSetting("Palette", this.getEmulationSettings().getDMGPalette(), DMGPaletteSettingChangedEvent::new);
        });
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangeEvent);
        if (coreSettingChangeEvent instanceof DMGPaletteSettingChangedEvent(GameBoySettings.DMGPalette dmgPalette)) {
            this.getEmulationSettings().setDMGPalette(dmgPalette);
        }
    }

    public record DMGPaletteSettingChangedEvent(GameBoySettings.DMGPalette dmgPalette) implements CoreSettingChangeEvent, FrameRequesterVideoEvent {}


}
