package io.github.arkosammy12.jemu.frontend.events.core;

public record UseIntegerScalingSettingChangedEvent(boolean useIntegerScaling) implements VideoSettingChangedEvent {}
