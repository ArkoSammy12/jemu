package io.github.arkosammy12.jemu.core.exceptions;

public class InvalidInstructionException extends EmulatorException {

    public InvalidInstructionException(int firstByte, int secondByte, String systemDisplayName) {
        super("Instruction "
                + String.format("%04X", ((firstByte << 8) | secondByte))
                + " is invalid on the "
                + systemDisplayName + " system!");
    }

    public InvalidInstructionException(int opcode, String systemName) {
        super("Instruction "
                + String.format("%02X", opcode)
                + " is invalid on the "
                + systemName + " system!");
    }

}

