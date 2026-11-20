package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.ListenableEvent;

public record VolumeChangedEvent(int newVolume) implements AudioSettingChangedEvent, ListenableEvent {}
