package io.github.arkosammy12.jemu.app.gui.menus.settings.menubar;

import io.github.arkosammy12.jemu.app.audio.AudioEngine;
import io.github.arkosammy12.jemu.app.audio.SampleRate;
import io.github.arkosammy12.jemu.app.audio.SoundDevice;
import io.github.arkosammy12.jemu.app.events.audio.*;
import io.github.arkosammy12.jemu.app.gui.MainWindow;
import io.github.arkosammy12.jemu.app.gui.MenuBarMenu;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SoundSettings extends MenuBarMenu {

    private final MainWindow mainWindow;

    private final JSlider volumeSlider;
    private final JRadioButtonMenuItem muteButton;

    private final JMenu soundDeviceMenu;
    private final JRadioButtonMenuItem autoSoundDeviceButton;

    public SoundSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Sound");

        this.mainWindow = mainWindow;

        JMenu volumeMenu = new JMenu("Volume");
        this.volumeSlider = new JSlider(AudioEngine.MIN_VOLUME, AudioEngine.MAX_VOLUME, mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getVolume());
        this.volumeSlider.setPaintTrack(true);
        this.volumeSlider.setPaintTicks(true);
        this.volumeSlider.setPaintLabels(true);
        this.volumeSlider.setMajorTickSpacing(25);
        this.volumeSlider.setMinorTickSpacing(5);
        ChangeListener volumeSiderChangeListener = _ -> mainWindow.publishEvent(new VolumeChangedEvent(Math.clamp(this.volumeSlider.getValue(), 0, 100)));
        this.volumeSlider.addChangeListener(volumeSiderChangeListener);
        JPanel volumePanel = new JPanel();
        volumePanel.add(this.volumeSlider);
        volumeMenu.add(volumePanel);
        mainWindow.onEvent(VolumeChangedEvent.class, volumeChangedEvent -> {
            mainWindow.getConfig().getPreferenceSettings().getAudioSettings().setVolume(volumeChangedEvent.newVolume());
            this.volumeSlider.removeChangeListener(volumeSiderChangeListener);
            this.volumeSlider.setValue(volumeChangedEvent.newVolume());
            this.volumeSlider.addChangeListener(volumeSiderChangeListener);
        });

        this.muteButton = new JRadioButtonMenuItem("Mute");
        this.muteButton.addActionListener(_ -> mainWindow.publishEvent(new MuteEvent(this.muteButton.isSelected())));
        this.muteButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));
        mainWindow.onEvent(MuteEvent.class, muteEvent -> {
            mainWindow.getConfig().getPreferenceSettings().getAudioSettings().setMute(muteEvent.mute());
            this.muteButton.setSelected(muteEvent.mute());
        });

        JMenu sampleRateMenu = new JMenu("Sample Rate");
        ButtonGroup sampleRateButtonGroup = new ButtonGroup();
        Map<SampleRate, JRadioButtonMenuItem> sampleRateButtonMap = new HashMap<>();
        SampleRate selectedSampleRate = mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getSampleRate();

        for (SampleRate sampleRate : SampleRate.values()) {
            JRadioButtonMenuItem sampleRateButton = new JRadioButtonMenuItem(sampleRate.getDisplayName());
            sampleRateButton.addActionListener(_ -> mainWindow.publishEvent(new SampleRateChangedEvent(sampleRate)));
            sampleRateButtonGroup.add(sampleRateButton);
            sampleRateButtonMap.put(sampleRate, sampleRateButton);
            sampleRateMenu.add(sampleRateButton);

            sampleRateButton.setSelected(sampleRate == selectedSampleRate);
        }
        mainWindow.onEvent(SampleRateChangedEvent.class, sampleRateChangedEvent -> {
            mainWindow.getConfig().getPreferenceSettings().getAudioSettings().setSampleRate(sampleRateChangedEvent.sampleRate());

            sampleRateButtonGroup.clearSelection();
            JRadioButtonMenuItem sampleRateButton = sampleRateButtonMap.get(sampleRateChangedEvent.sampleRate());
            if (sampleRateButton != null) {
                sampleRateButton.setSelected(true);
            }
        });

        JMenu audioLatencyMenu = new JMenu("Latency");
        JPanel audioLatencyPanel = new JPanel();
        SpinnerNumberModel audioLatencySpinnerModel = new SpinnerNumberModel();
        audioLatencySpinnerModel.setMinimum(AudioEngine.MIN_LATENCY_MS);
        audioLatencySpinnerModel.setMaximum(AudioEngine.MAX_LATENCY_MS);
        JSpinner audioLatencySpinner = new JSpinner(audioLatencySpinnerModel);
        audioLatencySpinner.setValue(mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getLatencyMs());
        ChangeListener audioLatencySpinnerChangeListener = _ -> {
            if (audioLatencySpinner.getValue() instanceof Integer value) {
                mainWindow.publishEvent(new AudioLatencyChangedEvent(value));
            }
        };
        audioLatencySpinner.addChangeListener(audioLatencySpinnerChangeListener);

        mainWindow.onEvent(AudioLatencyChangedEvent.class, audioLatencyChangedEvent -> {
            mainWindow.getConfig().getPreferenceSettings().getAudioSettings().setLatencyMs(audioLatencyChangedEvent.latencyMs());

            audioLatencySpinner.removeChangeListener(audioLatencySpinnerChangeListener);
            audioLatencySpinner.setValue(audioLatencyChangedEvent.latencyMs());
            audioLatencySpinner.addChangeListener(audioLatencySpinnerChangeListener);
        });

        audioLatencyPanel.add(audioLatencySpinner);
        audioLatencyPanel.add(new JLabel("ms"));
        audioLatencyMenu.add(audioLatencyPanel);

        this.soundDeviceMenu = new JMenu("Sound Device");
        this.soundDeviceMenu.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                rebuildSoundDeviceMenu();
            }

            @Override
            public void menuDeselected(MenuEvent e) {}

            @Override
            public void menuCanceled(MenuEvent e) {}

        });

        this.autoSoundDeviceButton = new JRadioButtonMenuItem("Auto");
        this.autoSoundDeviceButton.addActionListener(_ -> {
            mainWindow.getConfig().getPreferenceSettings().getAudioSettings().setSoundDevice(null);
            mainWindow.publishEvent(new SoundDeviceChangedEvent(null));
        });

        this.getJMenu().add(muteButton);
        this.getJMenu().add(volumeMenu);
        this.getJMenu().add(sampleRateMenu);
        this.getJMenu().add(audioLatencyMenu);
        this.getJMenu().add(soundDeviceMenu);

        this.volumeSlider.setValue(mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getVolume());
        this.muteButton.setSelected(mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getMute());
    }

    private void rebuildSoundDeviceMenu() {
        this.soundDeviceMenu.removeAll();
        ButtonGroup buttonGroup = new ButtonGroup();
        SoundDevice selectedSoundDevice = this.mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getSoundDevice().orElse(null);

        boolean soundDeviceMatch;
        this.soundDeviceMenu.add(this.autoSoundDeviceButton);
        buttonGroup.add(this.autoSoundDeviceButton);
        this.autoSoundDeviceButton.setSelected(selectedSoundDevice == null);
        soundDeviceMatch = selectedSoundDevice == null;

        Collection<SoundDevice> availableSoundDevices = SoundDevice.getAvailableSoundDevices();

        boolean separatorAdded = false;
        if (!availableSoundDevices.isEmpty()) {
            separatorAdded = true;
            this.soundDeviceMenu.addSeparator();
        }

        for (SoundDevice availableSoundDevice : availableSoundDevices) {
            JRadioButtonMenuItem soundDeviceButton = new JRadioButtonMenuItem(availableSoundDevice.getName());
            soundDeviceButton.addActionListener(_ -> {
                mainWindow.getConfig().getPreferenceSettings().getAudioSettings().setSoundDevice(availableSoundDevice);
                mainWindow.publishEvent(new SoundDeviceChangedEvent(availableSoundDevice));
            });
            buttonGroup.add(soundDeviceButton);
            this.soundDeviceMenu.add(soundDeviceButton);

            boolean soundDeviceMatches = availableSoundDevice.matches(selectedSoundDevice);
            soundDeviceMatch |= soundDeviceMatches;
            soundDeviceButton.setSelected(soundDeviceMatches);
        }

        if (selectedSoundDevice != null && !soundDeviceMatch) {
            if (!separatorAdded) {
                this.soundDeviceMenu.addSeparator();
            }
            JRadioButtonMenuItem unavailableSoundDeviceButton = new JRadioButtonMenuItem(selectedSoundDevice.getName());
            buttonGroup.add(unavailableSoundDeviceButton);

            unavailableSoundDeviceButton.setEnabled(false);
            unavailableSoundDeviceButton.setSelected(true);

            this.soundDeviceMenu.add(unavailableSoundDeviceButton);
        }

        this.soundDeviceMenu.revalidate();
        this.soundDeviceMenu.repaint();

    }

}
