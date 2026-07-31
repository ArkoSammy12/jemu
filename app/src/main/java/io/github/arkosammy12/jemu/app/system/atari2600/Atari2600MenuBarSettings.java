package io.github.arkosammy12.jemu.app.system.atari2600;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.MenuBarSettingsMenu;

public class Atari2600MenuBarSettings extends MenuBarSettingsMenu {

    public Atari2600MenuBarSettings(Atari2600Manager atari2600Manager, MainWindow mainWindow) {
        super(mainWindow, "Atari 2600");

        this.addEnumSetting("TV Type", atari2600Manager.getEmulationSettings().getTVType(), Atari2600Manager.TVTypeSettingChangedEvent.class, null, Atari2600Manager.TVTypeSettingChangedEvent::new);
        this.addEnumSetting("Left Difficulty", atari2600Manager.getEmulationSettings().getLeftDifficulty(), Atari2600Manager.LeftPlayerDifficultyChangedEvent.class, null, Atari2600Manager.LeftPlayerDifficultyChangedEvent::new);
        this.addEnumSetting("Right Difficulty", atari2600Manager.getEmulationSettings().getRightDifficulty(), Atari2600Manager.RightPlayerDifficultyChangedEvent.class, null, Atari2600Manager.RightPlayerDifficultyChangedEvent::new);

        MenuBarSettingsMenu overridesMenu = this.addMenu("Overrides");
        overridesMenu.addEnumSetting("TV Format", atari2600Manager.getEmulationSettings().getTVFormatOverride(), Atari2600Manager.TVFormatOverrideSettingChangedEvent.class, null, Atari2600Manager.TVFormatOverrideSettingChangedEvent::new);
        overridesMenu.addEnumSetting("Cartridge Type", atari2600Manager.getEmulationSettings().getCartridgeTypeOverride(), Atari2600Manager.CartridgeTypeOverrideSettingChangedEvent.class, null, Atari2600Manager.CartridgeTypeOverrideSettingChangedEvent::new);
    }

}
