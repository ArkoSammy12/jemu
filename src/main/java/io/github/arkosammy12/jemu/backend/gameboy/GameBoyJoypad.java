package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.Processor;
import io.github.arkosammy12.jemu.backend.common.SystemController;
import io.github.arkosammy12.jemu.backend.cores.SM83;
import io.github.arkosammy12.jemu.backend.drivers.KeyMapping;

import java.util.Collection;
import java.util.List;

public class GameBoyJoypad<E extends GameBoyEmulator> extends SystemController<E> {

    private static final int SELECT_BUTTONS_MASK = 1 << 5;
    private static final int SELECT_DPAD_MASK = 1 << 4;

    private static final int START_DOWN_MASK = 1 << 3;
    private static final int SELECT_UP_MASK = 1 << 2;
    private static final int B_LEFT_MASK = 1 << 1;
    private static final int A_RIGHT_MASK = 1;

    private final JoypadButton BUTTON_UP = new JoypadButton('w');
    private final JoypadButton BUTTON_DOWN = new JoypadButton('s');
    private final JoypadButton BUTTON_LEFT = new JoypadButton('a');
    private final JoypadButton BUTTON_RIGHT = new JoypadButton('d');
    private final JoypadButton BUTTON_START = new JoypadButton('\n');
    private final JoypadButton BUTTON_SELECT = new JoypadButton('\b');
    private final JoypadButton BUTTON_A = new JoypadButton('j');
    private final JoypadButton BUTTON_B = new JoypadButton('k');

    private int joyP = 0xFF;

    public GameBoyJoypad(E emulator) {
        super(emulator);
    }

    @Override
    public Collection<KeyMapping> getKeyMappings() {
        return List.of(BUTTON_UP, BUTTON_DOWN, BUTTON_LEFT, BUTTON_RIGHT, BUTTON_START, BUTTON_SELECT, BUTTON_A, BUTTON_B);
    }

    private class JoypadButton implements KeyMapping {

        private final int codePoint;
        private boolean pressed;

        private final Runnable pressedCallback = () -> {
            this.pressed = true;
            updateJoyP();
        };

        private final Runnable releasedCallback = () -> {
            this.pressed = false;
            updateJoyP();
        };

        private JoypadButton(int codePoint) {
            this.codePoint = codePoint;
        }

        @Override
        public int getDefaultCodePoint() {
            return this.codePoint;
        }

        @Override
        public Runnable getKeyPressedCallback() {
            return this.pressedCallback;
        }

        @Override
        public Runnable getKeyReleasedCallback() {
            return this.releasedCallback;
        }

    }

    public synchronized void writeJoyP(int value) {
        this.joyP = (0b11000000) | (value & 0b00110000) | (this.joyP & 0b00001111);
        this.updateJoyP();
    }

    public synchronized int readJoyP() {
        return this.joyP;
    }

    private synchronized void updateJoyP() {
        boolean originalBit0 = (this.joyP & A_RIGHT_MASK) != 0;
        boolean originalBit1 = (this.joyP & B_LEFT_MASK) != 0;
        boolean originalBit2 = (this.joyP & SELECT_UP_MASK) != 0;
        boolean originalBit3 = (this.joyP & START_DOWN_MASK) != 0;

        boolean selectButtons = (this.joyP & SELECT_BUTTONS_MASK) == 0;
        boolean selectDPad = (this.joyP & SELECT_DPAD_MASK) == 0;

        int newJoyPLow = START_DOWN_MASK | SELECT_UP_MASK | B_LEFT_MASK | A_RIGHT_MASK;

        if (selectButtons) {
            if (this.BUTTON_A.pressed) {
                newJoyPLow &= ~A_RIGHT_MASK;
            }
            if (this.BUTTON_B.pressed) {
                newJoyPLow &= ~B_LEFT_MASK;
            }
            if (this.BUTTON_SELECT.pressed) {
                newJoyPLow &= ~SELECT_UP_MASK;
            }
            if (this.BUTTON_START.pressed) {
                newJoyPLow &= ~START_DOWN_MASK;
            }
        }

        if (selectDPad) {
            if (this.BUTTON_RIGHT.pressed) {
                newJoyPLow &= ~A_RIGHT_MASK;
            }
            if (this.BUTTON_LEFT.pressed) {
                newJoyPLow &= ~B_LEFT_MASK;
            }
            if (this.BUTTON_UP.pressed) {
                newJoyPLow &= ~SELECT_UP_MASK;
            }
            if (this.BUTTON_DOWN.pressed) {
                newJoyPLow &= ~START_DOWN_MASK;
            }
        }

        this.joyP = (this.joyP & 0xF0) | (newJoyPLow & 0x0F);

        boolean newBit0 = (this.joyP & A_RIGHT_MASK) != 0;
        boolean newBit1 = (this.joyP & B_LEFT_MASK) != 0;
        boolean newBit2 = (this.joyP & SELECT_UP_MASK) != 0;
        boolean newBit3 = (this.joyP & START_DOWN_MASK) != 0;

        if ((originalBit0 && !newBit0) || (originalBit1 && !newBit1) || (originalBit2 && !newBit2) || (originalBit3 && !newBit3)) {
            this.triggerJoyPInterrupt();
        }

    }

    private void triggerJoyPInterrupt() {
        int IF = this.emulator.getMMIOController().getIF();
        this.emulator.getMMIOController().setIF(Processor.setBit(IF, SM83.JOYP_MASK));
    }

}
