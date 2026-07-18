package io.github.arkosammy12.jemu.frontend.gui.system.builder.internal;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.BooleanSystemSetting;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.SystemSettingElement;

import java.util.function.Function;

class BooleanSettingBuilder extends SystemSettingElementBuilder {

    private final boolean startingValue;
    private final Function<? super Boolean, ? extends Event> eventSupplier;

    BooleanSettingBuilder(String name, boolean startingValue, Function<? super Boolean, ? extends Event> eventSupplier) {
        super(name);
        this.startingValue = startingValue;
        this.eventSupplier = eventSupplier;
    }

    @Override
    protected SystemSettingElement build() {
        return new BooleanSystemSetting(this.name, this.startingValue, this.eventSupplier);
    }
}
