package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ExposableEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalAudioSettingChangeEvent;
import org.jetbrains.annotations.NotNull;

public record InternalSampleRateChangedEvent(@NotNull SampleRate sampleRate) implements InternalAudioSettingChangeEvent, ExposableEvent {

    @Override
    public Event getEvent() {
        return new SampleRateChangedEvent(sampleRate);
    }

}
