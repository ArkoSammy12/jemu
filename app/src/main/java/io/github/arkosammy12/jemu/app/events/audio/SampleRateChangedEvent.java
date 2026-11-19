package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.audio.SampleRate;
import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.ListenableEvent;
import org.jetbrains.annotations.NotNull;

public record SampleRateChangedEvent(@NotNull SampleRate sampleRate) implements AudioSettingChangedEvent, ListenableEvent {}
