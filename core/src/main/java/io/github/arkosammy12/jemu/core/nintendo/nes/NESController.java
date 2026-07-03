package io.github.arkosammy12.jemu.core.nintendo.nes;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class NESController<E extends NESEmulator> implements SystemController {

    private static final int A_MASK = 1;
    private static final int B_MASK = 1 << 1;
    private static final int SELECT_MASK = 1 << 2;
    private static final int START_MASK = 1 << 3;
    private static final int UP_MASK = 1 << 4;
    private static final int DOWN_MASK = 1 << 5;
    private static final int LEFT_MASK = 1 << 6;
    private static final int RIGHT_MASK = 1 << 7;

    private final E emulator;

    private boolean strobeSignal;

    private int physicalController1State;
    private int currentController1State;
    private int joy1ShiftRegister;

    private int physicalController2State;
    private int currentController2State;
    private int joy2ShiftRegister;

    public NESController(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case JOY1_UP -> {
                this.physicalController1State |= UP_MASK;
                if ((this.physicalController1State & DOWN_MASK) == 0) {
                    this.currentController1State |= UP_MASK;
                }
            }
            case JOY1_DOWN -> {
                this.physicalController1State |= DOWN_MASK;
                if ((this.physicalController1State & UP_MASK) == 0) {
                    this.currentController1State |= DOWN_MASK;
                }
            }
            case JOY1_LEFT -> {
                this.physicalController1State |= LEFT_MASK;
                if ((this.physicalController1State & RIGHT_MASK) == 0) {
                    this.currentController1State |= LEFT_MASK;
                }
            }
            case JOY1_RIGHT -> {
                this.physicalController1State |= RIGHT_MASK;
                if ((this.physicalController1State & LEFT_MASK) == 0) {
                    this.currentController1State |= RIGHT_MASK;
                }
            }
            case JOY1_START -> this.currentController1State |= START_MASK;
            case JOY1_SELECT -> this.currentController1State |= SELECT_MASK;
            case JOY1_A -> this.currentController1State |= A_MASK;
            case JOY1_B -> this.currentController1State |= B_MASK;
            case JOY2_UP -> {
                this.physicalController2State |= UP_MASK;
                if ((this.physicalController2State & DOWN_MASK) == 0) {
                    this.currentController2State |= UP_MASK;
                }
            }
            case JOY2_DOWN -> {
                this.physicalController2State |= DOWN_MASK;
                if ((this.physicalController2State & UP_MASK) == 0) {
                    this.currentController2State |= DOWN_MASK;
                }
            }
            case JOY2_LEFT -> {
                this.physicalController2State |= LEFT_MASK;
                if ((this.physicalController2State & RIGHT_MASK) == 0) {
                    this.currentController2State |= LEFT_MASK;
                }
            }
            case JOY2_RIGHT -> {
                this.physicalController2State |= RIGHT_MASK;
                if ((this.physicalController2State & LEFT_MASK) == 0) {
                    this.currentController2State |= RIGHT_MASK;
                }
            }
            case JOY2_START -> this.currentController2State |= START_MASK;
            case JOY2_SELECT -> this.currentController2State |= SELECT_MASK;
            case JOY2_A -> this.currentController2State |= A_MASK;
            case JOY2_B -> this.currentController2State |= B_MASK;

        }
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case JOY1_UP -> {
                this.physicalController1State &= ~UP_MASK;
                this.currentController1State &= ~UP_MASK;
                if ((this.physicalController1State & DOWN_MASK) != 0) {
                    this.currentController1State |= DOWN_MASK;
                }
            }
            case JOY1_DOWN -> {
                this.physicalController1State &= ~DOWN_MASK;
                this.currentController1State &= ~DOWN_MASK;
                if ((this.physicalController1State & UP_MASK) != 0) {
                    this.currentController1State |= UP_MASK;
                }
            }
            case JOY1_LEFT -> {
                this.physicalController1State &= ~LEFT_MASK;
                this.currentController1State &= ~LEFT_MASK;
                if ((this.physicalController1State & RIGHT_MASK) != 0) {
                    this.currentController1State |= RIGHT_MASK;
                }
            }
            case JOY1_RIGHT -> {
                this.physicalController1State &= ~RIGHT_MASK;
                this.currentController1State &= ~RIGHT_MASK;
                if ((this.physicalController1State & LEFT_MASK) != 0) {
                    this.currentController1State |= LEFT_MASK;
                }
            }
            case JOY1_START  -> this.currentController1State &= ~START_MASK;
            case JOY1_SELECT -> this.currentController1State &= ~SELECT_MASK;
            case JOY1_A -> this.currentController1State &= ~A_MASK;
            case JOY1_B -> this.currentController1State &= ~B_MASK;

            case JOY2_UP -> {
                this.physicalController2State &= ~UP_MASK;
                this.currentController2State &= ~UP_MASK;
                if ((this.physicalController2State & DOWN_MASK) != 0) {
                    this.currentController2State |= DOWN_MASK;
                }
            }
            case JOY2_DOWN -> {
                this.physicalController2State &= ~DOWN_MASK;
                this.currentController2State &= ~DOWN_MASK;
                if ((this.physicalController2State & UP_MASK) != 0) {
                    this.currentController2State |= UP_MASK;
                }
            }
            case JOY2_LEFT -> {
                this.physicalController2State &= ~LEFT_MASK;
                this.currentController2State &= ~LEFT_MASK;
                if ((this.physicalController2State & RIGHT_MASK) != 0) {
                    this.currentController2State |= RIGHT_MASK;
                }
            }
            case JOY2_RIGHT -> {
                this.physicalController2State &= ~RIGHT_MASK;
                this.currentController2State &= ~RIGHT_MASK;
                if ((this.physicalController2State & LEFT_MASK) != 0) {
                    this.currentController2State |= LEFT_MASK;
                }
            }
            case JOY2_START  -> this.currentController2State &= ~START_MASK;
            case JOY2_SELECT -> this.currentController2State &= ~SELECT_MASK;
            case JOY2_A -> this.currentController2State &= ~A_MASK;
            case JOY2_B -> this.currentController2State &= ~B_MASK;
        }
    }

    public void writeJoy1(int value) {
        this.strobeSignal = (value & 1) != 0;
    }

    public int readJoy1() {
        if (this.strobeSignal) {
            return (this.currentController1State & 1);
        }
        int bit = this.joy1ShiftRegister & 1;
        this.joy1ShiftRegister = (this.joy1ShiftRegister >> 1) | 0x80;
        return bit;
    }

    public int readJoy2() {
        if (this.strobeSignal) {
            return (this.currentController2State & 1);
        }
        int bit = this.joy2ShiftRegister & 1;
        this.joy2ShiftRegister = (this.joy2ShiftRegister >> 1) | 0x80;
        return bit;
    }

    public void cycle() {
        if (this.strobeSignal && this.emulator.getRicohCore().getCurrentApuHalfCycleType() == RP2A03.APUHalfCycleType.GET) {
            this.joy1ShiftRegister = this.currentController1State;
            this.joy2ShiftRegister = this.currentController2State;
        }
    }

    public enum Actions implements Action {
        JOY1_UP("Joy1 Up"),
        JOY1_DOWN("Joy1 Down"),
        JOY1_LEFT("Joy1 Left"),
        JOY1_RIGHT("Joy1 Right"),
        JOY1_START("Joy1 Start"),
        JOY1_SELECT("Joy1 Select"),
        JOY1_A("Joy1 A"),
        JOY1_B("Joy1 B"),

        JOY2_UP("Joy2 Up"),
        JOY2_DOWN("Joy2 Down"),
        JOY2_LEFT("Joy2 Left"),
        JOY2_RIGHT("Joy2 Right"),
        JOY2_START("Joy2 Start"),
        JOY2_SELECT("Joy2 Select"),
        JOY2_A("Joy2 A"),
        JOY2_B("Joy2 B");

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
