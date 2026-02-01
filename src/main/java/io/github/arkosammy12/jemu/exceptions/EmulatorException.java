package io.github.arkosammy12.jemu.exceptions;

public class EmulatorException extends RuntimeException {

    public EmulatorException(String message) {
        super(message);
    }

    public EmulatorException(String message, Throwable cause) {
        super(message + ". \n" + cause);
    }

    public EmulatorException(Throwable cause) {
        super(cause);
    }

}
