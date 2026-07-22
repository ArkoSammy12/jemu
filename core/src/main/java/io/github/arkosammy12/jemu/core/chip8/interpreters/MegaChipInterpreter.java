package io.github.arkosammy12.jemu.core.chip8.interpreters;

import io.github.arkosammy12.jemu.core.chip8.MegaChipEmulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.chip8.display.MegaChipDisplay;

public class MegaChipInterpreter<E extends MegaChipEmulator> extends SuperChip11Interpreter<E> {

    private boolean megaMode;
    private int cachedFontSpriteIndex;

    public MegaChipInterpreter(E emulator) {
        super(emulator);
    }

    private void setMegaMode(boolean value) {
        this.megaMode = value;
    }

    public boolean isMegaModeEnabled() {
        return this.megaMode;
    }

    public boolean isPointingToFontSprite() {
        return getI() == this.cachedFontSpriteIndex;
    }

    @Override
    protected int execute(int firstNibble, int firstByte, int NN) {
        boolean megaModeInstruction = false;
        if (firstByte == 0x00) {
            megaModeInstruction = switch (NN) {
                case 0x10 -> { //  0010: megaoff
                    setMegaMode(false);
                    yield true;
                }
                case 0x11 -> { // 0011: megaon
                    setMegaMode(true);
                    yield true;
                }
                default -> false;
            };
        }
        if (megaModeInstruction) {
            return VALID_INSTRUCTION;
        }
        if (!this.isMegaModeEnabled()) {
            return super.execute(firstNibble, firstByte, NN);
        }
        return switch (firstNibble) {
            case 0x0 -> switch (firstByte) {
                case 0x00 -> switch (NN) {
                    case 0xFE -> VALID_INSTRUCTION; // 00FE: lores. Doesn't work when mega mode is on
                    case 0xFF -> VALID_INSTRUCTION; // 00FF: hires. Doesn't work when mega mode is on
                    default -> {
                        if (getY(firstByte, NN) == 0xB) { // 00BN: scroll_up N
                            this.emulator.getVideoGenerator().scrollUp(getN(firstByte, NN));
                            yield VALID_INSTRUCTION;
                        } else {
                            yield super.execute(firstNibble, firstByte, NN);
                        }
                    }
                };
                case 0x01 -> { // 01NN NNNN: ldhi NNNNNN
                    Chip8Bus bus = this.emulator.getBus();
                    int pc = getPC();
                    setI((NN << 16) | (bus.readByte(pc) << 8) | bus.readByte(pc + 1));
                    incrementPC();
                    yield VALID_INSTRUCTION;
                }
                case 0x02 -> { // 02NN: ldpal NN
                    this.emulator.getVideoGenerator().loadPalette(getI(), NN);
                    yield VALID_INSTRUCTION;
                }
                case 0x03 -> { // 03NN: sprw NN
                    this.emulator.getVideoGenerator().setSpriteWidth(NN);
                    yield VALID_INSTRUCTION;
                }
                case 0x04 -> { // 04NN: sprh NN
                    this.emulator.getVideoGenerator().setSpriteHeight(NN);
                    yield VALID_INSTRUCTION;
                }
                case 0x05 -> { // 05NN: alpha NN
                    this.emulator.getVideoGenerator().setAlpha(NN);
                    yield VALID_INSTRUCTION;
                }
                case 0x06 -> { // 060N: digisnd N
                    if (getY(firstByte, NN) == 0x0) {
                        Chip8Bus bus = this.emulator.getBus();
                        int I = getI();
                        this.emulator.getAudioGenerator().playTrack(
                                ((bus.readByte(I) & 0xFF) << 8) | bus.readByte(I + 1) & 0xFF,
                                ((bus.readByte(I + 2) & 0xFF) << 16) | ((bus.readByte(I + 3) & 0xFF) << 8) | (bus.readByte(I + 4) & 0xFF),
                                getN(firstByte, NN) == 0,
                                I + 6
                        );
                        yield VALID_INSTRUCTION;
                    } else {
                        yield super.execute(firstNibble, firstByte, NN);
                    }
                }
                case 0x07 -> {
                    if (NN == 0x00) { // 0700: stopsnd
                        this.emulator.getAudioGenerator().stopTrack();
                        yield VALID_INSTRUCTION;
                    } else {
                        yield super.execute(firstNibble, firstByte, NN);
                    }
                }
                case 0x08 -> switch (NN) { // 080N: bmode N
                    case 0x00 -> {
                        this.emulator.getVideoGenerator().setBlendMode(MegaChipDisplay.BlendMode.BLEND_NORMAL);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x01 -> {
                        this.emulator.getVideoGenerator().setBlendMode(MegaChipDisplay.BlendMode.BLEND_25);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x02 -> {
                        this.emulator.getVideoGenerator().setBlendMode(MegaChipDisplay.BlendMode.BLEND_50);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x03 -> {
                        this.emulator.getVideoGenerator().setBlendMode(MegaChipDisplay.BlendMode.BLEND_75);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x04 -> {
                        this.emulator.getVideoGenerator().setBlendMode(MegaChipDisplay.BlendMode.BLEND_ADD);
                        yield VALID_INSTRUCTION;
                    }
                    case 0x05 -> {
                        this.emulator.getVideoGenerator().setBlendMode(MegaChipDisplay.BlendMode.BLEND_MULTIPLY);
                        yield VALID_INSTRUCTION;
                    }
                    default -> super.execute(firstNibble, firstByte, NN);
                };
                case 0x09 -> { // 09NN: ccol NN
                    this.emulator.getVideoGenerator().setCollisionColor(NN);
                    yield VALID_INSTRUCTION;
                }
                default -> super.execute(firstNibble, firstByte, NN);
            };
            case 0x3, 0x4, 0x5, 0x9, 0xE -> longSkipIfNecessary(super.execute(firstNibble, firstByte, NN));
            case 0xF -> {
                int flags = super.execute(firstNibble, firstByte, NN);
                if ((flags & GET_KEY_EXECUTED) != 0) {
                    this.emulator.getVideoGenerator().flushBackBuffer();
                }
                if ((flags & FONT_SPRITE_POINTER) != 0) {
                    this.cachedFontSpriteIndex = getI();
                }
                yield flags;
            }
            default -> super.execute(firstNibble, firstByte, NN);
        };
    }

    private int longSkipIfNecessary(int flags) {
        if ((flags & VALID_INSTRUCTION) != 0 && (flags & SKIP_TAKEN) != 0 && this.skippedLongInstruction()) {
            incrementPC();
        }
        return flags;
    }

    private boolean skippedLongInstruction() {
        return this.emulator.getBus().readByte(getPC() - 2) == 0x01;
    }

}
