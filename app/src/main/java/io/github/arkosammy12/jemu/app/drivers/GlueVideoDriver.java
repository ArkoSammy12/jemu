package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import io.github.arkosammy12.jemu.frontend.events.VideoSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.SystemDisplayComponent;

public interface GlueVideoDriver extends VideoDriver, SystemDisplayComponent {

    void requestFrame();

    void onVideoSettingChangedEvent(VideoSettingChangedEvent videoSettingChangedEvent);

    default void close() {

    }

}
