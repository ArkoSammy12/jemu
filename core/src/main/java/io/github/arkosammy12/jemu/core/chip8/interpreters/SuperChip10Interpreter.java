package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.Chip8Host;
import io.github.arkosammy12.jemu.core.chip8.SuperChip10Emulator;

public class SuperChip10Interpreter<E extends SuperChip10Emulator> extends Chip8Interpreter<E>  {

    public SuperChip10Interpreter(E emulator) {
        super(emulator);
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        return switch (firstNibble) {
            case 0x0 -> {
                if (firstByte == 0x00) {
                    yield switch (NN) { // 00FD: exit
                        case 0xFD -> {
                            this.emulator.getHost().exit();
                            yield VALID_INSTRUCTION;
                        }
                        case 0xFE -> { // 00FE: lores
                            this.emulator.getVideoGenerator().setHires(false);
                            yield VALID_INSTRUCTION;
                        }
                        case 0xFF -> { // 00FF: hires
                            this.emulator.getVideoGenerator().setHires(true);
                            yield VALID_INSTRUCTION;
                        }
                        default -> super.execute(firstNibble, firstByte, NN);
                    };
                } else {
                    yield super.execute(firstNibble, firstByte, NN);
                }
            }
            case 0xF -> switch (NN) {
                case 0x30 -> { // FX30: i := bighex vX
                    setI(this.emulator.getHost().getSpriteFont().getBigFontSpriteOffset(getV(getX(firstByte, NN)) & 0xF));
                    yield VALID_INSTRUCTION | FONT_SPRITE_POINTER;
                }
                case 0x75 -> { // FX75: saveflags vX
                    Chip8Host host = this.emulator.getHost();
                    int X = getX(firstByte, NN);
                    for (int i = 0; i <= X; i++) {
                        host.setPersistentFlag(i, getV(i));
                    }
                    yield VALID_INSTRUCTION;
                }
                case 0x85 -> { // FX85: loadflags vX
                    Chip8Host host = this.emulator.getHost();
                    int X = getX(firstByte, NN);
                    for (int i = 0; i <= X; i++) {
                        setV(i, host.getPersistentFlag(i));
                    }
                    yield VALID_INSTRUCTION;
                }
                default -> super.execute(firstNibble, firstByte, NN);
            };
            default -> super.execute(firstNibble, firstByte, NN);
        };
    }

}
