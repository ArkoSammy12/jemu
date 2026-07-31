package io.github.arkosammy12.jemu.app.system.gameboy;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.PanelSettingsMenu;

public class GameBoyPanelSettings extends PanelSettingsMenu {

    public GameBoyPanelSettings(GameBoyManager gameBoyManager, MainWindow mainWindow) {
        super(mainWindow);

        this.addHeader("Video");
        this.addEnumSetting("Palette", gameBoyManager.getEmulationSettings().getDMGPalette(), GameBoyManager.DMGPaletteSettingChangedEvent.class, null, GameBoyManager.DMGPaletteSettingChangedEvent::new);
    }

}
