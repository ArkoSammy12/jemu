package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract sealed class EmulationSetting<T> implements EmulationSettingElement permits BooleanEmulationSetting, EnumEmulationSetting, IntegerEmulationSetting {

    private final String name;
    protected final T startingValue;
    private final Function<? super T, ? extends Event> eventSupplier;

    public EmulationSetting(@NotNull String name, @NotNull T startingValue, @NotNull Function<? super T, ? extends Event> eventSupplier) {
        this.name = name;
        this.startingValue = startingValue;
        this.eventSupplier = eventSupplier;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public T getStartingValue() {
        return this.startingValue;
    }

    public Function<? super T, ? extends Event> getEventSupplier() {
        return this.eventSupplier;
    }

}
