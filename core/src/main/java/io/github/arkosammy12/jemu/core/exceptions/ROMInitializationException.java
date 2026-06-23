package io.github.arkosammy12.jemu.core.exceptions;

public class ROMInitializationException extends EmulatorException {

    public ROMInitializationException(String message) {
        super(message);
    }

    public ROMInitializationException(String message, Throwable cause) {
        super(message + "\n" + cause);
    }

    public ROMInitializationException(Throwable cause) {
        super(cause);
    }

}
