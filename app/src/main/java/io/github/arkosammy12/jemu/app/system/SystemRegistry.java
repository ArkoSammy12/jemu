package io.github.arkosammy12.jemu.app.system;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.managers.*;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemCatalog;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.EmulationSettingsBuilder;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import picocli.CommandLine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class SystemRegistry implements SystemCatalog {

    private final List<SystemManager> systemManagers;

    @NotNull
    private volatile EmulationSettings emulationSettings = new EmulationSettings();

    public SystemRegistry(Jemu jemu) {
        this.systemManagers = List.of(
            new Chip8Manager(jemu, this, Chip8Manager.Variant.CHIP_8),
            new Chip8Manager(jemu, this, Chip8Manager.Variant.CHIP_48),
            new Chip8Manager(jemu, this, Chip8Manager.Variant.SUPER_CHIP_10),
            new Chip8Manager(jemu, this, Chip8Manager.Variant.SUPER_CHIP_11),
            new Chip8Manager(jemu, this, Chip8Manager.Variant.SUPER_CHIP_MODERN),
            new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.NONE),
            new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.CHIP_8),
            new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.CHIP_8X),
            new RCAStudioIIManager(jemu, this),
            new GameBoyManager(jemu, this, GameBoyManager.GameBoyModel.DMG),
            new GameBoyManager(jemu, this, GameBoyManager.GameBoyModel.CGB),
            new NESManager(jemu, this),
            new Atari2600Manager(jemu, this)
        );
    }

    @Override
    public Collection<SystemManager> getSystemDescriptors() {
        return this.systemManagers;
    }

    @Override
    public void buildSystemSettings(EmulationSettingsBuilder emulationSettingsBuilder) {
        this.getSystemDescriptors().forEach(systemManager -> systemManager.buildSystemSettings(emulationSettingsBuilder));
    }

    @NotNull
    public EmulationSettings getEmulationSettings() {
        return this.emulationSettings;
    }

    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {
        this.getSystemDescriptors().forEach(systemManager -> systemManager.onCoreSettingChangedEvent(coreSettingChangeEvent));
    }

    @Override
    public Optional<JsonElement> serializeSettings() {
        try {
            Gson gson = new Gson();
            return Optional.ofNullable(gson.toJsonTree(this.emulationSettings));
        } catch (Exception e) {
            Logger.error(e, "Failed to save emulation settings!");
            return Optional.empty();
        }
    }

    @Override
    public void deserializeSettings(@NotNull JsonElement settings) {
        EmulationSettings deserializedEmulationSettings = null;
        try {
            Gson gson = new Gson();
            deserializedEmulationSettings = gson.fromJson(settings, EmulationSettings.class);
        } catch (Exception e) {
            Logger.error(e, "Failed to read emulation settings!");
        }
        if (deserializedEmulationSettings != null) {
            this.emulationSettings = deserializedEmulationSettings;
        }
    }

    public class SystemManagerConverter implements CommandLine.ITypeConverter<SystemManager> {

        @Override
        public SystemManager convert(String value) {
            for (SystemManager systemManager : systemManagers) {
                if (systemManager.getId().equals(value)) {
                    return systemManager;
                }
            }
            throw new IllegalArgumentException("Unknown system identifier \"" + value + "\"!");
        }

    }

    public static class EmulationSettings {

        @SerializedName("atari_2600")
        private volatile Atari2600Manager.EmulationSettings atari2600Settings = new Atari2600Manager.EmulationSettings();

        @SerializedName("gameboy")
        private volatile GameBoyManager.EmulationSettings gameboySettings = new GameBoyManager.EmulationSettings();

        public Atari2600Manager.EmulationSettings getAtari2600Settings() {
            return this.atari2600Settings;
        }

        public GameBoyManager.EmulationSettings getGameBoySettings() {
            return this.gameboySettings;
        }

    }

}
