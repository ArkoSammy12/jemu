package io.github.arkosammy12.jemu.app.system.chip8;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.chip8.database.Chip8Database;
import io.github.arkosammy12.jemu.core.chip8.*;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.commands.StopEmulatorCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class Chip8Adapter extends SystemAdapter implements Chip8Host {

    private final Chip8Manager chip8Manager;

    @NotNull
    private Chip8Variant variant;

    @Nullable
    private Chip8Database.Entry databaseEntry;

    private String romTitle;

    public Chip8Adapter(Jemu jemu, Chip8Manager systemManager, @NotNull Chip8Variant variant) throws LineUnavailableException {
        super(jemu, systemManager);
        this.chip8Manager = systemManager;
        this.variant = variant;
    }

    @NotNull
    public Chip8Variant getVariant() {
        return this.variant;
    }

    Optional<Chip8Database.Entry> getDatabaseEntry() {
        return Optional.ofNullable(this.databaseEntry);
    }

    @Override
    protected Emulator createEmulator() {
        if (this.rom != null) {
            this.databaseEntry = this.chip8Manager.getSettings().getEntryForRom(this.rom).orElse(null);
        }

        if (this.databaseEntry != null) {
            this.databaseEntry.getProgramTitle().ifPresent(programTitle -> this.romTitle = programTitle);
            if (this.chip8Manager.getSettings().getVariantSource() == Chip8Settings.VariantSource.USE_FROM_DATABASE) {
                this.databaseEntry.getVariant().ifPresent(variant -> this.variant = variant);
            }
        }

        Chip8Emulator emulator = switch (this.variant) {
            case CHIP_8 -> new Chip8Emulator(this);
            case STRICT_CHIP_8 -> new StrictChip8Emulator(this);
            case CHIP_8X -> new Chip8XEmulator(this);
            case CHIP_48 -> new Chip48Emulator(this);
            case SUPER_CHIP_10 -> new SuperChip10Emulator(this);
            case SUPER_CHIP_11 -> new SuperChip11Emulator(this);
            case SUPER_CHIP_MODERN -> new SuperChipModernEmulator(this);
            case XO_CHIP -> new XOChipEmulator(this);
            case MEGA_CHIP -> new MegaChipEmulator(this);
            case HYPERWAVE_CHIP_8 -> new HyperWaveChip64Emulator(this);
        };

        emulator.setTargetInstructionsPerFrame(this.getIpf());
        return emulator;
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) throws LineUnavailableException {
        super.onCoreSettingChangedEvent(coreSettingChangeEvent);
        if (coreSettingChangeEvent instanceof Chip8Manager.TriggerIpfUpdate && this.emulator instanceof Chip8Emulator chip8Emulator) {
            chip8Emulator.setTargetInstructionsPerFrame(this.getIpf());
        }
    }

    @Override
    protected @Nullable SystemController.Action getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_X -> Chip8Keypad.Actions.KEY_0;
            case KeyEvent.VK_1 -> Chip8Keypad.Actions.KEY_1;
            case KeyEvent.VK_2 -> Chip8Keypad.Actions.KEY_2;
            case KeyEvent.VK_3 -> Chip8Keypad.Actions.KEY_3;
            case KeyEvent.VK_Q -> Chip8Keypad.Actions.KEY_4;
            case KeyEvent.VK_W -> Chip8Keypad.Actions.KEY_5;
            case KeyEvent.VK_E -> Chip8Keypad.Actions.KEY_6;
            case KeyEvent.VK_A -> Chip8Keypad.Actions.KEY_7;
            case KeyEvent.VK_S -> Chip8Keypad.Actions.KEY_8;
            case KeyEvent.VK_D -> Chip8Keypad.Actions.KEY_9;
            case KeyEvent.VK_Z -> Chip8Keypad.Actions.KEY_A;
            case KeyEvent.VK_C -> Chip8Keypad.Actions.KEY_B;
            case KeyEvent.VK_4 -> Chip8Keypad.Actions.KEY_C;
            case KeyEvent.VK_R -> Chip8Keypad.Actions.KEY_D;
            case KeyEvent.VK_F -> Chip8Keypad.Actions.KEY_E;
            case KeyEvent.VK_V -> Chip8Keypad.Actions.KEY_F;
            default -> null;
        };
    }

    @Override
    public ColorPalette getColorPalette() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> BuiltInChip8Palette.CADMIUM;
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getColorPalette)
                    .orElse(BuiltInChip8Palette.CADMIUM);
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getColorPalette()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::getColorPalette))
                    .orElse(BuiltInChip8Palette.CADMIUM);
        };
    }

    @Override
    public VideoGenerator.DisplayOrientation getDisplayOrientation() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> VideoGenerator.DisplayOrientation.DEG_0;
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getDisplayOrientation)
                    .orElse(VideoGenerator.DisplayOrientation.DEG_0);
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getDisplayOrientation()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::getDisplayOrientation))
                    .orElse(VideoGenerator.DisplayOrientation.DEG_0);
        };
    }

    private int getIpf() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().instructionsPerFrameSupplier().applyAsInt(this.doDisplayWait());
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getIpf)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().instructionsPerFrameSupplier().applyAsInt(this.doDisplayWait()));
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getIpf()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::getIpf))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().instructionsPerFrameSupplier().applyAsInt(this.doDisplayWait()));
        };
    }

    @Override
    public SpriteFont getSpriteFont() {
        return this.variant.getSpriteFont();
    }

    @Override
    public boolean doVFReset() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doVFReset();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doVFReset)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doVFReset());
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getDoVFReset()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doVFReset))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doVFReset());
        };
    }

    @Override
    public MemoryIncrementQuirk getMemoryIncrementQuirk() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().memoryIncrementQuirk();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getMemoryIncrementQuirk)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().memoryIncrementQuirk());
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getMemoryIncrementQuirk()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::getMemoryIncrementQuirk))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().memoryIncrementQuirk());
        };
    }

    @Override
    public boolean doDisplayWait() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doDisplayWait();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doDisplayWait)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doDisplayWait());
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getDoDisplayWait()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doDisplayWait))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doDisplayWait());
        };
    }

    @Override
    public boolean doClipping() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doClipping();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doClipping)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doClipping());
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getDoClipping()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doClipping))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doClipping());
        };
    }

    @Override
    public boolean doShiftVXInPlace() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doShiftVXInPlace();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doShiftVXInPlace)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doShiftVXInPlace());
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getDoShiftVXInPlace()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doShiftVXInPlace))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doShiftVXInPlace());
        };
    }

    @Override
    public boolean doJumpWithVX() {
        return switch (this.chip8Manager.getSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doJumpWithVX();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doJumpWithVX)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doJumpWithVX());
            case PREFER_OVERRIDES -> this.chip8Manager.getSettings().getDoJumpWithVX()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doJumpWithVX))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doJumpWithVX());
        };
    }

    @Override
    public void setPersistentFlag(int index, int value) {
        this.chip8Manager.getSettings().setPersistentFlag(index, value);
    }

    @Override
    public int getPersistentFlag(int index) {
        return this.chip8Manager.getSettings().getPersistentFlag(index);
    }

    @Override
    public void exit() {
        this.jemu.getMainWindow().submitEmulatorCommand(new StopEmulatorCommand());
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
