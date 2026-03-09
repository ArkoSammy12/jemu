package io.github.arkosammy12.jemu.frontend.swing.menus;

import io.github.arkosammy12.jemu.frontend.swing.MainWindow;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class SettingsMenu extends MenuBarMenu {

    public SettingsMenu(MainWindow mainWindow) {
        this.getJMenu().setText("Settings");
        this.getJMenu().setMnemonic(KeyEvent.VK_S);

        JRadioButtonMenuItem showInfoBarButton = new JRadioButtonMenuItem("Show status bar");
        showInfoBarButton.setSelected(true);
        showInfoBarButton.addChangeListener(_ -> mainWindow.setStatusBarEnabled(showInfoBarButton.isSelected()));

        this.getJMenu().add(showInfoBarButton);

    }

}
