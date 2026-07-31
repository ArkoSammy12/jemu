package io.github.arkosammy12.jemu.frontend.gui.system;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;

import javax.swing.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public interface SystemDescriptor {

    String getName();

    String getId();

    Collection<String> getFileExtensions();

    default Optional<Category> getCategory() {
        return Optional.empty();
    }

    default Optional<? extends Function<? super MainWindow, ? extends JMenu>> getSettingsMenuBarContents() {
        return Optional.empty();
    }

    default Optional<? extends Function<? super MainWindow, ? extends JPanel>> getSettingsWindowContents() {
        return Optional.empty();
    }

    interface Category {

        String getName();

    }

}
