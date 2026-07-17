package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.audio.SoundDevice;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalSoundDeviceChangedEvent;

import java.util.Optional;

public sealed interface SoundDeviceChangedEvent extends AudioSettingChangeEvent permits InternalSoundDeviceChangedEvent {

    Optional<SoundDevice> getSoundDevice();

}
