package io.github.arkosammy12.jemu.frontend.gui.system.builder;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.InternalEmulationSettingBuilder;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.EmulationSettingsElementBuilderCollector;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.EmulationSettingSectionBuilder;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface EmulationSettingsBuilder permits EmulationSettingsElementBuilderCollector, InternalEmulationSettingBuilder, EmulationSettingSectionBuilder {

    EmulationSettingsBuilder addSection(String name, Consumer<EmulationSettingsBuilder> sectionBuilder);

    EmulationSettingsBuilder addIntegerSetting(String name, int startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, Function<? super Integer, ? extends Event> eventSupplier);

    EmulationSettingsBuilder addBooleanSetting(String name, boolean startingValue, Function<? super Boolean, ? extends Event> eventSupplier);

    <E extends Enum<E> & DisplayNamerProvider> EmulationSettingsBuilder addEnumSetting(String name, E startingValue, Function<? super E, ? extends Event> eventSupplier);

}
