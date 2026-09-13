package io.github.arkosammy12.jemu.app.system.atari2600;

import com.google.gson.Gson;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Controller;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class Atari2600Manager extends SystemManager {

    private final Map<String, Atari2600Database.Entry> databaseMap;

    @Nullable
    private volatile Atari2600MenuBarSettings atari2600MenuBarSettings;

    @Nullable
    private volatile Atari2600PanelSettings atari2600PanelSettings;

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

        this.keyActionMap.put(KeyAction.E, Atari2600Controller.Actions.GAME_SELECT);
        this.keyActionMap.put(KeyAction.R, Atari2600Controller.Actions.GAME_RESET);

        this.keyActionMap.put(KeyAction.W, Atari2600Controller.Actions.JOYSTICK0_UP);
        this.keyActionMap.put(KeyAction.S, Atari2600Controller.Actions.JOYSTICK0_UP);
        this.keyActionMap.put(KeyAction.A, Atari2600Controller.Actions.JOYSTICK0_LEFT);
        this.keyActionMap.put(KeyAction.D, Atari2600Controller.Actions.JOYSTICK0_RIGHT);
        this.keyActionMap.put(KeyAction.F, Atari2600Controller.Actions.JOYSTICK0_FIRE);

        this.keyActionMap.put(KeyAction.I, Atari2600Controller.Actions.JOYSTICK1_UP);
        this.keyActionMap.put(KeyAction.K, Atari2600Controller.Actions.JOYSTICK1_UP);
        this.keyActionMap.put(KeyAction.J, Atari2600Controller.Actions.JOYSTICK1_LEFT);
        this.keyActionMap.put(KeyAction.L, Atari2600Controller.Actions.JOYSTICK1_RIGHT);
        this.keyActionMap.put(KeyAction.SEMICOLON, Atari2600Controller.Actions.JOYSTICK1_FIRE);
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

    public SystemAdapter createSystem(boolean detectedAutomatically) throws LineUnavailableException {
        return new Atari2600Adapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof Atari2600Adapter;
    }

    Atari2600Settings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getAtari2600Settings();
    }

    private Optional<Atari2600MenuBarSettings> getMenuBarSettings() {
        return Optional.ofNullable(this.atari2600MenuBarSettings);
    }

    private Optional<Atari2600PanelSettings> getPanelSettings() {
        return Optional.ofNullable(this.atari2600PanelSettings);
    }

    Optional<Atari2600Database.Entry> getDatabaseEntryForRom(byte[] rom) {
        try {
            return Optional.ofNullable(this.databaseMap.get(SystemManager.getSha1Hash(rom)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<? extends Function<? super EventPublisher, ? extends JMenu>> getSettingsMenuBarContents() {
        return Optional.of(mainWindow -> {
            Atari2600MenuBarSettings atari2600MenuBarSettings = new Atari2600MenuBarSettings(this, mainWindow);
            this.atari2600MenuBarSettings = atari2600MenuBarSettings;
            return atari2600MenuBarSettings;
        });
    }

    @Override
    public Optional<? extends Function<? super EventPublisher, ? extends JPanel>> getSettingsWindowContents() {
        return Optional.of(mainWindow -> {
            Atari2600PanelSettings atari2600PanelSettings = new Atari2600PanelSettings(this, mainWindow);
            this.atari2600PanelSettings = atari2600PanelSettings;
            return atari2600PanelSettings;
        });
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangedEvent);
        this.getMenuBarSettings().ifPresent(atari2600MenuBarSettings -> atari2600MenuBarSettings.onEvent(coreSettingChangedEvent));
        this.getPanelSettings().ifPresent(atari2600PanelSettings -> atari2600PanelSettings.onEvent(coreSettingChangedEvent));
        switch (coreSettingChangedEvent) {
            case TVTypeSettingChangedEvent(Atari2600Settings.TVType tvType) -> this.getEmulationSettings().setTVType(tvType);
            case LeftPlayerDifficultyChangedEvent(Atari2600Settings.PlayerDifficulty playerDifficulty) -> this.getEmulationSettings().setLeftDifficulty(playerDifficulty);
            case RightPlayerDifficultyChangedEvent(Atari2600Settings.PlayerDifficulty playerDifficulty) -> this.getEmulationSettings().setRightDifficulty(playerDifficulty);
            case TVFormatOverrideSettingChangedEvent(Atari2600Settings.TVFormatOverride tvFormatOverride) -> this.getEmulationSettings().setTVFormatOverride(tvFormatOverride);
            case CartridgeTypeOverrideSettingChangedEvent(Atari2600Settings.CartridgeTypeOverride cartridgeTypeOverride) -> this.getEmulationSettings().setCartridgeTypeOverride(cartridgeTypeOverride);
            default -> {}
        }
    }

    record TVTypeSettingChangedEvent(Atari2600Settings.TVType tvType) implements CoreSettingChangedEvent, Supplier<Atari2600Settings.TVType> {

        @Override
        public Atari2600Settings.TVType get() {
            return this.tvType();
        }

    }

    record LeftPlayerDifficultyChangedEvent(Atari2600Settings.PlayerDifficulty playerDifficulty) implements CoreSettingChangedEvent, Supplier<Atari2600Settings.PlayerDifficulty> {

        @Override
        public Atari2600Settings.PlayerDifficulty get() {
            return this.playerDifficulty();
        }

    }

    record RightPlayerDifficultyChangedEvent(Atari2600Settings.PlayerDifficulty playerDifficulty) implements CoreSettingChangedEvent, Supplier<Atari2600Settings.PlayerDifficulty> {

        @Override
        public Atari2600Settings.PlayerDifficulty get() {
            return this.playerDifficulty();
        }

    }

    record TVFormatOverrideSettingChangedEvent(Atari2600Settings.TVFormatOverride tvFormatOverride) implements CoreSettingChangedEvent, Supplier<Atari2600Settings.TVFormatOverride> {

        @Override
        public Atari2600Settings.TVFormatOverride get() {
            return this.tvFormatOverride();
        }

    }

    record CartridgeTypeOverrideSettingChangedEvent(Atari2600Settings.CartridgeTypeOverride cartridgeTypeOverride) implements CoreSettingChangedEvent, Supplier<Atari2600Settings.CartridgeTypeOverride> {

        @Override
        public Atari2600Settings.CartridgeTypeOverride get() {
            return this.cartridgeTypeOverride();
        }

    }

}
