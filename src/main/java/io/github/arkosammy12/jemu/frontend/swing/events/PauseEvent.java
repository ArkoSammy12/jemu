package io.github.arkosammy12.jemu.frontend.swing.events;

public non-sealed class PauseEvent implements Event {

    private final boolean pause;

    public PauseEvent(boolean pause) {
        this.pause = pause;
    }

    public boolean getPause() {
        return this.pause;
    }

    public void onCompleted(boolean stopped) {

    }

}
