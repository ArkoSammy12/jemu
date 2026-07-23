package io.github.arkosammy12.jemu.app.system.chip8;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.util.FrameRequesterVideoEvent;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.EmulationSettingsBuilder;

import java.util.*;

public class Chip8Manager extends SystemManager {

    public static final Category CATEGORY = () -> "CHIP-8";

    private final Chip8Variant variant;

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
    public SystemAdapter createSystem() throws Exception {
        return new Chip8Adapter(this.jemu, this, this.variant);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof Chip8Adapter chip8Adapter && this.variant == chip8Adapter.getVariant();
    }

    public Chip8Settings getSettings() {
        return this.systemRegistry.getEmulationSettings().getChip8Settings();
    }

    @Override
    public EmulationSettingsBuilder buildSystemSettings(EmulationSettingsBuilder emulationSettingsBuilder) {
        EmulationSettingsBuilder builder = super.buildSystemSettings(emulationSettingsBuilder);
        if (this.variant != Chip8Variant.CHIP_8) {
            return builder;
        }
        return builder.addSection("CHIP-8", section -> {
            section.addEnumSetting("Use settings from", this.getSettings().getSettingSourcePreference(), SettingSourcePreferenceSettingsChangedEvent::new);
            section.addEnumSetting("Use variant from", this.getSettings().getVariantSource(), VariantSourceSettingChangedEvent::new);
            section.addEnumSetting("Color Palette", this.getSettings().getColorPaletteSetting(), ColorPaletteSettingChangedEVent::new);
            section.addEnumSetting("Display Orientation", this.getSettings().getDisplayOrientationSetting(), DisplayOrientationSettingChangedEVent::new);
            section.addSection("Instructions per frame", ipfSection -> {
                ipfSection.addBooleanSetting("Override", this.getSettings().getOverrideIpfSetting(), OverrideIpfSettingChangedEvent::new);
                ipfSection.addIntegerSetting("IPF", this.getSettings().getIpfSetting(), 1, null, IpfSettingChangedEvent::new);
            });
            section.addSection("Quirks", quirksSection -> {
                quirksSection.addEnumSetting("VF Reset", this.getSettings().getDoVFResetSetting(), value -> new BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.VF_RESET, value));
                quirksSection.addEnumSetting("I Increment", this.getSettings().getMemoryIncrementQuirkSetting(), MemoryIncrementQuirkSettingChangedEvent::new);
                quirksSection.addEnumSetting("Display Wait", this.getSettings().getDoDisplayWaitSetting(), value -> new BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.DISPLAY_WAIT, value));
                quirksSection.addEnumSetting("Clipping", this.getSettings().getDoClippingSetting(), value -> new BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.CLIPPING, value));
                quirksSection.addEnumSetting("Shift VX in Place", this.getSettings().getDoShiftVXInPlaceSetting(), value -> new BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.SHIFT_VX_IN_PLACE, value));
                quirksSection.addEnumSetting("Jump with VX", this.getSettings().getDoJumpWithVXSetting(), value -> new BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks.JUMP_WITH_VX, value));
            });
        });
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {
        switch (coreSettingChangeEvent) {
            case SettingSourcePreferenceSettingsChangedEvent(Chip8Settings.SettingSourcePreference settingSourcePreference) -> this.getSettings().setSettingSourcePreference(settingSourcePreference);
            case VariantSourceSettingChangedEvent(Chip8Settings.VariantSource variantSource) -> this.getSettings().setVariantSource(variantSource);
            case ColorPaletteSettingChangedEVent(Chip8Settings.ColorPaletteSetting colorPaletteSetting) -> this.getSettings().setColorPaletteSetting(colorPaletteSetting);
            case DisplayOrientationSettingChangedEVent(Chip8Settings.DisplayOrientationSetting displayOrientationSetting) -> this.getSettings().setDisplayOrientationSetting(displayOrientationSetting);
            case OverrideIpfSettingChangedEvent(boolean value) -> this.getSettings().setOverrideIpf(value);
            case IpfSettingChangedEvent(int value) -> this.getSettings().setIpf(value);
            case MemoryIncrementQuirkSettingChangedEvent(Chip8Settings.MemoryIncrementQuirkSetting memoryIncrementQuirkSetting) -> this.getSettings().setMemoryIncrementQuirkSetting(memoryIncrementQuirkSetting);
            case BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks booleanQuirk, Chip8Settings.BooleanQuirkSetting booleanQuirkSetting) -> {
                switch (booleanQuirk) {
                    case VF_RESET -> this.getSettings().setDoVFReset(booleanQuirkSetting);
                    case DISPLAY_WAIT -> this.getSettings().setDoDisplayWait(booleanQuirkSetting);
                    case CLIPPING -> this.getSettings().setDoClipping(booleanQuirkSetting);
                    case SHIFT_VX_IN_PLACE -> this.getSettings().setDoShiftVXInPlace(booleanQuirkSetting);
                    case JUMP_WITH_VX -> this.getSettings().setDoJumpWithVX(booleanQuirkSetting);
                }
            }
            default -> {}
        }
    }

    public interface TriggerIpfUpdate extends Event {}

    record SettingSourcePreferenceSettingsChangedEvent(Chip8Settings.SettingSourcePreference settingSourcePreference) implements CoreSettingChangeEvent, TriggerIpfUpdate, FrameRequesterVideoEvent {}

    record VariantSourceSettingChangedEvent(Chip8Settings.VariantSource variantSource) implements CoreSettingChangeEvent {}

    record ColorPaletteSettingChangedEVent(Chip8Settings.ColorPaletteSetting colorPaletteSetting) implements CoreSettingChangeEvent, FrameRequesterVideoEvent {}

    record DisplayOrientationSettingChangedEVent(Chip8Settings.DisplayOrientationSetting displayOrientationSetting) implements CoreSettingChangeEvent, FrameRequesterVideoEvent {}

    record OverrideIpfSettingChangedEvent(boolean value) implements CoreSettingChangeEvent, TriggerIpfUpdate {}

    record IpfSettingChangedEvent(int value) implements CoreSettingChangeEvent, TriggerIpfUpdate {}

    record BooleanQuirkSettingChangedEvent(Chip8Settings.BooleanQuirks booleanQuirk, Chip8Settings.BooleanQuirkSetting booleanQuirkSetting) implements CoreSettingChangeEvent {}

    record MemoryIncrementQuirkSettingChangedEvent(Chip8Settings.MemoryIncrementQuirkSetting memoryIncrementQuirkSetting) implements CoreSettingChangeEvent {}

}
