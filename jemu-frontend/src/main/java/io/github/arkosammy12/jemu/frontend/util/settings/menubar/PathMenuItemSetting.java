package io.github.arkosammy12.jemu.frontend.util.settings.menubar;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.PathUISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PathMenuItemSetting<E extends Event & Supplier<@Nullable Path>> extends PathUISetting<E> implements MenuItemSetting {

    private final JMenuItem openFileMenuItem;

    public PathMenuItemSetting(EventPublisher eventPublisher, PathSelectionMode pathSelectionMode, @NotNull String name, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super @Nullable Path, ? extends Event> eventSupplier) {
        super(eventPublisher, pathSelectionMode, name, eventClass, eventPredicate, eventSupplier);
        this.openFileMenuItem = new JMenuItem(name);
        this.openFileMenuItem.addActionListener(_ -> eventPublisher.publishEvent(eventSupplier.apply(this.showFileDialog(SwingUtilities.getWindowAncestor(this.openFileMenuItem)))));
    }

    @Override
    public void setValue(@Nullable Path value) {

    }

    @Override
    public void addToJMenu(JMenu jMenu) {
        jMenu.add(this.openFileMenuItem);
    }

}
