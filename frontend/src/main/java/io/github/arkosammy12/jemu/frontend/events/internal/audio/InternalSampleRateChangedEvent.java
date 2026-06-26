package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalAudioSettingChangeEvent;

public record InternalSampleRateChangedEvent(SampleRate sampleRate) implements InternalAudioSettingChangeEvent {

    @Override
    public Event getEvent() {
        return new SampleRateChangedEvent(sampleRate);
    }

}
