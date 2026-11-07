package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.HyperWaveChip64Emulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.chip8.display.HyperWaveChip64Display;

public class HyperWaveChip64Interpreter<E extends HyperWaveChip64Emulator> extends XOChipInterpreter<E> {

    public HyperWaveChip64Interpreter(E emulator) {
        super(emulator);
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        return switch (firstNibble) {
            case 0x0 -> {
                if (firstByte == 0x00) {
                    yield switch (NN) {
                        case 0xE1 -> { // 00E1: INVERT
                            this.emulator.getVideoGenerator().invert();
                            yield VALID_INSTRUCTION;
                        }
                        case 0xF1 -> { // 00F1: OR MODE
                            this.emulator.getVideoGenerator().setDrawingMode(HyperWaveChip64Display.DrawingMode.OR);
                            yield VALID_INSTRUCTION;
                        }
                        case 0xF2 -> { // 00F2: SUBTRACT MODE
                            this.emulator.getVideoGenerator().setDrawingMode(HyperWaveChip64Display.DrawingMode.SUBTRACT);
                            yield VALID_INSTRUCTION;
                        }
                        case 0xF3 -> { // 00F3: XOR MODE
                            this.emulator.getVideoGenerator().setDrawingMode(HyperWaveChip64Display.DrawingMode.XOR);
                            yield VALID_INSTRUCTION;
                        }
                        default -> super.execute(firstNibble, firstByte, NN);
                    };
                } else {
                    yield super.execute(firstNibble, firstByte, NN);
                }
            }
            case 0x5 -> {
                if (getN(firstByte, NN) == 0x1) { // 5XY1: SKP VX > VY
                    int flags = VALID_INSTRUCTION;
                    if (getV(getX(firstByte, NN)) > getV(getY(firstByte, NN))) {
                        incrementPC();
                        if (this.skippedLongInstruction()) {
                            incrementPC();
                        }
                        flags |= SKIP_TAKEN;
                    }
                    yield flags;
                } else {
                    yield super.execute(firstNibble, firstByte, NN);
                }
            }
            case 0x8 -> switch (getN(firstByte, NN)) {
                case 0xC -> { // 8XYC: MULT VX, VY
                    int X = getX(firstByte, NN);
                    int product = getV(X) * getV(getY(firstByte, NN));
                    setV(X, product);
                    setV(0xF, (product & 0xFF00) >>> 8);
                    yield VALID_INSTRUCTION;
                }
                case 0xD -> { // 8XYD: DIV VX, VY
                    int X = getX(firstByte, NN);
                    int vY = getV(getY(firstByte, NN));
                    if (vY == 0) {
                        setV(X, 0);
                        setVF(false);
                    } else {
                        int vX = getV(X);
                        setV(X, vX / vY);
                        setV(0xF, vX % vY);
                    }
                    yield VALID_INSTRUCTION;
                }
                case 0xF -> { // 8XYF: DIV VY, VX
                    int X = getX(firstByte, NN);
                    int Y = getY(firstByte, NN);
                    int vX = getV(X);
                    if (vX == 0) {
                        setV(Y, 0);
                        setVF(false);
                    } else {
                        int vY = getV(Y);
                        setV(X, vY / vX);
                        setV(0xF, vY % vX);
                    }
                    yield VALID_INSTRUCTION;
                }
                default -> super.execute(firstNibble, firstByte, NN);
            };
            case 0xF -> switch (NN) {
                case 0x00 -> switch (firstByte) {
                    case 0xF1 -> { // F100 NNNN: LONG JUMP
                        Chip8Bus bus = this.emulator.getBus();
                        int pc = getPC();
                        setPC((bus.readByte(pc) << 8) | bus.readByte(pc + 1));
                        yield VALID_INSTRUCTION;
                    }
                    case 0xF2 -> { // F200 NNNN: LONG CALL SUBROUTINE
                        Chip8Bus bus = this.emulator.getBus();
                        int pc = getPC();
                        push(pc + 2);
                        setPC((bus.readByte(pc) << 8) | bus.readByte(pc + 1));
                        yield VALID_INSTRUCTION;
                    }
                    case 0xF3 -> { // F300 NNNN: LONG JUMP0
                        Chip8Bus bus = this.emulator.getBus();
                        int pc = getPC();
                        setPC(((bus.readByte(pc) << 8) | bus.readByte(pc + 1)) + getV(0x0));
                        yield VALID_INSTRUCTION;
                    }
                    default -> super.execute(firstNibble, firstByte, NN);
                };
                case 0x03 -> { // FX03: PALETTE N
                    this.emulator.getVideoGenerator().loadPalette(getX(firstByte, NN) & 0xF, getI());
                    yield VALID_INSTRUCTION;
                }
                case 0x1F -> { // FX1F: SUB I, VX
                    setI(getI() - getV(getX(firstByte, NN)));
                    yield VALID_INSTRUCTION;
                }
                default -> super.execute(firstNibble, firstByte, NN);
            };
            default -> super.execute(firstNibble, firstByte, NN);
        };
    }

    @Override
    protected boolean skippedLongInstruction() {
        Chip8Bus bus = this.emulator.getBus();
        int pc = getPC();
        int firstByte = bus.readByte(pc - 2);
        return (firstByte == 0xF0 || firstByte == 0xF1 || firstByte == 0xF2 || firstByte == 0xF3) && (bus.readByte(pc - 1) == 0x00);
    }

}
