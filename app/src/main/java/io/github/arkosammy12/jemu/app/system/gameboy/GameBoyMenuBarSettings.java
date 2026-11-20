package io.github.arkosammy12.jemu.app.system.gameboy;

import io.github.arkosammy12.jemu.app.util.EventPublisher;
import io.github.arkosammy12.jemu.app.util.MenuBarSettingsMenu;

public class GameBoyMenuBarSettings extends MenuBarSettingsMenu {

    public GameBoyMenuBarSettings(GameBoyManager gameBoyManager, EventPublisher eventPublisher) {
        super(eventPublisher, "Game Boy");

        this.addBooleanSetting("Prefer GameBoy Color", gameBoyManager.getEmulationSettings().preferGameBoyColor(), GameBoyManager.PreferGameBoyColorSettingChangedEvent.class, null, GameBoyManager.PreferGameBoyColorSettingChangedEvent::new);
        this.addEnumSetting("Palette", gameBoyManager.getEmulationSettings().getDMGPalette(), GameBoyManager.DMGPaletteSettingChangedEvent.class, null, GameBoyManager.DMGPaletteSettingChangedEvent::new);
    }

}
