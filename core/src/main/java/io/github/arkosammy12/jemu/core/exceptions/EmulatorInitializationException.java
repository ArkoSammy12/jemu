package io.github.arkosammy12.jemu.core.exceptions;

public class EmulatorInitializationException extends EmulatorException {

    public EmulatorInitializationException(String message) {
        super(message);
    }

    public EmulatorInitializationException(String message, Throwable cause) {
        super(message + "\n" + cause);
    }

    public EmulatorInitializationException(Throwable cause) {
        super(cause);
    }

}
