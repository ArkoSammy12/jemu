package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

public sealed interface EmulationSettingElement permits EmulationSetting, EmulationSettingSection {

    String getName();

}
