package io.github.arkosammy12.jemu.core.studioii;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class RCAStudioIIKeypad implements SystemController {

    private final boolean[] keypad1Keys = new boolean[10];
    private final boolean[] keypad2Keys = new boolean[10];
    private int latchedKey;

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions studioIIAction)) {
            return;
        }
        switch (studioIIAction.keypad) {
            case KEYPAD_A -> this.keypad1Keys[studioIIAction.key] = true;
            case KEYPAD_B -> this.keypad2Keys[studioIIAction.key] = true;
        }
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions studioIIAction)) {
            return;
        }
        switch (studioIIAction.keypad) {
            case KEYPAD_A -> this.keypad1Keys[studioIIAction.key] = false;
            case KEYPAD_B -> this.keypad2Keys[studioIIAction.key] = false;
        }
    }

    public boolean getKeypad1EFX() {
        if (this.latchedKey <= 9) {
            return this.keypad1Keys[this.latchedKey];
        } else {
            return false;
        }
    }

    public boolean getKeypad2EFX() {
        if (this.latchedKey <= 9) {
            return this.keypad2Keys[this.latchedKey];
        } else {
            return false;
        }
    }

    public void setLatchedKey(int value) {
        this.latchedKey = value & 0xF;
    }

    public enum Actions implements Action {
        KEYPADA_0("Keypad A key 0", 0x0, Keypad.KEYPAD_A),
        KEYPADA_1("Keypad A key 1", 0x1, Keypad.KEYPAD_A),
        KEYPADA_2("Keypad A key 2", 0x2, Keypad.KEYPAD_A),
        KEYPADA_3("Keypad A key 3", 0x3, Keypad.KEYPAD_A),
        KEYPADA_4("Keypad A key 4", 0x4, Keypad.KEYPAD_A),
        KEYPADA_5("Keypad A key 5", 0x5, Keypad.KEYPAD_A),
        KEYPADA_6("Keypad A key 6", 0x6, Keypad.KEYPAD_A),
        KEYPADA_7("Keypad A key 7", 0x7, Keypad.KEYPAD_A),
        KEYPADA_8("Keypad A key 8", 0x8, Keypad.KEYPAD_A),
        KEYPADA_9("Keypad A key 9", 0x9, Keypad.KEYPAD_A),

        KEYPADB_0("Keypad B key 0", 0x0, Keypad.KEYPAD_B),
        KEYPADB_1("Keypad B key 1", 0x1, Keypad.KEYPAD_B),
        KEYPADB_2("Keypad B key 2", 0x2, Keypad.KEYPAD_B),
        KEYPADB_3("Keypad B key 3", 0x3, Keypad.KEYPAD_B),
        KEYPADB_4("Keypad B key 4", 0x4, Keypad.KEYPAD_B),
        KEYPADB_5("Keypad B key 5", 0x5, Keypad.KEYPAD_B),
        KEYPADB_6("Keypad B key 6", 0x6, Keypad.KEYPAD_B),
        KEYPADB_7("Keypad B key 7", 0x7, Keypad.KEYPAD_B),
        KEYPADB_8("Keypad B key 8", 0x8, Keypad.KEYPAD_B),
        KEYPADB_9("Keypad B key 9", 0x9, Keypad.KEYPAD_B);

        private final String label;
        private final Keypad keypad;
        private final int key;

        Actions(String label, int key, Keypad keypad) {
            this.label = label;
            this.key = key;
            this.keypad = keypad;
        }

        @Override
        public String getLabel() {
            return this.label;
        }

    }

    private enum Keypad {
        KEYPAD_A,
        KEYPAD_B
    }

}
