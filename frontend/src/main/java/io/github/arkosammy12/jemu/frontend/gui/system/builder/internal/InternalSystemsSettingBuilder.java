package io.github.arkosammy12.jemu.frontend.gui.system.builder.internal;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.SystemSettingsBuilder;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.SystemSettingElement;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public final class InternalSystemsSettingBuilder implements SystemSettingsBuilder {

    private final SystemSettingsElementBuilderCollector systemSettingsElementBuilderCollector = new SystemSettingsElementBuilderCollector();

    @Override
    public SystemSettingsBuilder addSection(String name, Consumer<SystemSettingsBuilder> sectionBuilder) {
        this.systemSettingsElementBuilderCollector.addSection(name, sectionBuilder);
        return this;
    }

    @Override
    public SystemSettingsBuilder addBooleanSetting(String name, Boolean startingValue, Function<? super Boolean, ? extends Event> eventSupplier) {
        this.systemSettingsElementBuilderCollector.addBooleanSetting(name, startingValue, eventSupplier);
        return this;
    }

    @Override
    public <E extends Enum<E> & DisplayNamerProvider> SystemSettingsBuilder addEnumSetting(String name, E startingValue, Function<? super E, ? extends Event> eventSupplier) {
        this.systemSettingsElementBuilderCollector.addEnumSetting(name, startingValue, eventSupplier);
        return this;
    }

    public Collection<SystemSettingElement> build() {
        return this.systemSettingsElementBuilderCollector.build();
    }

}
