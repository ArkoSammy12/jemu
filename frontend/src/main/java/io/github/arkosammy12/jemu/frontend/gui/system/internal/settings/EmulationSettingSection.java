package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

import java.util.Collection;

public record EmulationSettingSection(String name, Collection<EmulationSettingElement> emulationSettingElements) implements EmulationSettingElement {

    @Override
    public String getName() {
        return this.name();
    }

}
