package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.audio.SoundDevice;
import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.audio.InternalSoundDeviceChangedEvent;

import java.util.Optional;

public sealed interface SoundDeviceChangedEvent extends AudioSettingChangedEvent permits InternalSoundDeviceChangedEvent {

    Optional<SoundDevice> getSoundDevice();

}
