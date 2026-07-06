package io.github.arkosammy12.jemu.core.atari.atari2600.tia;

import io.github.arkosammy12.jemu.core.atari.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.util.ActionSignalDispatcher;

import static io.github.arkosammy12.jemu.core.atari.atari2600.tia.TIA.*;

class TIAVideo<E extends Atari2600Emulator> implements Bus, VideoGenerator {

    private static final int[] TIA_NTSC_PALETTE = {
            0x000000, 0x444400, 0x702800, 0x841800,
            0x880000, 0x78005c, 0x480078, 0x140084,
            0x000088, 0x00187c, 0x002c5c, 0x00402c,
            0x003c00, 0x143800, 0x2c3000, 0x442800,
            0x404040, 0x646410, 0x844414, 0x983418,
            0x9c2020, 0x8c2074, 0x602090, 0x302098,
            0x1c209c, 0x1c3890, 0x1c4c78, 0x1c5c48,
            0x205c20, 0x345c1c, 0x4c501c, 0x644818,
            0x6c6c6c, 0x848424, 0x985c28, 0xac5030,
            0xb03c3c, 0xa03c88, 0x783ca4, 0x4c3cac,
            0x3840b0, 0x3854a8, 0x386890, 0x387c64,
            0x407c40, 0x507c38, 0x687034, 0x846830,
            0x909090, 0xa0a034, 0xac783c, 0xc06848,
            0xc05858, 0xb0589c, 0x8c58b8, 0x6858c0,
            0x505cc0, 0x5070bc, 0x5084ac, 0x509c80,
            0x5c9c5c, 0x6c9850, 0x848c4c, 0xa08444,
            0xb0b0b0, 0xb8b840, 0xbc8c4c, 0xd0805c,
            0xd07070, 0xc070b0, 0xa070cc, 0x7c70d0,
            0x6874d0, 0x6888cc, 0x689cc0, 0x68b494,
            0x74b474, 0x84b468, 0x9ca864, 0xb89c58,
            0xc8c8c8, 0xd0d050, 0xcca05c, 0xe09470,
            0xe08888, 0xd084c0, 0xb484dc, 0x9488e0,
            0x7c8ce0, 0x7c9cdc, 0x7cb4d4, 0x7cd0ac,
            0x8cd08c, 0x9ccc7c, 0xb4c078, 0xd0b46c,
            0xdcdcdc, 0xe8e85c, 0xdcb468, 0xeca880,
            0xeca0a0, 0xdc9cd0, 0xc49cec, 0xa8a0ec,
            0x90a4ec, 0x90b4ec, 0x90cce8, 0x90e4c0,
            0xa4e4a4, 0xb4e490, 0xccd488, 0xe8cc7c,
            0xececec, 0xfcfc68, 0xfcbc94, 0xfcb4b4,
            0xecb0e0, 0xd4b0fc, 0xbcb4fc, 0xa4b8fc,
            0xa4c8fc, 0xa4e0fc, 0xa4fcd4, 0xb8fcb8,
            0xc8fca4, 0xe0ec9c, 0xfce08c, 0xffffff
    };

    // TODO: PAL palette

    private static final int NTSC_SCANLINES_PER_FRAME = 262;
    private static final int NTSC_VBLANK_SCANLINES = 40;
    private static final int NTSC_KERNEL_SCANLINES = 192;
    private static final int NTSC_OVERSCAN_SCANLINES = 30;

    private static final int PAL_SCANLINES_PER_FRAME = 312;
    private static final int PAL_VBLANK_SCANLINES = 48;
    private static final int PAL_KERNEL_SCANLINES = 228;
    private static final int PAL_OVERSCAN_SCANLINES = 36;

    private static final int VSYNC_SCANLINES = 3;

    private static final int CLOCKS_PER_SCANLINE = 228;
    private static final int HBLANK_CLOCKS = 68;
    private static final int VISIBLE_CLOCKS = 160;
    private static final int COLOR_CLOCK_DIVISOR = 4;

    private final E emulator;

    private final ActionSignalDispatcher actionSignalDispatcher = new ActionSignalDispatcher();
    private final int vBlankWriteSignal;
    private final int reflectPlayer0WriteSignal;
    private final int reflectPlayer1WriteSignal;
    private final int playfield0WriteSignal;
    private final int playfield1WriteSignal;
    private final int playfield2WriteSignal;
    private final int graphicsPlayer0WriteSignal;
    private final int graphicsPlayer1WriteSignal;
    private final int enableMissile0WriteSignal;
    private final int enableMissile1WriteSignal;
    private final int enableBallWriteSignal;
    private final int horizontalMotionPlayer0WriteSignal;
    private final int horizontalMotionPlayer1WriteSignal;
    private final int horizontalMotionMissile0WriteSignal;
    private final int horizontalMotionMissile1WriteSignal;
    private final int horizontalMotionBallWriteSignal;
    private final int applyHorizontalMotionWriteSignal;
    private final int clearHorizontalMotionWriteSignal;

    private final int scanlinesPerFrame;
    private final int kernelScanlines;
    private final int vblankEndScanline;
    private final int kernelEndScanline;
    private final int overscanEndScanline;

    private final int[] video;

    private final Player player0 = new Player();
    private final Player player1 = new Player();
    private final Missile missile0 = new Missile();
    private final Missile missile1 = new Missile();
    private final Ball ball = new Ball();

    private int colorClockNumber;
    private int hSyncCounter;
    private boolean hMove;

    private boolean vsync;
    private int scanlineNumber;

    private boolean vSync;
    private boolean vBlank;
    private boolean wSyncRdySignal;

    private int colorLuminance0;
    private int colorLuminance1;
    private int colorLuminancePlayfield;
    private int colorLuminanceBackground;

    // CTRLPF registers
    private boolean playfieldPriority;
    private boolean scoreMode;
    private boolean reflectPlayfield;

    private int playfield;

    private int collisionLatchMissile0Player;
    private int collisionLatchMissile1Player;
    private int collisionLatchPlayer0PlayfieldBall;
    private int collisionLatchPlayer1PlayfieldBall;
    private int collisionLatchMissile0PlayfieldBall;
    private int collisionLatchMissile1PlayfieldBall;
    private int collisionLatchBallPlayfield;
    private int collisionLatchPlayerPlayerMissileMissile;

    TIAVideo(E emulator) {
        this.emulator = emulator;
        this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
        this.kernelScanlines = NTSC_KERNEL_SCANLINES;

        this.vblankEndScanline = NTSC_VBLANK_SCANLINES;
        this.kernelEndScanline = this.vblankEndScanline + this.kernelScanlines;
        this.overscanEndScanline = this.kernelEndScanline + NTSC_OVERSCAN_SCANLINES;

        this.video = new int[this.getImageWidth() * this.getImageHeight()];

        this.vBlankWriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
            // TODO: Input bits
            this.vBlank = (value & (1 << 1)) != 0;
        });
        this.reflectPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.player0.setReflectGraphics((value & (1 << 3)) != 0));
        this.reflectPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.player1.setReflectGraphics((value & (1 << 3)) != 0));
        this.playfield0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.playfield = ((reverseBits(value) & 0xF) << 16) | (this.playfield & 0x0FFFF));
        this.playfield1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.playfield = (value << 8) | (this.playfield & 0xF00FF));
        this.playfield2WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.playfield = (reverseBits(value)) | (this.playfield & 0xFFF00));
        this.graphicsPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(1, this.player0::setGraphics);
        this.graphicsPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(1, this.player1::setGraphics);
        this.enableMissile0WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.missile0.setEnabled((value & (1 << 1)) != 0));
        this.enableMissile1WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.missile1.setEnabled((value & (1 << 1)) != 0));
        this.enableBallWriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.ball.setEnabled((value & (1 << 1)) != 0));
        this.horizontalMotionPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.player0.setHorizontalMotion(value >>> 4));
        this.horizontalMotionPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.player1.setHorizontalMotion(value >>> 4));
        this.horizontalMotionMissile0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile0.setHorizontalMotion(value >>> 4));
        this.horizontalMotionMissile1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile1.setHorizontalMotion(value >>> 4));
        this.horizontalMotionBallWriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.ball.setHorizontalMotion(value >>> 4));
        this.applyHorizontalMotionWriteSignal = this.actionSignalDispatcher.addSignal(6, _ -> this.hMove = true);
        this.clearHorizontalMotionWriteSignal = this.actionSignalDispatcher.addSignal(2, _ -> {
            this.player0.clearHorizontalMotion();
            this.player1.clearHorizontalMotion();
            this.missile0.clearHorizontalMotion();
            this.missile1.clearHorizontalMotion();
            this.ball.clearHorizontalMotion();
        });
    }

    @Override
    public int getImageWidth() {
        return VISIBLE_CLOCKS;
    }

    @Override
    public int getImageHeight() {
        return this.kernelScanlines;
    }

    @Override
    public double getPixelAspectRatio() {
        return 2.0;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case CXM0P -> this.emulator.getBus().combineWithDataBus(this.collisionLatchMissile0Player, 0xC0);
            case CXM1P -> this.emulator.getBus().combineWithDataBus(this.collisionLatchMissile1Player, 0xC0);
            case CXP0FB -> this.emulator.getBus().combineWithDataBus(this.collisionLatchPlayer0PlayfieldBall, 0xC0);
            case CXP1FB -> this.emulator.getBus().combineWithDataBus(this.collisionLatchPlayer1PlayfieldBall, 0xC0);
            case CXM0FB -> this.emulator.getBus().combineWithDataBus(this.collisionLatchMissile0PlayfieldBall, 0xC0);
            case CXM1FB -> this.emulator.getBus().combineWithDataBus(this.collisionLatchMissile1PlayfieldBall, 0xC0);
            case CXBLPF -> this.emulator.getBus().combineWithDataBus(this.collisionLatchBallPlayfield, 0x80);
            case CXPPMM -> this.emulator.getBus().combineWithDataBus(this.collisionLatchPlayerPlayerMissileMissile, 0xC0);
            default -> this.emulator.getBus().combineWithDataBus(0, 0x00);
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case VSYNC -> {
                boolean vSync = (value & (1 << 1)) != 0;
                if (!this.vsync && vSync) {
                    this.scanlineNumber = 0;
                    this.emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.video));
                }
                this.vsync = vSync;
            }
            case VBLANK -> this.actionSignalDispatcher.trigger(this.vBlankWriteSignal, value);
            case WSYNC -> this.wSyncRdySignal = true;
            case RSYNC -> this.resetScanline();
            case NUSIZ0 -> {
                this.missile0.setSize(value >>> 4);
                this.player0.setSize(value);
            }
            case NUSIZ1 -> {
                this.missile1.setSize(value >>> 4);
                this.player1.setSize(value);
            }
            case COLUP0 -> this.colorLuminance0 = (value >>> 1) & 0x7F;
            case COLUP1 -> this.colorLuminance1 = (value >>> 1) & 0x7F;
            case COLUPF -> this.colorLuminancePlayfield = (value >>> 1) & 0x7F;
            case COLUBK -> this.colorLuminanceBackground = (value >>> 1) & 0x7F;
            case CTRLPF -> {
                this.playfieldPriority = (value & (1 << 2)) != 0;
                this.scoreMode = (value & (1 << 1)) != 0;
                this.reflectPlayfield = (value & 1) != 0;
                this.ball.setSize(value >>> 4);
            }
            case REFP0 -> this.actionSignalDispatcher.trigger(this.reflectPlayer0WriteSignal, value);
            case REFP1 -> this.actionSignalDispatcher.trigger(this.reflectPlayer1WriteSignal, value);
            case PF0 -> this.actionSignalDispatcher.trigger(this.playfield0WriteSignal, value);
            case PF1 -> this.actionSignalDispatcher.trigger(this.playfield1WriteSignal, value);
            case PF2 -> this.actionSignalDispatcher.trigger(this.playfield2WriteSignal, value);
            case RESP0 -> this.player0.resetHorizontalPosition();
            case RESP1 -> this.player1.resetHorizontalPosition();
            case RESM0 -> this.missile0.resetHorizontalPosition();
            case RESM1 -> this.missile1.resetHorizontalPosition();
            case RESBL -> this.ball.resetHorizontalPosition();
            case GRP0 -> this.actionSignalDispatcher.trigger(this.graphicsPlayer0WriteSignal, value);
            case GRP1 -> this.actionSignalDispatcher.trigger(this.graphicsPlayer1WriteSignal, value);
            case ENAM0 -> this.actionSignalDispatcher.trigger(this.enableMissile0WriteSignal, value);
            case ENAM1 -> this.actionSignalDispatcher.trigger(this.enableMissile1WriteSignal, value);
            case ENABL -> this.actionSignalDispatcher.trigger(this.enableBallWriteSignal, value);
            case HMP0 -> this.actionSignalDispatcher.trigger(this.horizontalMotionPlayer0WriteSignal, value);
            case HMP1 -> this.actionSignalDispatcher.trigger(this.horizontalMotionPlayer1WriteSignal, value);
            case HMM0 -> this.actionSignalDispatcher.trigger(this.horizontalMotionMissile0WriteSignal, value);
            case HMM1 -> this.actionSignalDispatcher.trigger(this.horizontalMotionMissile1WriteSignal, value);
            case HMBL -> this.actionSignalDispatcher.trigger(this.horizontalMotionBallWriteSignal, value);
            case VDELP0 -> this.player0.setVerticalDelay((value & 1) != 0);
            case VDELP1 -> this.player1.setVerticalDelay((value & 1) != 0);
            case VDELBL -> this.ball.setVerticalDelay((value & 1) != 0);
            case RESMP0 -> this.missile0.resetToPlayer((value & (1 << 1)) != 0);
            case RESMP1 -> this.missile1.resetToPlayer((value & (1 << 1)) != 0);
            case HMOVE -> this.actionSignalDispatcher.trigger(this.applyHorizontalMotionWriteSignal, value);
            case HMCLR -> this.actionSignalDispatcher.trigger(this.clearHorizontalMotionWriteSignal, value);
            case CXCLR -> {
                this.collisionLatchMissile0Player = 0;
                this.collisionLatchMissile1Player = 0;
                this.collisionLatchPlayer0PlayfieldBall = 0;
                this.collisionLatchPlayer1PlayfieldBall = 0;
                this.collisionLatchMissile0PlayfieldBall = 0;
                this.collisionLatchMissile1PlayfieldBall = 0;
                this.collisionLatchBallPlayfield = 0;
                this.collisionLatchPlayerPlayerMissileMissile = 0;
            }
        }
    }

    boolean getRDYSignal() {
        return this.wSyncRdySignal;
    }

    void clock() {

        this.actionSignalDispatcher.tick();

        if (this.colorClockNumber == 0) {
            this.wSyncRdySignal = false;
        }

        boolean hBlank = this.isHBlank();

        if (!hBlank) {
            this.player0.clock();
            this.player1.clock();
            this.missile0.clock();
            this.missile1.clock();
            this.ball.clock();
        }

        int pixelX = this.colorClockNumber - HBLANK_CLOCKS;
        if (this.isKernelScanline() && pixelX >= 0 && pixelX < 160) {
            int colorIndex;
            if (hBlank || this.vBlank) {
                colorIndex = 0;
            } else {
                boolean pf = this.getPlayfieldPixel();
                boolean p0 = this.player0.getPixel();
                boolean p1 = this.player1.getPixel();
                boolean m0 = this.missile0.getPixel();
                boolean m1 = this.missile1.getPixel();
                boolean bl =  this.ball.getPixel();

                if (m0) {
                    if (p1) {
                        this.collisionLatchMissile0Player |= 1 << 7;
                    }
                    if (p0) {
                        this.collisionLatchMissile0Player |= 1 << 6;
                    }
                    if (pf) {
                        this.collisionLatchMissile0PlayfieldBall |= 1 << 7;
                    }
                    if (bl) {
                        this.collisionLatchMissile0PlayfieldBall |= 1 << 6;
                    }
                    if (m1) {
                        this.collisionLatchPlayerPlayerMissileMissile |= 1 << 6;
                    }
                }

                if (m1) {
                    if (p1) {
                        this.collisionLatchMissile1Player |= 1 << 7;
                    }
                    if (p0) {
                        this.collisionLatchMissile1Player |= 1 << 6;
                    }
                    if (pf) {
                        this.collisionLatchMissile1PlayfieldBall |= 1 << 7;
                    }
                    if (bl) {
                        this.collisionLatchMissile1PlayfieldBall |= 1 << 6;
                    }
                }

                if (p0) {
                    if (pf) {
                        this.collisionLatchPlayer0PlayfieldBall |= 1 << 7;
                    }
                    if (bl) {
                        this.collisionLatchPlayer0PlayfieldBall |=  1 << 6;
                    }
                    if (p1) {
                        this.collisionLatchPlayerPlayerMissileMissile |= 1 << 7;
                    }
                }

                if (p1) {
                    if (pf) {
                        this.collisionLatchPlayer1PlayfieldBall |= 1 << 7;
                    }
                    if (bl) {
                        this.collisionLatchPlayer1PlayfieldBall |=  1 << 6;
                    }
                }

                if (bl && pf) {
                    this.collisionLatchBallPlayfield |= 1 << 7;
                }

                if (this.playfieldPriority) {
                    if (bl || pf) {
                        colorIndex = this.colorLuminancePlayfield;
                    } else if (p0 || m0) {
                        colorIndex = this.colorLuminance0;
                    } else if (p1 || m1) {
                        colorIndex = this.colorLuminance1;
                    } else {
                        colorIndex = this.colorLuminanceBackground;
                    }
                } else {
                    if (p0 || m0) {
                        colorIndex = this.colorLuminance0;
                    } else if (p1 || m1) {
                        colorIndex = this.colorLuminance1;
                    } else if (bl || pf) { // TODO: Only BL in SCORE-mode (what does that mean)
                        colorIndex = this.scoreMode ? ((pixelX < 80 ? this.colorLuminance0 : this.colorLuminance1)) : this.colorLuminancePlayfield;
                    } else {
                        colorIndex = this.colorLuminanceBackground;
                    }
                }

            }
            this.video[((this.scanlineNumber - this.vblankEndScanline) * VISIBLE_CLOCKS + pixelX)] = TIA_NTSC_PALETTE[colorIndex];
        }

        this.colorClockNumber++;
        if (this.colorClockNumber % COLOR_CLOCK_DIVISOR == 0) {
            this.hSyncCounter++;
            if (this.hSyncCounter >= 57) {
                this.hSyncCounter = 0;
            }
        }
        if (this.colorClockNumber >= CLOCKS_PER_SCANLINE) {
            this.scanlineNumber++;
            if (this.scanlineNumber >= this.scanlinesPerFrame) {
                this.scanlineNumber = 0;
                //this.emulator.getHost().getVideoDriver().ifPresent(driver -> driver.outputFrame(this.video));
            }
            this.hMove = false;
            this.resetScanline();
        }
    }

    private boolean getPlayfieldPixel() {
        int playfieldBit;
        int playfieldColumn = this.hSyncCounter - 17;
        if (playfieldColumn < 20) {
            playfieldBit = 19 - playfieldColumn;
        } else {
            playfieldColumn -= 20;
            if (this.reflectPlayfield) {
                playfieldBit = playfieldColumn;
            } else {
                playfieldBit = 19 - playfieldColumn;
            }
        }
        return (this.playfield & (1 << playfieldBit)) != 0;
    }

    private boolean isKernelScanline() {
        return this.scanlineNumber >= this.vblankEndScanline && this.scanlineNumber < this.kernelEndScanline;
    }

    private boolean isHBlank() {
        return this.colorClockNumber < HBLANK_CLOCKS || (this.hMove && this.colorClockNumber < HBLANK_CLOCKS + 8);
    }

    private void resetScanline() {
        this.colorClockNumber = 0;
        this.hSyncCounter = 0;
    }

    private static int reverseBits(int b) {
        b &= 0xFF; // Assuming 8-bit number
        b = (b & 0xF0) >> 4 | (b & 0x0F) << 4;
        b = (b & 0xCC) >> 2 | (b & 0x33) << 2;
        b = (b & 0xAA) >> 1 | (b & 0x55) << 1;
        return b;
    }

    private abstract class Sprite {

        protected abstract void setSize(int size);

        protected abstract void resetHorizontalPosition();

        protected abstract void setHorizontalMotion(int value);

        protected abstract void applyHorizontalMotion();

        protected abstract void clearHorizontalMotion();

        protected abstract void clock();

        protected abstract boolean getPixel();

    }

    private class Player extends Sprite {

        private void setGraphics(int graphics) {

        }

        private void setReflectGraphics(boolean reflectGraphics) {

        }

        private void setVerticalDelay(boolean verticalDelay) {

        }

        @Override
        protected void setSize(int size) {
            size &= 0b111;
        }

        @Override
        protected void resetHorizontalPosition() {

        }

        @Override
        protected void setHorizontalMotion(int value) {
            value &= 0xF;
        }

        @Override
        protected void applyHorizontalMotion() {

        }

        @Override
        protected void clearHorizontalMotion() {

        }

        @Override
        protected void clock() {

        }

        @Override
        protected boolean getPixel() {
            return false;
        }

    }

    private class Missile extends Sprite {

        protected void setEnabled(boolean enabled) {

        }

        @Override
        protected void setSize(int size) {
            size &= 0b11;
        }

        @Override
        protected void resetHorizontalPosition() {

        }

        @Override
        protected void setHorizontalMotion(int value) {
            value &= 0xF;
        }

        @Override
        protected void applyHorizontalMotion() {

        }

        @Override
        protected void clearHorizontalMotion() {

        }

        protected void resetToPlayer(boolean resetToPlayer) {

        }

        @Override
        protected void clock() {

        }

        @Override
        protected boolean getPixel() {
            return false;
        }

    }

    private class Ball extends Sprite {

        protected void setEnabled(boolean enabled) {

        }

        @Override
        protected void setSize(int size) {
            size &= 0b11;

        }

        private void setVerticalDelay(boolean verticalDelay) {

        }

        @Override
        protected void resetHorizontalPosition() {

        }

        @Override
        protected void setHorizontalMotion(int value) {
            value &= 0xF;
        }

        @Override
        protected void applyHorizontalMotion() {

        }

        @Override
        protected void clearHorizontalMotion() {

        }

        @Override
        protected void clock() {

        }

        @Override
        protected boolean getPixel() {
            return false;
        }

    }

}
