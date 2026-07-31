package io.github.arkosammy12.jemu.app.system.chip8;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.IntegerPanelSpinnerSetting;

import javax.swing.*;

public class Chip8PanelSettings extends PanelSettingsMenu {

    public Chip8PanelSettings(Chip8Manager chip8Manager, MainWindow mainWindow) {
        super(mainWindow);

        this.addHeader("General");
        this.addBooleanSetting("Show used variant", chip8Manager.getEmulationSettings().showUsedVariant(), Chip8Manager.ShowUsedVariantSettingChangedEvent.class, null, Chip8Manager.ShowUsedVariantSettingChangedEvent::new);
        this.addBooleanSetting("Show IPF metrics", chip8Manager.getEmulationSettings().showIpfMetrics(), Chip8Manager.ShowIpfMetricsSettingChangedEvent.class, null, Chip8Manager.ShowIpfMetricsSettingChangedEvent::new);
        this.addEnumSetting("Use settings from", chip8Manager.getEmulationSettings().getSettingSourcePreference(), Chip8Manager.SettingSourcePreferenceSettingsChangedEvent.class, null, Chip8Manager.SettingSourcePreferenceSettingsChangedEvent::new);
        this.addEnumSetting("Use variant from", chip8Manager.getEmulationSettings().getVariantSource(), Chip8Manager.VariantSourceSettingChangedEvent.class, null, Chip8Manager.VariantSourceSettingChangedEvent::new);
        this.addEnumSetting("Color Palette", chip8Manager.getEmulationSettings().getColorPaletteSetting(), Chip8Manager.ColorPaletteSettingChangedEVent.class, null, Chip8Manager.ColorPaletteSettingChangedEVent::new);
        this.addEnumSetting("Display Orientation", chip8Manager.getEmulationSettings().getDisplayOrientationSetting(), Chip8Manager.DisplayOrientationSettingChangedEVent.class, null, Chip8Manager.DisplayOrientationSettingChangedEVent::new);
        this.addEmptyLine();

        this.addHeader("Instructions per frame");
        IntegerPanelSpinnerSetting<Chip8Manager.IpfSettingChangedEvent> ipfSetting = new IntegerPanelSpinnerSetting<>(mainWindow, "IPF", "instructions per frame", chip8Manager.getEmulationSettings().getIpfSetting(), 1, null, Chip8Manager.IpfSettingChangedEvent.class, null, Chip8Manager.IpfSettingChangedEvent::new);
        ipfSetting.getJSpinner().setEnabled(chip8Manager.getEmulationSettings().getOverrideIpfSetting());

        BooleanPanelSetting<Chip8Manager.OverrideIpfSettingChangedEvent> overrideIpfButton = this.addBooleanSetting("Override", chip8Manager.getEmulationSettings().getOverrideIpfSetting(), Chip8Manager.OverrideIpfSettingChangedEvent.class, null, Chip8Manager.OverrideIpfSettingChangedEvent::new);
        overrideIpfButton.setOnEventCallback(value -> SwingUtilities.invokeLater(() -> ipfSetting.getJSpinner().setEnabled(value)));

        this.addIntegerSetting(ipfSetting);
        this.addEmptyLine();

        this.addHeader("Quirks");
        this.addEnumSetting("VF Reset", chip8Manager.getEmulationSettings().getDoVFResetSetting(), Chip8Manager.BooleanQuirkSettingChangedEvent.class, event -> event.booleanQuirk() == Chip8Settings.BooleanQuirks.VF_RESET, value -> new Chip8Manager.BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.VF_RESET, value));
        this.addEnumSetting("I Increment", chip8Manager.getEmulationSettings().getMemoryIncrementQuirkSetting(), Chip8Manager.MemoryIncrementQuirkSettingChangedEvent.class, null, Chip8Manager.MemoryIncrementQuirkSettingChangedEvent::new);
        this.addEnumSetting("Display Wait", chip8Manager.getEmulationSettings().getDoDisplayWaitSetting(), Chip8Manager.BooleanQuirkSettingChangedEvent.class, event -> event.booleanQuirk() == Chip8Settings.BooleanQuirks.DISPLAY_WAIT, value -> new Chip8Manager.BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.DISPLAY_WAIT, value));
        this.addEnumSetting("Clipping", chip8Manager.getEmulationSettings().getDoClippingSetting(), Chip8Manager.BooleanQuirkSettingChangedEvent.class, event -> event.booleanQuirk() == Chip8Settings.BooleanQuirks.CLIPPING, value -> new Chip8Manager.BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.CLIPPING, value));
        this.addEnumSetting("Shift VX in Place", chip8Manager.getEmulationSettings().getDoShiftVXInPlaceSetting(), Chip8Manager.BooleanQuirkSettingChangedEvent.class, event -> event.booleanQuirk() == Chip8Settings.BooleanQuirks.SHIFT_VX_IN_PLACE, value -> new Chip8Manager.BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.SHIFT_VX_IN_PLACE, value));
        this.addEnumSetting("Jump with VX", chip8Manager.getEmulationSettings().getDoJumpWithVXSetting(), Chip8Manager.BooleanQuirkSettingChangedEvent.class, event -> event.booleanQuirk() == Chip8Settings.BooleanQuirks.JUMP_WITH_VX, value -> new Chip8Manager.BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.JUMP_WITH_VX, value));
    }

}
