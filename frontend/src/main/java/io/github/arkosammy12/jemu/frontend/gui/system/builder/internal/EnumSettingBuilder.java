package io.github.arkosammy12.jemu.frontend.gui.system.builder.internal;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.EnumSystemSetting;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.SystemSettingElement;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

import java.util.function.Function;

class EnumSettingBuilder<E extends Enum<E> & DisplayNamerProvider> extends SystemSettingElementBuilder {

    private final E startingValue;
    private final Function<? super E, ? extends Event> eventSupplier;

    EnumSettingBuilder(String name, E startingValue, Function<? super E, ? extends Event> eventSupplier) {
        super(name);
        this.startingValue = startingValue;
        this.eventSupplier = eventSupplier;
    }

    @Override
    protected SystemSettingElement build() {
        return new EnumSystemSetting<>(this.name, this.startingValue, this.eventSupplier);
    }

}
