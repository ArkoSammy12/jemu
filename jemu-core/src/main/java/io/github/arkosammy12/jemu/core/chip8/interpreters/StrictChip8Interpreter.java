package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.Chip8Keypad;
import io.github.arkosammy12.jemu.core.chip8.StrictChip8Emulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.chip8.bus.StrictChip8Bus;
import io.github.arkosammy12.jemu.core.chip8.display.StrictChip8Display;

import static io.github.arkosammy12.jemu.core.chip8.display.Chip8Display.BASE_SLICE_MASK_8;

/// Implementation of cycle accurate CHIP-8 generously provided by @gulrak's [Cadmium](https://github.com/gulrak/cadmium)
public final class StrictChip8Interpreter extends Chip8Interpreter<StrictChip8Emulator> {

    public static final int WAITING = 1 << 7;

    private long instructionCycles;
    private boolean waiting;

    public StrictChip8Interpreter(StrictChip8Emulator emulator) {
        super(emulator);
    }

    private long getInstructionCycles() {
        return this.instructionCycles;
    }

    private void setInstructionCycles(long instructionCycles) {
        this.instructionCycles = instructionCycles;
    }

    private void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isWaiting() {
        return this.waiting;
    }

    @Override
    protected void setPC(int programCounter) {
        this.programCounter = programCounter & 0xFFFF;
    }

    @Override
    protected void incrementPC() {
        this.programCounter = (programCounter + 2) & 0xFFFF;
    }

    @Override
    protected void decrementPC() {
        this.programCounter = (programCounter - 2) & 0xFFFF;
    }

    @Override
    protected void setI(int indexRegister) {
        this.indexRegister = indexRegister & 0xFFFF;
    }

    @Override
    protected void push(int value) {
        this.emulator.getBus().writeStackWord(this.stackPointer, value);
        if (this.stackPointer >= 0 && this.stackPointer < this.stack.length) {
            this.stack[stackPointer] = value;
        }
        this.stackPointer = (this.stackPointer + 1) & 0xFFFF;
    }

    @Override
    protected int pop() {
        this.stackPointer = (this.stackPointer - 1) & 0xFFFF;
        return this.emulator.getBus().readStackWord(this.stackPointer);
    }

    @Override
    protected void setV(int register, int value) {
        this.emulator.getBus().setV(register, value);
        super.setV(register, value);
    }

    @Override
    protected void setVF(boolean value) {
        this.emulator.getBus().setV(0xF, value ? 1 : 0);
        super.setVF(value);
    }

    @Override
    public int getV(int index) {
        return this.emulator.getBus().getV(index);
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        int flags = WAITING;
        if (!this.isWaiting()) {
            flags = 0;
            this.emulator.addCycles((firstByte & 0xF0) != 0 ? 68 : 40);
        }
        return this.executeInternal(firstNibble, firstByte, NN) | flags;
    }

    private int executeInternal(int firstNibble, int firstByte, int NN) {
        return switch (firstNibble) {
            case 0x0 -> {
                if (firstByte == 0x00) {
                    yield switch (NN) {
                        case 0xE0 -> { // 00E0: cls
                            final int eraseCycles = 3078;
                            long cyclesLeftInFrame = this.emulator.getCyclesLeftInCurrentFrame();
                            if (!this.isWaiting()) {
                                setWaiting(true);
                                decrementPC();
                                setInstructionCycles((eraseCycles > cyclesLeftInFrame) ? eraseCycles - cyclesLeftInFrame : 0);
                                this.emulator.addCycles(cyclesLeftInFrame);
                            } else {
                                if (this.getInstructionCycles() != 0) {
                                    long currentInstructionCycles = this.getInstructionCycles();
                                    this.setInstructionCycles(currentInstructionCycles - (Math.min(currentInstructionCycles, cyclesLeftInFrame)));
                                    this.emulator.addCycles(cyclesLeftInFrame);
                                }
                                if (this.getInstructionCycles() == 0) {
                                    this.setWaiting(false);
                                    this.emulator.getVideoGenerator().clear();
                                } else {
                                    this.decrementPC();
                                }
                            }
                            yield VALID_INSTRUCTION | LONG_INSTRUCTION | CLS_EXECUTED;
                        }
                        case 0xEE -> { // 00EE: return
                            this.setPC(this.pop());
                            this.emulator.addCycles(10);
                            yield VALID_INSTRUCTION;
                        }
                        default -> 0;
                    };
                } else {
                    yield 0;
                }
            }
            case 0x1 -> { // 1NNN: jump NNN
                this.setPC(getNNN(firstByte, NN));
                this.emulator.addCycles(12);
                yield VALID_INSTRUCTION;
            }
            case 0x2 -> { // 2NNN: :call NNN
                this.push(this.getPC());
                this.setPC(getNNN(firstByte, NN));
                this.emulator.addCycles(26);
                yield VALID_INSTRUCTION;
            }
            case 0x3 -> { // 3XNN: if vX != NN then
                int flags = VALID_INSTRUCTION;
                if (NN == this.getV(getX(firstByte, NN))) {
                    flags |= SKIP_TAKEN;
                    this.incrementPC();
                    this.emulator.addCycles(14);
                } else {
                    this.emulator.addCycles(10);
                }
                yield flags;
            }
            case 0x4 -> { // 4XNN: if vX == NN then
                int flags = VALID_INSTRUCTION;
                if (NN != this.getV(getX(firstByte, NN))) {
                    flags |= SKIP_TAKEN;
                    this.incrementPC();
                    this.emulator.addCycles(14);
                } else {
                    this.emulator.addCycles(10);
                }
                yield flags;
            }
            case 0x5 -> { // 5XY0: if vX != vY then
                int flags = VALID_INSTRUCTION;
                if (this.getV(getX(firstByte, NN)) == this.getV(getY(firstByte, NN))) {
                    flags |= SKIP_TAKEN;
                    this.incrementPC();
                    this.emulator.addCycles(18);
                } else {
                    this.emulator.addCycles(14);
                }
                yield flags;
            }
            case 0x6 -> { // 6XNN: vX := NN
                this.setV(getX(firstByte, NN), NN);
                this.emulator.addCycles(6);
                yield VALID_INSTRUCTION;
            }
            case 0x7 -> { // 7XNN: vX += NN
                int X = getX(firstByte, NN);
                this.setV(X, this.getV(X) + NN);
                this.emulator.addCycles(10);
                yield VALID_INSTRUCTION;
            }
            case 0x8 -> {
                if ((NN & 0xF) != 0) {
                    int word = (0xF0 + (NN & 0xF)) << 8 | 0xD3;
                    if (this.stackPointer >= 0 && this.stackPointer < this.stack.length) {
                        this.stack[this.stackPointer] = word;
                    }
                    this.emulator.getBus().writeStackWord(this.stackPointer, word);
                }
                yield switch (getN(firstByte, NN)) {
                    case 0x0 -> { // 8XY0: vX := vY
                        this.setV(getX(firstByte, NN), this.getV(getY(firstByte, NN)));
                        this.emulator.addCycles(12);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x1 -> { // 8XY1: vX |= vY
                        int X = getX(firstByte, NN);
                        this.setV(X, this.getV(X) | this.getV(getY(firstByte, NN)));
                        this.setVF(false);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x2 -> { // 8XY2: vX &= vY
                        int X = getX(firstByte, NN);
                        this.setV(X, this.getV(X) & this.getV(getY(firstByte, NN)));
                        this.setVF(false);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x3 -> { // 8XY3: vX ^= vY
                        int X = getX(firstByte, NN);
                        this.setV(X, this.getV(X) ^ this.getV(getY(firstByte, NN)));
                        this.setVF(false);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x4 -> { // 8XY4: vX += vY
                        int X = getX(firstByte, NN);
                        int value = this.getV(X) + this.getV(getY(firstByte, NN));
                        this.setV(X, value);
                        this.setVF(value > 0xFF);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x5 -> { // 8XY5: vX -= vY
                        int X = getX(firstByte, NN);
                        int vX = this.getV(X);
                        int vY = this.getV(getY(firstByte, NN));
                        this.setV(X, vX - vY);
                        this.setVF(vX >= vY);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x6 -> { // 8XY6: vX >>= vY
                        int X = getX(firstByte, NN);
                        int vY = this.getV(getY(firstByte, NN));
                        this.setV(X, vY >>> 1);
                        this.setVF((vY & 1) != 0);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x7 -> { // 8XY7: vX =- vY
                        int X = getX(firstByte, NN);
                        int vX = this.getV(X);
                        int vY = this.getV(getY(firstByte, NN));
                        this.setV(X, vY - vX);
                        this.setVF(vY >= vX);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    case 0xE -> { // 8XYE: vX <<= vY
                        int X = getX(firstByte, NN);
                        int vY = this.getV(getY(firstByte, NN));
                        this.setV(X, vY << 1);
                        this.setVF((vY & 128) != 0);
                        this.emulator.addCycles(44);
                        yield VALID_INSTRUCTION;
                    }
                    default -> 0;
                };
            }
            case 0x9 -> { // 9XY0: if vX == vY then
                int flags = VALID_INSTRUCTION;
                if (this.getV(getX(firstByte, NN)) != this.getV(getY(firstByte, NN))) {
                    flags |= SKIP_TAKEN;
                    this.incrementPC();
                    this.emulator.addCycles(18);
                } else {
                    this.emulator.addCycles(14);
                }
                yield flags;
            }
            case 0xA -> { // ANNN: i := NNN
                this.setI(getNNN(firstByte, NN));
                this.emulator.addCycles(12);
                yield VALID_INSTRUCTION;
            }
            case 0xB -> { // BNNN: jump0 NNN / BXNN: jump0 NNN + vX
                int currentProgramCounter = this.getPC();
                int newProgramCounter = getNNN(firstByte, NN) + this.getV(0x0);
                this.setPC(newProgramCounter);
                this.emulator.addCycles((currentProgramCounter & 0xFF00) != (newProgramCounter & 0xFF00) ? 24 : 22);
                yield VALID_INSTRUCTION;
            }
            case 0xC -> { // CXNN: vX := random NN
                this.setV(getX(firstByte, NN), this.random.nextInt() & NN);
                this.emulator.addCycles(36);
                yield VALID_INSTRUCTION;
            }
            case 0xD -> { // DXYN: sprite vX vX N
                StrictChip8Display display = this.emulator.getVideoGenerator();
                int displayWidth = display.getWidth();
                int displayHeight = display.getHeight();

                int spriteX = this.getV(getX(firstByte, NN)) % displayWidth;
                int spriteY = this.getV(getY(firstByte, NN)) % displayHeight;
                int N = getN(firstByte, NN);

                long cyclesLeftInFrame = this.emulator.getCyclesLeftInCurrentFrame();
                if (!this.isWaiting()) {
                    long prepareTime = 68 + N * (46 + 20 * (spriteX & 7));
                    this.setWaiting(true);
                    this.decrementPC();
                    this.setInstructionCycles(prepareTime > cyclesLeftInFrame ? prepareTime - cyclesLeftInFrame : 0);
                    this.emulator.addCycles(cyclesLeftInFrame);
                } else {
                    if (this.getInstructionCycles() != 0) {
                        this.decrementPC();
                        long currentInstructionCycles = this.getInstructionCycles();
                        this.setInstructionCycles(currentInstructionCycles - (Math.min(currentInstructionCycles, cyclesLeftInFrame)));
                        this.emulator.addCycles(cyclesLeftInFrame);
                    } else {
                        this.setWaiting(false);
                        this.drawSprite(spriteX, spriteY, this.getI(), N);
                    }
                }
                yield VALID_INSTRUCTION;
            }
            case 0xE -> switch (NN) {
                case 0x9E -> { // EX9E: if vX -key then
                    int flags = VALID_INSTRUCTION;
                    if (this.emulator.getSystemController().isKeyPressed(this.getV(getX(firstByte, NN)) & 0xF)) {
                        flags |= SKIP_TAKEN;
                        this.incrementPC();
                        this.emulator.addCycles(18);
                    } else {
                        this.emulator.addCycles(14);
                    }
                    yield flags;
                }
                case 0xA1 -> { // EXA1: if vX key then
                    int flags = VALID_INSTRUCTION;
                    if (!this.emulator.getSystemController().isKeyPressed(this.getV(getX(firstByte, NN)) & 0xF)) {
                        flags |= SKIP_TAKEN;
                        this.incrementPC();
                        this.emulator.addCycles(18);
                    } else {
                        this.emulator.addCycles(14);
                    }
                    yield flags;
                }
                default -> 0;
            };
            case 0xF -> {
                this.emulator.addCycles(4);
                yield switch (NN) {
                    case 0x07 -> { // FX07: vX := delay
                        this.setV(getX(firstByte, NN), this.getDT());
                        this.emulator.addCycles(6);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x0A -> { // FX0A: vX := key
                        if (this.getInstructionCycles() != 0) {
                            if (this.getST() != 0) {
                                this.emulator.addCycles(this.emulator.getCyclesLeftInCurrentFrame());
                            } else {
                                this.setInstructionCycles(0);
                                this.setWaiting(false);
                                this.emulator.addCycles(10);
                            }
                        } else {
                            Chip8Keypad keypad = this.emulator.getSystemController();
                            int firstPressedKey = keypad.getFirstPressedKeypadKey();
                            int waitingKey = keypad.getWaitingKeypadKey();
                            if (waitingKey >= 0) {
                                if (firstPressedKey < 0 || waitingKey != firstPressedKey) {
                                    this.setV(getX(firstByte, NN), waitingKey);
                                    keypad.resetWaitingKeypadKey();
                                    this.emulator.addCycles(this.emulator.getCyclesLeftInCurrentFrame());
                                    this.setInstructionCycles(3 * 3668);
                                    this.setST(4);
                                    this.decrementPC();
                                    this.setWaiting(true);
                                } else {
                                    this.decrementPC();
                                    this.setST(4);
                                    this.setWaiting(true);
                                }
                            } else {
                                if (firstPressedKey >= 0) {
                                    keypad.setWaitingKeypadKey(firstPressedKey);
                                }
                                this.decrementPC();
                                this.setWaiting(true);
                            }
                        }
                        yield VALID_INSTRUCTION | GET_KEY_EXECUTED;
                    }
                    case 0x15 -> { // FX15: delay := vX
                        this.setDT(this.getV(getX(firstByte, NN)));
                        this.emulator.addCycles(6);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x18 -> { // FX18: buzzer := vX
                        this.setST(this.getV(getX(firstByte, NN)));
                        this.emulator.addCycles(6);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x1E -> { // FX1E: i += vX
                        int currentIndexRegister = this.getI();
                        int newIndexRegister = currentIndexRegister + this.getV(getX(firstByte, NN));
                        this.setI(newIndexRegister);
                        this.emulator.addCycles((currentIndexRegister & 0xFF00) != (newIndexRegister & 0xFF00) ? 18 : 12);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x29 -> { // FX29: i := hex vX
                        this.setI(StrictChip8Bus.getFontDigitOffset(this.getV(getX(firstByte, NN))));
                        this.emulator.addCycles(16);
                        yield VALID_INSTRUCTION | FONT_SPRITE_POINTER;
                    }
                    case 0x33 -> { // FX33: bcd vX
                        Chip8Bus bus = this.emulator.getBus();
                        int currentIndexPointer = this.getI();
                        int vX = this.getV(getX(firstByte, NN));
                        long hundreds = (vX * 0x51EB851FL) >>> 37;
                        long remainder = vX - hundreds * 100;
                        long tens = (remainder * 0xCCCDL) >>> 19;
                        long ones = remainder - tens * 10;
                        bus.writeByte(currentIndexPointer, (int) hundreds);
                        bus.writeByte(currentIndexPointer + 1, (int) tens);
                        bus.writeByte(currentIndexPointer + 2, (int) ones);
                        this.emulator.addCycles(80 + (hundreds + tens + ones) * 16);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x55 -> { // FX55: save vX
                        Chip8Bus bus = this.emulator.getBus();
                        int currentIndexPointer = this.getI();
                        int X = getX(firstByte, NN);
                        this.emulator.addCycles(14);
                        for (int i = 0; i <= X; i++) {
                            bus.writeByte(currentIndexPointer + i, this.getV(i));
                            this.emulator.addCycles(14);
                        }
                        this.setI(currentIndexPointer + X + 1);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x65 -> { // FX65: load vX
                        Chip8Bus bus = this.emulator.getBus();
                        int currentIndexRegister = this.getI();
                        int X = getX(firstByte, NN);
                        this.emulator.addCycles(14);
                        for (int i = 0; i <= X; i++) {
                            this.setV(i, bus.readByte(currentIndexRegister + i));
                            this.emulator.addCycles(14);
                        }
                        this.setI(currentIndexRegister + X + 1);
                        yield VALID_INSTRUCTION;
                    }
                    default -> 0;
                };
            }
            default -> 0;
        };
    }

    private void drawSprite(int spriteX, int spriteY, int currentIndexRegister, int N) {
        StrictChip8Display display = this.emulator.getVideoGenerator();
        StrictChip8Bus bus = this.emulator.getBus();
        int displayWidth = display.getWidth();
        int displayHeight = display.getHeight();

        boolean collided = false;
        long drawTime = 26;
        int bitOffset = spriteX & 7;

        this.setVF(false);
        for (int i = 0; i < N; i++) {
            int sliceY = spriteY + i;
            if (sliceY >= displayHeight) {
                break;
            }
            boolean col1 = false;
            boolean col2 = false;

            int slice = bus.readByte(currentIndexRegister + i);

            int workAreaAddressOffset = bus.getMemorySize() - 0x130 + i * 2;
            bus.writeByte(workAreaAddressOffset, slice >>> bitOffset);
            bus.writeByte(workAreaAddressOffset + 1, bitOffset != 0 ? slice << (8 - bitOffset) : 0);

            for (int j = 0, sliceMask = BASE_SLICE_MASK_8; j < 8; j++, sliceMask >>>= 1) {
                int sliceX = spriteX + j;
                if (sliceX >= displayWidth) {
                    break;
                }
                if ((slice & sliceMask) == 0) {
                    continue;
                }
                if (display.drawPixel(sliceX, sliceY)) {
                    if (j + bitOffset < 8) {
                        col1 = true;
                    } else {
                        col2 = true;
                    }
                    collided = true;
                }
            }
            drawTime += 34 + (col1 ? 4 : 0) + (spriteX < 56 ? 16 : 0) + (col2 ? 4 : 0);
        }
        this.emulator.addCycles(drawTime);
        this.setVF(collided);
    }

}
