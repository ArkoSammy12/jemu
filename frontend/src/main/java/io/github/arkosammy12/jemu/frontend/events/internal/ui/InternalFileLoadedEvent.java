package io.github.arkosammy12.jemu.frontend.events.internal.ui;

import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record InternalFileLoadedEvent(@NotNull Path loadedFilePath) implements InternalEvent {}
