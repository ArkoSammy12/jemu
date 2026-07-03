package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.events.audio.MuteEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ExposableEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalAudioSettingChangeEvent;

public record InternalMuteEvent(boolean mute) implements InternalAudioSettingChangeEvent, ExposableEvent {

    @Override
    public MuteEvent getEvent() {
        return new MuteEvent(this.mute());
    }

}
