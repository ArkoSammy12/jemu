package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalSampleRateChangedEvent;
import org.jetbrains.annotations.NotNull;

public sealed interface SampleRateChangedEvent extends AudioSettingChangeEvent permits InternalSampleRateChangedEvent {

    @NotNull
    SampleRate getSampleRate();

}
