package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.panel;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalMuteEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalSampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalVolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.EnumPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.IntegerPanelSpinnerSetting;

import javax.swing.*;
import javax.swing.event.ChangeListener;

public class AudioSettingsPanel extends PanelSettingsMenu {

    public AudioSettingsPanel(MainWindow mainWindow) {
        super(mainWindow);

        this.addHeader("General");
        BooleanPanelSetting<?> muteSetting = this.addBooleanSetting("Mute audio", mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getMute(), null, null, InternalMuteEvent::new);
        mainWindow.onEvent(InternalMuteEvent.class, muteEvent -> muteSetting.setValue(muteEvent.getMute()));

        int startingVolume = mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getVolume();
        JSlider volumeSlider = new JSlider(0, 100, startingVolume);
        volumeSlider.setPaintTrack(true);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);

        ChangeListener volumeSiderChangeListener = _ -> mainWindow.publishEvent(new InternalVolumeChangedEvent(Math.clamp(volumeSlider.getValue(), 0, 100)));

        volumeSlider.addChangeListener(volumeSiderChangeListener);

        IntegerPanelSpinnerSetting<?> volumeSpinnerSetting = new IntegerPanelSpinnerSetting<>(mainWindow, "Volume", "%", startingVolume, 0, 100, null, null, InternalVolumeChangedEvent::new);

        mainWindow.onEvent(InternalVolumeChangedEvent.class, volumeChangedEvent -> {
            volumeSlider.removeChangeListener(volumeSiderChangeListener);
            volumeSlider.setValue(volumeChangedEvent.getNewVolume());
            volumeSlider.addChangeListener(volumeSiderChangeListener);

            volumeSpinnerSetting.setValue(volumeChangedEvent.getNewVolume());
        });

        this.addIntegerSetting(volumeSpinnerSetting);
        this.innerPanel.add(volumeSlider, "growx, align center, wrap");

        EnumPanelSetting<SampleRate, ?> sampleRateSetting = this.addEnumSetting("Sample Rate", mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getSampleRate(), null, null, InternalSampleRateChangedEvent::new);
        mainWindow.onEvent(InternalSampleRateChangedEvent.class, sampleRateChangedEvent -> sampleRateSetting.setValue(sampleRateChangedEvent.getSampleRate()));
    }

}
