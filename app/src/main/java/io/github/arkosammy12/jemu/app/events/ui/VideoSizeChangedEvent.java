package io.github.arkosammy12.jemu.app.events.ui;

import io.github.arkosammy12.jemu.app.config.settings.VideoSize;
import io.github.arkosammy12.jemu.app.events.ListenableEvent;
import org.jetbrains.annotations.Nullable;

public record VideoSizeChangedEvent(@Nullable VideoSize videoSize) implements ListenableEvent {}
