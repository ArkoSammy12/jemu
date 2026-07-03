package io.github.arkosammy12.jemu.core.rca.cosmacvip;

import io.github.arkosammy12.jemu.core.common.SystemController;

import java.util.function.BooleanSupplier;

public class CosmacVIPKeypad implements SystemController {

    private final boolean[] keys = new boolean[16];
    private int latchedKey = 0;

    private final BooleanSupplier regularEfxSupplier = () -> this.keys[this.latchedKey];
    private BooleanSupplier efxSupplier = this.regularEfxSupplier;

    public void forceCKeyPressOnFirstPoll() {
        this.efxSupplier = () -> {
            if (this.latchedKey == 0xC) {
                this.efxSupplier = this.regularEfxSupplier;
                return true;
            } else {
                return this.keys[this.latchedKey];
            }
        };
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

    public boolean getEFX() {
        return this.efxSupplier.getAsBoolean();
    }

    public void setLatchedKey(int value) {
        this.latchedKey = value & 0xF;
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
