package io.github.arkosammy12.jemu.backend.cosmacvip;

import io.github.arkosammy12.jemu.backend.common.SystemController;

public class CosmacVIPKeypad<E extends CosmacVipEmulator> extends SystemController<E> implements IODevice {

    private final boolean[] keys = new boolean[16];
    private int latchedKey = 0;

    public CosmacVIPKeypad(E emulator) {
        super(emulator);
    }

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions vipActions)) {
            return;
        }
        this.keys[vipActions.key] = true;
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions vipActions)) {
            return;
        }
        this.keys[vipActions.key] = false;
    }

    @Override
    public void cycle() {
        this.emulator.getCpu().setEF(2, this.keys[this.latchedKey]);
    }

    @Override
    public boolean isOutputPort(int port)  {
        return port == 2;
    }

    @Override
    public void onOutput(int port, int value) {
        this.latchedKey = value & 0xF;
    }

    public enum Actions implements Action {
        KEY_0("0", 0x0),
        KEY_1("1", 0x1),
        KEY_2("2", 0x2),
        KEY_3("3", 0x3),
        KEY_4("4", 0x4),
        KEY_5("5", 0x5),
        KEY_6("6", 0x6),
        KEY_7("7", 0x7),
        KEY_8("8", 0x8),
        KEY_9("9", 0x9),
        KEY_A("A", 0xA),
        KEY_B("B", 0xB),
        KEY_C("C", 0xC),
        KEY_D("D", 0xD),
        KEY_E("E", 0xE),
        KEY_F("F", 0xF);

        private final String label;
        private final int key;

        Actions(String label, int key) {
            this.label = label;
            this.key = key;
        }

        @Override
        public String getLabel() {
            return this.label;
        }

    }

    // TODO: Handle custom key mappings
    /*
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

     */

}
