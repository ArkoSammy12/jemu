package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.hardware.NMOS6502;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MOS6569<E extends Commodore64Emulator> implements VideoGenerator, Bus {

    // https://www.godot64.de/german/hpalet.htm
    private static final int[] GODOTS_PALETTE = {
            0x000000, 0xFFFFFF, 0x880000, 0xAAFFEE,
            0xCCFFCC, 0x00CC55, 0x0000AA, 0xEEEE77,
            0xDD8855, 0x664400, 0xFF7777, 0x333333,
            0x777777, 0xAAFF66, 0x0088FF, 0xBBBBBB
    };

    private static final int[] GULRAK_PALETTE = {
            0x000000, 0xFFFFFF, 0x753B2F, 0x73AEBE,
            0x784193, 0x619A47, 0x392C85, 0xC2D073,
            0x7B5629, 0x4D4000, 0xA76B5D, 0x4A4A4A,
            0x707070, 0xA1D988, 0x7062C0, 0x989898
    };

    // http://www.pepto.de/projects/colorvic/
    private static final int[] COLORDORE_PALETTE = {
            0x000000, 0xFFFFFF, 0x68372B, 0x70A4B2,
            0x6F3D86, 0x588D43, 0x352879, 0xB8C76F,
            0x6F4F25, 0x433900, 0x9A6759, 0x444444,
            0x6C6C6C, 0x9AD284, 0x6C5EB5, 0x959595
    };

    private final E emulator;

    private final Sprite[] sprites = new Sprite[8];

    private int rasterIrq;
    private boolean extendedColorMode;
    private boolean bitmapMode;
    private boolean displayEnable;
    private boolean rowsSelect;
    private int yScroll;

    private boolean multicolorMode;
    private boolean columnsSelect;
    private int xScroll;

    private int videoMemoryBase;
    private int characterBaseAddrss;

    private int raster;

    private boolean irqVic;
    private boolean irqLightpen;
    private boolean irqSpriteSprite;
    private boolean irqSpriteBackground;
    private boolean irqGridBeam;

    private boolean irqLightpenEnable;
    private boolean irqSpriteSpriteEnable;
    private boolean irqSpriteBackgroundEnable;
    private boolean irqGridBeamEnable;

    private int borderColor;
    private int backgroundColor0;
    private int backgroundColor1;
    private int backgroundColor2;
    private int backgroundColor3;
    private int spriteMulticolor0;
    private int spriteMulticolor1;

    public MOS6569(E emulator) {
        this.emulator = emulator;
        Arrays.fill(this.sprites, new Sprite());
    }

    @Override
    public int getImageWidth() {
        return 0;
    }

    @Override
    public int getImageHeight() {
        return 0;
    }

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return GULRAK_PALETTE[frameBufferValue];
    }

    public boolean getIRQ() {
        return (this.irqLightpenEnable && this.irqLightpen)
                || (this.irqSpriteSpriteEnable && this.irqSpriteSprite)
                || (this.irqSpriteBackgroundEnable && this.irqSpriteBackground)
                || (this.irqGridBeamEnable && this.irqGridBeam);
    }

    public boolean getBA() {
        return false;
    }

    public boolean getAEC() {
        return false;
    }

    @Override
    public int readByte(int address) {
        address &= 0x3F;
        return switch (address) {
            case 0 -> this.sprites[0].getX();
            case 1 -> this.sprites[0].getY();
            case 2 -> this.sprites[1].getX();
            case 3 -> this.sprites[1].getY();
            case 4 -> this.sprites[2].getX();
            case 5 -> this.sprites[2].getY();
            case 6 -> this.sprites[3].getX();
            case 7 -> this.sprites[3].getY();
            case 8 -> this.sprites[4].getX();
            case 9 -> this.sprites[4].getY();
            case 10 -> this.sprites[5].getX();
            case 11 -> this.sprites[5].getY();
            case 12 -> this.sprites[6].getX();
            case 13 -> this.sprites[6].getY();
            case 14 -> this.sprites[7].getX();
            case 15 -> this.sprites[7].getY();
            case 16 -> this.getValueForSprites(Sprite::getXMSB);
            case 17 -> {
                int ret = ((this.raster & (1 << 8)) >>> 1) | this.yScroll;
                ret |= this.extendedColorMode ? 1 << 6 : 0;
                ret |= this.bitmapMode ? 1 << 5 : 0;
                ret |= this.displayEnable ? 1 << 4 : 0;
                ret |= this.rowsSelect ? 1 << 3 : 0;
                yield ret;
            }
            case 18 -> this.raster & 0xFF;
            case 19 -> 0x00; // Lightpen X coordinate
            case 20 -> 0x00; // Lightpen Y coordinate
            case 21 -> this.getValueForSprites(Sprite::isEnabled);
            case 22 -> {
                int ret = 0b11000000 | this.xScroll;
                ret |= this.multicolorMode ? 1 << 4 : 0;
                ret |= this.columnsSelect ? 1 << 3 : 0;
                yield ret;
            }
            case 23 -> this.getValueForSprites(Sprite::getYExpansion);
            case 24 -> 1 | (this.characterBaseAddrss << 1) | (this.videoMemoryBase << 4);
            case 25 -> {
                int ret = 0b01110000;
                ret |= this.irqVic ? 1 << 7 : 0;
                ret |= this.irqLightpen ? 1 << 3 : 0;
                ret |= this.irqSpriteSprite ? 1 << 2 : 0;
                ret |= this.irqSpriteBackground ? 1 << 1 : 0;
                ret |= this.irqGridBeam ? 1 : 0;
                yield ret;
            }
            case 26 -> {
                int ret = 0b11110000;
                ret |= this.irqLightpenEnable ? 1 << 3 : 0;
                ret |= this.irqSpriteSpriteEnable ? 1 << 2 : 0;
                ret |= this.irqSpriteBackgroundEnable ? 1 << 1 : 0;
                ret |= this.irqGridBeamEnable ? 1 : 0;
                yield ret;
            }
            case 27 -> this.getValueForSprites(Sprite::getDataPriority);
            case 28 -> this.getValueForSprites(Sprite::isMulticolor);
            case 29 -> this.getValueForSprites(Sprite::getXExpansion);
            case 30 -> {
                int ret = this.getValueForSprites(Sprite::getSpriteCollision);
                this.setValueForSprites(0x00, Sprite::setSpriteCollision);
                yield ret;
            }
            case 31 -> {
                int ret = this.getValueForSprites(Sprite::getBackgroundCollision);
                this.setValueForSprites(0x00, Sprite::setBackgroundCollision);
                yield ret;
            }
            case 32 -> 0xF0 | this.borderColor;
            case 33 -> 0xF0 | this.backgroundColor0;
            case 34 -> 0xF0 | this.backgroundColor1;
            case 35 -> 0xF0 | this.backgroundColor2;
            case 36 -> 0xF0 | this.backgroundColor3;
            case 37 -> 0xF0 | this.spriteMulticolor0;
            case 38 -> 0xF0 | this.spriteMulticolor1;
            case 39 -> 0xF0 | this.sprites[0].getColor();
            case 40 -> 0xF0 | this.sprites[1].getColor();
            case 41 -> 0xF0 | this.sprites[2].getColor();
            case 42 -> 0xF0 | this.sprites[3].getColor();
            case 43 -> 0xF0 | this.sprites[4].getColor();
            case 44 -> 0xF0 | this.sprites[5].getColor();
            case 45 -> 0xF0 | this.sprites[6].getColor();
            case 46 -> 0xF0 | this.sprites[7].getColor();
            default -> 0xFF;
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x3F;
        switch (address) {
            case 0 -> this.sprites[0].setX(value);
            case 1 -> this.sprites[0].setY(value);
            case 2 -> this.sprites[1].setX(value);
            case 3 -> this.sprites[1].setY(value);
            case 4 -> this.sprites[2].setX(value);
            case 5 -> this.sprites[2].setY(value);
            case 6 -> this.sprites[3].setX(value);
            case 7 -> this.sprites[3].setY(value);
            case 8 -> this.sprites[4].setX(value);
            case 9 -> this.sprites[4].setY(value);
            case 10 -> this.sprites[5].setX(value);
            case 11 -> this.sprites[5].setY(value);
            case 12 -> this.sprites[6].setX(value);
            case 13 -> this.sprites[6].setY(value);
            case 14 -> this.sprites[7].setX(value);
            case 15 -> this.sprites[7].setY(value);
            case 16 -> this.setValueForSprites(value, Sprite::setXMSB);
            case 17 -> {
                this.rasterIrq = (this.rasterIrq & 0xFF) | ((value & (1 << 7)) << 1);
                this.extendedColorMode = (value & (1 << 6)) != 0;
                this.bitmapMode = (value & (1 << 5)) != 0;
                this.displayEnable = (value & (1 << 4)) != 0;
                this.rowsSelect = (value & (1 << 3)) != 0;
                this.yScroll = value & 0b111;
            }
            case 18 -> this.rasterIrq = (this.rasterIrq & (1 << 8)) | (value & 0xFF);
            case 19 -> {} // Lightpen X coordinate
            case 20 -> {} // Lightpen Y coordinate
            case 21 -> this.setValueForSprites(value, Sprite::setEnabled);
            case 22 -> {
                this.multicolorMode = (value & (1 << 4)) != 0;
                this.columnsSelect = (value & (1 << 3)) != 0;
                this.xScroll = value & 0b111;
            }
            case 23 -> this.setValueForSprites(value, Sprite::setYExpansion);
            case 24 -> {
                this.characterBaseAddrss = (value >>> 1) & 0b111;
                this.videoMemoryBase = (value >>> 4) & 0b1111;
            }
            case 25 -> {
                if ((value & (1 << 7)) != 0) {
                    this.irqVic = false;
                }
                if ((value & (1 << 3)) != 0) {
                    this.irqLightpen = false;
                }
                if ((value & (1 << 2)) != 0) {
                    this.irqSpriteSprite = false;
                }
                if ((value & (1 << 1)) != 0) {
                    this.irqSpriteBackground = false;
                }
                if ((value & 1) != 0) {
                    this.irqGridBeam = false;
                }
            }
            case 26 -> {
                this.irqLightpenEnable = (value & (1 << 3)) != 0;
                this.irqSpriteSpriteEnable = (value & (1 << 2)) != 0;
                this.irqSpriteBackgroundEnable = (value & (1 << 1)) != 0;
                this.irqGridBeamEnable = (value & 1) != 0;
                if (this.getIRQ()) {
                    this.irqVic = true;
                }
            }
            case 27 -> this.setValueForSprites(value, Sprite::setDataPriority);
            case 28 -> this.setValueForSprites(value, Sprite::setMulticolor);
            case 29 -> this.setValueForSprites(value, Sprite::setXExpansion);
            case 30 -> this.setValueForSprites(0x00, Sprite::setSpriteCollision);
            case 31 -> this.setValueForSprites(0x00, Sprite::setBackgroundCollision);
            case 32 -> this.borderColor = value & 0xF;
            case 33 -> this.backgroundColor0 = value & 0xF;
            case 34 -> this.backgroundColor1 = value & 0xF;
            case 35 -> this.backgroundColor2 = value & 0xF;
            case 36 -> this.backgroundColor3 = value & 0xF;
            case 37 -> this.spriteMulticolor0 = value & 0xF;
            case 38 -> this.spriteMulticolor1 = value & 0xF;
            case 39 -> this.sprites[0].setColor(value);
            case 40 -> this.sprites[1].setColor(value);
            case 41 -> this.sprites[2].setColor(value);
            case 42 -> this.sprites[3].setColor(value);
            case 43 -> this.sprites[4].setColor(value);
            case 44 -> this.sprites[5].setColor(value);
            case 45 -> this.sprites[6].setColor(value);
            case 46 -> this.sprites[7].setColor(value);
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

    public void clockBusAccess(NMOS6502.Phase cpuPhase) {

    }

    public void clockPixel() {

    }

    private static class Sprite {

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
            this.yExpansion = value;
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

        private void setBackgroundCollision(boolean value) {
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

    }

}
