package io.github.arkosammy12.jemu.frontend.util.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class IntegerUISetting<E extends Event & Supplier<Integer>> extends UISetting<Integer, E> {

    @Nullable
    protected final Integer minimumValue;

    @Nullable
    protected final Integer maximumValue;

    public IntegerUISetting(EventPublisher eventPublisher, @NotNull String name, @NotNull Integer startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Integer, ? extends Event> eventSupplier) {
        super(eventPublisher, name, eventClass, eventPredicate, eventSupplier);
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;

        if (minimumValue != null && maximumValue != null && minimumValue > maximumValue) {
            throw new IllegalArgumentException("Minimum value %d for setting '%s' is greater than maximum value %d".formatted(minimumValue, name, maximumValue));
        }

        if (maximumValue != null && startingValue > maximumValue) {
            throw new IllegalArgumentException("Starting value %d for setting '%s' exceeds maximum value %d".formatted(startingValue, name, maximumValue));
        } else if (minimumValue != null && startingValue < minimumValue) {
            throw new IllegalArgumentException("Starting value %d for setting '%s' is below minimum value %d".formatted(startingValue, name, minimumValue));
        }
    }

}
