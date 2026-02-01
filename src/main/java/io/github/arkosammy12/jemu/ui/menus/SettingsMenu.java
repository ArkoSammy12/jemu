package io.github.arkosammy12.jemu.ui.menus;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.DataManager;
import io.github.arkosammy12.jemu.config.Serializable;
import io.github.arkosammy12.jemu.config.initializers.ApplicationInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.main.MainWindow;
import io.github.arkosammy12.jemu.ui.util.EnumMenu;
import io.github.arkosammy12.jemu.util.KeyboardLayout;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class SettingsMenu extends JMenu implements EmulatorInitializerConsumer {

    private final JSlider volumeSlider;
    private final JRadioButtonMenuItem muteButton;

    private final EnumMenu<KeyboardLayout> keyboardLayoutMenu;

    private final JRadioButtonMenuItem showInfoBarButton;

    public SettingsMenu(Jemu jemu, MainWindow mainWindow) {
        super("Settings");

        this.setMnemonic(KeyEvent.VK_S);

        JMenu volumeMenu = new JMenu("Volume");
        this.volumeSlider = new JSlider(0, 100, 50);
        this.volumeSlider.setPaintTrack(true);
        this.volumeSlider.setPaintTicks(true);
        this.volumeSlider.setPaintLabels(true);
        this.volumeSlider.setMajorTickSpacing(25);
        this.volumeSlider.setMinorTickSpacing(5);
        this.volumeSlider.addChangeListener(_ -> jemu.getAudioRenderer().setVolume(this.volumeSlider.getValue()));
        JPanel volumePanel = new JPanel();
        volumePanel.add(this.volumeSlider);
        volumeMenu.add(volumePanel);

        this.muteButton = new JRadioButtonMenuItem("Mute");
        this.muteButton.addChangeListener(_ -> jemu.getAudioRenderer().setMuted(this.muteButton.isSelected()));
        this.muteButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));
        this.muteButton.setSelected(false);

        this.keyboardLayoutMenu = new EnumMenu<>("Keyboard Layout", KeyboardLayout.class, false);
        this.keyboardLayoutMenu.setState(KeyboardLayout.QWERTY);
        this.keyboardLayoutMenu.setMnemonic(KeyEvent.VK_K);
        this.keyboardLayoutMenu.setToolTipText("Select the desired keyboard layout configuration for using the CHIP-8 keypad.");

        this.showInfoBarButton = new JRadioButtonMenuItem("Show info bar");
        this.showInfoBarButton.setSelected(true);
        this.showInfoBarButton.addChangeListener(_ -> mainWindow.setInfoBarEnabled(this.showInfoBarButton.isSelected()));

        this.add(volumeMenu);
        this.add(muteButton);
        this.addSeparator();
        this.add(keyboardLayoutMenu);
        this.addSeparator();
        this.add(showInfoBarButton);

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent(DataManager.VOLUME, String.valueOf(this.volumeSlider.getValue()));
            dataManager.putPersistent(DataManager.MUTED, String.valueOf(this.muteButton.isSelected()));
            dataManager.putPersistent(DataManager.SHOW_INFO_BAR, String.valueOf(this.showInfoBarButton.isSelected()));
            dataManager.putPersistent(DataManager.KEYBOARD_LAYOUT, Serializable.serialize(this.getKeyboardLayout().orElse(null)));
        });
    }

    public Optional<KeyboardLayout> getKeyboardLayout() {
        return this.keyboardLayoutMenu.getState();
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        initializer.getKeyboardLayout().ifPresent(this.keyboardLayoutMenu::setState);
        if (initializer instanceof ApplicationInitializer applicationInitializer) {
            applicationInitializer.getVolume().ifPresent(this.volumeSlider::setValue);
            applicationInitializer.getMuted().ifPresent(this.muteButton::setSelected);
            applicationInitializer.getShowingInfoBar().ifPresent(this.showInfoBarButton::setSelected);
        }
    }

}
