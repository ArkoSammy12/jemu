package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.core.commodore64.Commodore64Controller;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.MenuBarSettingsMenu;

import javax.swing.*;

public class Commodore64MenuBarSettings extends MenuBarSettingsMenu {

    public Commodore64MenuBarSettings(Commodore64Manager commodore64Manager, EventPublisher eventPublisher) {
        super(eventPublisher, "Commodore 64");

        this.addEnumSetting("VIC-II Palette", commodore64Manager.getEmulationSettings().getVICIIPalette(), Commodore64Manager.VICIIPaletteSettingChangedEvent.class, null, Commodore64Manager.VICIIPaletteSettingChangedEvent::new);

        MenuBarSettingsMenu datasetteMenu = this.addMenu("Datasette");
        JMenuItem datasettePlayMenuItem = new JMenuItem("Play");
        datasettePlayMenuItem.addActionListener(_ -> eventPublisher.publishEvent(new Commodore64Manager.DatasetteButtonPressedEvent(Commodore64Controller.Datasette.PLAY)));
        JMenuItem datasetteStopMenuItem = new JMenuItem("Stop");
        datasetteStopMenuItem.addActionListener(_ -> eventPublisher.publishEvent(new Commodore64Manager.DatasetteButtonPressedEvent(Commodore64Controller.Datasette.STOP)));

        datasetteMenu.add(datasettePlayMenuItem);
        datasetteMenu.add(datasetteStopMenuItem);
    }

}
