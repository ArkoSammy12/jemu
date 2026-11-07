package io.github.arkosammy12.jemu.frontend.util.settings;

import com.formdev.flatlaf.util.SystemFileChooser;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class PathUISetting<E extends Event & Supplier<@Nullable Path>> extends UISetting<@Nullable Path, E> {

    private final PathSelectionMode pathSelectionMode;

    public PathUISetting(EventPublisher eventPublisher, PathSelectionMode pathSelectionMode, @NotNull String name, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super @Nullable Path, ? extends Event> eventSupplier) {
        super(eventPublisher, name, eventClass, eventPredicate, eventSupplier);
        this.pathSelectionMode = pathSelectionMode;
    }

    @Nullable
    protected Path showFileDialog(Component parent) {
        SystemFileChooser fileChooser = new SystemFileChooser();
        fileChooser.setFileSelectionMode(this.pathSelectionMode.getFileSelectionMode());
        fileChooser.setDialogTitle(this.pathSelectionMode.getDialogTitle());
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().toPath();
        } else {
            return null;
        }
    }

    public enum PathSelectionMode {
        FILES_ONLY(JFileChooser.FILES_ONLY, "Select a File"),
        DIRECTORIES_ONLY(JFileChooser.DIRECTORIES_ONLY, "Select a Directory"),
        FILES_AND_DIRECTORIES(JFileChooser.FILES_AND_DIRECTORIES, "Select a File or Directory")
        ;

        private final int fileSelectionMode;
        private final String dialogTitle;

        PathSelectionMode(int fileSelectionMode, String dialogTitle) {
            this.fileSelectionMode = fileSelectionMode;
            this.dialogTitle = dialogTitle;
        }

        private int getFileSelectionMode() {
            return this.fileSelectionMode;
        }

        private String getDialogTitle() {
            return this.dialogTitle;
        }

    }

}
