package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.Processor;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.cpu.SM83;

public class GameBoyJoypad<E extends GameBoyEmulator> extends SystemController<E> {

    private static final int SELECT_BUTTONS_MASK = 1 << 5;
    private static final int SELECT_DPAD_MASK = 1 << 4;

    private static final int START_DOWN_MASK = 1 << 3;
    private static final int SELECT_UP_MASK = 1 << 2;
    private static final int B_LEFT_MASK = 1 << 1;
    private static final int A_RIGHT_MASK = 1;

    private boolean upPhysical;
    private boolean downPhysical;
    private boolean leftPhysical;
    private boolean rightPhysical;

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    private boolean start;
    private boolean select;

    private boolean A;
    private boolean B;

    private int joyP = 0xFF;

    public GameBoyJoypad(E emulator) {
        super(emulator);
    }

    public boolean isButtonHeld() {
        return (this.readJoyP() & 0b1111) != 0b1111;
    }

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case UP -> {
                this.upPhysical = true;
                if (!this.downPhysical) {
                    this.up = true;
                }
            }
            case DOWN -> {
                this.downPhysical = true;
                if (!this.upPhysical) {
                    this.down = true;
                }
            }
            case LEFT -> {
                this.leftPhysical = true;
                if (!this.rightPhysical) {
                    this.left = true;
                }
            }
            case RIGHT -> {
                this.rightPhysical = true;
                if (!this.leftPhysical) {
                    this.right = true;
                }
            }
            case START  -> this.start = true;
            case SELECT -> this.select = true;
            case A -> this.A = true;
            case B -> this.B = true;
        }
        this.updateJoyP();
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case UP -> {
                this.upPhysical = false;
                this.up = false;
                if (this.downPhysical) {
                    this.down = true;
                }
            }
            case DOWN -> {
                this.downPhysical = false;
                this.down = false;
                if (this.upPhysical) {
                    this.up = true;
                }
            }
            case LEFT -> {
                this.leftPhysical = false;
                this.left = false;
                if (this.rightPhysical) {
                    this.right = true;
                }
            }
            case RIGHT -> {
                this.rightPhysical = false;
                this.right = false;
                if (this.leftPhysical) {
                    this.left = true;
                }
            }
            case START -> this.start = false;
            case SELECT -> this.select = false;
            case A -> this.A = false;
            case B -> this.B = false;
        }
        this.updateJoyP();
    }

    public synchronized void writeJoyP(int value) {
        this.joyP = (0b11000000) | (value & 0b00110000) | (this.joyP & 0b00001111);
        this.updateJoyP();
    }

    public synchronized int readJoyP() {
        return this.joyP;
    }

    private synchronized void updateJoyP() {
        boolean originalJoypLowBitsAnd = (this.joyP & A_RIGHT_MASK) != 0;
        originalJoypLowBitsAnd &= (this.joyP & B_LEFT_MASK) != 0;
        originalJoypLowBitsAnd &= (this.joyP & SELECT_UP_MASK) != 0;
        originalJoypLowBitsAnd &= (this.joyP & START_DOWN_MASK) != 0;

        boolean selectButtons = (this.joyP & SELECT_BUTTONS_MASK) == 0;
        boolean selectDPad = (this.joyP & SELECT_DPAD_MASK) == 0;

        int newJoyPLow = START_DOWN_MASK | SELECT_UP_MASK | B_LEFT_MASK | A_RIGHT_MASK;

        if (selectButtons) {
            if (this.A) {
                newJoyPLow &= ~A_RIGHT_MASK;
            }
            if (this.B) {
                newJoyPLow &= ~B_LEFT_MASK;
            }
            if (this.select) {
                newJoyPLow &= ~SELECT_UP_MASK;
            }
            if (this.start) {
                newJoyPLow &= ~START_DOWN_MASK;
            }
        }

        if (selectDPad) {
            if (this.right) {
                newJoyPLow &= ~A_RIGHT_MASK;
            }
            if (this.left) {
                newJoyPLow &= ~B_LEFT_MASK;
            }
            if (this.up) {
                newJoyPLow &= ~SELECT_UP_MASK;
            }
            if (this.down) {
                newJoyPLow &= ~START_DOWN_MASK;
            }
        }

        this.joyP = (this.joyP & 0xF0) | (newJoyPLow & 0x0F);

        boolean newJoypLowAnd = (this.joyP & A_RIGHT_MASK) != 0;
        newJoypLowAnd &= (this.joyP & B_LEFT_MASK) != 0;
        newJoypLowAnd &= (this.joyP & SELECT_UP_MASK) != 0;
        newJoypLowAnd &= (this.joyP & START_DOWN_MASK) != 0;

        if (originalJoypLowBitsAnd && !newJoypLowAnd) {
            this.triggerJoyPInterrupt();
        }

    }

    private void triggerJoyPInterrupt() {
        int IF = this.emulator.getMMIOBus().getIF();
        this.emulator.getMMIOBus().setIF(Processor.setBit(IF, SM83.JOYP_MASK));
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
