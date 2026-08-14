package io.github.arkosammy12.jemu.frontend.util.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class EnumUISetting<V extends Enum<V> & DisplayNamerProvider, E extends Event & Supplier<V>> extends UISetting<V, E> {

    protected final Class<V> enumClass;

    public EnumUISetting(EventPublisher eventPublisher, @NotNull String name, @NotNull V startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super V, ? extends Event> eventSupplier) {
        this.enumClass = startingValue.getDeclaringClass();
        super(eventPublisher, name, eventClass, eventPredicate, eventSupplier);
    }

}
