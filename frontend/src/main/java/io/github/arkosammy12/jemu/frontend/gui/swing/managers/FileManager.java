package io.github.arkosammy12.jemu.frontend.gui.swing.managers;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public interface FileManager {

    void loadFile(@NotNull Path filePath);

    Optional<Path> getSelectedRomPath();

}
