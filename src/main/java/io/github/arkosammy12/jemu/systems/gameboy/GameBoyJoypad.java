package io.github.arkosammy12.jemu.systems.gameboy;

import io.github.arkosammy12.jemu.systems.common.Processor;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GameBoyJoypad extends KeyAdapter {

    private final GameBoyEmulator emulator;

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

    public GameBoyJoypad(GameBoyEmulator emulator) {
        this.emulator = emulator;
    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_W -> up = true;
            case KeyEvent.VK_S -> down = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_J -> A = true;
            case KeyEvent.VK_K -> B = true;
            case KeyEvent.VK_ENTER -> start = true;
            case KeyEvent.VK_BACK_SPACE -> select = true;
        }
        this.updateJoyP();
    }

    @Override
    public synchronized void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_W -> up = false;
            case KeyEvent.VK_S -> down = false;
            case KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_J -> A = false;
            case KeyEvent.VK_K -> B = false;
            case KeyEvent.VK_ENTER -> start = false;
            case KeyEvent.VK_BACK_SPACE -> select = false;
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
        boolean originalBit0 = (this.joyP & A_RIGHT_MASK) != 0;
        boolean originalBit1 = (this.joyP & B_LEFT_MASK) != 0;
        boolean originalBit2 = (this.joyP & SELECT_UP_MASK) != 0;
        boolean originalBit3 = (this.joyP & START_DOWN_MASK) != 0;

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
