package io.github.arkosammy12.jemu.frontend.util.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class EnumUISetting<V extends Enum<V> & DisplayNamerProvider, E extends Event & Supplier<V>> extends UISetting<V, E> {

    protected final Class<V> enumClass;

    public EnumUISetting(MainWindow mainWindow, @NotNull String name, @NotNull V startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super V, ? extends Event> eventSupplier) {
        this.enumClass = startingValue.getDeclaringClass();
        super(mainWindow, name, startingValue, eventClass, eventPredicate, eventSupplier);
    }

}
