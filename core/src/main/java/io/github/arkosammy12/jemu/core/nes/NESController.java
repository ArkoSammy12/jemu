package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class NESController<E extends NESEmulator> extends SystemController<E> {

    public NESController(E emulator) {
        super(emulator);
    }

    @Override
    public void onActionPressed(Action action) {

    }

    @Override
    public void onActionReleased(Action action) {

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
