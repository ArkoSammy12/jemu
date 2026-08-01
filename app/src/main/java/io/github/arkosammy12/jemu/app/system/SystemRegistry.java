package io.github.arkosammy12.jemu.app.system;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.appleii.AppleIIManager;
import io.github.arkosammy12.jemu.app.system.chip8.Chip8Manager;
import io.github.arkosammy12.jemu.app.system.chip8.Chip8Settings;
import io.github.arkosammy12.jemu.app.system.chip8.Chip8Variant;
import io.github.arkosammy12.jemu.app.system.cosmacvip.CosmacVIPManager;
import io.github.arkosammy12.jemu.app.system.gameboy.GameBoyManager;
import io.github.arkosammy12.jemu.app.system.atari2600.Atari2600Manager;
import io.github.arkosammy12.jemu.app.system.atari2600.Atari2600Settings;
import io.github.arkosammy12.jemu.app.system.gameboy.GameBoySettings;
import io.github.arkosammy12.jemu.app.system.nes.NESManager;
import io.github.arkosammy12.jemu.app.system.rcastudioii.RCAStudioIIManager;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemCatalog;
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
            new Chip8Manager(jemu, this, Chip8Variant.CHIP_8),
            new Chip8Manager(jemu, this, Chip8Variant.STRICT_CHIP_8),
            new Chip8Manager(jemu, this, Chip8Variant.CHIP_8X),
            new Chip8Manager(jemu, this, Chip8Variant.CHIP_48),
            new Chip8Manager(jemu, this, Chip8Variant.SUPER_CHIP_10),
            new Chip8Manager(jemu, this, Chip8Variant.SUPER_CHIP_11),
            new Chip8Manager(jemu, this, Chip8Variant.SUPER_CHIP_MODERN),
            new Chip8Manager(jemu, this, Chip8Variant.XO_CHIP),
            new Chip8Manager(jemu, this, Chip8Variant.MEGA_CHIP),
            new Chip8Manager(jemu, this, Chip8Variant.HYPERWAVE_CHIP_8),
            new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.NONE),
            new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.CHIP_8),
            new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.CHIP_8X),
            new RCAStudioIIManager(jemu, this),
            new GameBoyManager(jemu, this, GameBoyHost.Model.DMG),
            new GameBoyManager(jemu, this, GameBoyHost.Model.CGB),
            new NESManager(jemu, this),
            new Atari2600Manager(jemu, this),
            new AppleIIManager(jemu, this)
        );
    }

    @Override
    public Collection<SystemManager> getSystemDescriptors() {
        return this.systemManagers;
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

        @SerializedName("chip8")
        private volatile Chip8Settings chip8Settings = new Chip8Settings();

        @SerializedName("atari_2600")
        private volatile Atari2600Settings atari2600Settings = new Atari2600Settings();

        @SerializedName("gameboy")
        private volatile GameBoySettings gameboySettings = new GameBoySettings();

        public Chip8Settings getChip8Settings() {
            return this.chip8Settings;
        }

        public Atari2600Settings getAtari2600Settings() {
            return this.atari2600Settings;
        }

        public GameBoySettings getGameBoySettings() {
            return this.gameboySettings;
        }

    }

}
