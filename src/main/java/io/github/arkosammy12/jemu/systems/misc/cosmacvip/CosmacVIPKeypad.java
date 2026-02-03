package io.github.arkosammy12.jemu.systems.misc.cosmacvip;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.CosmacVipEmulator;
import io.github.arkosammy12.jemu.util.KeyboardLayout;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CosmacVIPKeypad extends KeyAdapter implements IODevice {

    private final Jemu jemu;
    private final CosmacVipEmulator emulator;

    private final boolean[] keys = new boolean[16];
    private int latchedKey = 0;

    @Override
    public void keyPressed(KeyEvent e) {
        int hex = getKeypadHexForKeyCode(this.jemu.getMainWindow().getSettingsBar().getKeyboardLayout().orElse(KeyboardLayout.QWERTY), e.getKeyCode());
        if (hex > -1) {
            this.setKeypadKeyPressed(hex);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int hex = getKeypadHexForKeyCode(this.jemu.getMainWindow().getSettingsBar().getKeyboardLayout().orElse(KeyboardLayout.QWERTY), e.getKeyCode());
        if (hex > -1) {
            this.setKeypadKeyUnpressed(hex);
        }
    }

    public synchronized boolean isKeyPressed(int hex) {
        return this.keys[hex];
    }

    private synchronized void setKeypadKeyPressed(int keyCode) {
        this.keys[keyCode] = true;
    }

    private synchronized void setKeypadKeyUnpressed(int keyCode) {
        this.keys[keyCode] = false;
    }

    public CosmacVIPKeypad(CosmacVipEmulator emulator) {
        this.jemu = emulator.getEmulatorSettings().getJemu();
        this.emulator = emulator;
    }

    @Override
    public void cycle() {
        this.emulator.getProcessor().setEF(2, this.isKeyPressed(this.latchedKey));
    }

    @Override
    public boolean isOutputPort(int port)  {
        return port == 2;
    }

    @Override
    public void onOutput(int port, int value) {
        this.latchedKey = value & 0xF;
    }

    private static int getKeypadHexForKeyCode(KeyboardLayout layout, int keyCode) {
        return switch (layout) {
            case QWERTY -> switch (keyCode) {
                case KeyEvent.VK_X -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_Q -> 0x4;
                case KeyEvent.VK_W -> 0x5;
                case KeyEvent.VK_E -> 0x6;
                case KeyEvent.VK_A -> 0x7;
                case KeyEvent.VK_S -> 0x8;
                case KeyEvent.VK_D -> 0x9;
                case KeyEvent.VK_Z -> 0xA;
                case KeyEvent.VK_C -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_R -> 0xD;
                case KeyEvent.VK_F -> 0xE;
                case KeyEvent.VK_V -> 0xF;
                default -> -1;
            };
            case DVORAK -> switch (keyCode) {
                case KeyEvent.VK_Q -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_QUOTE -> 0x4;
                case KeyEvent.VK_COMMA -> 0x5;
                case KeyEvent.VK_PERIOD -> 0x6;
                case KeyEvent.VK_A -> 0x7;
                case KeyEvent.VK_O -> 0x8;
                case KeyEvent.VK_E -> 0x9;
                case KeyEvent.VK_SEMICOLON -> 0xA;
                case KeyEvent.VK_J -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_P -> 0xD;
                case KeyEvent.VK_U -> 0xE;
                case KeyEvent.VK_K -> 0xF;
                default -> -1;
            };
            case AZERTY -> switch (keyCode) {
                case KeyEvent.VK_X -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_A -> 0x4;
                case KeyEvent.VK_Z -> 0x5;
                case KeyEvent.VK_E -> 0x6;
                case KeyEvent.VK_Q -> 0x7;
                case KeyEvent.VK_S -> 0x8;
                case KeyEvent.VK_D -> 0x9;
                case KeyEvent.VK_W -> 0xA;
                case KeyEvent.VK_C -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_R -> 0xD;
                case KeyEvent.VK_F -> 0xE;
                case KeyEvent.VK_V -> 0xF;
                default -> -1;
            };
            case COLEMAK -> switch (keyCode) {
                case KeyEvent.VK_X -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_Q -> 0x4;
                case KeyEvent.VK_W -> 0x5;
                case KeyEvent.VK_F -> 0x6;
                case KeyEvent.VK_A -> 0x7;
                case KeyEvent.VK_R -> 0x8;
                case KeyEvent.VK_S -> 0x9;
                case KeyEvent.VK_Z -> 0xA;
                case KeyEvent.VK_C -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_P -> 0xD;
                case KeyEvent.VK_T -> 0xE;
                case KeyEvent.VK_V -> 0xF;
                default -> -1;
            };
        };
    }

}
