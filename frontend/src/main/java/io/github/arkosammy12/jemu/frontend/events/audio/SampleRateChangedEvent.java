package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalSampleRateChangedEvent;
import org.jetbrains.annotations.NotNull;

public sealed interface SampleRateChangedEvent extends AudioSettingChangedEvent permits InternalSampleRateChangedEvent {

    @NotNull
    SampleRate getSampleRate();

}
