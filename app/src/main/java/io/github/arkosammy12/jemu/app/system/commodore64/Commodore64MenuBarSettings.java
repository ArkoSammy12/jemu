package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.MenuBarSettingsMenu;

public class Commodore64MenuBarSettings extends MenuBarSettingsMenu {

    public Commodore64MenuBarSettings(Commodore64Manager commodore64Manager, EventPublisher eventPublisher) {
        super(eventPublisher, "Commodore 64");

        this.addEnumSetting("VIC-II Palette", commodore64Manager.getEmulationSettings().getVICIIPalette(), Commodore64Manager.VICIIPaletteSettingChangedEvent.class, null, Commodore64Manager.VICIIPaletteSettingChangedEvent::new);
    }

}
