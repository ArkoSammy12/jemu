package io.github.arkosammy12.jemu.app.gui.menus.settings.panel;

import io.github.arkosammy12.jemu.app.audio.AudioEngine;
import io.github.arkosammy12.jemu.app.audio.SampleRate;
import io.github.arkosammy12.jemu.app.events.Event;
import io.github.arkosammy12.jemu.app.events.ListenableEvent;
import io.github.arkosammy12.jemu.app.events.audio.AudioLatencyChangedEvent;
import io.github.arkosammy12.jemu.app.events.audio.MuteEvent;
import io.github.arkosammy12.jemu.app.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.app.events.audio.VolumeChangedEvent;
import io.github.arkosammy12.jemu.app.gui.MainWindow;
import io.github.arkosammy12.jemu.app.util.EventPublisher;
import io.github.arkosammy12.jemu.app.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.app.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.app.util.settings.panel.EnumPanelSetting;
import io.github.arkosammy12.jemu.app.util.settings.panel.IntegerPanelSpinnerSetting;

import javax.swing.*;
import javax.swing.event.ChangeListener;

public class AudioSettingsPanel extends PanelSettingsMenu {

    public AudioSettingsPanel(MainWindow mainWindow, EventPublisher eventPublisher) {
        super(eventPublisher);

        this.addHeader("General");
        BooleanPanelSetting<?> muteSetting = this.addBooleanSetting("Mute audio", mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getMute(), null, null, MuteEvent::new);
        mainWindow.onEvent(MuteEvent.class, muteEvent -> muteSetting.setValue(muteEvent.mute()));

        int startingVolume = mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getVolume();

        JSlider volumeSlider = new JSlider(AudioEngine.MIN_VOLUME, AudioEngine.MAX_VOLUME, startingVolume);
        volumeSlider.setPaintTrack(true);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        ChangeListener volumeSliderChangeListener = _ -> eventPublisher.publishEvent(new VolumeUIChanged(Math.clamp(volumeSlider.getValue(), 0, 100)));
        volumeSlider.addChangeListener(volumeSliderChangeListener);

        IntegerPanelSpinnerSetting<?> volumeSpinnerSetting = new IntegerPanelSpinnerSetting<>(eventPublisher, "Volume: ", "%", startingVolume, 0, 100, null, null, VolumeUIChanged::new);

        mainWindow.onEvent(VolumeUIChanged.class, volumeUIChanged -> {
            volumeSlider.removeChangeListener(volumeSliderChangeListener);
            volumeSlider.setValue(volumeUIChanged.volume());
            volumeSlider.addChangeListener(volumeSliderChangeListener);

            volumeSpinnerSetting.setValue(volumeUIChanged.volume());

            eventPublisher.publishEvent(new VolumeChangedEvent(volumeUIChanged.volume()));
        });

        mainWindow.onEvent(VolumeChangedEvent.class, volumeChangedEvent -> {
            volumeSlider.removeChangeListener(volumeSliderChangeListener);
            volumeSlider.setValue(volumeChangedEvent.newVolume());
            volumeSlider.addChangeListener(volumeSliderChangeListener);

            volumeSpinnerSetting.setValue(volumeChangedEvent.newVolume());
        });

        this.addIntegerSetting(volumeSpinnerSetting);
        this.innerPanel.add(volumeSlider, "growx, align center, wrap");

        EnumPanelSetting<SampleRate, ?> sampleRateSetting = this.addEnumSetting("Sample Rate", mainWindow.getConfig().getPreferenceSettings().getAudioSettings().getSampleRate(), null, null, SampleRateChangedEvent::new);
        mainWindow.onEvent(SampleRateChangedEvent.class, sampleRateChangedEvent -> sampleRateSetting.setValue(sampleRateChangedEvent.sampleRate()));

        IntegerPanelSpinnerSetting<?> latencySetting = this.addIntegerSetting("Latency: ", "ms", mainWindow.getConfigurations().getSettings().getAudioSettings().getLatencyMs(), AudioEngine.MIN_LATENCY_MS, AudioEngine.MAX_LATENCY_MS, null, null, AudioLatencyChangedEvent::new);
        mainWindow.onEvent(AudioLatencyChangedEvent.class, audioLatencyChangedEvent -> latencySetting.setValue(audioLatencyChangedEvent.latencyMs()));
    }

    private record VolumeUIChanged(int volume) implements ListenableEvent, Event {}

}
