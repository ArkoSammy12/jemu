package io.github.arkosammy12.jemu.frontend.events.internal.ui;

import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;

public record CloseSettingsWindowEvent(Type type) implements ListenableEvent {

    public enum Type {
        SAVE_CHANGES,
        DISCARD_CHANGES
    }

}
