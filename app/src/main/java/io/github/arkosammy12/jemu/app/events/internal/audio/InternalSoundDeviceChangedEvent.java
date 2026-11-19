package io.github.arkosammy12.jemu.app.events.internal.audio;

import io.github.arkosammy12.jemu.app.audio.SoundDevice;
import io.github.arkosammy12.jemu.app.events.audio.SoundDeviceChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record InternalSoundDeviceChangedEvent(@Nullable SoundDevice soundDevice) implements InternalEvent, SoundDeviceChangedEvent {

    @Override
    public Optional<SoundDevice> getSoundDevice() {
        return Optional.ofNullable(this.soundDevice());
    }

}
