package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.audio.SampleRate;
import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.audio.InternalSampleRateChangedEvent;
import org.jetbrains.annotations.NotNull;

public sealed interface SampleRateChangedEvent extends AudioSettingChangedEvent permits InternalSampleRateChangedEvent {

    @NotNull
    SampleRate getSampleRate();

}
