package io.github.arkosammy12.jemu.app.util;

import java.nio.file.Path;
import java.util.Collection;

public interface InputListener {

    void onKeyActionPressed(KeyAction keyAction);

    void onKeyActionReleased(KeyAction keyAction);

    default boolean onFilesDropped(EventPublisher eventPublisher, Collection<Path> paths) {
        return false;
    }

}
