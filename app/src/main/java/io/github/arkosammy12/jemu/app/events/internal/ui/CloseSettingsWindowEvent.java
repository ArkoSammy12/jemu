package io.github.arkosammy12.jemu.app.events.internal.ui;

import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;

public record CloseSettingsWindowEvent(Type type) implements ListenableEvent {

    public enum Type {
        SAVE_CHANGES,
        DISCARD_CHANGES
    }

}
