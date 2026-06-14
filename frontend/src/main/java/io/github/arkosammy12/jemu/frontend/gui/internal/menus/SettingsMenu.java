package io.github.arkosammy12.jemu.frontend.gui.internal.menus;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.gui.internal.SerializedEntry;
import io.github.arkosammy12.jemu.frontend.gui.internal.events.InternalMuteEvent;
import io.github.arkosammy12.jemu.frontend.gui.internal.events.InternalSampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.internal.events.InternalVolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.SettingsManager;
import io.github.arkosammy12.jemu.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow.tryParseInt;

public class SettingsMenu extends MenuBarMenu implements SettingsManager {

    private final JSlider volumeSlider;
    private final JRadioButtonMenuItem muteButton;

    private volatile int volume = 50;
    private volatile boolean muted = false;
    private volatile boolean fullScreen = false;
    private volatile SampleRate sampleRate = SampleRate.HZ_44100;
    private Rectangle windowBounds;
    private int windowExtendedState;

    public SettingsMenu(MainWindow mainWindow, JFrame jFrame) {
        this.getJMenu().setText("Settings");
        this.getJMenu().setMnemonic(KeyEvent.VK_S);

        JMenu windowMenu = new JMenu("Window");
        JRadioButtonMenuItem alwaysOnTopButton = new JRadioButtonMenuItem("Always on Top");
        alwaysOnTopButton.addChangeListener(_ -> jFrame.setAlwaysOnTop(alwaysOnTopButton.isSelected()));

        JMenuItem fullScreenButton = new JMenuItem("Toggle Fullscreen");
        fullScreenButton.addActionListener(_ -> this.toggleFullScreen(jFrame, false));
        fullScreenButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0, true));

        JRadioButtonMenuItem startInFullScreenButton = new JRadioButtonMenuItem("Start in Fullscreen");

        windowMenu.add(alwaysOnTopButton);
        windowMenu.add(fullScreenButton);
        windowMenu.add(startInFullScreenButton);

        JMenu soundMenu = new JMenu("Sound");

        JMenu volumeMenu = new JMenu("Volume");
        this.volumeSlider = new JSlider(0, 100, this.volume);
        this.volumeSlider.setPaintTrack(true);
        this.volumeSlider.setPaintTicks(true);
        this.volumeSlider.setPaintLabels(true);
        this.volumeSlider.setMajorTickSpacing(25);
        this.volumeSlider.setMinorTickSpacing(5);
        this.volumeSlider.addChangeListener(_ -> {
            this.volume = Math.clamp(this.volumeSlider.getValue(), 0, 100);
            mainWindow.pushEvent(new InternalVolumeChangedEvent(this.volume));
        });
        JPanel volumePanel = new JPanel();
        volumePanel.add(this.volumeSlider);
        volumeMenu.add(volumePanel);

        this.muteButton = new JRadioButtonMenuItem("Mute");
        this.muteButton.addChangeListener(_ -> {
            this.muted = this.muteButton.isSelected();
            mainWindow.pushEvent(new InternalMuteEvent(this.muted));
        });
        this.muteButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));
        this.muteButton.setSelected(this.muted);

        JMenu sampleRateMenu = new JMenu("Sample Rate");
        ButtonGroup sampleRateButtonGroup = new ButtonGroup();
        JRadioButtonMenuItem kHz44100Button = new JRadioButtonMenuItem("44100 Hz");
        kHz44100Button.addActionListener(_ -> {
            this.sampleRate = SampleRate.HZ_44100;
            mainWindow.pushEvent(new InternalSampleRateChangedEvent(this.sampleRate));
        });
        sampleRateButtonGroup.add(kHz44100Button);

        JRadioButtonMenuItem kHz48000Button = new JRadioButtonMenuItem("48000 Hz");
        kHz48000Button.addActionListener(_ -> {
            this.sampleRate = SampleRate.HZ_48000;
            mainWindow.pushEvent(new InternalSampleRateChangedEvent(this.sampleRate));
        });
        sampleRateButtonGroup.add(kHz48000Button);

        JMenuItem openDataDirectoryButton = new JMenuItem("Open data directory");
        openDataDirectoryButton.addActionListener(_ -> {
            Optional<Path> optionalDataDirectoryPath = mainWindow.getDataDirectoryPath();
            if (optionalDataDirectoryPath.isEmpty()) {
                mainWindow.showDialog("Failed to open data directory", "Data directory path was not specified or failed to be acquired!", MainWindow.DialogType.ERROR);
                return;
            }
            Path dataDirectory = optionalDataDirectoryPath.get();
            if (!Files.exists(dataDirectory) || !Files.isDirectory(dataDirectory)) {
                mainWindow.showDialog("Failed to open data directory", "Directory does not exist: " + dataDirectory, MainWindow.DialogType.ERROR);
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                mainWindow.showDialog("Failed to open data directory", "Desktop API not supported!", MainWindow.DialogType.ERROR);
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.open(dataDirectory.toFile());
            } catch (IOException e) {
                mainWindow.showDialog("Failed to open data directory", e.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        sampleRateMenu.add(kHz44100Button);
        sampleRateMenu.add(kHz48000Button);

        soundMenu.add(volumeMenu);
        soundMenu.add(muteButton);
        soundMenu.add(sampleRateMenu);

        this.getJMenu().add(windowMenu);
        this.getJMenu().add(soundMenu);
        this.getJMenu().addSeparator();
        this.getJMenu().add(openDataDirectoryButton);

        kHz44100Button.setSelected(true);
        mainWindow.pushEvent(new InternalSampleRateChangedEvent(SampleRate.HZ_44100));

        Map<Integer, Pair<SampleRate, JRadioButtonMenuItem>> sampleRateButtonMap = Map.of(
            SampleRate.HZ_44100.getId(), new Pair<>(SampleRate.HZ_44100, kHz44100Button),
            SampleRate.HZ_48000.getId(), new Pair<>(SampleRate.HZ_48000, kHz48000Button)
        );

        mainWindow.registerSettingProperty(new SerializedEntry("settings.sound.volume", () -> String.valueOf(this.volumeSlider.getValue()), s -> tryParseInt(s).ifPresent(this.volumeSlider::setValue)));
        mainWindow.registerSettingProperty(new SerializedEntry("settings.sound.muted", () -> String.valueOf(this.muteButton.isSelected()), s -> this.muteButton.setSelected(Boolean.parseBoolean(s))));

        mainWindow.registerSettingProperty(new SerializedEntry("settings.sound.sample_rate", () -> String.valueOf(this.sampleRate.getId()), s -> tryParseInt(s).ifPresent(id -> {
            Pair<SampleRate, JRadioButtonMenuItem> pair = sampleRateButtonMap.get(id);
            if (pair != null) {
                pair.second().setSelected(true);
                mainWindow.pushEvent(new InternalSampleRateChangedEvent(pair.first()));
            }
        })));

        mainWindow.registerSettingProperty(new SerializedEntry("settings.window.always_on_top", () -> String.valueOf(alwaysOnTopButton.isSelected()), s -> alwaysOnTopButton.setSelected(Boolean.parseBoolean(s))));
        mainWindow.registerSettingProperty(new SerializedEntry("settings.window.start_in_fullscreen", () -> String.valueOf(startInFullScreenButton.isSelected()), s -> {
            startInFullScreenButton.setSelected(Boolean.parseBoolean(s));
            if (startInFullScreenButton.isSelected()) {
                SwingUtilities.invokeLater(() -> this.toggleFullScreen(jFrame, true));
            }
        }));

    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public boolean getMuted() {
        return this.muted;
    }

    private void toggleFullScreen(JFrame jFrame, boolean forceFullScreen) {
        this.fullScreen = forceFullScreen || !this.fullScreen;
        if (this.fullScreen) {
            this.windowBounds = jFrame.getBounds();
            this.windowExtendedState = jFrame.getExtendedState();
            jFrame.dispose();
            jFrame.setUndecorated(true);
            jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            jFrame.setVisible(true);
        } else {
            jFrame.dispose();
            jFrame.setUndecorated(false);
            jFrame.setBounds(this.windowBounds);
            jFrame.setExtendedState(this.windowExtendedState);
            jFrame.setVisible(true);
        }
    }

}
