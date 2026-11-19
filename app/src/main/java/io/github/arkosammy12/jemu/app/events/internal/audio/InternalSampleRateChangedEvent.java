package io.github.arkosammy12.jemu.app.events.internal.audio;

import io.github.arkosammy12.jemu.app.audio.SampleRate;
import io.github.arkosammy12.jemu.app.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;
import org.jetbrains.annotations.NotNull;

public record InternalSampleRateChangedEvent(@NotNull SampleRate sampleRate) implements InternalEvent, SampleRateChangedEvent, ListenableEvent {

    @Override
    @NotNull
    public SampleRate getSampleRate() {
        return this.sampleRate();
    }

}
