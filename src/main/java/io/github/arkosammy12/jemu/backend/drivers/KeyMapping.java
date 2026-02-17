package io.github.arkosammy12.jemu.backend.drivers;

public interface KeyMapping {

    int getDefaultCodePoint();

    Runnable getKeyPressedCallback();

    Runnable getKeyReleasedCallback();

}
