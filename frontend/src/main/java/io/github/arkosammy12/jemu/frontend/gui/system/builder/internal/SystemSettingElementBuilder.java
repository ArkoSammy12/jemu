package io.github.arkosammy12.jemu.frontend.gui.system.builder.internal;

import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.EmulationSettingElement;

abstract class SystemSettingElementBuilder {

    protected final String name;

    SystemSettingElementBuilder(String name) {
        this.name = name;
    }

    protected abstract EmulationSettingElement build();

}
