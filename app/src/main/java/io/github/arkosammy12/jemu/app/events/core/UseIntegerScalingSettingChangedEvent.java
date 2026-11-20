package io.github.arkosammy12.jemu.app.events.core;

import io.github.arkosammy12.jemu.app.events.ListenableEvent;
import io.github.arkosammy12.jemu.app.events.VideoSettingChangedEvent;

public record UseIntegerScalingSettingChangedEvent(boolean useIntegerScaling) implements VideoSettingChangedEvent, ListenableEvent {}
