package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract sealed class SystemSetting<T> implements SystemSettingElement permits BooleanSystemSetting, EnumSystemSetting {

    private final String name;
    private final T startingValue;
    private final Function<? super T, ? extends Event> eventSupplier;

    public SystemSetting(@NotNull String name, @NotNull T startingValue, @NotNull Function<? super T, ? extends Event> eventSupplier) {
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
