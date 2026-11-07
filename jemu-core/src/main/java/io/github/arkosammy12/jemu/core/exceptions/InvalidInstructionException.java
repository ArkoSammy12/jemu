package io.github.arkosammy12.jemu.core.exceptions;

import io.github.arkosammy12.jemu.core.common.Emulator;

public class InvalidInstructionException extends EmulatorException {

    public InvalidInstructionException(int opcode, Emulator emulator) {
        super("Instruction opcode " + "$%02X".formatted(opcode) + " is invalid on the " + emulator.getHost().getSystemName() + " system!");
    }

}

