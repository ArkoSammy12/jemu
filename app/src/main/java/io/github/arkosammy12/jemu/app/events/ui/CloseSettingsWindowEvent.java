package io.github.arkosammy12.jemu.app.events.ui;

import io.github.arkosammy12.jemu.app.events.ListenableEvent;

public record CloseSettingsWindowEvent(Type type) implements ListenableEvent {

    public enum Type {
        SAVE_CHANGES,
        DISCARD_CHANGES
    }

}
