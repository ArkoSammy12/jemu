package io.github.arkosammy12.jemu.app.system.atari2600;

import com.google.gson.Gson;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.EmulationSettingsBuilder;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import java.util.*;

public class Atari2600Manager extends SystemManager {

    private final Map<String, Atari2600Database.Entry> databaseMap;

    public Atari2600Manager(Jemu jemu, SystemRegistry systemRegistry) {
        super(jemu, systemRegistry);
        Map<String, Atari2600Database.Entry> map = new HashMap<>();
        dbInit: try {
            byte[] bytes = loadFromResources(this.getClass(), "/system/atari2600/vcs_cart_db/db.json");
            if (bytes == null) {
                Logger.error("Atari 2600 database file not found!");
                break dbInit;
            }
            Atari2600Database db = new Gson().fromJson(new String(bytes), Atari2600Database.class);
            for (Atari2600Database.Entry entry : db.getRoms()) {
                map.put(entry.getSha1(), entry);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to load Atari 2600 database!");
        }
        this.databaseMap = Map.copyOf(map);
    }

    @Override
    public String getName() {
        return "Atari 2600";
    }

    @Override
    public String getId() {
        return "atari-2600";
    }

    @Override
    public Collection<String> getFileExtensions() {
        return List.of("a26");
    }

    public SystemAdapter createSystem() throws LineUnavailableException {
        return new Atari2600Adapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof Atari2600Adapter;
    }

    public Atari2600Settings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getAtari2600Settings();
    }

    public Optional<Atari2600Database.Entry> getDatabaseEntryForRom(byte[] rom) {
        try {
            return Optional.ofNullable(this.databaseMap.get(SystemManager.getSha1Hash(rom)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public EmulationSettingsBuilder buildSystemSettings(EmulationSettingsBuilder emulationSettingsBuilder) {
        return super.buildSystemSettings(emulationSettingsBuilder)
                .addSection(this.getName(), section -> {
                    section.addEnumSetting("TV Type", this.getEmulationSettings().getTVType(), TVTypeSettingChangedEvent::new);
                    section.addEnumSetting("Left Difficulty", this.getEmulationSettings().getLeftDifficulty(), playerDifficulty -> new PlayerDifficultyChangedEvent(Atari2600Settings.PlayerSide.LEFT, playerDifficulty));
                    section.addEnumSetting("Right Difficulty", this.getEmulationSettings().getRightDifficulty(), playerDifficulty -> new PlayerDifficultyChangedEvent(Atari2600Settings.PlayerSide.RIGHT, playerDifficulty));
                    section.addSection("Overrides", overridesSection -> {
                        overridesSection.addEnumSetting("TV Format", this.getEmulationSettings().getTVFormatOverride(), TVFormatOverrideSettingChangedEvent::new);
                        overridesSection.addEnumSetting("Cartridge Type", this.getEmulationSettings().getCartridgeTypeOverride(), CartridgeTypeOverrideSettingChangedEvent::new);
                    });
        });
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangeEvent);
        switch (coreSettingChangeEvent) {
            case TVTypeSettingChangedEvent(Atari2600Settings.TVType tvType) -> this.getEmulationSettings().setTVType(tvType);
            case PlayerDifficultyChangedEvent(Atari2600Settings.PlayerSide playerSide, Atari2600Settings.PlayerDifficulty playerDifficulty) -> {
                switch (playerSide) {
                    case LEFT -> this.getEmulationSettings().setLeftDifficulty(playerDifficulty);
                    case RIGHT -> this.getEmulationSettings().setRightDifficulty(playerDifficulty);
                }
            }
            case TVFormatOverrideSettingChangedEvent(Atari2600Settings.TVFormatOverride tvFormatOverride) -> this.getEmulationSettings().setTVFormatOverride(tvFormatOverride);
            case CartridgeTypeOverrideSettingChangedEvent(Atari2600Settings.CartridgeTypeOverride cartridgeTypeOverride) -> this.getEmulationSettings().setCartridgeTypeOverride(cartridgeTypeOverride);
            default -> {}
        }
    }

    private record TVTypeSettingChangedEvent(Atari2600Settings.TVType tvType) implements CoreSettingChangeEvent {}

    private record PlayerDifficultyChangedEvent(Atari2600Settings.PlayerSide playerSide, Atari2600Settings.PlayerDifficulty playerDifficulty) implements CoreSettingChangeEvent {}

    private record TVFormatOverrideSettingChangedEvent(Atari2600Settings.TVFormatOverride tvFormatOverride) implements CoreSettingChangeEvent {}

    private record CartridgeTypeOverrideSettingChangedEvent(Atari2600Settings.CartridgeTypeOverride cartridgeTypeOverride) implements CoreSettingChangeEvent {}

}
