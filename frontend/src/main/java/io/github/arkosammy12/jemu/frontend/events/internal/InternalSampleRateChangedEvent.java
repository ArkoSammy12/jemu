package io.github.arkosammy12.jemu.frontend.events.internal;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.SampleRateChangedEvent;

public record InternalSampleRateChangedEvent(SampleRate sampleRate) implements InternalEvent {

    @Override
    public Event getEvent() {
        return new SampleRateChangedEvent(sampleRate);
    }

}
