package io.github.arkosammy12.jemu.frontend.gui.system.builder;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.InternalSystemsSettingBuilder;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.SystemSettingsElementBuilderCollector;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.SystemSettingSectionBuilder;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface SystemSettingsBuilder permits SystemSettingsElementBuilderCollector, InternalSystemsSettingBuilder, SystemSettingSectionBuilder {

    SystemSettingsBuilder addSection(String name, Consumer<SystemSettingsBuilder> sectionBuilder);

    SystemSettingsBuilder addBooleanSetting(String name, Boolean startingValue, Function<? super Boolean, ? extends Event> eventSupplier);

    <E extends Enum<E> & DisplayNamerProvider> SystemSettingsBuilder addEnumSetting(String name, E startingValue, Function<? super E, ? extends Event> eventSupplier);

}
