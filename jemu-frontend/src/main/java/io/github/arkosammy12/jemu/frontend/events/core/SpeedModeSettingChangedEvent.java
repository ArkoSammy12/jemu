package io.github.arkosammy12.jemu.frontend.events.core;

import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalSpeedModeSettingChangedEvent;

public sealed interface SpeedModeSettingChangedEvent extends CoreSettingChangedEvent permits InternalSpeedModeSettingChangedEvent {

    SpeedMode getSpeedMode();

}
