package io.github.arkosammy12.jemu.frontend.events.internal.core;

import io.github.arkosammy12.jemu.frontend.events.core.UseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;

public record InternalUseIntegerScalingSettingChangedEvent(boolean useIntegerScaling) implements InternalEvent, UseIntegerScalingSettingChangedEvent, ListenableEvent {}
