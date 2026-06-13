package io.github.arkosammy12.jemu.frontend.gui.swing.managers;

import java.nio.file.Path;
import java.util.Optional;

public interface FileManager {

    void loadFile(Path filePath);

    Optional<Path> getSelectedRomPath();

}
