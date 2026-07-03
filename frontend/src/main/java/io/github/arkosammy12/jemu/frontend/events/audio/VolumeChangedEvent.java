package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;

public record VolumeChangedEvent(int newVolume) implements AudioSettingChangeEvent {}
