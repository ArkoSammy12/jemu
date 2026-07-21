package io.github.arkosammy12.jemu.app.system.managers;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.adapters.GameBoyAdapter;
import io.github.arkosammy12.jemu.app.system.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.FrameRequesterVideoEvent;
import io.github.arkosammy12.jemu.core.gameboy.DMGPPU;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.EmulationSettingsBuilder;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

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

    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangeEvent);
        if (coreSettingChangeEvent instanceof DMGPaletteSettingChangedEvent(DMGPalette dmgPalette)) {
            this.getEmulationSettings().setDMGPalette(dmgPalette);
        }
    }

    public EmulationSettings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getGameBoySettings();
    }

    public record DMGPaletteSettingChangedEvent(DMGPalette dmgPalette) implements CoreSettingChangeEvent, FrameRequesterVideoEvent {}

    public enum DMGPalette implements DisplayNamerProvider {
        @SerializedName("gameboy_green")
        DMG_GREEN("Game Boy Green", DMGPPU.Palette.DMG_GREEN),

        @SerializedName("greyscale")
        GREYSCALE("Greyscale", DMGPPU.Palette.GREYSCALE),

        @SerializedName("sameboy")
        SAMEBOY("SameBoy", DMGPPU.Palette.SAMEBOY)
        ;

        private final String displayName;
        private final DMGPPU.Palette dmgPalette;

        DMGPalette(String displayName, DMGPPU.Palette dmgPalette) {
            this.displayName = displayName;
            this.dmgPalette = dmgPalette;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public DMGPPU.Palette mapToHost() {
            return this.dmgPalette;
        }

    }

    public static class EmulationSettings {

        @SerializedName("palette")
        private volatile DMGPalette dmgPalette = DMGPalette.DMG_GREEN;

        private void setDMGPalette(DMGPalette dmgPalette) {
            this.dmgPalette = dmgPalette;
        }

        public DMGPalette getDMGPalette() {
            return this.dmgPalette;
        }

    }

}
