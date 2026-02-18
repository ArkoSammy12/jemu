package io.github.arkosammy12.jemu.backend.common;

public abstract class SystemController<E extends Emulator> {

    protected final E emulator;

    public SystemController(E emulator) {
        this.emulator = emulator;
    }

    abstract public void onActionPressed(Action action);

    abstract public void onActionReleased(Action action);

    public interface Action {

        String getLabel();

    }

}
