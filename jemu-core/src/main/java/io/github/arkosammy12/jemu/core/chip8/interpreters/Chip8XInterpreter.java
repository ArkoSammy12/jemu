package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.Chip8XEmulator;

public class Chip8XInterpreter<E extends Chip8XEmulator> extends Chip8Interpreter<E> {

    public Chip8XInterpreter(E emulator) {
        super(emulator);
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        return switch (firstNibble) {
            case 0x0 -> {
                if (firstByte == 0x02 && NN == 0xA0) { // 02A0: cycle-bgcol
                    this.emulator.getVideoGenerator().cycleBackgroundColor();
                    yield VALID_INSTRUCTION;
                } else {
                    yield super.execute(firstNibble, firstByte, NN);
                }
            }
            case 0x5 -> {
                if (getN(firstByte, NN) == 0x1) { // 5XY1: 0x5X 0xY1. Add as packed octal digits
                    int X = getX(firstByte, NN);
                    setV(X, ((getV(X) & 0x77) + (getV(getY(firstByte, NN)) & 0x77)) & 0x77);
                    yield VALID_INSTRUCTION;
                } else {
                    yield super.execute(firstNibble, firstByte, NN);
                }
            }
            case 0xB -> { // BXYN: col-high X Y N / BXY0: col-low X Y
                int X = getX(firstByte, NN);
                this.emulator.getVideoGenerator().drawColor(getV(X), getV((X + 1) & 0xF), getV(getY(firstByte, NN)) & 0x7, getN(firstByte, NN));
                yield VALID_INSTRUCTION;
            }
            case 0xE -> switch (NN) {
                case 0xF2 -> VALID_INSTRUCTION; // EXF2: 0xeX 0xf2. Skip if key on keypad 2 is pressed. Stubbed
                case 0xF5 -> VALID_INSTRUCTION; // EXF5: 0xeX 0xf5. Skip if key on keypad 2 is not pressed. Stubbed
                default -> super.execute(firstNibble, firstByte, NN);
            };
            case 0xF -> switch (NN) {
                case 0xF8 -> { // FXF8: 0xfX 0xf8. Output register to IO port
                    this.emulator.getAudioGenerator().setPitch(getV(getX(firstByte, NN)));
                    yield VALID_INSTRUCTION;
                }
                case 0xFB -> VALID_INSTRUCTION; // FXFB: 0xfX 0xfb. Wait for input from IO port and load into register. Stubbed
                default -> super.execute(firstNibble, firstByte, NN);
            };
            default -> super.execute(firstNibble, firstByte, NN);
        };
    }

}
