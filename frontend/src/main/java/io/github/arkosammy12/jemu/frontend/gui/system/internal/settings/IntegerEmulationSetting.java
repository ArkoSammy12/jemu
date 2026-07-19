package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.Function;

public final class IntegerEmulationSetting extends EmulationSetting<Integer> {

    @Nullable
    private final Integer minimumValue;

    @Nullable
    private final Integer maximumValue;

    public IntegerEmulationSetting(@NotNull String name, @NotNull Integer startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, @NotNull Function<? super Integer, ? extends Event> eventSupplier) {
        super(name, startingValue, eventSupplier);
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;

        if (this.minimumValue != null && this.maximumValue != null && this.minimumValue > this.maximumValue) {
            throw new IllegalArgumentException("Minimum value %d for setting '%s' is greater than maximum value %d".formatted(this.minimumValue, name, this.maximumValue));
        }

        if (this.maximumValue != null && this.startingValue > this.maximumValue) {
            throw new IllegalArgumentException("Starting value %d for setting '%s' exceeds maximum value %d".formatted(this.startingValue, name, this.maximumValue));
        } else if (this.minimumValue != null && this.startingValue < this.minimumValue) {
            throw new IllegalArgumentException("Starting value %d for setting '%s' is below minimum value %d".formatted(this.startingValue, name, this.minimumValue));
        }
    }

    public OptionalInt getMinimumValue() {
        return this.minimumValue == null ? OptionalInt.empty() : OptionalInt.of(this.minimumValue);
    }

    public OptionalInt getMaximumValue() {
        return this.maximumValue == null ? OptionalInt.empty() : OptionalInt.of(this.maximumValue);
    }

}
