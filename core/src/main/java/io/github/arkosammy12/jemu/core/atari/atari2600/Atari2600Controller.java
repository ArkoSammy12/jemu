package io.github.arkosammy12.jemu.core.atari.atari2600;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class Atari2600Controller<E extends Atari2600Emulator> extends SystemController<E> {

    public Atari2600Controller(E emulator) {
        super(emulator);
    }

    @Override
    public void onActionPressed(Action action) {

    }

    @Override
    public void onActionReleased(Action action) {

    }

}
