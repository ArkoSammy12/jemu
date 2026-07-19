package io.github.arkosammy12.jemu.app.util;

import io.github.arkosammy12.jemu.frontend.events.core.VideoSettingChangedEvent;

public interface FrameRequesterVideoEvent extends VideoSettingChangedEvent {

    default boolean invalidatesDisplay() {
        return false;
    }

}