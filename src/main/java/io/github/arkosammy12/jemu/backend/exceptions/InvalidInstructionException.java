package io.github.arkosammy12.jemu.backend.exceptions;

import io.github.arkosammy12.jemu.application.util.System;

public class InvalidInstructionException extends EmulatorException {

    public InvalidInstructionException(int firstByte, int secondByte, System system) {
        super("Instruction "
                + String.format("%04X", ((firstByte << 8) | secondByte))
                + " is invalid on the "
                + system.getDisplayName() + " system!");
    }

    public InvalidInstructionException(int opcode, String systemName) {
        super("Instruction "
                + String.format("%02X", opcode)
                + " is invalid on the "
                + systemName + " system!");
    }

}

