package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.audio.SoundDevice;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import org.jetbrains.annotations.Nullable;

public record SoundDeviceChangedEvent(@Nullable SoundDevice soundDevice) implements AudioSettingChangeEvent {}
