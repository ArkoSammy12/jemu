package io.github.arkosammy12.jemu.app.events.core;

import io.github.arkosammy12.jemu.app.events.VideoSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.core.InternalUseIntegerScalingSettingChangedEvent;

public sealed interface UseIntegerScalingSettingChangedEvent extends VideoSettingChangedEvent permits InternalUseIntegerScalingSettingChangedEvent {

    boolean useIntegerScaling();

}
