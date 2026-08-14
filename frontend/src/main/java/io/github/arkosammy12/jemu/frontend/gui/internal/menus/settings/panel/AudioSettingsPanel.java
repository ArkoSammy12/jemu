package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.panel;

import io.github.arkosammy12.jemu.frontend.audio.AudioEngine;
import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalAudioLatencyChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalMuteEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalSampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalVolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.EnumPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.IntegerPanelSpinnerSetting;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.util.function.IntConsumer;

public class AudioSettingsPanel extends PanelSettingsMenu {

    public AudioSettingsPanel(MainWindow mainWindow, EventPublisher eventPublisher) {
        super(eventPublisher);

        this.addHeader("General");
        BooleanPanelSetting<?> muteSetting = this.addBooleanSetting("Mute audio", mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getMute(), null, null, InternalMuteEvent::new);
        mainWindow.onEvent(InternalMuteEvent.class, muteEvent -> muteSetting.setValue(muteEvent.getMute()));

        int startingVolume = mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getVolume();

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

            eventPublisher.publishEvent(new InternalVolumeChangedEvent(volumeUIChanged.volume()));
        });

        mainWindow.onEvent(InternalVolumeChangedEvent.class, volumeChangedEvent -> {
            volumeSlider.removeChangeListener(volumeSliderChangeListener);
            volumeSlider.setValue(volumeChangedEvent.getNewVolume());
            volumeSlider.addChangeListener(volumeSliderChangeListener);

            volumeSpinnerSetting.setValue(volumeChangedEvent.getNewVolume());
        });

        this.addIntegerSetting(volumeSpinnerSetting);
        this.innerPanel.add(volumeSlider, "growx, align center, wrap");

        EnumPanelSetting<SampleRate, ?> sampleRateSetting = this.addEnumSetting("Sample Rate", mainWindow.getConfig().getInternalPreferenceSettings().getInternalAudioSettings().getSampleRate(), null, null, InternalSampleRateChangedEvent::new);
        mainWindow.onEvent(InternalSampleRateChangedEvent.class, sampleRateChangedEvent -> sampleRateSetting.setValue(sampleRateChangedEvent.getSampleRate()));

        IntegerPanelSpinnerSetting<?> latencySetting = this.addIntegerSetting("Latency: ", "ms", mainWindow.getConfigurations().getSettings().getAudioSettings().getLatencyMs(), AudioEngine.MIN_LATENCY_MS, AudioEngine.MAX_LATENCY_MS, null, null, InternalAudioLatencyChangedEvent::new);
        mainWindow.onEvent(InternalAudioLatencyChangedEvent.class, internalAudioLatencyChangedEvent -> latencySetting.setValue(internalAudioLatencyChangedEvent.getLatencyMs()));
    }

    private record VolumeUIChanged(int volume) implements ListenableEvent, Event {}

}
