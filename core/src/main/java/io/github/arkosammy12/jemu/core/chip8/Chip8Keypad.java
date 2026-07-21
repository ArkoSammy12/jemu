package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class Chip8Keypad implements SystemController {

    private final boolean[] keys = new boolean[16];
    private int waitingKey = -1;

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions chip8Actions)) {
            return;
        }
        this.keys[chip8Actions.key] = true;
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions chip8Actions)) {
            return;
        }
        this.keys[chip8Actions.key] = false;
    }

    public boolean isKeyPressed(int hex) {
        return this.keys[hex & 0xF];
    }

    public int getFirstPressedKeypadKey() {
        for (int i = 0; i < 16; i++) {
            if (this.keys[i]) {
                return i;
            }
        }
        return -1;
    }

    public void setWaitingKeypadKey(int hex) {
        this.waitingKey = hex;
    }

    public int getWaitingKeypadKey() {
        return waitingKey;
    }

    public void resetWaitingKeypadKey() {
        this.waitingKey = -1;
    }

    public enum Actions implements Action {
        KEY_0("Key 0", 0x0),
        KEY_1("Key 1", 0x1),
        KEY_2("Key 2", 0x2),
        KEY_3("Key 3", 0x3),
        KEY_4("Key 4", 0x4),
        KEY_5("Key 5", 0x5),
        KEY_6("Key 6", 0x6),
        KEY_7("Key 7", 0x7),
        KEY_8("Key 8", 0x8),
        KEY_9("Key 9", 0x9),
        KEY_A("Key A", 0xA),
        KEY_B("Key B", 0xB),
        KEY_C("Key C", 0xC),
        KEY_D("Key D", 0xD),
        KEY_E("Key E", 0xE),
        KEY_F("Key F", 0xF);

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


}
