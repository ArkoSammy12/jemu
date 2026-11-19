package io.github.arkosammy12.jemu.app.events.core;

import io.github.arkosammy12.jemu.app.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.app.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.core.InternalSpeedModeSettingChangedEvent;

public sealed interface SpeedModeSettingChangedEvent extends CoreSettingChangedEvent permits InternalSpeedModeSettingChangedEvent {

    SpeedMode getSpeedMode();

}
