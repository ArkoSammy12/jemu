package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.SuperChip11Emulator;

public class SuperChip11Interpreter<E extends SuperChip11Emulator> extends SuperChip10Interpreter<E> {

    public SuperChip11Interpreter(E emulator) {
        super(emulator);
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        if (firstByte == 0x00) {
            return switch (NN) {
                case 0xFB -> { // 00FB: scroll-right
                    this.emulator.getVideoGenerator().scrollRight(4);
                    yield VALID_INSTRUCTION;
                }
                case 0xFC -> { // 00FC: scroll-left
                    this.emulator.getVideoGenerator().scrollLeft(4);
                    yield VALID_INSTRUCTION;
                }
                default -> {
                    if (getY(firstByte, NN) == 0xC) { // 00CN: scroll-down N
                        int N = getN(firstByte, NN);
                        if (N == 0x0) {
                            super.execute(firstNibble, firstByte, NN);
                        }
                        this.emulator.getVideoGenerator().scrollDown(N);
                        yield VALID_INSTRUCTION;
                    } else {
                        yield super.execute(firstNibble, firstByte, NN);
                    }
                }
            };
        } else {
            return super.execute(firstNibble, firstByte, NN);
        }
    }

}
