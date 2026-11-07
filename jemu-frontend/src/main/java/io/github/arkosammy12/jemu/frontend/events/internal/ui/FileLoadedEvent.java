package io.github.arkosammy12.jemu.frontend.events.internal.ui;

import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record FileLoadedEvent(@NotNull Path loadedFilePath) implements ListenableEvent {}
