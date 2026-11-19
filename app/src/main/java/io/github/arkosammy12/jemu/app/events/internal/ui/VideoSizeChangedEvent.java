package io.github.arkosammy12.jemu.app.events.internal.ui;

import io.github.arkosammy12.jemu.app.config.settings.internal.VideoSize;
import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;
import org.jetbrains.annotations.Nullable;

public record VideoSizeChangedEvent(@Nullable VideoSize videoSize) implements ListenableEvent {}
