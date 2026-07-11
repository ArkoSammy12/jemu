package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.util.ActionSignalDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public class TIA<E extends Emulator & TIA.SystemBus> implements Bus, VideoGenerator, AudioGenerator {

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

    private boolean dump;
    private boolean latchesEnabled;

    private boolean i4Latch = true;
    private boolean i5Latch = true;

    public TIA(E emulator, int samplesPerFrame) {
        this.emulator = emulator;
        this.video = new Video();
        this.audio = new Audio(samplesPerFrame);
    }

    @Override
    public boolean isStereo() {
        return this.audio.isStereo();
    }

    @Override
    public @NotNull SampleSize getBytesPerSample() {
        return this.audio.getBytesPerSample();
    }

    @Override
    public Optional<SampleFrame> getSampleFrame() {
        return this.audio.getSampleFrame();
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.audio.getSampleFrameResampler();
    }

    @Override
    public int getImageWidth() {
        return this.video.getImageWidth();
    }

    @Override
    public int getImageHeight() {
        return this.video.getImageHeight();
    }

    @Override
    public double getPixelAspectRatio() {
        return this.video.getPixelAspectRatio();
    }

    public void setI4(boolean value) {
        if (this.latchesEnabled && !value) {
            this.i4Latch = false;
        }
    }

    public void setI5(boolean value) {
        if (this.latchesEnabled && !value) {
            this.i5Latch = false;
        }
    }

    void setLatch(boolean value) {
        this.latchesEnabled = value;
        if (this.latchesEnabled) {
            // Capture the current state of the I4 and I5 input lines in case we need to clear the latches immediately to maintain continuity,
            // as these are level sensitive latches.
            if (!this.emulator.getI4()) {
                this.i4Latch = false;
            }
            if (!this.emulator.getI5()) {
                this.i5Latch = false;
            }
        } else {
            this.i4Latch = true;
            this.i5Latch = true;
        }
    }

    void setDump(boolean value) {
        this.dump = value;
    }

    @Override
    public int readByte(int address) {
        address &= 0xF;
        return switch (address) {
            case CXM0P, CXM1P, CXP0FB, CXP1FB, CXM0FB, CXM1FB, CXBLPF, CXPPMM -> this.video.readByte(address);
            case INPT0 -> this.emulator.combineWithDataBus(this.dump ? 0x00 : this.emulator.getI0() ? 0x80 : 0x00, 0x80);
            case INPT1 -> this.emulator.combineWithDataBus(this.dump ? 0x00 : this.emulator.getI1() ? 0x80 : 0x00, 0x80);
            case INPT2 -> this.emulator.combineWithDataBus(this.dump ? 0x00 : this.emulator.getI2() ? 0x80 : 0x00, 0x80);
            case INPT3 -> this.emulator.combineWithDataBus(this.dump ? 0x00 : this.emulator.getI3() ? 0x80 : 0x00, 0x80);
            case INPT4 -> this.emulator.combineWithDataBus(this.latchesEnabled ? (this.i4Latch ? 0x80 : 0x00) : this.emulator.getI4() ? 0x80 : 0x00, 0x80);
            case INPT5 -> this.emulator.combineWithDataBus(this.latchesEnabled ? (this.i5Latch ? 0x80 : 0x00) : this.emulator.getI5() ? 0x80 : 0x00, 0x80);
            default -> this.emulator.combineWithDataBus(0x00, 0x00);
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x3F;
        switch (address) {
            case VSYNC, VBLANK, WSYNC, RSYNC, NUSIZ0, NUSIZ1, COLUP0, COLUP1, COLUPF, COLUBK, CTRLPF, REFP0, REFP1,
                 PF0, PF1, PF2, RESP0, RESP1, RESM0, RESM1, RESBL, GRP0, GRP1, ENAM0, ENAM1, ENABL, HMP0, HMP1, HMM0,
                 HMM1, HMBL, VDELP0, VDELP1, VDELBL, RESMP0, RESMP1, HMOVE, HMCLR, CXCLR -> this.video.writeByte(address, value);
            case AUDC0, AUDC1, AUDF0, AUDF1, AUDV0, AUDV1 -> this.audio.writeByte(address, value);
        }
    }

    public void cycle() {
        this.video.cycle();
        this.video.cycle();
        this.video.cycle();
        this.audio.cycle();
    }

    public boolean getRDYSignal() {
        return this.video.getRDYSignal();
    }

    public interface SystemBus {

        boolean getI0();

        boolean getI1();

        boolean getI2();

        boolean getI3();

        boolean getI4();

        boolean getI5();

        int combineWithDataBus(int value, int validBitsMask);

    }

    private class Audio implements AudioGenerator, Bus {

        private static final int AUDIO_CPU_CLK_DIVISOR = 38;
        private static final double OUTPUT_GAIN = Short.MAX_VALUE;

        private final SampleFrameResampler sampleFrameResampler;

        private final AudioChannel channel0 = new AudioChannel();
        private final AudioChannel channel1 = new AudioChannel();

        private final double[] sampleBuffer;
        private int currentSampleIndex;

        private int audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;

        private Audio(int samplesPerFrame) {
            this.sampleBuffer = new double[samplesPerFrame];
            this.sampleFrameResampler = (_, outputSamplesPerFrame, inputSampleFrame) -> {
                if (inputSampleFrame instanceof SampleFrame(double[] sampleFrame)) {

                    byte[] out = new byte[outputSamplesPerFrame * 2];
                    double step = (double) sampleFrame.length / (double) outputSamplesPerFrame;
                    double pos = 0.0;

                    for (int i = 0; i < outputSamplesPerFrame; i++) {
                        int index = Math.min((int) Math.round(pos), sampleFrame.length - 1);
                        short sample = (short) Math.clamp((long)(sampleFrame[index] * OUTPUT_GAIN), -Short.MAX_VALUE, Short.MAX_VALUE);
                        out[i * 2] = (byte) ((int) sample & 0xFF);
                        out[i * 2 + 1] = (byte) (((int) sample >>> 8) & 0xFF);
                        pos += step;
                    }

                    return Optional.of(out);
                } else {
                    return Optional.empty();
                }
            };
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
            this.currentSampleIndex = 0;
            Optional<? extends AudioDriver> optionalAudioDriver = emulator.getHost().getAudioDriver();
            if (optionalAudioDriver.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SampleFrame(Arrays.copyOf(this.sampleBuffer, this.sampleBuffer.length)));
        }

        @Override
        public SampleFrameResampler getSampleFrameResampler() {
            return this.sampleFrameResampler;
        }

        private void cycle() {
            this.audioClockDivisor--;
            if (this.audioClockDivisor <= 0) {
                this.audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;
                this.channel0.clock();
                this.channel1.clock();
            }
            double ch0 = this.channel0.getDigitalOutput();
            double ch1 = this.channel1.getDigitalOutput();
            this.sampleBuffer[this.currentSampleIndex] = (ch0 + ch1) / 31.0;
            this.currentSampleIndex++;
        }

        @Override
        public int readByte(int address) {
            return emulator.combineWithDataBus(0, 0x00);
        }

        @Override
        public void writeByte(int address, int value) {
            switch (address) {
                case AUDC0 -> this.channel0.setControl(value);
                case AUDC1 -> this.channel1.setControl(value);
                case AUDF0 -> this.channel0.setFrequency(value);
                case AUDF1 -> this.channel1.setFrequency(value);
                case AUDV0 -> this.channel0.setVolume(value);
                case AUDV1 -> this.channel1.setVolume(value);
            }
        }

        private record SampleFrame(double[] sampleFrame) implements AudioGenerator.SampleFrame {}

        private static class AudioChannel {

            private int control;
            private int frequency;
            private int volume;

            private boolean holdPulseCounter;

            private int frequencyDivisorCounter = 1;
            private int pulseCounter;
            private int noiseCounter;

            private void setControl(int value) {
                this.control = value & 0xF;
            }

            private void setFrequency(int value) {
                this.frequency = (value & 0x1F) + 1;
            }

            private void setVolume(int value) {
                this.volume = value & 0xF;
            }

            // The following implementation of the feedback logic for both LFSRs has been derived from the Atari 2600 emulator Stella,
            // using the source file https://github.com/stella-emu/stella/blob/master/src/emucore/tia/AudioChannel.cxx,
            // itself based on Christian Speckner's 6502.ts (https://github.com/6502ts/6502.ts/blob/master/src/machine/stella/tia/PCMChannel.ts).
            // Many thanks to Stephen Anthony and Christian Speckner for letting me borrow their implementation.
            private void clock() {
                this.frequencyDivisorCounter--;
                if (this.frequencyDivisorCounter <= 0) {
                    this.frequencyDivisorCounter = this.frequency;

                    // Corresponds to bit 0 of the 5-bit poly counter
                    boolean nineBitPolyBit4 = (this.noiseCounter & 1) != 0;

                    switch (this.control & 0b11) {
                        case 0 -> {}
                        case 1 -> this.holdPulseCounter = false;
                        case 2 -> this.holdPulseCounter = (this.noiseCounter & 0x1E) != 2;
                        case 3 -> this.holdPulseCounter = !nineBitPolyBit4;
                    }

                    boolean noiseFeedback;
                    if ((this.control & 0b11) == 0) {
                        noiseFeedback = ((this.pulseCounter ^ this.noiseCounter) & 1) != 0 || !(this.noiseCounter != 0 || (this.pulseCounter != 0xA)) || (this.control & 0xC) == 0;
                    } else {
                        noiseFeedback = ((((this.noiseCounter & (1 << 2)) != 0) ? 1 : 0) ^ (this.noiseCounter & 1)) != 0 || this.noiseCounter == 0;
                    }

                    boolean pulseFeedback = switch ((this.control >>> 2) & 0b11) {
                        case 0 -> ((((this.pulseCounter & (1 << 1)) != 0) ? 1 : 0) ^ (this.pulseCounter & 1)) != 0 && (this.pulseCounter != 0xA) && ((this.control & 0b11) != 0);
                        case 1 -> (this.pulseCounter & (1 << 3)) == 0;
                        case 2 -> !nineBitPolyBit4;
                        case 3 -> !(((this.pulseCounter & (1 << 1)) != 0) || (this.pulseCounter & 0xE) == 0);
                        default -> false;
                    };

                    this.noiseCounter >>>= 1;
                    if (noiseFeedback) {
                        this.noiseCounter |= (1 << 4);
                    }

                    if (!this.holdPulseCounter) {
                        this.pulseCounter = ~(this.pulseCounter >>> 1) & 0b111;

                        if (pulseFeedback) {
                            this.pulseCounter |= (1 << 3);
                        }
                    }
                }
            }

            private int getDigitalOutput() {
                return (this.pulseCounter & 1) * this.volume;
            }

        }

    }

    private class Video implements VideoGenerator, Bus {

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
        private final int vBlankEndScanline;
        private final int kernelEndScanline;
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

        private int scanlineNumber;

        private boolean vSync;
        private boolean vBlank;
        private boolean wSync;

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

        private Video() {
            this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
            this.kernelScanlines = NTSC_KERNEL_SCANLINES;

            this.vBlankEndScanline = NTSC_VBLANK_SCANLINES;
            this.kernelEndScanline = this.vBlankEndScanline + this.kernelScanlines;
            this.pixelAspectRatio = NTSC_PAR;
            this.palette = TIA_NTSC_PALETTE;

            this.video = new int[this.getImageWidth() * this.getImageHeight()];

            this.vBlankWriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
                setDump((value & (1 << 7)) != 0);
                setLatch((value & (1 << 6)) != 0);
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
                case CXM0P -> emulator.combineWithDataBus(this.collisionLatchMissile0Player, 0xC0);
                case CXM1P -> emulator.combineWithDataBus(this.collisionLatchMissile1Player, 0xC0);
                case CXP0FB -> emulator.combineWithDataBus(this.collisionLatchPlayer0PlayfieldBall, 0xC0);
                case CXP1FB -> emulator.combineWithDataBus(this.collisionLatchPlayer1PlayfieldBall, 0xC0);
                case CXM0FB -> emulator.combineWithDataBus(this.collisionLatchMissile0PlayfieldBall, 0xC0);
                case CXM1FB -> emulator.combineWithDataBus(this.collisionLatchMissile1PlayfieldBall, 0xC0);
                case CXBLPF -> emulator.combineWithDataBus(this.collisionLatchBallPlayfield, 0x80);
                case CXPPMM -> emulator.combineWithDataBus(this.collisionLatchPlayerPlayerMissileMissile, 0xC0);
                default -> emulator.combineWithDataBus(0, 0x00);
            };
        }

        @Override
        public void writeByte(int address, int value) {
            switch (address) {
                case VSYNC -> {
                    boolean vSync = (value & (1 << 1)) != 0;
                    if (!this.vSync && vSync) {
                        this.scanlineNumber = 0;
                        emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.video));
                    }
                    this.vSync = vSync;
                }
                case VBLANK -> this.actionSignalDispatcher.trigger(this.vBlankWriteSignal, value);
                case WSYNC -> this.wSync = true;
                case RSYNC -> this.hSyncCounter = 0;
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

        private boolean getRDYSignal() {
            return this.wSync;
        }

        private void cycle() {

            this.actionSignalDispatcher.tick();

            if (this.colorClockNumber == 0) {
                this.wSync = false;
            }

            boolean hBlank = this.isHBlank();

            if (!hBlank) {
                this.player0.clock();
                this.player1.clock();
                this.missile0.clock();
                this.missile1.clock();
                this.ball.clock();
            }

            boolean pf = this.getPlayfieldPixel();
            boolean p0 = this.player0.getPixel();
            boolean p1 = this.player1.getPixel();
            boolean m0 = this.missile0.getPixel();
            boolean m1 = this.missile1.getPixel();
            boolean bl =  this.ball.getPixel();

            if (!this.vBlank) {
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
            }

            int pixelX = this.colorClockNumber - HBLANK_CLOCKS;
            if (this.isKernelScanline() && pixelX >= 0 && pixelX < 160) {
                int colorIndex;
                if (hBlank || this.vBlank || this.vSync) {
                    colorIndex = 0;
                } else if (this.playfieldPriority) {
                    if (bl || pf) {
                        colorIndex = this.colorLuminancePlayfield;
                    } else if (p0 || m0) {
                        colorIndex = this.colorLuminance0;
                    } else if (p1 || m1) {
                        colorIndex = this.colorLuminance1;
                    } else {
                        colorIndex = this.colorLuminanceBackground;
                    }
                } else if (this.scoreMode) {
                    if (p0 || m0 || (pf && pixelX < 80)) {
                        colorIndex = this.colorLuminance0;
                    } else if (p1 || m1 || pf) {
                        colorIndex = this.colorLuminance1;
                    } else if (bl) {
                        colorIndex = this.colorLuminancePlayfield;
                    } else {
                        colorIndex = this.colorLuminanceBackground;
                    }
                } else {
                    if (p0 || m0) {
                        colorIndex = this.colorLuminance0;
                    } else if (p1 || m1) {
                        colorIndex = this.colorLuminance1;
                    } else if (bl || pf) {
                        colorIndex = this.colorLuminancePlayfield;
                    } else {
                        colorIndex = this.colorLuminanceBackground;
                    }
                }
                this.video[((this.scanlineNumber - this.vBlankEndScanline) * VISIBLE_CLOCKS + pixelX)] = this.palette[colorIndex];
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
                }
                this.hMove = false;
                this.colorClockNumber = 0;
                this.hSyncCounter = 0;
            }
        }

        private boolean getPlayfieldPixel() {
            if (this.hSyncCounter < 17) {
                return false;
            }
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
            return this.scanlineNumber >= this.vBlankEndScanline && this.scanlineNumber < this.kernelEndScanline;
        }

        private boolean isHBlank() {
            return this.colorClockNumber < HBLANK_CLOCKS || (this.hMove && this.colorClockNumber < HBLANK_CLOCKS + 8);
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

    public enum TVFormat {
        NSTC,
        PAL,
        SECAM,
        NTSC50,
        PAL60,
        SECAM60

    }

    public enum CartridgeType {
        CART_2K,
        CART_4K,
        CART_4KSC,
        CART_F4,
        CART_F4SC,
        CART_F6,
        CART_F6SC,
        CART_F8,
        CART_F8SC,
        CART_F0,
        CART_FA,
        CART_FA2,
        CART_FE,
        CART_E0,
        CART_E7,
        CART_EF,
        CART_EFSC,
        CART_3E,
        CART_3EPLUS,
        CART_3F,
        CART_0840,
        CART_4A50,
        CART_AR,
        CART_CV,
        CART_UA,
        CART_SB,
        CART_WD,
        CART_X07,
        CART_MDM,
        CART_MVC,
        CART_BF,
        CART_BFSC,
        CART_DF,
        CART_DFSC,
        CART_DPC,
        CART_DPCPLUS,
        CART_CDF,
        CART_GL,
        CART_TVBOY

    }

}
