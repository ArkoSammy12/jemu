package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;

public record SampleRateChangedEvent(SampleRate sampleRate) implements AudioSettingChangeEvent {}
