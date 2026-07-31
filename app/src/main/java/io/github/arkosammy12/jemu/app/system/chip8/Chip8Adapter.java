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
import java.text.NumberFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class Chip8Adapter extends SystemAdapter implements Chip8Host {

    private final Chip8Manager chip8Manager;

    @NotNull
    private Chip8Variant variant;

    @Nullable
    private Chip8Database.Entry databaseEntry;

    @NotNull
    private String programTitle = "No title";

    @NotNull
    private String romTitle = "No ROM";

    @NotNull
    private String variantName;

    private final NumberFormat ipfFormatter = NumberFormat.getIntegerInstance();
    private final NumberFormat mipsFormatter = NumberFormat.getNumberInstance();

    private int framesUntilTitleUpdate;

    public Chip8Adapter(Jemu jemu, Chip8Manager systemManager, @NotNull Chip8Variant variant) throws LineUnavailableException {
        super(jemu, systemManager);
        this.chip8Manager = systemManager;
        this.variant = variant;
        this.variantName = this.variant.getName();
        this.mipsFormatter.setMinimumFractionDigits(2);
        this.mipsFormatter.setMaximumFractionDigits(2);
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
            this.databaseEntry = this.chip8Manager.getEmulationSettings().getEntryForRom(this.rom).orElse(null);
        }

        if (this.databaseEntry != null) {
            this.databaseEntry.getRomName().ifPresent(programTitle -> this.romTitle = programTitle);
            if (this.chip8Manager.getEmulationSettings().getVariantSource() == Chip8Settings.VariantSource.USE_FROM_DATABASE) {
                this.databaseEntry.getVariant().ifPresent(variant -> {
                    this.variant = variant;
                    this.variantName = variant.getName();
                });
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

        this.updateTitle();
        return emulator;
    }

    @Override
    public void onFrame() {
        super.onFrame();
        Emulator emulator = this.emulator;
        if (emulator != null) {
            AtomicInteger integer = new AtomicInteger(12);
            this.framesUntilTitleUpdate++;
            if (this.framesUntilTitleUpdate >= emulator.getFramerate()) {
                this.framesUntilTitleUpdate = 0;
                this.updateTitle();
            }
        }
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) throws LineUnavailableException {
        super.onCoreSettingChangedEvent(coreSettingChangeEvent);
        switch (coreSettingChangeEvent) {
            case Chip8Manager.ShowUsedVariantSettingChangedEvent _ -> this.updateTitle();
            case Chip8Manager.ShowIpfMetricsSettingChangedEvent _ -> {
                this.framesUntilTitleUpdate = 0;
                this.updateTitle();
            }
            case Chip8Manager.TriggerIpfUpdate _ -> {
                if (this.emulator instanceof Chip8Emulator chip8Emulator) {
                    chip8Emulator.setTargetInstructionsPerFrame(this.getIpf());
                    this.updateTitle();
                    this.framesUntilTitleUpdate = 0;
                }
            }
            default -> {}
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
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> BuiltInChip8Palette.CADMIUM;
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getColorPalette)
                    .orElse(BuiltInChip8Palette.CADMIUM);
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getColorPalette()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::getColorPalette))
                    .orElse(BuiltInChip8Palette.CADMIUM);
        };
    }

    @Override
    public VideoGenerator.DisplayOrientation getDisplayOrientation() {
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> VideoGenerator.DisplayOrientation.DEG_0;
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getDisplayOrientation)
                    .orElse(VideoGenerator.DisplayOrientation.DEG_0);
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getDisplayOrientation()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::getDisplayOrientation))
                    .orElse(VideoGenerator.DisplayOrientation.DEG_0);
        };
    }

    private int getIpf() {
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().instructionsPerFrameSupplier().applyAsInt(this.doDisplayWait());
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getIpf)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().instructionsPerFrameSupplier().applyAsInt(this.doDisplayWait()));
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getIpf()
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
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doVFReset();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doVFReset)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doVFReset());
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getDoVFReset()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doVFReset))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doVFReset());
        };
    }

    @Override
    public MemoryIncrementQuirk getMemoryIncrementQuirk() {
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().memoryIncrementQuirk();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::getMemoryIncrementQuirk)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().memoryIncrementQuirk());
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getMemoryIncrementQuirk()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::getMemoryIncrementQuirk))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().memoryIncrementQuirk());
        };
    }

    @Override
    public boolean doDisplayWait() {
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doDisplayWait();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doDisplayWait)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doDisplayWait());
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getDoDisplayWait()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doDisplayWait))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doDisplayWait());
        };
    }

    @Override
    public boolean doClipping() {
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doClipping();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doClipping)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doClipping());
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getDoClipping()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doClipping))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doClipping());
        };
    }

    @Override
    public boolean doShiftVXInPlace() {
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doShiftVXInPlace();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doShiftVXInPlace)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doShiftVXInPlace());
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getDoShiftVXInPlace()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doShiftVXInPlace))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doShiftVXInPlace());
        };
    }

    @Override
    public boolean doJumpWithVX() {
        return switch (this.chip8Manager.getEmulationSettings().getSettingSourcePreference()) {
            case PREFER_VARIANT -> this.variant.getDefaultQuirkset().doJumpWithVX();
            case PREFER_DATABASE -> this.getDatabaseEntry()
                    .flatMap(Chip8Database.Entry::doJumpWithVX)
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doJumpWithVX());
            case PREFER_OVERRIDES -> this.chip8Manager.getEmulationSettings().getDoJumpWithVX()
                    .or(() -> this.getDatabaseEntry().flatMap(Chip8Database.Entry::doJumpWithVX))
                    .orElseGet(() -> this.variant.getDefaultQuirkset().doJumpWithVX());
        };
    }

    @Override
    public void setPersistentFlag(int index, int value) {
        this.chip8Manager.getEmulationSettings().setPersistentFlag(index, value);
    }

    @Override
    public int getPersistentFlag(int index) {
        return this.chip8Manager.getEmulationSettings().getPersistentFlag(index);
    }

    @Override
    public void exit() {
        this.jemu.getMainWindow().submitEmulatorCommand(new StopEmulatorCommand());
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.of(this.programTitle);
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        initializer.getRomPath().map(path -> path.getFileName().toString()).ifPresent(fileName -> this.romTitle = fileName);
        super.initialize(initializer, tryReset);
    }

    private void updateTitle() {
        Emulator emulator = this.emulator;
        Chip8Settings settings = this.chip8Manager.getEmulationSettings();
        String title = this.romTitle;

        if (settings.showUsedVariant()) {
            title = this.variantName + " - " + title;
        }
        if (settings.showIpfMetrics()) {
            int ipf = emulator instanceof Chip8Emulator chip8Emulator ? chip8Emulator.getCurrentInstructionsPerFrame() : this.getIpf();
            double mips = emulator != null ? (ipf * emulator.getFramerate()) / 1_000_000.0 : 0.0;
            title = title + " - " + "%s IPF (%s MIPS)".formatted(this.ipfFormatter.format(ipf), this.mipsFormatter.format(mips));
        }

        this.programTitle = title;
    }

}
