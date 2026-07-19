package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.internal.InternalEmulationSettingBuilder;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.*;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

import javax.swing.*;
import java.util.Collection;

public class EmulationSettings extends MenuBarMenu {

    private final MainWindow mainWindow;

    public EmulationSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Emulation");

        this.mainWindow = mainWindow;
        InternalEmulationSettingBuilder settingsBuilder = new InternalEmulationSettingBuilder();
        mainWindow.getSystemCatalog().buildSystemSettings(settingsBuilder);
        this.buildSection(settingsBuilder.build(), this.getJMenu());
    }

    private void buildSection(Collection<EmulationSettingElement> emulationSettingElements, JMenu sectionMenu) {
        for (EmulationSettingElement emulationSettingElement : emulationSettingElements) {
            switch (emulationSettingElement) {
                case EmulationSetting<?> systemSetting -> {
                    switch (systemSetting) {
                        case BooleanEmulationSetting booleanSystemSetting -> {
                            JRadioButtonMenuItem booleanSettingButton = new JRadioButtonMenuItem(booleanSystemSetting.getName());
                            booleanSettingButton.setSelected(booleanSystemSetting.getStartingValue());
                            booleanSettingButton.addActionListener(_ -> this.mainWindow.publishEvent(booleanSystemSetting.getEventSupplier().apply(booleanSettingButton.isSelected())));
                            sectionMenu.add(booleanSettingButton);
                        }
                        case IntegerEmulationSetting integerSystemSetting -> {
                            JMenu integerSettingMenu = new JMenu(integerSystemSetting.getName());
                            SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel();
                            spinnerNumberModel.setValue(integerSystemSetting.getStartingValue());
                            integerSystemSetting.getMinimumValue().ifPresent(spinnerNumberModel::setMinimum);
                            integerSystemSetting.getMaximumValue().ifPresent(spinnerNumberModel::setMaximum);
                            JSpinner jSpinner = new JSpinner(spinnerNumberModel);
                            jSpinner.addChangeListener(_ -> {
                                if (jSpinner.getValue() instanceof Integer value) {
                                    this.mainWindow.publishEvent(integerSystemSetting.getEventSupplier().apply(value));
                                }
                            });
                            integerSettingMenu.add(jSpinner);
                            sectionMenu.add(integerSettingMenu);
                        }
                        case EnumEmulationSetting<?> enumSystemSetting -> sectionMenu.add(this.buildEnumSetting(enumSystemSetting));
                    }
                }
                case EmulationSettingSection systemSettingSection -> {
                    JMenu nestedSectionMenu = new JMenu(systemSettingSection.getName());
                    this.buildSection(systemSettingSection.emulationSettingElements(), nestedSectionMenu);
                    sectionMenu.add(nestedSectionMenu);
                }
            }
        }
    }

    private <E extends Enum<E> & DisplayNamerProvider> JMenu buildEnumSetting(EnumEmulationSetting<E> enumSystemSetting) {
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
