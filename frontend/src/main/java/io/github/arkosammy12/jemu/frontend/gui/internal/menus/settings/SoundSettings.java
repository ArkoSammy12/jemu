package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalMuteEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalSampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalVolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class SoundSettings extends MenuBarMenu {

    private final JSlider volumeSlider;
    private final JRadioButtonMenuItem muteButton;

    public SoundSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Sound");

        JMenu volumeMenu = new JMenu("Volume");
        this.volumeSlider = new JSlider(0, 100, mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getVolume());
        this.volumeSlider.setPaintTrack(true);
        this.volumeSlider.setPaintTicks(true);
        this.volumeSlider.setPaintLabels(true);
        this.volumeSlider.setMajorTickSpacing(25);
        this.volumeSlider.setMinorTickSpacing(5);
        this.volumeSlider.addChangeListener(_ -> {
            int volume = Math.clamp(this.volumeSlider.getValue(), 0, 100);
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setVolume(volume);
            mainWindow.pushEvent(new InternalVolumeChangedEvent(volume));
        });
        JPanel volumePanel = new JPanel();
        volumePanel.add(this.volumeSlider);
        volumeMenu.add(volumePanel);

        this.muteButton = new JRadioButtonMenuItem("Mute");
        this.muteButton.addActionListener(_ -> {
            boolean mute = this.muteButton.isSelected();
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setMute(mute);
            mainWindow.pushEvent(new InternalMuteEvent(mute));
        });
        this.muteButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));

        JMenu sampleRateMenu = new JMenu("Sample Rate");
        ButtonGroup sampleRateButtonGroup = new ButtonGroup();
        SampleRate selectedSampleRate = mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getSampleRate();

        for (SampleRate sampleRate : SampleRate.values()) {
            JRadioButtonMenuItem sampleRateButton = new JRadioButtonMenuItem(sampleRate.getDisplayName());
            sampleRateButton.addActionListener(_ -> {
                mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().setSampleRate(sampleRate);
                mainWindow.pushEvent(new InternalSampleRateChangedEvent(sampleRate));
            });
            sampleRateButtonGroup.add(sampleRateButton);
            sampleRateMenu.add(sampleRateButton);

            sampleRateButton.setSelected(sampleRate == selectedSampleRate);
        }

        this.getJMenu().add(volumeMenu);
        this.getJMenu().add(muteButton);
        this.getJMenu().add(sampleRateMenu);

        this.volumeSlider.setValue(mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getVolume());
        this.muteButton.setSelected(mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getMute());
    }

}
