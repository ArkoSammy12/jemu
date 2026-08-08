package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.menubar;

import io.github.arkosammy12.jemu.frontend.audio.AudioEngine;
import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.audio.SoundDevice;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.*;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;

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
        this.volumeSlider = new JSlider(AudioEngine.MIN_VOLUME, AudioEngine.MAX_VOLUME, mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getVolume());
        this.volumeSlider.setPaintTrack(true);
        this.volumeSlider.setPaintTicks(true);
        this.volumeSlider.setPaintLabels(true);
        this.volumeSlider.setMajorTickSpacing(25);
        this.volumeSlider.setMinorTickSpacing(5);
        ChangeListener volumeSiderChangeListener = _ -> mainWindow.publishEvent(new InternalVolumeChangedEvent(Math.clamp(this.volumeSlider.getValue(), 0, 100)));
        this.volumeSlider.addChangeListener(volumeSiderChangeListener);
        JPanel volumePanel = new JPanel();
        volumePanel.add(this.volumeSlider);
        volumeMenu.add(volumePanel);
        mainWindow.onEvent(InternalVolumeChangedEvent.class, volumeChangedEvent -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setVolume(volumeChangedEvent.getNewVolume());
            this.volumeSlider.removeChangeListener(volumeSiderChangeListener);
            this.volumeSlider.setValue(volumeChangedEvent.getNewVolume());
            this.volumeSlider.addChangeListener(volumeSiderChangeListener);
        });

        this.muteButton = new JRadioButtonMenuItem("Mute");
        this.muteButton.addActionListener(_ -> mainWindow.publishEvent(new InternalMuteEvent(this.muteButton.isSelected())));
        this.muteButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));
        mainWindow.onEvent(InternalMuteEvent.class, muteEvent -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setMute(muteEvent.getMute());
            this.muteButton.setSelected(muteEvent.getMute());
        });

        JMenu sampleRateMenu = new JMenu("Sample Rate");
        ButtonGroup sampleRateButtonGroup = new ButtonGroup();
        Map<SampleRate, JRadioButtonMenuItem> sampleRateButtonMap = new HashMap<>();
        SampleRate selectedSampleRate = mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getSampleRate();

        for (SampleRate sampleRate : SampleRate.values()) {
            JRadioButtonMenuItem sampleRateButton = new JRadioButtonMenuItem(sampleRate.getDisplayName());
            sampleRateButton.addActionListener(_ -> mainWindow.publishEvent(new InternalSampleRateChangedEvent(sampleRate)));
            sampleRateButtonGroup.add(sampleRateButton);
            sampleRateButtonMap.put(sampleRate, sampleRateButton);
            sampleRateMenu.add(sampleRateButton);

            sampleRateButton.setSelected(sampleRate == selectedSampleRate);
        }
        mainWindow.onEvent(InternalSampleRateChangedEvent.class, sampleRateChangedEvent -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setSampleRate(sampleRateChangedEvent.getSampleRate());

            sampleRateButtonGroup.clearSelection();
            JRadioButtonMenuItem sampleRateButton = sampleRateButtonMap.get(sampleRateChangedEvent.getSampleRate());
            if (sampleRateButton != null) {
                sampleRateButton.setSelected(true);
            }
        });

        JMenu audioLatencyMenu = new JMenu("Latency");
        SpinnerNumberModel audioLatencySpinnerModel = new SpinnerNumberModel();
        audioLatencySpinnerModel.setMinimum(AudioEngine.MIN_LATENCY_MS);
        audioLatencySpinnerModel.setMaximum(AudioEngine.MAX_LATENCY_MS);
        JSpinner audioLatencySpinner = new JSpinner(audioLatencySpinnerModel);
        audioLatencySpinner.setValue(mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getLatencyMs());
        ChangeListener audioLatencySpinnerChangeListener = _ -> {
            if (audioLatencySpinner.getValue() instanceof Integer value) {
                mainWindow.publishEvent(new InternalAudioLatencyChangedEvent(value));
            }
        };
        audioLatencySpinner.addChangeListener(audioLatencySpinnerChangeListener);

        mainWindow.onEvent(InternalAudioLatencyChangedEvent.class, internalAudioLatencyChangedEvent -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setLatencyMs(internalAudioLatencyChangedEvent.getLatencyMs());

            audioLatencySpinner.removeChangeListener(audioLatencySpinnerChangeListener);
            audioLatencySpinner.setValue(internalAudioLatencyChangedEvent.getLatencyMs());
            audioLatencySpinner.addChangeListener(audioLatencySpinnerChangeListener);
        });
        audioLatencyMenu.add(audioLatencySpinner);

        this.soundDeviceMenu = new JMenu("Sound Device");
        this.soundDeviceMenu.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                rebuildSoundDeviceMenu();
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }

        });

        this.autoSoundDeviceButton = new JRadioButtonMenuItem("Auto");
        this.autoSoundDeviceButton.addActionListener(_ -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setSoundDevice(null);
            mainWindow.publishEvent(new InternalSoundDeviceChangedEvent(null));
        });

        this.getJMenu().add(muteButton);
        this.getJMenu().add(volumeMenu);
        this.getJMenu().add(sampleRateMenu);
        this.getJMenu().add(audioLatencyMenu);
        this.getJMenu().add(soundDeviceMenu);

        this.volumeSlider.setValue(mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getVolume());
        this.muteButton.setSelected(mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getMute());
    }

    private void rebuildSoundDeviceMenu() {
        this.soundDeviceMenu.removeAll();
        ButtonGroup buttonGroup = new ButtonGroup();
        SoundDevice selectedSoundDevice = this.mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getSoundDevice().orElse(null);

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
                mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setSoundDevice(availableSoundDevice);
                mainWindow.publishEvent(new InternalSoundDeviceChangedEvent(availableSoundDevice));
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
