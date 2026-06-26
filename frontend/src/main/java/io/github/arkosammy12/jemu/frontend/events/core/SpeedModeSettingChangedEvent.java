package io.github.arkosammy12.jemu.frontend.events.core;

import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;

public record SpeedModeSettingChangedEvent(SpeedMode speedMode) implements CoreSettingChangeEvent {}
