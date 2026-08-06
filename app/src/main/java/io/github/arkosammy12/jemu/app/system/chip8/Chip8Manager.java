package io.github.arkosammy12.jemu.app.system.chip8;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.util.FrameRequesterVideoEvent;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class Chip8Manager extends SystemManager {

    public static final Category CATEGORY = () -> "CHIP-8";

    private final Chip8Variant variant;

    @Nullable
    private volatile Chip8MenuBarSettings chip8MenuBarSettings;

    @Nullable
    private volatile Chip8PanelSettings chip8PanelSettings;

    public Chip8Manager(Jemu jemu, SystemRegistry systemRegistry, Chip8Variant variant) {
        super(jemu, systemRegistry);
        this.variant = variant;
    }

    @Override
    public String getName() {
        return this.variant.getName();
    }

    @Override
    public String getId() {
        return this.variant.getId();
    }

    @Override
    public Collection<String> getFileExtensions() {
        return this.variant.getFileExtensions();
    }

    @Override
    public Optional<Category> getCategory() {
        return Optional.of(CATEGORY);
    }

    @Override
    public SystemAdapter createSystem(boolean detectedAutomatically) throws Exception {
        return new Chip8Adapter(this.jemu, this, this.variant);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof Chip8Adapter chip8Adapter && this.variant == chip8Adapter.getVariant();
    }

    Chip8Settings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getChip8Settings();
    }

    private Optional<Chip8MenuBarSettings> getMenuBarSettings() {
        return Optional.ofNullable(this.chip8MenuBarSettings);
    }

    private Optional<Chip8PanelSettings> getPanelSettings() {
        return Optional.ofNullable(this.chip8PanelSettings);
    }

    @Override
    public Optional<? extends Function<? super MainWindow, ? extends JMenu>> getSettingsMenuBarContents() {
        if (this.variant == Chip8Variant.CHIP_8) {
            return Optional.of(mainWindow -> {
                Chip8MenuBarSettings chip8MenuBarSettings = new Chip8MenuBarSettings(this, mainWindow);
                this.chip8MenuBarSettings = chip8MenuBarSettings;
                return chip8MenuBarSettings;
            });
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<? extends Function<? super MainWindow, ? extends JPanel>> getSettingsWindowContents() {
        if (this.variant == Chip8Variant.CHIP_8) {
            return Optional.of(mainWindow -> {
                Chip8PanelSettings chip8PanelSettings = new Chip8PanelSettings(this, mainWindow);
                this.chip8PanelSettings = chip8PanelSettings;
                return chip8PanelSettings;
            });
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangeEvent);
        this.getMenuBarSettings().ifPresent(chip8MenuBarSettings -> chip8MenuBarSettings.onEvent(coreSettingChangeEvent));
        this.getPanelSettings().ifPresent(chip8PanelSettings -> chip8PanelSettings.onEvent(coreSettingChangeEvent));
        switch (coreSettingChangeEvent) {
            case ShowUsedVariantSettingChangedEvent(boolean value) -> this.getEmulationSettings().setShowUsedVariant(value);
            case ShowIpfMetricsSettingChangedEvent(boolean value) -> this.getEmulationSettings().setShowIpfMetrics(value);
            case SettingSourcePreferenceSettingsChangedEvent(Chip8Settings.SettingSourcePreference settingSourcePreference) -> this.getEmulationSettings().setSettingSourcePreference(settingSourcePreference);
            case VariantSourceSettingChangedEvent(Chip8Settings.VariantSource variantSource) -> this.getEmulationSettings().setVariantSource(variantSource);
            case ColorPaletteSettingChangedEVent(Chip8Settings.ColorPaletteSetting colorPaletteSetting) -> this.getEmulationSettings().setColorPaletteSetting(colorPaletteSetting);
            case DisplayOrientationSettingChangedEVent(Chip8Settings.DisplayOrientationSetting displayOrientationSetting) -> this.getEmulationSettings().setDisplayOrientationSetting(displayOrientationSetting);
            case OverrideIpfSettingChangedEvent(boolean value) -> this.getEmulationSettings().setOverrideIpf(value);
            case IpfSettingChangedEvent(int value) -> this.getEmulationSettings().setIpf(value);
            case MemoryIncrementQuirkSettingChangedEvent(Chip8Settings.MemoryIncrementQuirkSetting memoryIncrementQuirkSetting) -> this.getEmulationSettings().setMemoryIncrementQuirkSetting(memoryIncrementQuirkSetting);
            case BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks booleanQuirk, Chip8Settings.BooleanQuirkSetting booleanQuirkSetting) -> {
                switch (booleanQuirk) {
                    case VF_RESET -> this.getEmulationSettings().setDoVFReset(booleanQuirkSetting);
                    case DISPLAY_WAIT -> this.getEmulationSettings().setDoDisplayWait(booleanQuirkSetting);
                    case CLIPPING -> this.getEmulationSettings().setDoClipping(booleanQuirkSetting);
                    case SHIFT_VX_IN_PLACE -> this.getEmulationSettings().setDoShiftVXInPlace(booleanQuirkSetting);
                    case JUMP_WITH_VX -> this.getEmulationSettings().setDoJumpWithVX(booleanQuirkSetting);
                }
            }
            default -> {}
        }
    }

    interface TriggerIpfUpdate extends Event {}

    record ShowUsedVariantSettingChangedEvent(boolean value) implements CoreSettingChangeEvent, Supplier<Boolean> {

        @Override
        public Boolean get() {
            return this.value();
        }

    }

    record ShowIpfMetricsSettingChangedEvent(boolean value) implements CoreSettingChangeEvent, Supplier<Boolean> {

        @Override
        public Boolean get() {
            return this.value();
        }

    }

    record SettingSourcePreferenceSettingsChangedEvent(Chip8Settings.SettingSourcePreference settingSourcePreference) implements CoreSettingChangeEvent, TriggerIpfUpdate, FrameRequesterVideoEvent, Supplier<Chip8Settings.SettingSourcePreference> {

        @Override
        public Chip8Settings.SettingSourcePreference get() {
            return this.settingSourcePreference();
        }

    }

    record VariantSourceSettingChangedEvent(Chip8Settings.VariantSource variantSource) implements CoreSettingChangeEvent, Supplier<Chip8Settings.VariantSource> {

        @Override
        public Chip8Settings.VariantSource get() {
            return this.variantSource();
        }

    }

    record ColorPaletteSettingChangedEVent(Chip8Settings.ColorPaletteSetting colorPaletteSetting) implements CoreSettingChangeEvent, FrameRequesterVideoEvent, Supplier<Chip8Settings.ColorPaletteSetting> {


        @Override
        public Chip8Settings.ColorPaletteSetting get() {
            return this.colorPaletteSetting();
        }

    }

    record DisplayOrientationSettingChangedEVent(Chip8Settings.DisplayOrientationSetting displayOrientationSetting) implements CoreSettingChangeEvent, FrameRequesterVideoEvent, Supplier<Chip8Settings.DisplayOrientationSetting> {

        @Override
        public Chip8Settings.DisplayOrientationSetting get() {
            return this.displayOrientationSetting();
        }

    }

    record OverrideIpfSettingChangedEvent(boolean value) implements CoreSettingChangeEvent, TriggerIpfUpdate, Supplier<Boolean> {

        @Override
        public Boolean get() {
            return this.value();
        }

    }

    record IpfSettingChangedEvent(int value) implements CoreSettingChangeEvent, TriggerIpfUpdate, Supplier<Integer> {

        @Override
        public Integer get() {
            return this.value();
        }

    }

    record BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks booleanQuirk, Chip8Settings.BooleanQuirkSetting booleanQuirkSetting) implements CoreSettingChangeEvent, Supplier<Chip8Settings.BooleanQuirkSetting> {

        @Override
        public Chip8Settings.BooleanQuirkSetting get() {
            return this.booleanQuirkSetting();
        }

    }

    record MemoryIncrementQuirkSettingChangedEvent(Chip8Settings.MemoryIncrementQuirkSetting memoryIncrementQuirkSetting) implements CoreSettingChangeEvent, Supplier<Chip8Settings.MemoryIncrementQuirkSetting> {

        @Override
        public Chip8Settings.MemoryIncrementQuirkSetting get() {
            return this.memoryIncrementQuirkSetting();
        }

    }

}
