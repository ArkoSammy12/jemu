package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.audio.SoundDevice;
import io.github.arkosammy12.jemu.frontend.events.audio.SoundDeviceChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record InternalSoundDeviceChangedEvent(@Nullable SoundDevice soundDevice) implements InternalEvent, SoundDeviceChangedEvent {

    @Override
    public Optional<SoundDevice> getSoundDevice() {
        return Optional.ofNullable(this.soundDevice());
    }

}
