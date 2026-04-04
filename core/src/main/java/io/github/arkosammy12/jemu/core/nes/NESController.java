package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class NESController<E extends NESEmulator> extends SystemController<E> {

    private static final int A_MASK = 1;
    private static final int B_MASK = 1 << 1;
    private static final int SELECT_MASK = 1 << 2;
    private static final int START_MASK = 1 << 3;
    private static final int UP_MASK = 1 << 4;
    private static final int DOWN_MASK = 1 << 5;
    private static final int LEFT_MASK = 1 << 6;
    private static final int RIGHT_MASK = 1 << 7;

    private boolean strobeSignal;
    private boolean previousStrobe;
    private int currentControllerState;
    private int joy1ShiftRegister;

    public NESController(E emulator) {
        super(emulator);
    }

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case UP -> this.currentControllerState |= UP_MASK;
            case DOWN -> this.currentControllerState |= DOWN_MASK;
            case LEFT -> this.currentControllerState |= LEFT_MASK;
            case RIGHT -> this.currentControllerState |= RIGHT_MASK;
            case START -> this.currentControllerState |= START_MASK;
            case SELECT -> this.currentControllerState |= SELECT_MASK;
            case A -> this.currentControllerState |= A_MASK;
            case B -> this.currentControllerState |= B_MASK;
        }
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case UP -> this.currentControllerState &= ~UP_MASK;
            case DOWN -> this.currentControllerState &= ~DOWN_MASK;
            case LEFT -> this.currentControllerState &= ~LEFT_MASK;
            case RIGHT -> this.currentControllerState &= ~RIGHT_MASK;
            case START -> this.currentControllerState &= ~START_MASK;
            case SELECT -> this.currentControllerState &= ~SELECT_MASK;
            case A -> this.currentControllerState &= ~A_MASK;
            case B -> this.currentControllerState &= ~B_MASK;
        }
    }

    public void writeJoy1(int value) {
        boolean newStrobe = (value & 1) != 0;
        if (this.previousStrobe && !newStrobe) {
            this.joy1ShiftRegister = this.currentControllerState;
        }
        this.strobeSignal = newStrobe;
        this.previousStrobe = newStrobe;
    }

    public int readJoy1() {
        if (this.strobeSignal) {
            return (this.currentControllerState & 1);
        }
        int bit = this.joy1ShiftRegister & 1;
        this.joy1ShiftRegister >>= 1;
        return bit;
    }

    public int readJoy2() {
        return 0x00;
    }

    public enum Actions implements Action {
        UP("Up"),
        DOWN("Down"),
        LEFT("Left"),
        RIGHT("Right"),
        START("Start"),
        SELECT("Select"),
        A("A"),
        B("B");

        private final String label;

        Actions(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return this.label;
        }
    }

}
