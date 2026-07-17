package io.github.arkosammy12.jemu.frontend.events.core;

import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalUseIntegerScalingSettingChangedEvent;

public sealed interface UseIntegerScalingSettingChangedEvent extends VideoSettingChangedEvent permits InternalUseIntegerScalingSettingChangedEvent {

    boolean useIntegerScaling();

}
