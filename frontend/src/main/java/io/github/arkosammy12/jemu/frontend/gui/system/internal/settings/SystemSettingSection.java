package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

import java.util.Collection;

public record SystemSettingSection(String name, Collection<SystemSettingElement> systemSettingElements) implements SystemSettingElement {

    @Override
    public String getName() {
        return this.name();
    }

}
