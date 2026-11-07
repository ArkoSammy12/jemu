package io.github.arkosammy12.jemu.frontend.events.internal.ui;

import io.github.arkosammy12.jemu.frontend.config.settings.internal.VideoSize;
import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;
import org.jetbrains.annotations.Nullable;

public record VideoSizeChangedEvent(@Nullable VideoSize videoSize) implements ListenableEvent {}
