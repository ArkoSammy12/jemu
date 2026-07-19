package io.github.arkosammy12.jemu.frontend.gui.system.builder.internal;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.EmulationSettingsBuilder;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.EmulationSettingElement;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public final class InternalEmulationSettingBuilder implements EmulationSettingsBuilder {

    private final EmulationSettingsElementBuilderCollector systemSettingsElementBuilderCollector = new EmulationSettingsElementBuilderCollector();

    @Override
    public EmulationSettingsBuilder addSection(String name, Consumer<EmulationSettingsBuilder> sectionBuilder) {
        this.systemSettingsElementBuilderCollector.addSection(name, sectionBuilder);
        return this;
    }

    @Override
    public EmulationSettingsBuilder addIntegerSetting(String name, int startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, Function<? super Integer, ? extends Event> eventSupplier) {
        this.systemSettingsElementBuilderCollector.addIntegerSetting(name, startingValue, minimumValue, maximumValue, eventSupplier);
        return this;
    }

    @Override
    public EmulationSettingsBuilder addBooleanSetting(String name, boolean startingValue, Function<? super Boolean, ? extends Event> eventSupplier) {
        this.systemSettingsElementBuilderCollector.addBooleanSetting(name, startingValue, eventSupplier);
        return this;
    }

    @Override
    public <E extends Enum<E> & DisplayNamerProvider> EmulationSettingsBuilder addEnumSetting(String name, E startingValue, Function<? super E, ? extends Event> eventSupplier) {
        this.systemSettingsElementBuilderCollector.addEnumSetting(name, startingValue, eventSupplier);
        return this;
    }

    public Collection<EmulationSettingElement> build() {
        return this.systemSettingsElementBuilderCollector.build();
    }

}
