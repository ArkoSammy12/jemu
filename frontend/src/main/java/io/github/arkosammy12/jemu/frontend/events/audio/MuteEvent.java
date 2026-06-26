package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;

public record MuteEvent(boolean mute) implements AudioSettingChangeEvent {}
