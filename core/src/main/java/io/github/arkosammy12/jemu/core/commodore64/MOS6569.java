package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.hardware.NMOS6502;
import io.github.arkosammy12.jemu.core.util.ShiftRegister;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class MOS6569<E extends Commodore64Emulator> implements VideoGenerator, Bus {

    private static final int SPRITE_0_X = 0x00;
    private static final int SPRITE_0_Y = 0x01;
    private static final int SPRITE_1_X = 0x02;
    private static final int SPRITE_1_Y = 0x03;
    private static final int SPRITE_2_X = 0x04;
    private static final int SPRITE_2_Y = 0x05;
    private static final int SPRITE_3_X = 0x06;
    private static final int SPRITE_3_Y = 0x07;
    private static final int SPRITE_4_X = 0x08;
    private static final int SPRITE_4_Y = 0x09;
    private static final int SPRITE_5_X = 0x0A;
    private static final int SPRITE_5_Y = 0x0B;
    private static final int SPRITE_6_X = 0x0C;
    private static final int SPRITE_6_Y = 0x0D;
    private static final int SPRITE_7_X = 0x0E;
    private static final int SPRITE_7_Y = 0x0F;
    private static final int SPRITE_X_MSB = 0x10;
    private static final int CONTROL_1 = 0x11;
    private static final int RASTER = 0x12;
    private static final int LPX = 0x13;
    private static final int LPY = 0x14;
    private static final int SPRITE_ENABLE = 0x15;
    private static final int CONTROL_2 = 0x16;
    private static final int SPRITE_Y_EXPANSION = 0x17;
    private static final int MEMORY_CONTROL = 0x18;
    private static final int INTERRUPT_REQUEST = 0x19;
    private static final int INTERRUPT_MASK = 0x1A;
    private static final int SPRITE_DATA_PRIORITY = 0x1B;
    private static final int SPRITE_MULTICOLOR = 0x1C;
    private static final int SPRITE_X_EXPANSION = 0x1D;
    private static final int SPRITE_SPRITE_COLLISION = 0x1E;
    private static final int SPRITE_DATA_COLLISION = 0x1F;
    private static final int BORDER_COLOR = 0x20;
    private static final int BACKGROUND_COLOR_0 = 0x21;
    private static final int BACKGROUND_COLOR_1 = 0x22;
    private static final int BACKGROUND_COLOR_2 = 0x23;
    private static final int BACKGROUND_COLOR_3 = 0x24;
    private static final int SPRITE_MULTICOLOR_0 = 0x25;
    private static final int SPRITE_MULTICOLOR_1 = 0x26;
    private static final int SPRITE_0_COLOR = 0x27;
    private static final int SPRITE_1_COLOR = 0x28;
    private static final int SPRITE_2_COLOR = 0x29;
    private static final int SPRITE_3_COLOR = 0x2A;
    private static final int SPRITE_4_COLOR = 0x2B;
    private static final int SPRITE_5_COLOR = 0x2C;
    private static final int SPRITE_6_COLOR = 0x2D;
    private static final int SPRITE_7_COLOR = 0x2E;

    private static final int SCANLINES_PER_FRAME = 312;
    private static final int VISIBLE_SCANLINES = 284;
    private static final int FIRST_VBLANK_SCANLINE = 300;
    private static final int LAST_VBLANK_SCANLINE = 15;
    private static final int FIRST_VISIBLE_SCANLINE = LAST_VBLANK_SCANLINE + 1;

    private static final int CYCLES_PER_SCANLINE = 63;
    private static final int PIXELS_PER_SCANLINE = CYCLES_PER_SCANLINE * 8;
    private static final int VISIBLE_PIXELS_PER_SCANLINE = 403;
    private static final int FIRST_VISIBLE_DOT_NUMBER = 76;
    private static final int LAST_VISIBLE_DOT_NUMBER = FIRST_VISIBLE_DOT_NUMBER + VISIBLE_PIXELS_PER_SCANLINE - 1;

    private final E emulator;

    private final int[] video;
    private final Sprite[] sprites;
    private final int[] videoMatrixBuffer = new int[40]; // 12 bit elements

    private int rasterCompare;
    private GraphicsMode graphicsMode = GraphicsMode.TEXT_STANDARD;
    private boolean displayEnable;
    private RowSelect rowSelect = RowSelect.NORMAL;
    private int yScroll;

    private ColumnSelect columnSelect = ColumnSelect.NORMAL;
    private int xScroll;

    private int videoMemoryBase;
    private int characterBaseAddress;

    private boolean irqLightPen;
    private boolean irqSpriteSprite;
    private boolean irqSpriteBackground;
    private boolean irqRaster;

    private boolean irqLightPenEnable;
    private boolean irqSpriteSpriteEnable;
    private boolean irqSpriteBackgroundEnable;
    private boolean irqRasterEnable;

    private int borderColor;
    private int backgroundColor0;
    private int backgroundColor1;
    private int backgroundColor2;
    private int backgroundColor3;
    private int spriteMulticolor0;
    private int spriteMulticolor1;

    private int dotNumber;
    private int cycleNumber = 1; // in the range [1, 63]
    private int scanlineNumber;
    private int raster = 0;
    private boolean displayEnabledInLine30;
    private boolean overflowRasterFlag;

    private int videoCounter; // VC, 10 bits
    private int videoCounterBase; // VCBASE, 10 data register
    private int rowCounter; // RC, 3 bits
    private int videoMatrixLine; // VMLI, 6 bits

    private boolean mainBorderFlipFlop;
    private boolean verticalBorderFlipFlop;

    private TextBitmapLogicMode textBitmapLogicMode = TextBitmapLogicMode.IDLE;

    private boolean graphicsBAOutput;
    private boolean cAccessing;
    private int cAccessingCountdown;

    private Sprite0BAOutputFlag sprite0BAOutputFlag = Sprite0BAOutputFlag.NONE;
    private boolean sprite1BAOutputFlag = false;
    private boolean sprite2BAOutputFlag = false;
    private boolean sprite3BAOutputFlag = false;
    private boolean sprite4BAOutputFlag = false;
    private boolean sprite5BAOutputFlag = false;
    private boolean sprite6BAOutputFlag = false;
    private boolean sprite7BAOutputFlag = false;
    private boolean spriteAECOutput;

    private int cDataPendingLatch;
    private int gDataPendingLatch;

    private int cDataCurrentLatch;
    private final ShiftRegister graphicsDataSequencer = new ShiftRegister(8, 2);

    private int dRAMRefreshCounter = 0xFF;

    @SuppressWarnings("unchecked")
    public MOS6569(E emulator) {
        this.emulator = emulator;
        this.sprites = new MOS6569.Sprite[8];
        for (int i = 0; i < 8; i++) {
            this.sprites[i] = new Sprite(i);
        }
        this.video = new int[this.getImageWidth() * this.getImageHeight()];
    }

    @Override
    public int getImageWidth() {
        return VISIBLE_PIXELS_PER_SCANLINE;
    }

    @Override
    public int getImageHeight() {
        return VISIBLE_SCANLINES;
    }

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return this.emulator.getHost().getRGB8ForPaletteIndex(frameBufferValue);
    }

    @Override
    public int readByte(int address) {
        address &= 0x3F;
        return switch (address) {
            case SPRITE_0_X -> this.sprites[0].getX();
            case SPRITE_0_Y -> this.sprites[0].getY();
            case SPRITE_1_X -> this.sprites[1].getX();
            case SPRITE_1_Y -> this.sprites[1].getY();
            case SPRITE_2_X -> this.sprites[2].getX();
            case SPRITE_2_Y -> this.sprites[2].getY();
            case SPRITE_3_X -> this.sprites[3].getX();
            case SPRITE_3_Y -> this.sprites[3].getY();
            case SPRITE_4_X -> this.sprites[4].getX();
            case SPRITE_4_Y -> this.sprites[4].getY();
            case SPRITE_5_X -> this.sprites[5].getX();
            case SPRITE_5_Y -> this.sprites[5].getY();
            case SPRITE_6_X -> this.sprites[6].getX();
            case SPRITE_6_Y -> this.sprites[6].getY();
            case SPRITE_7_X -> this.sprites[7].getX();
            case SPRITE_7_Y -> this.sprites[7].getY();
            case SPRITE_X_MSB -> this.getValueForSprites(Sprite::getXMSB);
            case CONTROL_1 -> {
                int ret = ((this.raster & (1 << 8)) >>> 1) | this.yScroll;
                ret |= this.graphicsMode.getECM() ? 1 << 6 : 0;
                ret |= this.graphicsMode.getBMM() ? 1 << 5 : 0;
                ret |= this.displayEnable ? 1 << 4 : 0;
                ret |= this.rowSelect == RowSelect.REDUCED_BORDER ? 1 << 3 : 0;
                yield ret;
            }
            case RASTER -> this.raster & 0xFF;
            case LPX -> 0x00; // Lightpen X coordinate
            case LPY -> 0x00; // Lightpen Y coordinate
            case SPRITE_ENABLE -> this.getValueForSprites(Sprite::isEnabled);
            case CONTROL_2 -> {
                int ret = 0b11000000 | this.xScroll;
                ret |= this.graphicsMode.getMCM() ? 1 << 4 : 0;
                ret |= this.columnSelect == ColumnSelect.REDUCED_BORDER ? 1 << 3 : 0;
                yield ret;
            }
            case SPRITE_Y_EXPANSION -> this.getValueForSprites(Sprite::getYExpansion);
            case MEMORY_CONTROL -> 1 | (this.characterBaseAddress << 1) | (this.videoMemoryBase << 4);
            case INTERRUPT_REQUEST -> {
                int ret = 0b01110000;
                ret |= this.getIRQ() ? 1 << 7 : 0;
                ret |= this.irqLightPen ? 1 << 3 : 0;
                ret |= this.irqSpriteSprite ? 1 << 2 : 0;
                ret |= this.irqSpriteBackground ? 1 << 1 : 0;
                ret |= this.irqRaster ? 1 : 0;
                yield ret;
            }
            case INTERRUPT_MASK -> {
                int ret = 0b11110000;
                ret |= this.irqLightPenEnable ? 1 << 3 : 0;
                ret |= this.irqSpriteSpriteEnable ? 1 << 2 : 0;
                ret |= this.irqSpriteBackgroundEnable ? 1 << 1 : 0;
                ret |= this.irqRasterEnable ? 1 : 0;
                yield ret;
            }
            case SPRITE_DATA_PRIORITY -> this.getValueForSprites(Sprite::getDataPriority);
            case SPRITE_MULTICOLOR -> this.getValueForSprites(Sprite::isMulticolor);
            case SPRITE_X_EXPANSION -> this.getValueForSprites(Sprite::getXExpansion);
            case SPRITE_SPRITE_COLLISION -> {
                int ret = this.getValueForSprites(Sprite::getSpriteCollision);
                this.setValueForSprites(0x00, Sprite::setSpriteCollision);
                yield ret;
            }
            case SPRITE_DATA_COLLISION -> {
                int ret = this.getValueForSprites(Sprite::getBackgroundCollision);
                this.setValueForSprites(0x00, Sprite::setGraphicsDataCollision);
                yield ret;
            }
            case BORDER_COLOR -> 0xF0 | this.borderColor;
            case BACKGROUND_COLOR_0 -> 0xF0 | this.backgroundColor0;
            case BACKGROUND_COLOR_1 -> 0xF0 | this.backgroundColor1;
            case BACKGROUND_COLOR_2 -> 0xF0 | this.backgroundColor2;
            case BACKGROUND_COLOR_3 -> 0xF0 | this.backgroundColor3;
            case SPRITE_MULTICOLOR_0 -> 0xF0 | this.spriteMulticolor0;
            case SPRITE_MULTICOLOR_1 -> 0xF0 | this.spriteMulticolor1;
            case SPRITE_0_COLOR -> 0xF0 | this.sprites[0].getColor();
            case SPRITE_1_COLOR -> 0xF0 | this.sprites[1].getColor();
            case SPRITE_2_COLOR -> 0xF0 | this.sprites[2].getColor();
            case SPRITE_3_COLOR -> 0xF0 | this.sprites[3].getColor();
            case SPRITE_4_COLOR -> 0xF0 | this.sprites[4].getColor();
            case SPRITE_5_COLOR -> 0xF0 | this.sprites[5].getColor();
            case SPRITE_6_COLOR -> 0xF0 | this.sprites[6].getColor();
            case SPRITE_7_COLOR -> 0xF0 | this.sprites[7].getColor();
            default -> 0xFF;
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x3F;
        switch (address) {
            case SPRITE_0_X -> this.sprites[0].setX(value);
            case SPRITE_0_Y -> this.sprites[0].setY(value);
            case SPRITE_1_X -> this.sprites[1].setX(value);
            case SPRITE_1_Y -> this.sprites[1].setY(value);
            case SPRITE_2_X -> this.sprites[2].setX(value);
            case SPRITE_2_Y -> this.sprites[2].setY(value);
            case SPRITE_3_X -> this.sprites[3].setX(value);
            case SPRITE_3_Y -> this.sprites[3].setY(value);
            case SPRITE_4_X -> this.sprites[4].setX(value);
            case SPRITE_4_Y -> this.sprites[4].setY(value);
            case SPRITE_5_X -> this.sprites[5].setX(value);
            case SPRITE_5_Y -> this.sprites[5].setY(value);
            case SPRITE_6_X -> this.sprites[6].setX(value);
            case SPRITE_6_Y -> this.sprites[6].setY(value);
            case SPRITE_7_X -> this.sprites[7].setX(value);
            case SPRITE_7_Y -> this.sprites[7].setY(value);
            case SPRITE_X_MSB -> this.setValueForSprites(value, Sprite::setXMSB);
            case CONTROL_1 -> {
                this.rasterCompare = (this.rasterCompare & 0xFF) | ((value & (1 << 7)) << 1);
                this.graphicsMode = this.graphicsMode.fromUpperBits((value >>> 5) & 0b11);
                this.displayEnable = (value & (1 << 4)) != 0;
                this.rowSelect = (value & (1 << 3)) != 0 ? RowSelect.REDUCED_BORDER : RowSelect.NORMAL;
                this.yScroll = value & 0b111;
                this.checkRasterIRQ();
            }
            case RASTER -> {
                this.rasterCompare = (this.rasterCompare & (1 << 8)) | (value & 0xFF);
                this.checkRasterIRQ();
            }
            case LPX -> {} // Lightpen X coordinate
            case LPY -> {} // Lightpen Y coordinate
            case SPRITE_ENABLE -> this.setValueForSprites(value, Sprite::setEnabled);
            case CONTROL_2 -> {
                this.graphicsMode = this.graphicsMode.fromMulticolorBit((value & (1 << 4)) != 0);
                this.columnSelect = (value & (1 << 3)) != 0 ? ColumnSelect.REDUCED_BORDER : ColumnSelect.NORMAL;
                this.xScroll = value & 0b111;
            }
            case SPRITE_Y_EXPANSION -> this.setValueForSprites(value, Sprite::setYExpansion);
            case MEMORY_CONTROL -> {
                this.characterBaseAddress = (value >>> 1) & 0b111;
                this.videoMemoryBase = (value >>> 4) & 0b1111;
            }
            case INTERRUPT_REQUEST -> {
                if ((value & (1 << 3)) != 0) {
                    this.irqLightPen = false;
                }
                if ((value & (1 << 2)) != 0) {
                    this.irqSpriteSprite = false;
                }
                if ((value & (1 << 1)) != 0) {
                    this.irqSpriteBackground = false;
                }
                if ((value & 1) != 0) {
                    this.irqRaster = false;
                }
            }
            case INTERRUPT_MASK -> {
                this.irqLightPenEnable = (value & (1 << 3)) != 0;
                this.irqSpriteSpriteEnable = (value & (1 << 2)) != 0;
                this.irqSpriteBackgroundEnable = (value & (1 << 1)) != 0;
                this.irqRasterEnable = (value & 1) != 0;
            }
            case SPRITE_DATA_PRIORITY -> this.setValueForSprites(value, Sprite::setDataPriority);
            case SPRITE_MULTICOLOR -> this.setValueForSprites(value, Sprite::setMulticolor);
            case SPRITE_X_EXPANSION -> this.setValueForSprites(value, Sprite::setXExpansion);
            case SPRITE_SPRITE_COLLISION -> {} // Sprite-sprite collision. Nothing on writes
            case SPRITE_DATA_COLLISION -> {} // Sprite-data collision. Nothing on writes
            case BORDER_COLOR -> this.borderColor = value & 0xF;
            case BACKGROUND_COLOR_0 -> this.backgroundColor0 = value & 0xF;
            case BACKGROUND_COLOR_1 -> this.backgroundColor1 = value & 0xF;
            case BACKGROUND_COLOR_2 -> this.backgroundColor2 = value & 0xF;
            case BACKGROUND_COLOR_3 -> this.backgroundColor3 = value & 0xF;
            case SPRITE_MULTICOLOR_0 -> this.spriteMulticolor0 = value & 0xF;
            case SPRITE_MULTICOLOR_1 -> this.spriteMulticolor1 = value & 0xF;
            case SPRITE_0_COLOR -> this.sprites[0].setColor(value);
            case SPRITE_1_COLOR -> this.sprites[1].setColor(value);
            case SPRITE_2_COLOR -> this.sprites[2].setColor(value);
            case SPRITE_3_COLOR -> this.sprites[3].setColor(value);
            case SPRITE_4_COLOR -> this.sprites[4].setColor(value);
            case SPRITE_5_COLOR -> this.sprites[5].setColor(value);
            case SPRITE_6_COLOR -> this.sprites[6].setColor(value);
            case SPRITE_7_COLOR -> this.sprites[7].setColor(value);
        }
    }

    private void setValueForSprites(int value, BiConsumer<Sprite, Boolean> setter) {
        for (int i = 0; i < this.sprites.length; i++) {
            setter.accept(this.sprites[i], (value & (1 << i)) != 0);
        }
    }

    private int getValueForSprites(Function<Sprite, Boolean> getter) {
        int ret = 0;
        for (int i = 0; i < this.sprites.length; i++) {
            ret |= getter.apply(this.sprites[i]) ? 1 << i : 0;
        }
        return ret;
    }

    public boolean getIRQ() {
        return (this.irqLightPenEnable && this.irqLightPen)
                || (this.irqSpriteSpriteEnable && this.irqSpriteSprite)
                || (this.irqSpriteBackgroundEnable && this.irqSpriteBackground)
                || (this.irqRasterEnable && this.irqRaster);
    }

    public boolean getBA() {
        return this.graphicsBAOutput
                || this.sprite0BAOutputFlag != Sprite0BAOutputFlag.NONE
                || this.sprite1BAOutputFlag
                || this.sprite2BAOutputFlag
                || this.sprite3BAOutputFlag
                || this.sprite4BAOutputFlag
                || this.sprite5BAOutputFlag
                || this.sprite6BAOutputFlag
                || this.sprite7BAOutputFlag;
    }

    public boolean getAEC() {
        return this.cAccessing || this.spriteAECOutput;
    }

    private boolean isVBlank() {
        return (this.scanlineNumber >= FIRST_VBLANK_SCANLINE) || (this.scanlineNumber <= LAST_VBLANK_SCANLINE);
    }

    private int getXCoordinate() {
        return (this.dotNumber + 404) % PIXELS_PER_SCANLINE;
    }

    public void cycleHalf(NMOS6502.Phase cpuPhase) {
        switch (cpuPhase) {
            case PHI_1 -> {
                this.clockPixel();
                this.clockPixel();
                this.clockPixel();
                this.clockPixel();

                if (this.cAccessingCountdown > 0) {
                    this.cAccessingCountdown--;
                    if (this.cAccessingCountdown <= 0) {
                        this.cAccessing = true;
                    }
                }

                if (this.raster == 0x30 && this.displayEnable) {
                    this.displayEnabledInLine30 = true;
                }
                boolean badLineCondition = this.raster >= 0x30 && this.raster <= 0xF7 && (this.raster & 0b111) == this.yScroll && this.displayEnabledInLine30;

                if (badLineCondition) {
                    this.textBitmapLogicMode = TextBitmapLogicMode.DISPLAY;
                }

                switch (this.cycleNumber) {
                    case 1 -> {
                        if (!this.overflowRasterFlag) {
                            this.checkRasterIRQ();
                        }
                        this.sprites[3].performPAccess();
                        this.sprite2BAOutputFlag = false;
                        this.spriteAECOutput = this.sprite3BAOutputFlag;
                    }
                    case 2 -> {
                        if (this.overflowRasterFlag) {
                            this.overflowRasterFlag = false;
                            this.checkRasterIRQ();
                        }
                        this.sprites[3].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                        if (this.sprites[5].isDMAOn()) {
                            this.sprite5BAOutputFlag = true;
                        }
                    }
                    case 3 -> {
                        this.sprites[4].performPAccess();
                        this.sprite3BAOutputFlag = false;
                        this.spriteAECOutput = this.sprite4BAOutputFlag;
                    }
                    case 4 -> {
                        this.sprites[4].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                        if (this.sprites[6].isDMAOn()) {
                            this.sprite6BAOutputFlag = true;
                        }
                    }
                    case 5 -> {
                        this.sprites[5].performPAccess();
                        this.sprite4BAOutputFlag = false;
                        this.spriteAECOutput = this.sprite5BAOutputFlag;
                    }
                    case 6 -> {
                        this.sprites[5].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                        if (this.sprites[7].isDMAOn()) {
                            this.sprite7BAOutputFlag = true;
                        }
                    }
                    case 7 -> {
                        this.sprites[6].performPAccess();
                        this.sprite5BAOutputFlag = false;
                        this.spriteAECOutput = this.sprite6BAOutputFlag;
                    }
                    case 8 -> this.sprites[6].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                    case 9 -> {
                        this.sprites[7].performPAccess();
                        this.sprite6BAOutputFlag = false;
                        this.spriteAECOutput = this.sprite7BAOutputFlag;
                    }
                    case 10 -> this.sprites[7].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                    case 56 -> {
                        this.performIdleAccess();
                        this.runForSprites(Sprite::checkStartDMA);
                        if (this.sprite0BAOutputFlag == Sprite0BAOutputFlag.NONE && this.sprites[0].isDMAOn()) {
                            this.sprite0BAOutputFlag = Sprite0BAOutputFlag.LATE;
                        }
                    }
                    case 57 -> {
                        this.performIdleAccess();
                        if (this.sprites[1].isDMAOn()) {
                            this.sprite1BAOutputFlag = true;
                        }
                    }
                    case 58 -> {
                        if (this.rowCounter == 7) {
                            this.videoCounterBase = this.videoCounter;
                            this.textBitmapLogicMode = TextBitmapLogicMode.IDLE;
                        }
                        if (this.textBitmapLogicMode == TextBitmapLogicMode.DISPLAY) {
                            this.rowCounter = (this.rowCounter + 1) & 0b111;
                        }
                        this.runForSprites(Sprite::loadDataCounter);
                        this.sprites[0].performPAccess();

                        if (this.sprite0BAOutputFlag == Sprite0BAOutputFlag.NORMAL) {
                            this.spriteAECOutput = true;
                        }
                    }
                    case 59 -> {
                        this.sprites[0].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                        if (this.sprite0BAOutputFlag == Sprite0BAOutputFlag.LATE) {
                            this.spriteAECOutput = true;
                        }
                        if (this.sprites[2].isDMAOn()) {
                            this.sprite2BAOutputFlag = true;
                        }
                    }
                    case 60 -> {
                        this.sprites[1].performPAccess();
                        this.sprite0BAOutputFlag = Sprite0BAOutputFlag.NONE;
                        this.spriteAECOutput = this.sprite1BAOutputFlag;
                    }
                    case 61 -> {
                        this.sprites[1].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                        if (this.sprites[3].isDMAOn()) {
                            this.sprite3BAOutputFlag = true;
                        }
                    }
                    case 62 -> {
                        this.sprites[2].performPAccess();
                        this.sprite1BAOutputFlag = false;
                        this.spriteAECOutput = this.sprite2BAOutputFlag;
                    }
                    case 63 -> {
                        if (this.raster == this.rowSelect.getBottonComparison()) {
                            this.verticalBorderFlipFlop = true;
                        } else if (this.raster == this.rowSelect.getTopComparison() && this.displayEnable) {
                            this.verticalBorderFlipFlop = false;
                        }
                        this.sprites[2].tryPerformSAccess(Sprite.SAccessStep.SECOND);
                        if (this.sprites[4].isDMAOn()) {
                            this.sprite4BAOutputFlag = true;
                        }
                    }
                    default -> {
                        if (this.cycleNumber >= 11 && this.cycleNumber <= 55) {
                            if (this.cycleNumber == 11) {
                                this.sprite0BAOutputFlag = Sprite0BAOutputFlag.NONE;
                                this.sprite1BAOutputFlag = false;
                                this.sprite2BAOutputFlag = false;
                                this.sprite3BAOutputFlag = false;
                                this.sprite4BAOutputFlag = false;
                                this.sprite5BAOutputFlag = false;
                                this.sprite6BAOutputFlag = false;
                                this.sprite7BAOutputFlag = false;
                                this.spriteAECOutput = false;
                            }

                            if (this.cycleNumber <= 15) {
                                this.performRefreshAccess();
                                if (this.cycleNumber == 14) {
                                    this.videoCounter = this.videoCounterBase;
                                    this.videoMatrixLine = 0;
                                    if (badLineCondition) {
                                        this.rowCounter = 0;
                                    }
                                }
                            }
                            if (this.cycleNumber >= 12) {
                                if (this.cycleNumber <= 54) {
                                    if (badLineCondition) {
                                        if (!this.graphicsBAOutput) {
                                            this.cAccessingCountdown = 3;
                                        }
                                        this.graphicsBAOutput = true;
                                    }
                                } else if (this.cycleNumber == 55) {
                                    this.cAccessingCountdown = 0;
                                    this.cAccessing = false;
                                    this.graphicsBAOutput = false;
                                    this.runForSprites(Sprite::checkStartDMA);
                                    if (this.sprites[0].isDMAOn()) {
                                        this.sprite0BAOutputFlag = Sprite0BAOutputFlag.NORMAL;
                                    }
                                }
                                if (this.cycleNumber >= 16) {
                                    if (this.cycleNumber == 16) {
                                        this.runForSprites(Sprite::checkAdvanceLineSet);
                                    }
                                    this.performGAccess();
                                }
                            }
                        }
                    }
                }
            }
            case PHI_2 -> {
                this.clockPixel();
                this.clockPixel();
                this.clockPixel();
                this.clockPixel();

                switch (this.cycleNumber) {
                    case 1 -> {
                        this.sprites[3].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                        if (this.overflowRasterFlag) {
                            this.incrementRaster();
                        }
                    }
                    case 2 -> this.sprites[3].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    case 3 -> this.sprites[4].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                    case 4 -> this.sprites[4].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    case 5 -> this.sprites[5].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                    case 6 -> this.sprites[5].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    case 7 -> this.sprites[6].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                    case 8 -> this.sprites[6].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    case 9 -> this.sprites[7].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                    case 10 -> this.sprites[7].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    case 56 -> this.runForSprites(Sprite::checkToggleAdvanceLine);
                    case 58 -> this.sprites[0].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                    case 59 -> this.sprites[0].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    case 60 -> this.sprites[1].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                    case 61 -> this.sprites[1].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    case 62 -> this.sprites[2].tryPerformSAccess(Sprite.SAccessStep.FIRST);
                    case 63 -> this.sprites[2].tryPerformSAccess(Sprite.SAccessStep.THIRD);
                    default -> {
                        if (this.cycleNumber >= 15 && this.cycleNumber <= 54) {
                            if (this.cAccessing) {
                                this.performCAccess();
                            }
                        }
                    }
                }

                this.cycleNumber++;
                if (this.cycleNumber >= CYCLES_PER_SCANLINE + 1) {
                    this.cycleNumber = 1;
                }
            }
        }

    }


    private void clockPixel() {
        int xCoordinate = this.getXCoordinate();
        if (xCoordinate == this.columnSelect.getRightComparison()) {
            this.mainBorderFlipFlop = true;
        } else if (xCoordinate == this.columnSelect.getLeftComparison()) {
            if (this.raster == this.rowSelect.getBottonComparison()) {
                this.verticalBorderFlipFlop = true;
            } else if (this.raster == this.rowSelect.getTopComparison() && this.displayEnable) {
                this.verticalBorderFlipFlop = false;
            }
            if (!this.verticalBorderFlipFlop) {
                this.mainBorderFlipFlop = false;
            }
        }

        int cyclePixelPhase = (this.dotNumber + 4) & 0b111;
        if (cyclePixelPhase == this.xScroll) {
            this.cDataCurrentLatch = this.cDataPendingLatch;
            switch (this.textBitmapLogicMode) {
                case DISPLAY -> {
                    switch (this.graphicsMode) {
                        case TEXT_STANDARD, BITMAP_STANDARD, EXTENDED_COLOR, BITMAP_INVALID_1 -> this.reloadGraphicsSequencerAsNormal();
                        case TEXT_MULTICOLOR, TEXT_INVALID -> {
                            if ((this.cDataCurrentLatch & (1 << 11)) != 0) {
                                this.reloadGraphicsSequencerAsMulticolor();
                            } else {
                                this.reloadGraphicsSequencerAsNormal();
                            }
                        }
                        case BITMAP_MULTICOLOR, BITMAP_INVALID_2 -> this.reloadGraphicsSequencerAsMulticolor();
                    }
                }
                case IDLE -> {
                    switch (this.graphicsMode) {
                        case TEXT_STANDARD, TEXT_MULTICOLOR, EXTENDED_COLOR, BITMAP_STANDARD, TEXT_INVALID, BITMAP_INVALID_1 -> this.reloadGraphicsSequencerAsNormal();
                        case BITMAP_MULTICOLOR, BITMAP_INVALID_2 -> this.reloadGraphicsSequencerAsMulticolor();
                    }
                }
            }
        }

        int graphicsData = this.graphicsDataSequencer.shiftHead(0b00);
        int paletteIndex;
        if (this.verticalBorderFlipFlop) {
            paletteIndex = 0b00;
        } else {
            paletteIndex = switch (this.graphicsMode) {
                case TEXT_STANDARD -> (graphicsData & 1) != 0 ? (this.cDataCurrentLatch >>> 8) & 0b1111 : this.backgroundColor0;
                case TEXT_MULTICOLOR -> {
                    if ((this.cDataCurrentLatch & (1 << 11)) != 0) {
                        yield switch (graphicsData & 0b11) {
                            case 0b00 -> this.backgroundColor0;
                            case 0b01 -> this.backgroundColor1;
                            case 0b10 -> this.backgroundColor2;
                            default -> (this.cDataCurrentLatch >>> 8) & 0b111;
                        };
                    } else {
                        yield (graphicsData & 1) != 0 ? (this.cDataCurrentLatch >>> 8) & 0b111 : this.backgroundColor0;
                    }
                }
                case BITMAP_STANDARD -> (graphicsData & 1) != 0 ? (this.cDataCurrentLatch >>> 4) & 0b1111 : this.cDataCurrentLatch & 0b1111;
                case BITMAP_MULTICOLOR -> switch (graphicsData & 0b11) {
                    case 0b00 -> this.backgroundColor0;
                    case 0b01 -> (this.cDataCurrentLatch >>> 4) & 0b1111;
                    case 0b10 -> this.cDataCurrentLatch & 0b1111;
                    default -> (this.cDataCurrentLatch >>> 8) & 0b1111;
                };
                case EXTENDED_COLOR -> {
                    if ((graphicsData & 1) != 0) {
                        yield (this.cDataCurrentLatch >>> 8) & 0b1111;
                    } else {
                        yield switch ((this.cDataCurrentLatch >>> 6) & 0b11) {
                            case 0b00 -> this.backgroundColor0;
                            case 0b01 -> this.backgroundColor1;
                            case 0b10 -> this.backgroundColor2;
                            default -> this.backgroundColor3;
                        };
                    }
                }
                case TEXT_INVALID, BITMAP_INVALID_1, BITMAP_INVALID_2 -> 0b0000;
            };
        }

        boolean isForegroundBitmapPixel;
        if (this.graphicsMode.getMCM()) {
            isForegroundBitmapPixel = switch (graphicsData & 0b11) {
                case 0b10, 0b11 -> true;
                default -> false;
            };
        } else {
            isForegroundBitmapPixel = (graphicsData & 1) != 0;
        }

        int highestPrioritySpriteNumber = 7;
        int firstOpaqueSpriteNumber = -1;
        boolean opaqueSpritePixelFound = false;
        boolean spriteCollision = false;
        for (int i = 7; i >= 0; i--) {
            Sprite sprite = this.sprites[i];
            sprite.shiftSequencer();
            if (sprite.isOpaque()) {
                if (opaqueSpritePixelFound) {
                    sprite.setSpriteCollision(true);
                    spriteCollision = true;
                }
                if (isForegroundBitmapPixel) {
                    sprite.setGraphicsDataCollision(true);
                    this.irqSpriteBackground = true;
                }
                if (firstOpaqueSpriteNumber < 0) {
                    firstOpaqueSpriteNumber = i;
                }
                highestPrioritySpriteNumber = i;
                opaqueSpritePixelFound = true;
            }
        }

        if (opaqueSpritePixelFound) {
            if (spriteCollision) {
                this.sprites[firstOpaqueSpriteNumber].setSpriteCollision(true);
                this.irqSpriteSprite = true;
            }
            if (this.sprites[highestPrioritySpriteNumber].getDataPriority()) {
                if (isForegroundBitmapPixel) {
                    paletteIndex = this.sprites[highestPrioritySpriteNumber].getPaletteIndex();
                }
            } else {
                paletteIndex = this.sprites[highestPrioritySpriteNumber].getPaletteIndex();
            }
        }

        if (this.mainBorderFlipFlop) {
            paletteIndex = this.borderColor;
        }

        if (this.dotNumber >= FIRST_VISIBLE_DOT_NUMBER && this.dotNumber <= LAST_VISIBLE_DOT_NUMBER && !this.isVBlank()) {
            this.video[((this.scanlineNumber - FIRST_VISIBLE_SCANLINE) * VISIBLE_PIXELS_PER_SCANLINE) + (this.dotNumber - FIRST_VISIBLE_DOT_NUMBER)] = paletteIndex;
        }

        this.dotNumber++;
        if (this.dotNumber >= PIXELS_PER_SCANLINE) {
            this.dotNumber = 0;
            this.scanlineNumber++;
            if (this.scanlineNumber >= SCANLINES_PER_FRAME) {
                this.overflowRasterFlag = true;
                this.scanlineNumber = 0;
                this.emulator.onVBlank();
                this.emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.video));
            } else {
                this.incrementRaster();
            }
        }
    }

    private void runForSprites(Consumer<Sprite> consumer) {
        for (Sprite sprite : this.sprites) {
            consumer.accept(sprite);
        }
    }

    private void incrementRaster() {
        this.raster = (this.raster + 1) % SCANLINES_PER_FRAME;
        switch (this.raster) {
            case 0 -> {
                this.videoCounterBase = 0;
                this.dRAMRefreshCounter = 0xFF;
            }
            case 0x30 -> this.displayEnabledInLine30 = false;
        }
    }

    private void checkRasterIRQ() {
        if (this.raster == this.rasterCompare) {
            this.irqRaster = true;
        }
    }

    private void performIdleAccess() {
        this.emulator.getBus().readVIC2(0x3FFF);
    }

    private void performRefreshAccess() {
        this.emulator.getBus().readVIC2(0x3F00 | this.dRAMRefreshCounter);
        this.dRAMRefreshCounter = (this.dRAMRefreshCounter - 1) & 0xFF;
    }

    private void performGAccess() {
        int address = switch (this.textBitmapLogicMode) {
            case IDLE -> 0x3FFF;
            case DISPLAY -> {
                if (this.graphicsMode.getBMM()) {
                    yield ((this.characterBaseAddress & 0b100) << 11) | (this.videoCounter << 3) | this.rowCounter;
                } else {
                    yield (this.characterBaseAddress << 11) | ((this.videoMatrixBuffer[this.videoMatrixLine] & 0xFF) << 3) | this.rowCounter;
                }
            }
        };

        if (this.graphicsMode.getECM()) {
            address &= ~(0b11 << 9);
        }

        this.gDataPendingLatch = this.emulator.getBus().readVIC2(address);
        this.cDataPendingLatch = this.videoMatrixBuffer[this.videoMatrixLine];

        if (textBitmapLogicMode == TextBitmapLogicMode.DISPLAY) {
            this.videoCounter = (this.videoCounter + 1) & 0b1111111111;
            this.videoMatrixLine = (this.videoMatrixLine + 1) & 0b111111;
        }
    }

    private void performCAccess() {
        this.videoMatrixBuffer[this.videoMatrixLine] = switch (this.textBitmapLogicMode) {
            case IDLE -> 0;
            case DISPLAY -> this.emulator.getBus().readVIC2((this.videoMemoryBase << 10) | this.videoCounter);
        };
    }

    private void reloadGraphicsSequencerAsNormal() {
        for (int i = 7; i >= 0; i--) {
            this.graphicsDataSequencer.set(7 - i, (this.gDataPendingLatch >> i) & 1);
        }
        this.graphicsDataSequencer.setFull();
    }

    private void reloadGraphicsSequencerAsMulticolor() {
        for (int i = 3; i >= 0; i--) {
            int bottomIndex = i * 2;
            int topIndex = bottomIndex + 1;

            int data = ((this.gDataPendingLatch >>> (topIndex - 1)) & 0b10) | ((this.gDataPendingLatch >>> bottomIndex) & 1);

            this.graphicsDataSequencer.set(7 - topIndex, data);
            this.graphicsDataSequencer.set(7 - bottomIndex, data);
        }
        this.graphicsDataSequencer.setFull();
    }

    private enum RowSelect {
        NORMAL(55, 247),
        REDUCED_BORDER(51, 251);

        private final int firstLine;
        private final int lastLine;

        RowSelect(int firstLine, int lastLine) {
            this.firstLine = firstLine;
            this.lastLine = lastLine;
        }

        private int getTopComparison() {
            return this.firstLine;
        }

        private int getBottonComparison() {
            return this.lastLine;
        }

    }

    private enum ColumnSelect {
        NORMAL(31, 335),
        REDUCED_BORDER(24, 344);

        private final int xCoordinateLeftComparison;
        private final int xCoordinateRightComparison;

        ColumnSelect(int xCoordinateLeftComparison, int xCoordinateRightComparisonValue) {
            this.xCoordinateLeftComparison = xCoordinateLeftComparison;
            this.xCoordinateRightComparison = xCoordinateRightComparisonValue;
        }

        private int getLeftComparison() {
            return this.xCoordinateLeftComparison;
        }

        private int getRightComparison() {
            return this.xCoordinateRightComparison;
        }

    }

    private enum TextBitmapLogicMode {
        DISPLAY,
        IDLE
    }

    private enum GraphicsMode {
        TEXT_STANDARD(false, false, false), // 000
        TEXT_MULTICOLOR(false, false, true), // 001
        BITMAP_STANDARD(false, true, false), // 010
        BITMAP_MULTICOLOR(false, true, true), // 011
        EXTENDED_COLOR(true, false, false), // 100
        TEXT_INVALID(true, false, true), // 101
        BITMAP_INVALID_1(true, true, false), // 110
        BITMAP_INVALID_2(true, true, true); // 111

        private final boolean ecm;
        private final boolean bmm;
        private final boolean mcm;

        GraphicsMode(boolean ecm, boolean bmm, boolean mcm) {
            this.ecm = ecm;
            this.bmm = bmm;
            this.mcm = mcm;
        }

        private boolean getECM() {
            return this.ecm;
        }

        private boolean getBMM() {
            return this.bmm;
        }

        private boolean getMCM() {
            return this.mcm;
        }

        private GraphicsMode fromMulticolorBit(boolean mcm) {
            if (mcm) {
                return switch (this) {
                    case TEXT_STANDARD -> TEXT_MULTICOLOR;
                    case BITMAP_STANDARD -> BITMAP_MULTICOLOR;
                    case EXTENDED_COLOR -> TEXT_INVALID;
                    case BITMAP_INVALID_1 -> BITMAP_INVALID_2;
                    default -> this;
                };
            } else {
                return switch (this) {
                    case TEXT_MULTICOLOR -> TEXT_STANDARD;
                    case BITMAP_MULTICOLOR -> BITMAP_STANDARD;
                    case TEXT_INVALID -> EXTENDED_COLOR;
                    case BITMAP_INVALID_2 -> BITMAP_INVALID_1;
                    default -> this;
                };
            }
        }

        private GraphicsMode fromUpperBits(int upperBits) {
            upperBits &= 0b11;
            if (this.mcm) {
                return switch (upperBits) {
                    case 0b00 -> TEXT_MULTICOLOR;
                    case 0b01 -> BITMAP_MULTICOLOR;
                    case 0b10 -> TEXT_INVALID;
                    default -> BITMAP_INVALID_2;
                };
            } else {
                return switch (upperBits) {
                    case 0b00 -> TEXT_STANDARD;
                    case 0b01 -> BITMAP_STANDARD;
                    case 0b10 -> EXTENDED_COLOR;
                    default -> BITMAP_INVALID_1;
                };
            }
        }

    }

    private enum Sprite0BAOutputFlag {
        NONE,
        NORMAL,
        LATE
    }

    private class Sprite {

        private final int spriteNumber;

        private int x;
        private int y;

        private boolean enabled;
        private boolean yExpansion;
        private boolean xExpansion;

        private boolean dataPriority;

        private boolean multicolor;

        private boolean spriteCollision;
        private boolean backgroundCollision;

        private int color;

        private int dataCounter; // MOB, 6 bit
        private int dataCounterBase; // MCBASE, 6 bit
        private boolean enableDisplay;

        private boolean advanceLine;
        private boolean dma;
        private boolean corruptDataCounterBaseFlag;

        private final ShiftRegister sequencer = new ShiftRegister(24, 2);
        private int pDataLatch;

        private int shiftedOutDataLatch;
        private boolean shiftOutData;
        private boolean shiftFlipFlop = true;

        private Sprite(int spriteNumber) {
            this.spriteNumber = spriteNumber;
        }

        private int getX() {
            return this.x;
        }

        private void setX(int value) {
            this.x = (this.x & (1 << 8)) | (value & 0xFF);
        }

        private int getY() {
            return this.y;
        }

        private void setY(int value) {
            this.y = value & 0xFF;
        }

        private void setXMSB(boolean msb) {
            this.x = (this.x & 0xFF) | (msb ? 1 << 8 : 0);
        }

        private boolean getXMSB() {
            return (this.x & (1 << 8)) != 0;
        }

        private boolean isEnabled() {
            return this.enabled;
        }

        private void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        private void setYExpansion(boolean value) {
            boolean originalYExpansion = this.yExpansion;
            boolean originalAdvanceLine = this.advanceLine;
            this.yExpansion = value;
            if (!this.yExpansion) {
                this.advanceLine = true;
                if (cycleNumber == 15 && originalYExpansion && !originalAdvanceLine) {
                    this.corruptDataCounterBaseFlag = true;
                }
            }
        }

        private boolean getYExpansion() {
            return this.yExpansion;
        }

        private void setDataPriority(boolean value) {
            this.dataPriority = value;
        }

        private boolean getDataPriority() {
            return this.dataPriority;
        }

        private void setMulticolor(boolean value) {
            this.multicolor = value;
        }

        private boolean isMulticolor() {
            return this.multicolor;
        }

        private void setXExpansion(boolean value) {
            this.xExpansion = value;
            if (!this.xExpansion) {
                this.shiftFlipFlop = true;
            }
        }

        private boolean getXExpansion() {
            return this.xExpansion;
        }

        private void setSpriteCollision(boolean value) {
            this.spriteCollision = value;
        }

        private boolean getSpriteCollision() {
            return this.spriteCollision;
        }

        private void setGraphicsDataCollision(boolean value) {
            this.backgroundCollision = value;
        }

        private boolean getBackgroundCollision() {
            return this.backgroundCollision;
        }

        private void setColor(int value) {
            this.color = value & 0xF;
        }

        private int getColor() {
            return this.color;
        }

        private boolean isDMAOn() {
            return this.dma;
        }

        private void performPAccess() {
            this.pDataLatch = emulator.getBus().readVIC2(0b00001111111000 | (videoMemoryBase << 10) | this.spriteNumber);
        }

        private void tryPerformSAccess(SAccessStep sAccessStep) {
            if (!this.dma || !spriteAECOutput) {
                if (sAccessStep == SAccessStep.SECOND) {
                    performIdleAccess();
                }
                return;
            }
            int spriteData = emulator.getBus().readVIC2((this.pDataLatch << 6) | this.dataCounter);
            int loadOffset = sAccessStep.getLoadOffset();
            if (this.multicolor) {
                for (int i = 3; i >= 0; i--) {
                    int bottomIndex = i * 2;
                    int topIndex = bottomIndex + 1;
                    int data = ((spriteData >>> (topIndex - 1)) & 0b10) | ((spriteData >>> bottomIndex) & 1);
                    this.sequencer.set((7 - topIndex) + loadOffset, data);
                    this.sequencer.set((7 - bottomIndex) + loadOffset, data);
                }
            } else {
                for (int i = 7; i >= 0; i--) {
                    this.sequencer.set((7 - i) + loadOffset, (spriteData >> i) & 1);
                }
            }
            this.dataCounter = (this.dataCounter + 1) & 0b111111;
        }

        private void checkStartDMA() {
            if (!this.dma && this.enabled && this.y == (raster & 0xFF)) {
                this.dma = true;
                this.dataCounterBase = 0;
                this.advanceLine = true;
            }
        }

        private void checkToggleAdvanceLine() {
            if (this.yExpansion && this.dma) {
                this.advanceLine = !this.advanceLine;
            }
        }

        private void loadDataCounter() {
            this.shiftFlipFlop = true;
            this.shiftOutData = false;
            this.shiftedOutDataLatch = 0b00;
            this.dataCounter = this.dataCounterBase;
            if (this.dma) {
                if (this.y == (raster & 0xFF)) {
                    this.enableDisplay = true;
                }
            } else {
                this.enableDisplay = false;
            }
        }

        private void checkAdvanceLineSet() {
            if (this.advanceLine) {
                if (this.corruptDataCounterBaseFlag) {
                    this.corruptDataCounterBaseFlag = false;
                    this.dataCounterBase = ((0b101010 & (this.dataCounterBase & this.dataCounter)) | (0b010101 & (this.dataCounterBase | this.dataCounter)));
                } else {
                    this.dataCounterBase = this.dataCounter;
                }
                if (this.dataCounterBase >= 63) {
                    this.dma = false;
                }
            }
        }

        private void shiftSequencer() {
            if (this.enableDisplay) {
                if (this.shiftOutData || this.x == getXCoordinate()) {
                    this.shiftOutData = true;
                    if (this.shiftFlipFlop) {
                        this.shiftedOutDataLatch = this.sequencer.shiftHead(0b00);
                    }
                    if (this.xExpansion) {
                        this.shiftFlipFlop = !this.shiftFlipFlop;
                    }
                }
            }
        }

        private int getPaletteIndex() {
            if (this.multicolor) {
                return switch (this.shiftedOutDataLatch & 0b11) {
                    case 0b00 -> 0b0000; // transparent
                    case 0b01 -> spriteMulticolor0;
                    case 0b10 -> this.color;
                    default -> spriteMulticolor1;
                };
            } else {
                return (this.shiftedOutDataLatch & 1) != 0 ? this.color : 0b0000;
            }
        }

        private boolean isOpaque() {
            if (this.multicolor) {
                return (this.shiftedOutDataLatch & 0b11) != 0b00;
            } else {
                return (this.shiftedOutDataLatch & 1) != 0;
            }
        }

        private enum SAccessStep {
            FIRST(0),
            SECOND(8),
            THIRD(16);

            private final int loadOffset;

            SAccessStep(int loadOffset) {
                this.loadOffset = loadOffset;
            }

            private int getLoadOffset() {
                return this.loadOffset;
            }

        }

    }

}