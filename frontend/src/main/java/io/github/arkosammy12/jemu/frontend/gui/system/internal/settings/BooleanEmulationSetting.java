package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;

import java.util.function.Function;

public final class BooleanEmulationSetting extends EmulationSetting<Boolean> {

    public BooleanEmulationSetting(String name, Boolean startingValue, Function<? super Boolean, ? extends Event> eventSupplier) {
        super(name, startingValue, eventSupplier);
    }

}
