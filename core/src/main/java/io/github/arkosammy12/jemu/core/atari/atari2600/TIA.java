package io.github.arkosammy12.jemu.core.atari.atari2600;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TIA<E extends Atari2600Emulator> implements Bus {

    private static final int AUDIO_CPU_CLK_DIVISOR = 38;

    private static final int CXM0P = 0x00;
    private static final int CXM1P = 0x01;
    private static final int CXP0FB = 0x02;
    private static final int CXP1FB = 0x03;
    private static final int CXM0FB = 0x04;
    private static final int CXM1FB = 0x05;
    private static final int CXBLPF = 0x06;
    private static final int CXPPMM = 0x07;
    private static final int INPT0 = 0x08;
    private static final int INPT1 = 0x09;
    private static final int INPT2 = 0x0A;
    private static final int INPT3 = 0x0B;
    private static final int INPT4 = 0x0C;
    private static final int INPT5 = 0x0D;

    private static final int VSYNC = 0x00;
    private static final int VBLANK = 0x01;
    private static final int WSYNC = 0x02;
    private static final int RSYNC = 0x03;
    private static final int NUSIZ0 = 0x04;
    private static final int NUSIZ1 = 0x05;
    private static final int COLUP0 = 0x06;
    private static final int COLUP1 = 0x07;
    private static final int COLUPF = 0x08;
    private static final int COLUBK = 0x09;
    private static final int CTRLPF = 0x0A;
    private static final int REFP0 = 0x0B;
    private static final int REFP1 = 0x0C;
    private static final int PF0 = 0x0D;
    private static final int PF1 = 0x0E;
    private static final int PF2 = 0x0F;
    private static final int RESP0 = 0x10;
    private static final int RESP1 = 0x11;
    private static final int RESM0 = 0x12;
    private static final int RESM1 = 0x13;
    private static final int RESBL = 0x14;
    private static final int AUDC0 = 0x15;
    private static final int AUDC1 = 0x16;
    private static final int AUDF0 = 0x17;
    private static final int AUDF1 = 0x18;
    private static final int AUDV0 = 0x19;
    private static final int AUDV1 = 0x1A;
    private static final int GRP0 = 0x1B;
    private static final int GRP1 = 0x1C;
    private static final int ENAM0 = 0x1D;
    private static final int ENAM1 = 0x1E;
    private static final int ENABL = 0x1F;
    private static final int HMP0 = 0x20;
    private static final int HMP1 = 0x21;
    private static final int HMM0 = 0x22;
    private static final int HMM1 = 0x23;
    private static final int HMBL = 0x24;
    private static final int VDELP0 = 0x25;
    private static final int VDELP1 = 0x26;
    private static final int VDELBL = 0x27;
    private static final int RESMP0 = 0x28;
    private static final int RESMP1 = 0x29;
    private static final int HMOVE = 0x2A;
    private static final int HMCLR = 0x2B;
    private static final int CXCLR = 0x2C;

    private final E emulator;
    private final Video video;
    private final Audio audio;

    private int audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;

    public TIA(E emulator) {
        this.emulator = emulator;
        this.video = new Video(emulator);
        this.audio = new Audio(emulator);
    }

    public Video getVideo() {
        return this.video;
    }

    public Audio getAudio() {
        return this.audio;
    }

    @Override
    public int readByte(int address) {
        address &= 0xF;
        return switch (address) {
            case CXM0P -> this.emulator.getBus().combineWithDataBus(0, 0);
            case CXM1P -> this.emulator.getBus().combineWithDataBus(0, 0);
            case CXP0FB -> this.emulator.getBus().combineWithDataBus(0, 0);
            case CXP1FB -> this.emulator.getBus().combineWithDataBus(0, 0);
            case CXM0FB -> this.emulator.getBus().combineWithDataBus(0, 0);
            case CXM1FB -> this.emulator.getBus().combineWithDataBus(0, 0);
            case CXBLPF -> this.emulator.getBus().combineWithDataBus(0, 0);
            case CXPPMM -> this.emulator.getBus().combineWithDataBus(0, 0);
            case INPT0 -> this.emulator.getBus().combineWithDataBus(0, 0);
            case INPT1 -> this.emulator.getBus().combineWithDataBus(0, 0);
            case INPT2 -> this.emulator.getBus().combineWithDataBus(0, 0);
            case INPT3 -> this.emulator.getBus().combineWithDataBus(0, 0);
            case INPT4 -> this.emulator.getBus().combineWithDataBus(0, 0);
            case INPT5 -> this.emulator.getBus().combineWithDataBus(0, 0);
            default -> this.emulator.getBus().combineWithDataBus(0, 0x00);
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x3F;
        switch (address) {
            case VSYNC -> {}
            case VBLANK -> {}
            case WSYNC -> {}
            case RSYNC -> {}
            case NUSIZ0 -> {}
            case NUSIZ1 -> {}
            case COLUP0 -> {}
            case COLUP1 -> {}
            case COLUPF -> {}
            case COLUBK -> {}
            case CTRLPF -> {}
            case REFP0 -> {}
            case REFP1 -> {}
            case PF0 -> {}
            case PF1 -> {}
            case PF2 -> {}
            case RESP0 -> {}
            case RESP1 -> {}
            case RESM0 -> {}
            case RESM1 -> {}
            case RESBL -> {}
            case AUDC0 -> {}
            case AUDC1 -> {}
            case AUDF0 -> {}
            case AUDF1 -> {}
            case AUDV0 -> {}
            case AUDV1 -> {}
            case GRP0 -> {}
            case GRP1 -> {}
            case ENAM0 -> {}
            case ENAM1 -> {}
            case ENABL -> {}
            case HMP0 -> {}
            case HMP1 -> {}
            case HMM0 -> {}
            case HMM1 -> {}
            case HMBL -> {}
            case VDELP0 -> {}
            case VDELP1 -> {}
            case VDELBL -> {}
            case RESMP0 -> {}
            case RESMP1 -> {}
            case HMOVE -> {}
            case HMCLR -> {}
            case CXCLR -> {}
        }
    }

    public void cycle() {
        this.video.clock();
        this.video.clock();
        this.video.clock();

        this.audioClockDivisor--;
        if (this.audioClockDivisor <= 0) {
            this.audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;
            this.audio.clock();
        }
    }

    public boolean getRDYSignal() {
        return this.video.getRDYSignal();
    }

    public class Video implements VideoGenerator {

        private static final int[] TIA_PALETTE = {
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

        private static final int NTSC_SCANLINES_PER_FRAME = 262;
        private static final int NTSC_VBLANK_SCANLINES = 40;
        private static final int NTSC_KERNAL_SCANLINES = 192;
        private static final int NTSC_OVERSCAN_SCANLINES = 30;

        private static final int PAL_SCANLINES_PER_FRAME = 312;
        private static final int PAL_VBLANK_SCANLINES = 48;
        private static final int PAL_KERNAL_SCANLINES = 228;
        private static final int PAL_OVERSCAN_SCANLINES = 36;

        private static final int VSYNC_SCANLINES = 3;

        private static final int CLOCKS_PER_SCANLINE = 228;
        private static final int HBLANK_CLOCKS = 68;
        private static final int VISIBLE_CLOCKS = 160;

        private final int scanlinesPerFrame;
        private final int kernalScanlines;
        private final int vblankEndScanline;
        private final int kernalEndScanline;
        private final int overscanEndScanline;

        private final int[] video;

        private int scanlineNumber;
        private int clockNumber;

        public Video(E emulator) {
            this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
            this.kernalScanlines = NTSC_KERNAL_SCANLINES;
            this.vblankEndScanline = NTSC_VBLANK_SCANLINES;
            this.kernalEndScanline = this.vblankEndScanline + this.kernalScanlines;
            this.overscanEndScanline = this.kernalEndScanline + NTSC_OVERSCAN_SCANLINES;

            this.video = new int[this.getImageWidth() * this.getImageHeight()];
        }

        @Override
        public int getImageWidth() {
            return VISIBLE_CLOCKS;
        }

        @Override
        public int getImageHeight() {
            return this.kernalScanlines;
        }

        private boolean getRDYSignal() {
            return false;
        }

        private void clock() {


            this.clockNumber++;
            if (this.clockNumber >= CLOCKS_PER_SCANLINE) {
                this.clockNumber = 0;
                this.scanlineNumber++;
                if (this.scanlineNumber >= scanlinesPerFrame) {
                    this.scanlineNumber = 0;

                }
            }
        }

    }

    public class Audio implements AudioGenerator {

        public Audio(E emulator) {

        }

        @Override
        public boolean isStereo() {
            return false;
        }

        @Override
        public @NotNull SampleSize getBytesPerSample() {
            return SampleSize.BYTES_2;
        }

        @Override
        public Optional<AudioGenerator.SampleFrame> getSampleFrame() {
            return Optional.empty();
        }

        @Override
        public SampleFrameResampler getSampleFrameResampler() {
            return null;
        }

        private void clock() {

        }

        private interface SampleFrame extends AudioGenerator.SampleFrame {

        }

    }

}
