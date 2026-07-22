package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.Chip8Emulator;
import io.github.arkosammy12.jemu.core.chip8.Chip8Keypad;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.chip8.display.Chip8Display;
import io.github.arkosammy12.jemu.core.common.Processor;
import io.github.arkosammy12.jemu.core.exceptions.InvalidInstructionException;

import java.util.Random;

public class Chip8Interpreter<E extends Chip8Emulator> implements Processor {

    public static final int VALID_INSTRUCTION = 1;
    public static final int SKIP_TAKEN = 1 << 1;
    public static final int DRAW_EXECUTED = 1 << 2;
    public static final int LONG_INSTRUCTION = 1 << 3;
    public static final int GET_KEY_EXECUTED = 1 << 4;
    public static final int FONT_SPRITE_POINTER = 1 << 5;
    public static final int CLS_EXECUTED = 1 << 6;

    protected final E emulator;
    protected final Random random = new Random();
    private final int memoryBoundsMask;

    private final int[] registers = new int[16];
    protected final int[] stack = new int[16];
    protected int programCounter;
    protected int indexRegister;
    protected int stackPointer;
    private int delayTimer;
    private int soundTimer;

    private long instructionCounter;
    protected boolean shouldExit;

    public Chip8Interpreter(E emulator) {
        this.emulator = emulator;
        this.memoryBoundsMask = emulator.getBus().getMemoryBoundsMask();
        setPC(emulator.getBus().getProgramStart());
    }

    public long getInstructionCount() {
        return this.instructionCounter;
    }

    protected void setPC(int value) {
        this.programCounter = value & this.memoryBoundsMask;
    }

    protected int getPC() {
        return this.programCounter;
    }

    protected void incrementPC() {
        this.programCounter = (this.programCounter + 2) & this.memoryBoundsMask;
    }

    protected void decrementPC() {
        this.programCounter = (this.programCounter - 2) & this.memoryBoundsMask;
    }

    protected void setI(int value) {
        this.indexRegister = value & this.memoryBoundsMask;
    }

    protected int getI() {
        return this.indexRegister;
    }

    protected void push(int value) {
        this.stack[this.stackPointer] = value;
        this.stackPointer = (this.stackPointer + 1) & 0xF;
    }

    protected int pop() {
        this.stackPointer = (this.stackPointer - 1) & 0xF;
        return this.stack[this.stackPointer];
    }

    protected void setDT(int value) {
        this.delayTimer = value & 0xFF;
    }

    public int getDT() {
        return this.delayTimer;
    }

    protected void setST(int value) {
        this.soundTimer = value & 0xFF;
    }

    public int getST() {
        return this.soundTimer;
    }

    public void decrementTimers() {
        if (this.delayTimer > 0) {
            this.delayTimer -= 1;
        }
        if (this.soundTimer > 0) {
            this.soundTimer -= 1;
        }
    }

    protected void setV(int index, int value) {
        this.registers[index] = value & 0xFF;
    }

    protected void setVF(boolean value) {
        this.registers[0xF] = value ? 1 : 0;
    }

    protected int getV(int index) {
        return this.registers[index];
    }

    public final boolean shouldExit() {
        return this.shouldExit;
    }

    @Override
    public final int cycle() {
        Chip8Bus bus = this.emulator.getBus();
        int pc = getPC();
        incrementPC();
        this.instructionCounter++;

        int firstByte = bus.readByte(pc);
        int secondByte = bus.readByte(pc + 1);
        int flags = this.execute(firstByte >>> 4, firstByte, secondByte);

        if ((flags & VALID_INSTRUCTION) == 0) {
            throw new InvalidInstructionException((firstByte << 8) | secondByte, this.emulator);
        }
        return flags;
    }

    protected int execute(int firstNibble, int firstByte, int NN) {
        return switch (firstNibble) {
            case 0x0 -> {
                if (firstByte == 0x00) {
                    yield switch (NN) {
                        case 0xE0 -> { // 00E0: cls
                            this.emulator.getVideoGenerator().clear();
                            yield VALID_INSTRUCTION | LONG_INSTRUCTION | CLS_EXECUTED;
                        }
                        case 0xEE -> { // 00EE: return
                            setPC(pop());
                            yield VALID_INSTRUCTION;
                        }
                        default -> 0;
                    };
                } else {
                    yield 0;
                }
            }
            case 0x1 -> { // 1NNN: jump NNN
                setPC(getNNN(firstByte, NN));
                yield VALID_INSTRUCTION;
            }
            case 0x2 -> { // 2NNN: :call NNN
                push(getPC());
                setPC(getNNN(firstByte, NN));
                yield VALID_INSTRUCTION;
            }
            case 0x3 -> { // 3XNN: if vX != NN then
                int flags = VALID_INSTRUCTION;
                if (NN == getV(getX(firstByte, NN))) {
                    flags |= SKIP_TAKEN;
                    incrementPC();
                }
                yield flags;
            }
            case 0x4 -> { // 4XNN: if vX == NN then
                int flags = VALID_INSTRUCTION;
                if (NN != getV(getX(firstByte, NN))) {
                    flags |= SKIP_TAKEN;
                    incrementPC();
                }
                yield flags;
            }
            case 0x5 -> {
                if (getN(firstByte, NN) == 0x0) { // 5XY0: if vX != vY then
                    int flags = VALID_INSTRUCTION;
                    if (getV(getX(firstByte, NN)) == getV(getY(firstByte, NN))) {
                        flags |= SKIP_TAKEN;
                        incrementPC();
                    }
                    yield flags;
                } else {
                    yield 0;
                }
            }
            case 0x6 -> { // 6XNN: vX := NN
                setV(getX(firstByte, NN), NN);
                yield VALID_INSTRUCTION;
            }
            case 0x7 -> { // 7XNN: vX += NN
                int X = getX(firstByte, NN);
                setV(X, getV(X) + NN);
                yield VALID_INSTRUCTION;
            }
            case 0x8 -> switch (getN(firstByte, NN)) {
                case 0x0 -> { // 8XY0: vX := vY
                    setV(getX(firstByte, NN), getV(getY(firstByte, NN)));
                    yield VALID_INSTRUCTION;
                }
                case 0x1 -> { // 8XY1: vX |= vY
                    int X = getX(firstByte, NN);
                    setV(X, getV(X) | getV(getY(firstByte, NN)));
                    if (this.emulator.getSettings().doVFReset()) {
                        setVF(false);
                    }
                    yield VALID_INSTRUCTION;
                }
                case 0x2 -> { // 8XY2: vX &= vY
                    int X = getX(firstByte, NN);
                    setV(X, getV(X) & getV(getY(firstByte, NN)));
                    if (this.emulator.getSettings().doVFReset()) {
                        setVF(false);
                    }
                    yield VALID_INSTRUCTION;
                }
                case 0x3 -> { // 8XY3: vX ^= vY
                    int X = getX(firstByte, NN);
                    setV(X, getV(X) ^ getV(getY(firstByte, NN)));
                    if (this.emulator.getSettings().doVFReset()) {
                        setVF(false);
                    }
                    yield VALID_INSTRUCTION;
                }
                case 0x4 -> { // 8XY4: vX += vY
                    int X = getX(firstByte, NN);
                    int value = getV(X) + getV(getY(firstByte, NN));
                    this.setV(X, value);
                    this.setVF(value > 0xFF);
                    yield VALID_INSTRUCTION;
                }
                case 0x5 -> { // 8XY5: vX -= vY
                    int X = getX(firstByte, NN);
                    int vX = getV(X);
                    int vY = getV(getY(firstByte, NN));
                    setV(X, vX - vY);
                    setVF(vX >= vY);
                    yield VALID_INSTRUCTION;
                }
                case 0x6 -> { // 8XY6: vX >>= vY
                    int X = getX(firstByte, NN);
                    int operand = this.emulator.getSettings().doShiftVXInPlace() ? getV(X) : getV(getY(firstByte, NN));
                    setV(X, operand >>> 1);
                    setVF((operand & 1) != 0);
                    yield VALID_INSTRUCTION;
                }
                case 0x7 -> { // 8XY7: vX =- vY
                    int X = getX(firstByte, NN);
                    int vX = getV(X);
                    int vY = getV(getY(firstByte, NN));
                    setV(X, vY - vX);
                    setVF(vY >= vX);
                    yield VALID_INSTRUCTION;
                }
                case 0xE -> { // 8XYE: vX <<= vY
                    int X = getX(firstByte, NN);
                    int operand = this.emulator.getSettings().doShiftVXInPlace() ? getV(X) : getV(getY(firstByte, NN));
                    setV(X, operand << 1);
                    setVF((operand & 128) != 0);
                    yield VALID_INSTRUCTION;
                }
                default -> 0;
            };
            case 0x9 -> {
                if (getN(firstByte, NN) == 0x0) { // 9XY0: if vX == vY then
                    int flags = VALID_INSTRUCTION;
                    if (getV(getX(firstByte, NN)) != getV(getY(firstByte, NN))) {
                        flags |= SKIP_TAKEN;
                        incrementPC();
                    }
                    yield flags;
                } else {
                    yield 0;
                }
            }
            case 0xA -> { // ANNN: i := NNN
                setI(getNNN(firstByte, NN));
                yield VALID_INSTRUCTION;
            }
            case 0xB -> { // BNNN: jump0 NNN / BXNN: jump0 NNN + vX
                setPC(getNNN(firstByte, NN) + getV(this.emulator.getSettings().doJumpWithVX() ? getX(firstByte, NN) : 0x0));
                yield VALID_INSTRUCTION;
            }
            case 0xC -> { // CXNN: vX := random NN
                setV(getX(firstByte, NN), this.random.nextInt() & NN);
                yield VALID_INSTRUCTION;
            }
            case 0xD -> { // DXYN: sprite vX vX N
                Chip8Display<?> display = this.emulator.getVideoGenerator();
                int spriteX = getV(getX(firstByte, NN)) % display.getWidth();
                int spriteHeight = getN(firstByte, NN);
                setV(0xF, display.draw(spriteX, getV(getY(firstByte, NN)) % display.getHeight(), spriteHeight, getI()));
                yield VALID_INSTRUCTION | DRAW_EXECUTED | ((spriteHeight > 4 && (spriteHeight + (spriteX & 7) > 9)) ? LONG_INSTRUCTION : 0);
            }
            case 0xE -> switch (NN) {
                case 0x9E -> { // EX9E: if vX -key then
                    int flags = VALID_INSTRUCTION;
                    if (this.emulator.getSystemController().isKeyPressed(getV(getX(firstByte, NN)))) {
                        flags |= SKIP_TAKEN;
                        incrementPC();
                    }
                    yield flags;
                }
                case 0xA1 -> { // EXA1: if vX key then
                    int flags = VALID_INSTRUCTION;
                    if (!this.emulator.getSystemController().isKeyPressed(getV(getX(firstByte, NN)))) {
                        flags |= SKIP_TAKEN;
                        incrementPC();
                    }
                    yield flags;
                }
                default -> 0;
            };
            case 0xF -> switch (NN) {
                case 0x07 -> { // FX07: vX := delay
                    setV(getX(firstByte, NN), getDT());
                    yield VALID_INSTRUCTION;
                }
                case 0x0A -> { // FX0A: vX := key
                    Chip8Keypad keypad = this.emulator.getSystemController();
                    int firstPressedKey = keypad.getFirstPressedKeypadKey();
                    int waitingKey = keypad.getWaitingKeypadKey();
                    if (waitingKey >= 0) {
                        if (firstPressedKey < 0 || waitingKey != firstPressedKey) {
                            setV(getX(firstByte, NN), waitingKey);
                            keypad.resetWaitingKeypadKey();
                        } else {
                            decrementPC();
                        }
                    } else {
                        if (firstPressedKey >= 0) {
                            keypad.setWaitingKeypadKey(firstPressedKey);
                        }
                        decrementPC();
                    }
                    yield VALID_INSTRUCTION | GET_KEY_EXECUTED;
                }
                case 0x15 -> { // FX15: delay := vX
                    this.setDT(getV(getX(firstByte, NN)));
                    yield VALID_INSTRUCTION;
                }
                case 0x18 -> { // FX18: buzzer := vX
                    setST(getV(getX(firstByte, NN)));
                    yield VALID_INSTRUCTION;
                }
                case 0x1E -> { // FX1E: i += vX
                    setI(getI() + getV(getX(firstByte, NN)));
                    yield VALID_INSTRUCTION;
                }
                case 0x29 -> { // FX29: i := hex vX
                    setI(this.emulator.getHost().getSpriteFont().getSmallFontSpriteOffset(getV(getX(firstByte, NN)) & 0xF));
                    yield VALID_INSTRUCTION | FONT_SPRITE_POINTER;
                }
                case 0x33 -> { // FX33: bcd vX
                    Chip8Bus bus = this.emulator.getBus();
                    int I = getI();
                    int vX = getV(getX(firstByte, NN));
                    long hundreds = (vX * 0x51EB851FL) >>> 37;
                    long remainder = vX - hundreds * 100;
                    long tens = (remainder * 0xCCCDL) >>> 19;
                    long ones = remainder - tens * 10;
                    bus.writeByte(I, (int) hundreds);
                    bus.writeByte(I + 1, (int) tens);
                    bus.writeByte(I + 2, (int) ones);
                    yield VALID_INSTRUCTION;
                }
                case 0x55 -> { // FX55: save vX
                    Chip8Bus bus = this.emulator.getBus();
                    int I = getI();
                    int X = getX(firstByte, NN);
                    for (int i = 0; i <= X; i++) {
                        bus.writeByte(I + i, getV(i));
                    }
                    switch (this.emulator.getSettings().getMemoryIncrementQuirk()) {
                        case INCREMENT_BY_X -> setI(I + X);
                        case INCREMENT_BY_X_PLUS_1 -> setI(I + X + 1);
                    }
                    yield VALID_INSTRUCTION;
                }
                case 0x65 -> { // FX65: load vX
                    Chip8Bus bus = this.emulator.getBus();
                    int I = getI();
                    int X = getX(firstByte, NN);
                    for (int i = 0; i <= X; i++) {
                        setV(i, bus.readByte(I + i));
                    }
                    switch (this.emulator.getSettings().getMemoryIncrementQuirk()) {
                        case INCREMENT_BY_X -> setI(I + X);
                        case INCREMENT_BY_X_PLUS_1 -> setI(I + X + 1);
                    }
                    yield VALID_INSTRUCTION;
                }
                default -> 0;
            };
            default -> 0;
        };
    }

    protected static int getX(int firstByte, int NN) {
        return firstByte & 0xF;
    }

    protected static int getY(int firstByte, int NN) {
        return NN >>> 4;
    }

    protected static int getN(int firstByte, int NN) {
        return NN & 0xF;
    }

    protected static int getNNN(int firstByte, int NN) {
        return ((firstByte << 8) | NN) & 0xFFF;
    }

}
