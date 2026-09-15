package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.core.commodore64.Commodore64Controller;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.MenuBarSettingsMenu;
import io.github.arkosammy12.jemu.frontend.util.settings.PathUISetting;

import javax.swing.*;

public class Commodore64MenuBarSettings extends MenuBarSettingsMenu {

    public Commodore64MenuBarSettings(Commodore64Manager commodore64Manager, EventPublisher eventPublisher) {
        super(eventPublisher, "Commodore 64");

        this.addEnumSetting("VIC-II Palette", commodore64Manager.getEmulationSettings().getVICIIPalette(), Commodore64Manager.VICIIPaletteSettingChangedEvent.class, null, Commodore64Manager.VICIIPaletteSettingChangedEvent::new);
        this.addBooleanSetting("Swap joysticks", commodore64Manager.getEmulationSettings().getSwapJoysticks(), Commodore64Manager.SwapJoysticksSettingChangedEvent.class, null, Commodore64Manager.SwapJoysticksSettingChangedEvent::new);

        MenuBarSettingsMenu datasetteMenu = this.addMenu("Datasette");
        datasetteMenu.addPathSetting(PathUISetting.PathSelectionMode.FILES_ONLY, "Insert tape image...", Commodore64Manager.TapeImagePathChangedEvent.class, null, Commodore64Manager.TapeImagePathChangedEvent::new);
        JMenuItem ejectTapeImageButton = new JMenuItem("Eject tape image");
        ejectTapeImageButton.addActionListener(_ -> eventPublisher.publishEvent(new Commodore64Manager.TapeImagePathChangedEvent(null)));
        JMenuItem datasettePlayMenuItem = new JMenuItem("Play");
        datasettePlayMenuItem.addActionListener(_ -> eventPublisher.publishEvent(new Commodore64Manager.DatasetteButtonPressedEvent(Commodore64Controller.Datasette.PLAY)));
        JMenuItem datasetteStopMenuItem = new JMenuItem("Stop");
        datasetteStopMenuItem.addActionListener(_ -> eventPublisher.publishEvent(new Commodore64Manager.DatasetteButtonPressedEvent(Commodore64Controller.Datasette.STOP)));
        datasetteMenu.addBooleanSetting("Load T64 to BASIC start", commodore64Manager.getEmulationSettings().getLoadT64toBASICStart(), Commodore64Manager.LoadT64ToBASICStartSettingChanged.class, null, Commodore64Manager.LoadT64ToBASICStartSettingChanged::new);

        datasetteMenu.add(ejectTapeImageButton);
        datasetteMenu.add(datasettePlayMenuItem);
        datasetteMenu.add(datasetteStopMenuItem);
    }

}
