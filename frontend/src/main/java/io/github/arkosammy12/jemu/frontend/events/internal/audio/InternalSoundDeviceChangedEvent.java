package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.audio.SoundDevice;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.audio.SoundDeviceChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ExposableEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalAudioSettingChangeEvent;
import org.jetbrains.annotations.Nullable;

public record InternalSoundDeviceChangedEvent(@Nullable SoundDevice soundDevice) implements InternalAudioSettingChangeEvent, ExposableEvent {

    @Override
    public Event getEvent() {
        return new SoundDeviceChangedEvent(this.soundDevice());
    }

}
