package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.events.audio.AudioLatencyChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;

public record InternalAudioLatencyChangedEvent(int latencyMs) implements InternalEvent, ListenableEvent, AudioLatencyChangedEvent {

    @Override
    public int getLatencyMs() {
        return this.latencyMs();
    }

}
