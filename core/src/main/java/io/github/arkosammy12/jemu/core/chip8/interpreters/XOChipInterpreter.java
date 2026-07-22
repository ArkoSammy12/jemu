package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.XOChipEmulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;

public class XOChipInterpreter<E extends XOChipEmulator> extends SuperChipModernInterpreter<E> {

    public XOChipInterpreter(E emulator) {
        super(emulator);
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        return switch (firstNibble) {
            case 0x0 -> {
                if (firstByte == 0x00 && getY(firstByte, NN) == 0xD) { // 00DN: scroll-up N
                    this.emulator.getVideoGenerator().scrollUp(getN(firstByte, NN));
                    yield VALID_INSTRUCTION;
                } else {
                    yield super.execute(firstNibble, firstByte, NN);
                }
            }
            case 0x3, 0x4, 0x9, 0xE -> longSkipIfNecessary(super.execute(firstNibble, firstByte, NN));
            case 0x5 -> switch (getN(firstByte, NN)) {
                case 0x0 -> longSkipIfNecessary(super.execute(firstNibble, firstByte, NN));
                case 0x2 -> { // 5XY2: save vX - vY
                    Chip8Bus bus = this.emulator.getBus();
                    int I = getI();
                    int X = getX(firstByte, NN);
                    int Y = getY(firstByte, NN);
                    if (X > Y) {
                        for (int i = X, j = 0; i >= Y; i--, j++) {
                            bus.writeByte(I + j, getV(i));
                        }
                    } else {
                        for (int i = X, j = 0; i <= Y; i++, j++) {
                            bus.writeByte(I + j, getV(i));
                        }
                    }
                    yield VALID_INSTRUCTION;
                }
                case 0x3 -> { // 5XY3: load vX - vY
                    Chip8Bus bus = this.emulator.getBus();
                    int I = getI();
                    int X = getX(firstByte, NN);
                    int Y = getY(firstByte, NN);
                    if (X > Y) {
                        for (int i = X, j = 0; i >= Y; i--, j++) {
                            setV(i, bus.readByte(I + j));
                        }
                    } else {
                        for (int i = X, j = 0; i <= Y; i++, j++) {
                            setV(i, bus.readByte(I + j));
                        }
                    }
                    yield VALID_INSTRUCTION;
                }
                default -> super.execute(firstNibble, firstByte, NN);
            };
            case 0xF -> switch (NN) {
                case 0x00 -> {
                    if (firstByte == 0xF0) { // F000 NNNN: i := long NNNN
                        Chip8Bus bus = this.emulator.getBus();
                        int pc = getPC();
                        setI((bus.readByte(pc) << 8) | bus.readByte(pc + 1));
                        incrementPC();
                        yield VALID_INSTRUCTION;
                    } else {
                        yield super.execute(firstNibble, firstByte, NN);
                    }
                }
                case 0x01 -> { // FX01: plane X
                    this.emulator.getVideoGenerator().setBitplanes(getX(firstByte, NN));
                    yield VALID_INSTRUCTION;
                }
                case 0x02 -> {
                    if (firstByte == 0xF0) { // F002: audio
                        this.emulator.getAudioGenerator().loadAudio(getI());
                        yield VALID_INSTRUCTION;
                    } else {
                        yield super.execute(firstNibble, firstByte, NN);
                    }
                }
                case 0x3A -> { // FX3A: pitch := vX
                    this.emulator.getAudioGenerator().setPitch(getV(getX(firstByte, NN)));
                    yield VALID_INSTRUCTION;
                }
                default -> super.execute(firstNibble, firstByte, NN);
            };
            default -> super.execute(firstNibble, firstByte, NN);
        };
    }

    private int longSkipIfNecessary(int flags) {
        if ((flags & VALID_INSTRUCTION) != 0 && (flags & SKIP_TAKEN) != 0 && this.skippedLongInstruction()) {
            incrementPC();
        }
        return flags;
    }

    protected boolean skippedLongInstruction() {
        Chip8Bus bus = this.emulator.getBus();
        int pc = getPC();
        return (bus.readByte(pc - 2) == 0xF0) && (bus.readByte(pc - 1) == 0x00);
    }

}
