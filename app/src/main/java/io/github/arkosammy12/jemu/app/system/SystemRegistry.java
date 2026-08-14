package io.github.arkosammy12.jemu.app.system;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.Jemu;
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
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemCatalog;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import picocli.CommandLine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class SystemRegistry implements SystemCatalog {

    private final Chip8Manager chip8Manager;
    private final Chip8Manager strictChip8Manager;
    private final Chip8Manager chip8Xmanager ;
    private final Chip8Manager chip48Manger;
    private final Chip8Manager superChip10Manager;
    private final Chip8Manager superChip11Manager;
    private final Chip8Manager superChipModernManager;
    private final Chip8Manager xoChipManager;
    private final Chip8Manager megaChipManager;
    private final Chip8Manager hyperwaveChip64Manager;
    private final CosmacVIPManager cosmacVIPManager;
    private final CosmacVIPManager hybridChip8Manager;
    private final CosmacVIPManager hybridChip8xManager;
    private final RCAStudioIIManager rcaStudioIIManager;
    private final GameBoyManager gameBoyManager;
    private final GameBoyManager gameBoyColorManager;
    private final NESManager nesManager;
    private final Atari2600Manager atari2600Manager;

    private final List<SystemManager> systemManagers;

    @NotNull
    private volatile EmulationSettings emulationSettings = new EmulationSettings();

    public SystemRegistry(Jemu jemu) {
        this.chip8Manager = new Chip8Manager(jemu, this, Chip8Variant.CHIP_8);
        this.strictChip8Manager = new Chip8Manager(jemu, this, Chip8Variant.STRICT_CHIP_8);
        this.chip8Xmanager  = new Chip8Manager(jemu, this, Chip8Variant.CHIP_8X);
        this.chip48Manger = new Chip8Manager(jemu, this, Chip8Variant.CHIP_48);
        this.superChip10Manager = new Chip8Manager(jemu, this, Chip8Variant.SUPER_CHIP_10);
        this.superChip11Manager = new Chip8Manager(jemu, this, Chip8Variant.SUPER_CHIP_11);
        this.superChipModernManager = new Chip8Manager(jemu, this, Chip8Variant.SUPER_CHIP_MODERN);
        this.xoChipManager = new Chip8Manager(jemu, this, Chip8Variant.XO_CHIP);
        this.megaChipManager = new Chip8Manager(jemu, this, Chip8Variant.MEGA_CHIP);
        this.hyperwaveChip64Manager = new Chip8Manager(jemu, this, Chip8Variant.HYPERWAVE_CHIP_8);
        this.cosmacVIPManager = new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.NONE);
        this.hybridChip8Manager = new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.CHIP_8);
        this.hybridChip8xManager = new CosmacVIPManager(jemu, this, CosmacVIPHost.Chip8Interpreter.CHIP_8X);
        this.rcaStudioIIManager = new RCAStudioIIManager(jemu, this);
        this.gameBoyManager = new GameBoyManager(jemu, this, GameBoyHost.Model.DMG);
        this.gameBoyColorManager = new GameBoyManager(jemu, this, GameBoyHost.Model.CGB);
        this.nesManager = new NESManager(jemu, this);
        this.atari2600Manager = new Atari2600Manager(jemu, this);

        this.systemManagers = List.of(
                this.chip8Manager,
                this.strictChip8Manager,
                this.chip8Xmanager,
                this.chip48Manger,
                this.superChip10Manager,
                this.superChip11Manager,
                this.superChipModernManager,
                this.xoChipManager,
                this.megaChipManager,
                this.hyperwaveChip64Manager,
                this.cosmacVIPManager,
                this.hybridChip8Manager,
                this.hybridChip8xManager,
                this.rcaStudioIIManager,
                this.gameBoyManager,
                this.gameBoyColorManager,
                this.nesManager,
                this.atari2600Manager
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

    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) {
        this.getSystemDescriptors().forEach(systemManager -> systemManager.onCoreSettingChangedEvent(coreSettingChangedEvent));
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

    public CosmacVIPManager getHybridChip8Manager() {
        return this.hybridChip8Manager;
    }

    public GameBoyManager getGameBoyColorManager() {
        return this.gameBoyColorManager;
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
