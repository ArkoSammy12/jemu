package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.audio.SoundDevice;
import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import org.jetbrains.annotations.Nullable;

public record SoundDeviceChangedEvent(@Nullable SoundDevice soundDevice) implements AudioSettingChangedEvent {}
