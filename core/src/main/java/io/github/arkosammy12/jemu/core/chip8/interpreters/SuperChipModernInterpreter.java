package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.SuperChipModernEmulator;
import io.github.arkosammy12.jemu.core.chip8.display.SuperChipModernDisplay;

public class SuperChipModernInterpreter<E extends SuperChipModernEmulator> extends SuperChip11Interpreter<E> {

    public SuperChipModernInterpreter(E emulator) {
        super(emulator);
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        if (firstByte == 0x00) {
            return switch (NN) {
                case 0xFE -> { // 00FE: lores
                    SuperChipModernDisplay<?> display = this.emulator.getVideoGenerator();
                    display.setHires(false);
                    display.clear();
                    yield VALID_INSTRUCTION;
                }
                case 0xFF -> { // 00FF: hires
                    SuperChipModernDisplay<?> display = this.emulator.getVideoGenerator();
                    display.setHires(true);
                    display.clear();
                    yield VALID_INSTRUCTION;
                }
                default -> {
                    if (getY(firstByte, NN) == 0xC) { // 00CN: scroll-down N
                        this.emulator.getVideoGenerator().scrollDown(getN(firstByte, NN));
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
