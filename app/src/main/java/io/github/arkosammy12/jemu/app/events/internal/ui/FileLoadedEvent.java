package io.github.arkosammy12.jemu.app.events.internal.ui;

import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record FileLoadedEvent(@NotNull Path loadedFilePath) implements ListenableEvent {}
