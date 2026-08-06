package io.github.arkosammy12.jemu.app.system.gameboy;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.MenuBarSettingsMenu;

public class GameBoyMenuBarSettings extends MenuBarSettingsMenu {

    public GameBoyMenuBarSettings(GameBoyManager gameBoyManager, MainWindow mainWindow) {
        super(mainWindow, "Game Boy");

        this.addBooleanSetting("Prefer GameBoy Color", gameBoyManager.getEmulationSettings().preferGameBoyColor(), GameBoyManager.PreferGameBoyColorSettingChangedEvent.class, null, GameBoyManager.PreferGameBoyColorSettingChangedEvent::new);
        this.addEnumSetting("Palette", gameBoyManager.getEmulationSettings().getDMGPalette(), GameBoyManager.DMGPaletteSettingChangedEvent.class, null, GameBoyManager.DMGPaletteSettingChangedEvent::new);
    }

}
