package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.InternalSystemsSettingBuilder;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.*;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

import javax.swing.*;
import java.util.Collection;

public class EmulationSettings extends MenuBarMenu {

    private final MainWindow mainWindow;

    public EmulationSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Emulation");

        this.mainWindow = mainWindow;
        InternalSystemsSettingBuilder settingsBuilder = new InternalSystemsSettingBuilder();
        mainWindow.getSystemCatalog().buildSystemSettings(settingsBuilder);
        this.buildSection(settingsBuilder.build(), this.getJMenu());
    }

    private void buildSection(Collection<SystemSettingElement> systemSettingElements, JMenu sectionMenu) {
        for (SystemSettingElement systemSettingElement : systemSettingElements) {
            switch (systemSettingElement) {
                case SystemSetting<?> systemSetting -> {
                    switch (systemSetting) {
                        case BooleanSystemSetting booleanSystemSetting -> {
                            JRadioButtonMenuItem booleanSettingButton = new JRadioButtonMenuItem(booleanSystemSetting.getName());
                            booleanSettingButton.setSelected(booleanSystemSetting.getStartingValue());
                            booleanSettingButton.addActionListener(_ -> this.mainWindow.publishEvent(booleanSystemSetting.getEventSupplier().apply(booleanSettingButton.isSelected())));
                            sectionMenu.add(booleanSettingButton);
                        }
                        case EnumSystemSetting<?> enumSystemSetting -> sectionMenu.add(this.buildEnumSetting(enumSystemSetting));
                    }
                }
                case SystemSettingSection systemSettingSection -> {
                    JMenu nestedSectionMenu = new JMenu(systemSettingSection.getName());
                    this.buildSection(systemSettingSection.systemSettingElements(), nestedSectionMenu);
                    sectionMenu.add(nestedSectionMenu);
                }
            }
        }
    }

    private <E extends Enum<E> & DisplayNamerProvider> JMenu buildEnumSetting(EnumSystemSetting<E> enumSystemSetting) {
        JMenu enumSettingMenu = new JMenu(enumSystemSetting.getName());
        ButtonGroup enumButtonGroup = new ButtonGroup();

        for (E enumVariant : enumSystemSetting.getEnumVariants()) {
            JRadioButtonMenuItem enumSettingButton = new JRadioButtonMenuItem(enumVariant.getDisplayName());
            enumButtonGroup.add(enumSettingButton);
            enumSettingButton.setSelected(enumSystemSetting.getStartingValue() == enumVariant);
            enumSettingButton.addActionListener(_ -> this.mainWindow.publishEvent(enumSystemSetting.getEventSupplier().apply(enumVariant)));
            enumSettingMenu.add(enumSettingButton);
        }

        return enumSettingMenu;
    }

}
