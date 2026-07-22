package io.github.arkosammy12.jemu.app.system.adapters;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.managers.Chip8Manager;
import io.github.arkosammy12.jemu.core.chip8.*;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class Chip8Adapter extends SystemAdapter implements Chip8Host {

    private final Chip8Manager chip8Manager;
    private final Chip8Manager.Variant variant;
    private final Chip8Host.Settings settings;

    private String romTitle;

    public Chip8Adapter(Jemu jemu, Chip8Manager systemManager, Chip8Manager.Variant variant) throws LineUnavailableException {
        super(jemu, systemManager);
        this.chip8Manager = systemManager;
        this.variant = variant;
        this.settings = new Chip8Host.Settings() {

            @Override
            public boolean doVFReset() {
                return variant.getDefaultQuirkset().doVFReset();
            }

            @Override
            public MemoryIncrementQuirk getMemoryIncrementQuirk() {
                return variant.getDefaultQuirkset().memoryIncrementQuirk();
            }

            @Override
            public boolean doDisplayWait() {
                return variant.getDefaultQuirkset().doDisplayWait();
            }

            @Override
            public boolean doClipping() {
                return variant.getDefaultQuirkset().doClipping();
            }

            @Override
            public boolean doShiftVXInPlace() {
                return variant.getDefaultQuirkset().doShiftVXInPlace();
            }

            @Override
            public boolean doJumpWithVX() {
                return variant.getDefaultQuirkset().doJumpWithVX();
            }

        };
    }

    @Override
    protected Emulator createEmulator() {
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
        emulator.setTargetInstructionsPerFrame(this.variant.getDefaultQuirkset().instructionsPerFrameSupplier().applyAsInt(this.variant.getDefaultQuirkset().doDisplayWait()));
        return emulator;
    }

    public Chip8Manager.Variant getVariant() {
        return this.variant;
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
    public Settings getSettings() {
        return this.settings;
    }

    @Override
    public ColorPalette getColorPalette() {
        return Chip8Manager.BuiltInColorPalette.CADMIUM;
    }

    @Override
    public SpriteFont getSpriteFont() {
        return this.variant.getSpriteFont();
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
