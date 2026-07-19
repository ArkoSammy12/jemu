package io.github.arkosammy12.jemu.frontend.gui.system.builder.internal;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.EmulationSettingsBuilder;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.EmulationSettingElement;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EmulationSettingsElementBuilderCollector implements EmulationSettingsBuilder {

    private final List<SystemSettingElementBuilder> elementBuilders = new ArrayList<>();

    @Override
    public EmulationSettingsBuilder addSection(String name, Consumer<EmulationSettingsBuilder> sectionBuilder) {
        EmulationSettingSectionBuilder section = new EmulationSettingSectionBuilder(name);
        sectionBuilder.accept(section);
        elementBuilders.add(section);
        return this;
    }

    @Override
    public EmulationSettingsBuilder addIntegerSetting(String name, int startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, Function<? super Integer, ? extends Event> eventSupplier) {
        elementBuilders.add(new IntegerSettingBuilder(name, startingValue, minimumValue, maximumValue, eventSupplier));
        return this;
    }

    @Override
    public EmulationSettingsBuilder addBooleanSetting(String name, boolean startingValue, Function<? super Boolean, ? extends Event> eventSupplier) {
        elementBuilders.add(new BooleanSettingBuilder(name, startingValue, eventSupplier));
        return this;
    }

    @Override
    public <E extends Enum<E> & DisplayNamerProvider> EmulationSettingsBuilder addEnumSetting(String name, E startingValue, Function<? super E, ? extends Event> eventSupplier) {
        elementBuilders.add(new EnumSettingBuilder<>(name, startingValue, eventSupplier));
        return this;
    }

    List<EmulationSettingElement> build() {
        return this.elementBuilders.stream().map(SystemSettingElementBuilder::build).toList();
    }

}
