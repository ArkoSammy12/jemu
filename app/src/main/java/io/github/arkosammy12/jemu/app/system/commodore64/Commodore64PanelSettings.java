package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.PathUISetting;

public class Commodore64PanelSettings extends PanelSettingsMenu {

    public Commodore64PanelSettings(Commodore64Manager commodore64Manager, EventPublisher eventPublisher) {
        super(eventPublisher);

        this.addHeader("Video");
        this.addEnumSetting("VIC-II Palette", commodore64Manager.getEmulationSettings().getVICIIPalette(), Commodore64Manager.VICIIPaletteSettingChangedEvent.class, null, Commodore64Manager.VICIIPaletteSettingChangedEvent::new);
        this.addEmptyLine();

        this.addHeader("Firmware");
        this.addPathSetting(PathUISetting.PathSelectionMode.FILES_ONLY, "Kernal ROM (8 KB): ", commodore64Manager.getEmulationSettings().getKernalRomPath().orElse(null), Commodore64Manager.KernalRomPathSettingChangedEvent.class, null, Commodore64Manager.KernalRomPathSettingChangedEvent::new);
        this.addPathSetting(PathUISetting.PathSelectionMode.FILES_ONLY, "BASIC ROM (8 KB): ", commodore64Manager.getEmulationSettings().getBasicRomPath().orElse(null), Commodore64Manager.BasicRomPathSettingChangedEvent.class, null, Commodore64Manager.BasicRomPathSettingChangedEvent::new);
        this.addPathSetting(PathUISetting.PathSelectionMode.FILES_ONLY, "Character ROM (4 KB): ", commodore64Manager.getEmulationSettings().getCharacterRomPath().orElse(null), Commodore64Manager.CharacterRomPathSettingChangedEvent.class, null, Commodore64Manager.CharacterRomPathSettingChangedEvent::new);
    }
}
