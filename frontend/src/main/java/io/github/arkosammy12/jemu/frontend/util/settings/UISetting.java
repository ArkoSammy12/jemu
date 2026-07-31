package io.github.arkosammy12.jemu.frontend.util.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class UISetting<T, E extends Event & Supplier<T>> {

    protected final MainWindow mainWindow;

    @Nullable
    private final Class<E> eventClass;

    @Nullable
    private final Predicate<E> eventPredicate;

    protected final String name;
    protected final T startingValue;
    protected final Function<? super T, ? extends Event> eventSupplier;

    @Nullable
    private volatile Consumer<T> onEventCallback;

    public UISetting(MainWindow mainWindow, @NotNull String name, @NotNull T startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super T, ? extends Event> eventSupplier) {
        this.mainWindow = mainWindow;
        this.eventClass = eventClass;
        this.eventPredicate = eventPredicate;
        this.name = name;
        this.startingValue = startingValue;
        this.eventSupplier = eventSupplier;
    }

    public UISetting(MainWindow mainWindow, @NotNull String name, @NotNull T startingValue, @Nullable Class<E> eventClass, @NotNull Function<? super T, ? extends Event> eventSupplier) {
        this(mainWindow, name, startingValue, eventClass, null, eventSupplier);
    }

    public UISetting(MainWindow mainWindow, @NotNull String name, @NotNull T startingValue, @NotNull Function<? super T, ? extends Event> eventSupplier) {
        this(mainWindow, name, startingValue, null, null, eventSupplier);
    }

    public void setOnEventCallback(@Nullable Consumer<T> onEventCallback) {
        this.onEventCallback = onEventCallback;
    }

    @SuppressWarnings("unchecked")
    public void onEvent(Event event) {
        if (this.eventClass != null && this.eventClass.isInstance(event)) {
            E eventInstance = (E) event;
            if (this.eventPredicate == null || this.eventPredicate.test(eventInstance)) {
                T value = eventInstance.get();
                this.setValue(value);
                Consumer<T> callback = this.onEventCallback;
                if (callback != null) {
                    callback.accept(value);
                }
            }
        }
    }

    abstract public void setValue(T value);

}
