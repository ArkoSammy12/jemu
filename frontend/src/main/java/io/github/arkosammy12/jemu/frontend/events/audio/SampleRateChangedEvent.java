package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import org.jetbrains.annotations.NotNull;

public record SampleRateChangedEvent(@NotNull SampleRate sampleRate) implements AudioSettingChangeEvent {}
