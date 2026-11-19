package io.github.arkosammy12.jemu.app.events.core;

import io.github.arkosammy12.jemu.app.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.app.events.CoreSettingChangedEvent;

public record SpeedModeSettingChangedEvent(SpeedMode speedMode) implements CoreSettingChangedEvent {}
