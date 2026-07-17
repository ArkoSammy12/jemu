package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import org.jetbrains.annotations.NotNull;

public record InternalSampleRateChangedEvent(@NotNull SampleRate sampleRate) implements InternalEvent, SampleRateChangedEvent {

    @Override
    @NotNull
    public SampleRate getSampleRate() {
        return this.sampleRate();
    }

}
