package io.github.arkosammy12.jemu.app.system.gameboy;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.PathUISetting;

public class GameBoyPanelSettings extends PanelSettingsMenu {

    public GameBoyPanelSettings(GameBoyManager gameBoyManager, MainWindow mainWindow) {
        super(mainWindow);

        this.addHeader("General");
        this.addBooleanSetting("Prefer GameBoy Color", gameBoyManager.getEmulationSettings().preferGameBoyColor(), GameBoyManager.PreferGameBoyColorSettingChangedEvent.class, null, GameBoyManager.PreferGameBoyColorSettingChangedEvent::new);
        this.addEmptyLine();

        this.addHeader("Video");
        this.addEnumSetting("Palette", gameBoyManager.getEmulationSettings().getDMGPalette(), GameBoyManager.DMGPaletteSettingChangedEvent.class, null, GameBoyManager.DMGPaletteSettingChangedEvent::new);
        this.addEmptyLine();

        this.addHeader("Boot ROMs");
        boolean useBuiltInBootRomsStartingValue = gameBoyManager.getEmulationSettings().useBuiltInBootROM();
        BooleanPanelSetting<?> useBuiltInBootRomsSetting = this.addBooleanSetting("Use built-in boot ROMs", useBuiltInBootRomsStartingValue, GameBoyManager.UseBuiltInBootRomSettingChangedEvent.class, null, GameBoyManager.UseBuiltInBootRomSettingChangedEvent::new);
        PathUISetting<?> gameBoyBootRomPathSetting = this.addPathSetting(PathUISetting.PathSelectionMode.FILES_ONLY, "GameBoy boot ROM: ", gameBoyManager.getEmulationSettings().getGameBoyBootROMPath().orElse(null), GameBoyManager.GameBoyBootRomPathChangedEvent.class, null, GameBoyManager.GameBoyBootRomPathChangedEvent::new);
        PathUISetting<?> gameBoyColorBootRomPathSetting = this.addPathSetting(PathUISetting.PathSelectionMode.FILES_ONLY, "GameBoy Color boot ROM: ", gameBoyManager.getEmulationSettings().getGameBoyColorBootRomPath().orElse(null), GameBoyManager.GameBoyColorBootRomPathChangedEvent.class, null, GameBoyManager.GameBoyColorBootRomPathChangedEvent::new);

        gameBoyBootRomPathSetting.getJTextField().setEnabled(!useBuiltInBootRomsStartingValue);
        gameBoyBootRomPathSetting.getSelectPathButton().setEnabled(!useBuiltInBootRomsStartingValue);
        gameBoyColorBootRomPathSetting.getJTextField().setEnabled(!useBuiltInBootRomsStartingValue);
        gameBoyColorBootRomPathSetting.getSelectPathButton().setEnabled(!useBuiltInBootRomsStartingValue);

        useBuiltInBootRomsSetting.setOnValueSetCallback(useBuiltInRoms -> {
            gameBoyBootRomPathSetting.getJTextField().setEnabled(!useBuiltInRoms);
            gameBoyBootRomPathSetting.getSelectPathButton().setEnabled(!useBuiltInRoms);
            gameBoyColorBootRomPathSetting.getJTextField().setEnabled(!useBuiltInRoms);
            gameBoyColorBootRomPathSetting.getSelectPathButton().setEnabled(!useBuiltInRoms);
        });

    }

}
