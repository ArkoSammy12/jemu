package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.Processor;
import io.github.arkosammy12.jemu.backend.common.SystemController;
import io.github.arkosammy12.jemu.backend.cores.SM83;

public class GameBoyJoypad<E extends GameBoyEmulator> extends SystemController<E> {

    private static final int SELECT_BUTTONS_MASK = 1 << 5;
    private static final int SELECT_DPAD_MASK = 1 << 4;

    private static final int START_DOWN_MASK = 1 << 3;
    private static final int SELECT_UP_MASK = 1 << 2;
    private static final int B_LEFT_MASK = 1 << 1;
    private static final int A_RIGHT_MASK = 1;

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

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case UP -> up = true;
            case DOWN -> down = true;
            case LEFT -> left = true;
            case RIGHT -> right = true;
            case START -> start = true;
            case SELECT -> select = true;
            case A -> A = true;
            case B -> B = true;
        }
        this.updateJoyP();
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case UP -> up = false;
            case DOWN -> down = false;
            case LEFT -> left = false;
            case RIGHT -> right = false;
            case START -> start = false;
            case SELECT -> select = false;
            case A -> A = false;
            case B -> B = false;
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
        int IF = this.emulator.getMMIOController().getIF();
        this.emulator.getMMIOController().setIF(Processor.setBit(IF, SM83.JOYP_MASK));
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
