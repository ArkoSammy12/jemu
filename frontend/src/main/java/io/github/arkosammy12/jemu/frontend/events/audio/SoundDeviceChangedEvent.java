package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.audio.SoundDevice;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalSoundDeviceChangedEvent;

import java.util.Optional;

public sealed interface SoundDeviceChangedEvent extends AudioSettingChangedEvent permits InternalSoundDeviceChangedEvent {

    Optional<SoundDevice> getSoundDevice();

}
