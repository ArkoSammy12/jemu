package io.github.arkosammy12.jemu.frontend.gui.system.internal.settings;

public sealed interface SystemSettingElement permits SystemSetting, SystemSettingSection {

    String getName();

}
