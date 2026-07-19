package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class EnumEmulationSetting<E extends Enum<E> & DisplayNamerProvider> extends EmulationSetting<E> {

    public EnumEmulationSetting(@NotNull String name, @NotNull E startingValue, @NotNull Function<? super E, ? extends Event> eventSupplier) {
        super(name, startingValue, eventSupplier);
    }

    public E[] getEnumVariants() {
        return this.getStartingValue().getDeclaringClass().getEnumConstants();
    }

}
