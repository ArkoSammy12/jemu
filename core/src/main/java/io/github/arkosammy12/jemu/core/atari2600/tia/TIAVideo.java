package io.github.arkosammy12.jemu.core.atari2600.tia;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.util.ActionSignalDispatcher;

import static io.github.arkosammy12.jemu.core.atari2600.tia.TIA.*;

class TIAVideo<E extends Emulator & TIA.SystemBus> implements Bus, VideoGenerator {

    private static final int[] TIA_NTSC_PALETTE = {
            0x000000, 0x404040, 0x6c6c6c, 0x909090,
            0xb0b0b0, 0xc8c8c8, 0xdcdcdc, 0xececec,
            0x444400, 0x646410, 0x848424, 0xa0a034,
            0xb8b840, 0xd0d050, 0xe8e85c, 0xfcfc68,
            0x702800, 0x844414, 0x985c28, 0xac783c,
            0xbc8c4c, 0xb89c58, 0xdcb468, 0xecc878,
            0x841800, 0x983418, 0xac5030, 0xc06848,
            0xd0805c, 0xe09470, 0xeca880, 0xfcbc94,
            0x880000, 0x9c2020, 0xb03c3c, 0xc05858,
            0xd07070, 0xe08888, 0xeca0a0, 0xfcb4b4,
            0x78005c, 0x8c2074, 0xa03c88, 0xb0589c,
            0xc070b0, 0xd084c0, 0xdc9cd0, 0xecb0e0,
            0x480078, 0x602090, 0x783ca4, 0x8c58b8,
            0xa070cc, 0xb484dc, 0xc49cec, 0xd4b0fc,
            0x140084, 0x302098, 0x4c3cac, 0x6858c0,
            0x7c70d0, 0x9488e0, 0xa8a0ec, 0xbcb4fc,
            0x000088, 0x1c209c, 0x3840b0, 0x505cc0,
            0x6874d0, 0x7c8ce0, 0x90a4ec, 0xa4c8fc,
            0x00187c, 0x1c3890, 0x3854a8, 0x5070bc,
            0x6888cc, 0x7c9cdc, 0x90b4ec, 0xa4c8fc,
            0x002c5c, 0x1c4c78, 0x386890, 0x5084ac,
            0x689cc0, 0x7cb4d4, 0x90cce8, 0xa4e0fc,
            0x003c2c, 0x1c5c48, 0x387c64, 0x509c80,
            0x68b494, 0x7cd0ac, 0x90e4c0, 0xa4fcd4,
            0x003c00, 0x205c20, 0x407c40, 0x5c9c5c,
            0x74b474, 0x8cd08c, 0xa4e4a4, 0xb8fcb8,
            0x143800, 0x345c1c, 0x507c38, 0x6c9850,
            0x84b468, 0x9ccc7c, 0xb4e490, 0xc8fca4,
            0x2c3000, 0x644818, 0x687034, 0x848c4c,
            0x9ca864, 0xb4c078, 0xb4e490, 0xc8fca4,
            0x442800, 0x644818, 0x846830, 0xa08444,
            0xb89c58, 0xd0b46c, 0xecc878, 0xfce08c,
    };

    private static final int[] TIA_PAL_PALETTE = {
            0x000000, 0x1a1a1a, 0x393939, 0x5b5b5b,
            0x7e7e7e, 0xa2a2a2, 0xc7c7c7, 0xededed,
            0x000000, 0x1a1a1a, 0x393939, 0x5b5b5b,
            0x7e7e7e, 0xa2a2a2, 0xc7c7c7, 0xededed,
            0x150400, 0x341f00, 0x553f00, 0x776100,
            0x9b8419, 0xc0a838, 0xe6cd5a, 0xfef47d,
            0x001e00, 0x003e00, 0x0d6000, 0x2a8318,
            0x4ba737, 0x6dcc59, 0x91f27b, 0xb5fea0,
            0x280000, 0x491000, 0x6b2e00, 0x8e4f17,
            0xb37136, 0xd99558, 0xffba7a, 0xfedf9e,
            0x001d00, 0x003c07, 0x045e23, 0x1f8143,
            0x3fa565, 0x61ca88, 0x84f0ad, 0xa8fed2,
            0x340000, 0x550405, 0x772021, 0x9b4041,
            0xc06263, 0xe68586, 0xfea9aa, 0xfecfd0,
            0x001413, 0x003332, 0x045453, 0x1f7675,
            0x3f9a99, 0x61bfbe, 0x84e5e4, 0xa8fefe,
            0x340012, 0x550031, 0x771852, 0x9b3674,
            0xc05898, 0xe67abd, 0xfe9ee3, 0xfec3fe,
            0x00112a, 0x00245f, 0x0d4482, 0x2a66a6,
            0x4b89cb, 0x6daef1, 0x91d3fe, 0xb5f9fe,
            0x28003c, 0x49005e, 0x6b1681, 0x8e34a5,
            0xb356ca, 0xd978f0, 0xff9cfe, 0xfec1fe,
            0x00005c, 0x04157f, 0x2034a3, 0x4055c8,
            0x6277ee, 0x859bfe, 0xa9c0fe, 0xcfe6fe,
            0x15005b, 0x34007e, 0x551aa2, 0x7739c7,
            0x9b5bed, 0xc07efe, 0xe6a2fe, 0xfec7fe,
            0x000067, 0x1a088a, 0x3925af, 0x5b45d4,
            0x7e67fa, 0xa28afe, 0xc7affe, 0xedd4fe,
            0x000000, 0x1a1a1a, 0x393939, 0x5b5b5b,
            0x7e7e7e, 0xa2a2a2, 0xc7c7c7, 0xededed,
            0x000000, 0x1a1a1a, 0x393939, 0x5b5b5b,
            0x7e7e7e, 0xa2a2a2, 0xc7c7c7, 0xededed,
    };

    private static final int NTSC_SCANLINES_PER_FRAME = 262;
    private static final int NTSC_VBLANK_SCANLINES = 40;
    private static final int NTSC_KERNEL_SCANLINES = 192;
    private static final int NTSC_OVERSCAN_SCANLINES = 30;
    private static final double NTSC_PAR = 12.0 / 7.0;

    private static final int PAL_SCANLINES_PER_FRAME = 312;
    private static final int PAL_VBLANK_SCANLINES = 48;
    private static final int PAL_KERNEL_SCANLINES = 228;
    private static final int PAL_OVERSCAN_SCANLINES = 36;
    private static final double PAL_PAR = 27.0 / 13.0;

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
    private final double pixelAspectRatio;
    private final int[] palette;

    private final int[] video;

    private final Missile missile0 = new Missile();
    private final Missile missile1 = new Missile();
    private final Player player0 = new Player(this.missile0);
    private final Player player1 = new Player(this.missile1);
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

    TIAVideo(E emulator, TIA<E> tia) {
        this.emulator = emulator;
        this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
        this.kernelScanlines = NTSC_KERNEL_SCANLINES;

        this.vblankEndScanline = NTSC_VBLANK_SCANLINES;
        this.kernelEndScanline = this.vblankEndScanline + this.kernelScanlines;
        this.overscanEndScanline = this.kernelEndScanline + NTSC_OVERSCAN_SCANLINES;
        this.pixelAspectRatio = NTSC_PAR;
        this.palette = TIA_NTSC_PALETTE;

        this.video = new int[this.getImageWidth() * this.getImageHeight()];

        this.vBlankWriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
            tia.setDump((value & (1 << 7)) != 0);
            tia.setLatch((value & (1 << 6)) != 0);
            this.vBlank = (value & (1 << 1)) != 0;
        });
        this.reflectPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.player0.setReflectGraphics((value & (1 << 3)) != 0));
        this.reflectPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.player1.setReflectGraphics((value & (1 << 3)) != 0));
        this.playfield0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.playfield = ((reverseBits(value) & 0xF) << 16) | (this.playfield & 0x0FFFF));
        this.playfield1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.playfield = (value << 8) | (this.playfield & 0xF00FF));
        this.playfield2WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.playfield = (reverseBits(value)) | (this.playfield & 0xFFF00));
        this.graphicsPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
            this.player0.setGraphics(value);
            this.player1.copyNewGraphicsToOld();
        });
        this.graphicsPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
            this.player1.setGraphics(value);
            this.player0.copyNewGraphicsToOld();
            this.ball.copyNewEnabledToOld();
        });
        this.enableMissile0WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.missile0.setEnabled((value & (1 << 1)) != 0));
        this.enableMissile1WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.missile1.setEnabled((value & (1 << 1)) != 0));
        this.enableBallWriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.ball.setEnabled((value & (1 << 1)) != 0));
        this.horizontalMotionPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.player0.setHorizontalMotion(value >>> 4));
        this.horizontalMotionPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.player1.setHorizontalMotion(value >>> 4));
        this.horizontalMotionMissile0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile0.setHorizontalMotion(value >>> 4));
        this.horizontalMotionMissile1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile1.setHorizontalMotion(value >>> 4));
        this.horizontalMotionBallWriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.ball.setHorizontalMotion(value >>> 4));
        this.applyHorizontalMotionWriteSignal = this.actionSignalDispatcher.addSignal(6, _ -> {
            this.hMove = true;
            this.player0.applyHorizontalMotion();
            this.player1.applyHorizontalMotion();
            this.missile0.applyHorizontalMotion();
            this.missile1.applyHorizontalMotion();
            this.ball.applyHorizontalMotion();
        });
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
        return this.pixelAspectRatio;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case CXM0P -> this.emulator.combineWithDataBus(this.collisionLatchMissile0Player, 0xC0);
            case CXM1P -> this.emulator.combineWithDataBus(this.collisionLatchMissile1Player, 0xC0);
            case CXP0FB -> this.emulator.combineWithDataBus(this.collisionLatchPlayer0PlayfieldBall, 0xC0);
            case CXP1FB -> this.emulator.combineWithDataBus(this.collisionLatchPlayer1PlayfieldBall, 0xC0);
            case CXM0FB -> this.emulator.combineWithDataBus(this.collisionLatchMissile0PlayfieldBall, 0xC0);
            case CXM1FB -> this.emulator.combineWithDataBus(this.collisionLatchMissile1PlayfieldBall, 0xC0);
            case CXBLPF -> this.emulator.combineWithDataBus(this.collisionLatchBallPlayfield, 0x80);
            case CXPPMM -> this.emulator.combineWithDataBus(this.collisionLatchPlayerPlayerMissileMissile, 0xC0);
            default -> this.emulator.combineWithDataBus(0, 0x00);
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
                this.missile0.setSize((value >>> 4) & 0b11);
                this.player0.setSize(value & 0b111);
            }
            case NUSIZ1 -> {
                this.missile1.setSize((value >>> 4) & 0b11);
                this.player1.setSize(value & 0b111);
            }
            case COLUP0 -> this.colorLuminance0 = (value >>> 1) & 0x7F;
            case COLUP1 -> this.colorLuminance1 = (value >>> 1) & 0x7F;
            case COLUPF -> this.colorLuminancePlayfield = (value >>> 1) & 0x7F;
            case COLUBK -> this.colorLuminanceBackground = (value >>> 1) & 0x7F;
            case CTRLPF -> {
                this.playfieldPriority = (value & (1 << 2)) != 0;
                this.scoreMode = (value & (1 << 1)) != 0;
                this.reflectPlayfield = (value & 1) != 0;
                this.ball.setSize((value >>> 4) & 0b11);
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

    void cycle() {

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
                    if (p0) {
                        this.collisionLatchMissile1Player |= 1 << 7;
                    }
                    if (p1) {
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
            this.video[((this.scanlineNumber - this.vblankEndScanline) * VISIBLE_CLOCKS + pixelX)] = this.palette[colorIndex];
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

    private abstract static class Sprite {

        protected int size;
        private int horizontalMotion;

        protected int phaseCounter = 4;
        protected int positionCounter;

        protected int startCounter;
        protected int pixelCounter;
        protected int widthCounter;

        protected boolean pixel;

        protected void setSize(int size) {
            this.size = size;
        }

        protected void resetHorizontalPosition() {
            this.phaseCounter = 4;
            this.positionCounter = 0;
        }

        protected void setHorizontalMotion(int value) {
            this.horizontalMotion = value & 0xF;
        }

        protected void applyHorizontalMotion() {
            int clocks = this.horizontalMotion ^ 8;
            for (int i = 0; i < clocks; i++) {
                this.clock();
            }
        }

        protected void clearHorizontalMotion() {
            this.horizontalMotion = 0;
        }

        protected abstract void clock();

        protected boolean getPixel() {
            return this.pixel;
        }

    }

    private static class Player extends Sprite {

        private final Missile associatedMissile;

        private int oldGraphics;
        private int newGraphics;

        private boolean verticalDelay;
        private boolean reflectGraphics;

        private boolean copy;

        private Player(Missile associatedMissile) {
            this.associatedMissile = associatedMissile;
            this.pixelCounter = 8;
        }

        private void setGraphics(int graphics) {
            this.newGraphics = graphics & 0xFF;
        }

        private void copyNewGraphicsToOld() {
            this.oldGraphics = this.newGraphics;
        }

        private void setReflectGraphics(boolean reflectGraphics) {
            this.reflectGraphics = reflectGraphics;
        }

        private void setVerticalDelay(boolean verticalDelay) {
            this.verticalDelay = verticalDelay;
        }

        @Override
        protected void clock() {
            this.phaseCounter--;
            if (this.phaseCounter <= 0) {
                this.phaseCounter = 4;

                boolean firstCopy = this.size == 1 || this.size == 3;
                boolean secondCopy = this.size == 2 || this.size == 3 || this.size == 6;
                boolean thirdCopy = this.size == 4 || this.size == 6;

                if (firstCopy && this.positionCounter == 3) {
                    this.copy = true;
                    this.start();
                } else if (secondCopy && this.positionCounter == 7) {
                    this.copy = true;
                    this.start();
                } else if (thirdCopy && this.positionCounter == 15) {
                    this.copy = true;
                    this.start();
                } else if (this.positionCounter == 39) {
                    this.start();
                }

                this.positionCounter++;
                if (this.positionCounter >= 40) {
                    this.copy = false;
                    this.positionCounter = 0;
                }
            }

            if (this.startCounter > 0) {
                this.startCounter--;
                if (this.startCounter <= 0) {
                    this.pixelCounter = 0;
                    this.widthCounter = this.getWidth();
                }
            }

            this.pixel = false;

            if (this.pixelCounter < 8) {
                int graphics = this.verticalDelay ? this.oldGraphics : this.newGraphics;
                int bit = this.reflectGraphics ? this.pixelCounter : (7 - this.pixelCounter);
                this.pixel = (graphics & (1 << bit)) != 0;

                if (!this.copy && this.associatedMissile.getResetToPlayer() && this.pixelCounter == 4) {
                    this.associatedMissile.resetHorizontalPosition();
                }

                this.widthCounter--;
                if (this.widthCounter <= 0) {
                    this.pixelCounter++;
                    this.widthCounter = this.getWidth();
                }
            }

        }


        private void start() {
            this.startCounter = 7;
        }

        private int getWidth() {
            return switch (this.size) {
                case 5 -> 2;
                case 7 -> 4;
                default -> 1;
            };
        }

    }

    private static class Missile extends Sprite {

        private boolean enabled;
        private boolean resetToPlayer;

        private Missile() {
            this.pixelCounter = 1;
        }

        protected void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        protected void resetToPlayer(boolean resetToPlayer) {
            this.resetToPlayer = resetToPlayer;
        }

        protected boolean getResetToPlayer() {
            return this.resetToPlayer;
        }

        @Override
        protected void clock() {
            this.phaseCounter--;
            if (this.phaseCounter <= 0) {
                this.phaseCounter = 4;

                if (this.positionCounter == 39) {
                    this.start();
                }

                this.positionCounter++;
                if (this.positionCounter >= 40) {
                    this.positionCounter = 0;
                }
            }

            if (this.startCounter > 0) {
                this.startCounter--;
                if (this.startCounter <= 0) {
                    this.pixelCounter = 0;
                    this.widthCounter = this.getWidth();
                }
            }

            this.pixel = false;

            if (this.pixelCounter < 1) {

                this.pixel = !this.resetToPlayer && this.enabled;

                this.widthCounter--;
                if (this.widthCounter <= 0) {
                    this.pixelCounter++;
                    this.widthCounter = this.getWidth();
                }

            }

        }

        private void start() {
            this.startCounter = 6;
        }

        private int getWidth() {
            return 1 << this.size;
        }

    }

    private static class Ball extends Sprite {

        private boolean oldEnabled;
        private boolean newEnabled;

        private boolean verticalDelay;

        private Ball() {
            this.pixelCounter = 1;
        }

        protected void setEnabled(boolean enabled) {
            this.newEnabled = enabled;
        }

        private void setVerticalDelay(boolean verticalDelay) {
            this.verticalDelay = verticalDelay;
        }

        private void copyNewEnabledToOld() {
            this.oldEnabled = this.newEnabled;
        }

        @Override
        protected void resetHorizontalPosition() {
            super.resetHorizontalPosition();
            this.start();
        }

        @Override
        protected void clock() {
            this.phaseCounter--;
            if (this.phaseCounter <= 0) {
                this.phaseCounter = 4;

                if (this.positionCounter == 39) {
                    this.start();
                }

                this.positionCounter++;
                if (this.positionCounter >= 40) {
                    this.positionCounter = 0;
                }
            }

            if (this.startCounter > 0) {
                this.startCounter--;
                if (this.startCounter <= 0) {
                    this.pixelCounter = 0;
                    this.widthCounter = this.getWidth();
                }
            }

            this.pixel = false;

            if (this.pixelCounter < 1) {

                this.pixel = this.verticalDelay ? this.oldEnabled : this.newEnabled;

                this.widthCounter--;
                if (this.widthCounter <= 0) {
                    this.pixelCounter++;
                    this.widthCounter = this.getWidth();
                }

            }


        }

        private void start() {
            this.startCounter = 6;
        }

        private int getWidth() {
            return 1 << this.size;
        }

    }

}
