package io.github.arkosammy12.jemu.app.events.internal.core;

import io.github.arkosammy12.jemu.app.events.core.UseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;

public record InternalUseIntegerScalingSettingChangedEvent(boolean useIntegerScaling) implements InternalEvent, UseIntegerScalingSettingChangedEvent, ListenableEvent {}
