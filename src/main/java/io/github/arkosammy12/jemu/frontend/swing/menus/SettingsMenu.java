package io.github.arkosammy12.jemu.frontend.swing.menus;

import io.github.arkosammy12.jemu.frontend.swing.MainWindow;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class SettingsMenu extends MenuBarMenu {

    private final JSlider volumeSlider;
    private final JRadioButtonMenuItem muteButton;

    private volatile int volume = 50;
    private volatile boolean muted = false;

    public SettingsMenu(MainWindow mainWindow) {
        this.getJMenu().setText("Settings");
        this.getJMenu().setMnemonic(KeyEvent.VK_S);

        JMenu volumeMenu = new JMenu("Volume");
        this.volumeSlider = new JSlider(0, 100, this.volume);
        this.volumeSlider.setPaintTrack(true);
        this.volumeSlider.setPaintTicks(true);
        this.volumeSlider.setPaintLabels(true);
        this.volumeSlider.setMajorTickSpacing(25);
        this.volumeSlider.setMinorTickSpacing(5);
        this.volumeSlider.addChangeListener(_ -> this.volume = this.volumeSlider.getValue());
        JPanel volumePanel = new JPanel();
        volumePanel.add(this.volumeSlider);
        volumeMenu.add(volumePanel);

        this.muteButton = new JRadioButtonMenuItem("Mute");
        this.muteButton.addChangeListener(_ -> this.muted = this.muteButton.isSelected());
        this.muteButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));
        this.muteButton.setSelected(this.muted);

        JRadioButtonMenuItem showInfoBarButton = new JRadioButtonMenuItem("Show status bar");
        showInfoBarButton.setSelected(true);
        showInfoBarButton.addChangeListener(_ -> mainWindow.setStatusBarEnabled(showInfoBarButton.isSelected()));

        this.getJMenu().add(volumeMenu);
        this.getJMenu().add(muteButton);
        this.getJMenu().addSeparator();
        this.getJMenu().add(showInfoBarButton);

    }

    public int getVolume() {
        return this.volume;
    }

    public boolean getMuted() {
        return this.muted;
    }

}
