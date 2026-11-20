package io.github.arkosammy12.jemu.app.events.ui;

import io.github.arkosammy12.jemu.app.events.ListenableEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record LoadDragAndDroppedFileEvent(@NotNull Path path) implements ListenableEvent {}
