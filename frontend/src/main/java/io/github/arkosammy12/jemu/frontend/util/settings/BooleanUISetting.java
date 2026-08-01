package io.github.arkosammy12.jemu.frontend.util.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class BooleanUISetting<E extends Event & Supplier<Boolean>> extends UISetting<Boolean, E> {

    public BooleanUISetting(MainWindow mainWindow, @NotNull String name, @NotNull Boolean startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Boolean, ? extends Event> eventSupplier) {
        super(mainWindow, name, eventClass, eventPredicate, eventSupplier);
    }

}
