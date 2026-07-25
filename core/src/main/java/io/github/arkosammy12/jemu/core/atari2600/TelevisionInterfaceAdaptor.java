package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.util.ActionSignalDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public class TelevisionInterfaceAdaptor<E extends Atari2600Emulator & TelevisionInterfaceAdaptor.SystemBus> implements Bus, VideoGenerator, AudioGenerator {

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

    public TelevisionInterfaceAdaptor(E emulator, int samplesPerFrame) {
        this.emulator = emulator;
        this.video = new Video(emulator);
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

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return this.video.mapToRGB8(frameBufferValue);
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
            this.sampleBuffer[this.currentSampleIndex] = (ch0 + ch1) / 30.0;
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

            private int frequencyDivisorCounter;
            private int pulseCounter;
            private int noiseCounter;

            private void setControl(int value) {
                this.control = value & 0xF;
            }

            private void setFrequency(int value) {
                this.frequency = value & 0x1F;
            }

            private void setVolume(int value) {
                this.volume = value & 0xF;
            }

            // The following implementation of the feedback logic for both LFSRs has been derived from the Atari 2600 emulator Stella,
            // using the source file https://github.com/stella-emu/stella/blob/master/src/emucore/tia/AudioChannel.cxx,
            // itself based on Christian Speckner's 6502.ts (https://github.com/6502ts/6502.ts/blob/master/src/machine/stella/tia/PCMChannel.ts).
            // Many thanks to Stephen Anthony and Christian Speckner for letting me borrow their implementation.
            private void clock() {
                if (this.frequencyDivisorCounter == this.frequency) {
                    this.frequencyDivisorCounter = 0;

                    // Corresponds to bit 0 of the 5-bit poly counter
                    boolean nineBitPolyBit4 = (this.noiseCounter & 1) != 0;

                    switch (this.control & 0b11) {
                        case 0, 1 -> this.holdPulseCounter = false;
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
                } else {
                    this.frequencyDivisorCounter = (this.frequencyDivisorCounter + 1) & 0x1F;
                }
            }

            private int getDigitalOutput() {
                return (this.pulseCounter & 1) * this.volume;
            }

        }

    }

    private class Video implements VideoGenerator, Bus {

        private static final int[] NTSC_PALETTE = {
                0x000000, 0x4A4A4A, 0x6F6F6F, 0x8E8E8E, 0xAAAAAA, 0xC0C0C0, 0xD6D6D6, 0xECECEC,
                0x484800, 0x69690F, 0x86861D, 0xA2A22A, 0xBBBB35, 0xD2D240, 0xE8E84A, 0xFCFC54,
                0x7C2C00, 0x904811, 0xA26221, 0xB47A30, 0xC3903D, 0xD2A44A, 0xDFB755, 0xECC860,
                0x901C00, 0xA33915, 0xB55328, 0xC66C3A, 0xD5824A, 0xE39759, 0xF0AA67, 0xFCBC74,
                0x940000, 0xA71A1A, 0xB83232, 0xC84848, 0xD65C5C, 0xE46F6F, 0xF08080, 0xFC9090,
                0x840064, 0x97197A, 0xA8308F, 0xB846A2, 0xC659B3, 0xD46CC3, 0xE07CD2, 0xEC8CE0,
                0x500084, 0x68199A, 0x7D30AD, 0x9246C0, 0xA459D0, 0xB56CE0, 0xC57CEE, 0xD48CFC,
                0x140090, 0x331AA3, 0x4E32B5, 0x6848C6, 0x7F5CD5, 0x956FE3, 0xA980F0, 0xBC90FC,
                0x000094, 0x181AA7, 0x2D32B8, 0x4248C8, 0x545CD6, 0x656FE4, 0x7580F0, 0x8490FC,
                0x001C88, 0x183B9D, 0x2D57B0, 0x4272C2, 0x548AD2, 0x65A0E1, 0x75B5EF, 0x84C8FC,
                0x003064, 0x185080, 0x2D6D98, 0x4288B0, 0x54A0C5, 0x65B7D9, 0x75CCEB, 0x84E0FC,
                0x004030, 0x18624E, 0x2D8169, 0x429E82, 0x54B899, 0x65D1AE, 0x75E7C2, 0x84FCD4,
                0x004400, 0x1A661A, 0x328432, 0x48A048, 0x5CBA5C, 0x6FD26F, 0x80E880, 0x90FC90,
                0x143C00, 0x355F18, 0x527E2D, 0x6E9C42, 0x87B754, 0x9ED065, 0xB4E775, 0xC8FC84,
                0x303800, 0x505916, 0x6D762B, 0x88923E, 0xA0AB4F, 0xB7C25F, 0xCCD86E, 0xE0EC7C,
                0x482C00, 0x694D14, 0x866A26, 0xA28638, 0xBB9F47, 0xD2B656, 0xE8CC63, 0xFCE070,
        };

        private static final int[] PAL_PALETTE = {
                0x000000, 0x333333, 0x595959, 0x7B7B7B, 0x999999, 0xB6B6B6, 0xCFCFCF, 0xE6E6E6,
                0x0B0B0B, 0x333333, 0x595959, 0x7B7B7B, 0x999999, 0xB6B6B6, 0xCFCFCF, 0xE6E6E6,
                0x3B2400, 0x664700, 0x8B7000, 0xAC9200, 0xC5AE36, 0xDEC85E, 0xF7E27F, 0xFFF19E,
                0x004500, 0x006F00, 0x3B9200, 0x65B009, 0x85CA3D, 0xA3E364, 0xBFFC84, 0xD5FFA5,
                0x590000, 0x802700, 0xA15700, 0xBC7937, 0xD6985F, 0xEEB381, 0xFFCE9E, 0xFFDCBD,
                0x004900, 0x007200, 0x169216, 0x45AF45, 0x6BC96B, 0x8BE38B, 0xA9FBA9, 0xC5FFC5,
                0x640012, 0x890821, 0xA73D4D, 0xC26472, 0xDC8491, 0xF4A3AE, 0xFFBECA, 0xFFDAE0,
                0x003D29, 0x006A48, 0x048E63, 0x3CAA84, 0x62C5A2, 0x83DFBE, 0xA1F8D9, 0xBEFFE9,
                0x550046, 0x88006E, 0xA5318D, 0xC159AA, 0xDA7CC5, 0xF39ADF, 0xFFB9F3, 0xFFD4F6,
                0x003651, 0x005A7D, 0x117E9C, 0x429CB8, 0x68B7D2, 0x88D2EB, 0xA6EBFF, 0xC3FFFF,
                0x4C007C, 0x75009D, 0x932EB8, 0xAF57D2, 0xCA7AEB, 0xE499FF, 0xECB7FF, 0xF3D4FF,
                0x002D83, 0x003EA4, 0x2D65BF, 0x5685DA, 0x79A2F2, 0x99BFFF, 0xB7DBFF, 0xD3F5FF,
                0x220096, 0x5200B6, 0x7538CF, 0x945FE8, 0xB181FF, 0xC5A0FF, 0xD6BDFF, 0xE8DAFF,
                0x00009A, 0x241DB6, 0x504AD0, 0x746FE9, 0x928EFF, 0xB1ADFF, 0xCECAFF, 0xE9E5FF,
                0x0B0B0B, 0x333333, 0x595959, 0x7B7B7B, 0x999999, 0xB6B6B6, 0xCFCFCF, 0xE6E6E6,
                0x0B0B0B, 0x333333, 0x595959, 0x7B7B7B, 0x999999, 0xB6B6B6, 0xCFCFCF, 0xE6E6E6,
        };

        private static final int[] SECAM_PALETTE = {
                0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000,
                0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000,
                0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF,
                0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF,
                0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000,
                0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000,
                0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF,
                0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF, 0xFF00FF,
                0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00,
                0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00, 0x00FF00,
                0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF,
                0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF, 0x00FFFF,
                0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00,
                0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00,
                0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF
        };

        private static final int NTSC_SCANLINES_PER_FRAME = 262;
        private static final int NTSC_VBLANK_SCANLINES = 37;
        private static final int NTSC_KERNEL_SCANLINES = 228 - 6;
        private static final double NTSC_PAR = 12.0 / 7.0;

        private static final int PAL_SCANLINES_PER_FRAME = 312;
        private static final int PAL_VBLANK_SCANLINES = 45;
        private static final int PAL_KERNEL_SCANLINES = 274;
        private static final double PAL_PAR = 27.0 / 13.0;

        private static final int VSYNC_SCANLINES = 3;
        private static final int CLOCKS_PER_SCANLINE = 228;
        private static final int HBLANK_CLOCKS = 68;
        private static final int VISIBLE_CLOCKS = 160;

        private static final int STUFF_PHASE = 2;
        private static final int COUNT_PHASE = 2;

        private final ActionSignalDispatcher actionSignalDispatcher = new ActionSignalDispatcher();
        private final int vBlankWriteSignal;
        private final int rSyncWriteSignal;
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

        private final Player player0 = new Player();
        private final Player player1 = new Player();
        private final Missile missile0 = new Missile(player0);
        private final Missile missile1 = new Missile(player1);
        private final Ball ball = new Ball();

        private int colorClockNumber;

        private boolean hMoveLatch;
        private boolean extendHBlank;

        private int hMoveCounter = 15;
        private boolean hMoveCountdownActive = false;

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


        private Video(E emulator) {
            switch (emulator.getTVFormat()) {
                case NTSC -> {
                    this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
                    this.vBlankEndScanline = NTSC_VBLANK_SCANLINES;
                    this.kernelScanlines = NTSC_KERNEL_SCANLINES;
                    this.pixelAspectRatio = NTSC_PAR;
                    this.palette = NTSC_PALETTE;
                }
                case PAL -> {
                    this.scanlinesPerFrame = PAL_SCANLINES_PER_FRAME;
                    this.vBlankEndScanline = PAL_VBLANK_SCANLINES;
                    this.kernelScanlines = PAL_KERNEL_SCANLINES;
                    this.pixelAspectRatio = PAL_PAR;
                    this.palette = PAL_PALETTE;
                }
                case SECAM -> {
                    this.scanlinesPerFrame = PAL_SCANLINES_PER_FRAME;
                    this.vBlankEndScanline = PAL_VBLANK_SCANLINES;
                    this.kernelScanlines = PAL_KERNEL_SCANLINES;
                    this.pixelAspectRatio = PAL_PAR;
                    this.palette = SECAM_PALETTE;
                }
                case NTSC50 -> {
                    this.scanlinesPerFrame = PAL_SCANLINES_PER_FRAME;
                    this.vBlankEndScanline = PAL_VBLANK_SCANLINES;
                    this.kernelScanlines = PAL_KERNEL_SCANLINES;
                    this.pixelAspectRatio = NTSC_PAR;
                    this.palette = NTSC_PALETTE;
                }
                case PAL60 -> {
                    this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
                    this.vBlankEndScanline = NTSC_VBLANK_SCANLINES;
                    this.kernelScanlines = NTSC_KERNEL_SCANLINES;
                    this.pixelAspectRatio = PAL_PAR;
                    this.palette = PAL_PALETTE;
                }
                case SECAM60 -> {
                    this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
                    this.vBlankEndScanline = NTSC_VBLANK_SCANLINES;
                    this.kernelScanlines = NTSC_KERNEL_SCANLINES;
                    this.pixelAspectRatio = PAL_PAR;
                    this.palette = SECAM_PALETTE;
                }
                default -> throw new EmulatorException("Atari 2600 TV format %s is not supported!".formatted(emulator.getTVFormat().getName()));
            }

            this.kernelEndScanline = this.vBlankEndScanline + this.kernelScanlines;

            this.video = new int[this.getImageWidth() * this.getImageHeight()];

            this.vBlankWriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
                setDump((value & (1 << 7)) != 0);
                setLatch((value & (1 << 6)) != 0);
                this.vBlank = (value & (1 << 1)) != 0;
            });
            this.rSyncWriteSignal = this.actionSignalDispatcher.addSignal(4, _ -> this.nextScanline());
            this.reflectPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.player0.setReflectGraphics((value & (1 << 3)) != 0));
            this.reflectPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.player1.setReflectGraphics((value & (1 << 3)) != 0));
            this.playfield0WriteSignal = this.actionSignalDispatcher.addSignal(3, value -> this.playfield = ((reverseBits(value) & 0xF) << 16) | (this.playfield & 0x0FFFF));
            this.playfield1WriteSignal = this.actionSignalDispatcher.addSignal(3, value -> this.playfield = (value << 8) | (this.playfield & 0xF00FF));
            this.playfield2WriteSignal = this.actionSignalDispatcher.addSignal(3, value -> this.playfield = (reverseBits(value)) | (this.playfield & 0xFFF00));
            this.graphicsPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
                this.player0.setGraphics(value);
                this.player1.copyNewGraphicsToOld();
            });
            this.graphicsPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(1, value -> {
                this.player1.setGraphics(value);
                this.player0.copyNewGraphicsToOld();
                this.ball.copyNewEnabledToOld();
            });
            this.enableMissile0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile0.setEnabled((value & (1 << 1)) != 0));
            this.enableMissile1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile1.setEnabled((value & (1 << 1)) != 0));
            this.enableBallWriteSignal = this.actionSignalDispatcher.addSignal(1, value -> this.ball.setEnabled((value & (1 << 1)) != 0));
            this.horizontalMotionPlayer0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.player0.setHorizontalMotion(value >>> 4));
            this.horizontalMotionPlayer1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.player1.setHorizontalMotion(value >>> 4));
            this.horizontalMotionMissile0WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile0.setHorizontalMotion(value >>> 4));
            this.horizontalMotionMissile1WriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.missile1.setHorizontalMotion(value >>> 4));
            this.horizontalMotionBallWriteSignal = this.actionSignalDispatcher.addSignal(2, value -> this.ball.setHorizontalMotion(value >>> 4));
            this.applyHorizontalMotionWriteSignal = this.actionSignalDispatcher.addSignal(6, _ -> {
                this.hMoveLatch = true;
                this.hMoveCountdownActive = true;
                this.hMoveCounter = 15;
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
        public int mapToRGB8(int frameBufferValue) {
            return this.palette[frameBufferValue];
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
                case RSYNC -> {
                    this.actionSignalDispatcher.trigger(this.rSyncWriteSignal, value);
                }
                case NUSIZ0 -> {
                    this.missile0.setNumberSize(value);
                    this.player0.setNumberSize(value);
                }
                case NUSIZ1 -> {
                    this.missile1.setNumberSize(value);
                    this.player1.setNumberSize(value);
                }
                case COLUP0 -> this.colorLuminance0 = (value >>> 1) & 0x7F;
                case COLUP1 -> this.colorLuminance1 = (value >>> 1) & 0x7F;
                case COLUPF -> this.colorLuminancePlayfield = (value >>> 1) & 0x7F;
                case COLUBK -> this.colorLuminanceBackground = (value >>> 1) & 0x7F;
                case CTRLPF -> {
                    this.playfieldPriority = (value & (1 << 2)) != 0;
                    this.scoreMode = (value & (1 << 1)) != 0;
                    this.reflectPlayfield = (value & 1) != 0;
                    this.ball.setWidth(value >>> 4);
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

        private int getHSyncCounter() {
            return (this.colorClockNumber & 0xFD) >>> 2;
        }

        private int getHSyncPhase() {
            return this.colorClockNumber & 0b11;
        }

        private void cycle() {

            this.actionSignalDispatcher.tick();

            if (this.colorClockNumber == 0) {
                this.wSync = false;
            } else if (this.colorClockNumber == 64 && this.hMoveLatch) {
                this.extendHBlank = true;
            }

            this.player0.tick();
            this.player1.tick();
            this.missile0.tick();
            this.missile1.tick();
            this.ball.tick();

            if (this.getHSyncPhase() == COUNT_PHASE) {
                if (this.hMoveCountdownActive) {
                    int oldHMoveCounter = this.hMoveCounter;
                    this.hMoveCounter = (this.hMoveCounter - 1) & 0xF;
                    if (this.hMoveCounter > oldHMoveCounter) {
                        this.hMoveCountdownActive = false;
                    }
                }
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
                if (this.isHBlank() || this.vBlank || this.vSync) {
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
                this.video[((this.scanlineNumber - this.vBlankEndScanline) * VISIBLE_CLOCKS + pixelX)] = colorIndex;
            }


            this.colorClockNumber++;
            if (this.colorClockNumber >= CLOCKS_PER_SCANLINE) {
                this.nextScanline();
            }
        }

        private void nextScanline() {
            this.colorClockNumber = 0;
            this.scanlineNumber++;
            if (this.scanlineNumber >= this.scanlinesPerFrame) {
                this.scanlineNumber = 0;
            }
            this.hMoveLatch = false;
            this.extendHBlank = false;
        }

        private boolean getPlayfieldPixel() {
            int hSyncCounter = this.getHSyncCounter();
            if (hSyncCounter < 17) {
                return false;
            }
            int playfieldBit;
            int playfieldColumn = hSyncCounter - 17;
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
            return this.colorClockNumber < HBLANK_CLOCKS || (this.extendHBlank && this.colorClockNumber < HBLANK_CLOCKS + 8);
        }

        private static int reverseBits(int b) {
            b &= 0xFF; // Assuming 8-bit number
            b = (b & 0xF0) >> 4 | (b & 0x0F) << 4;
            b = (b & 0xCC) >> 2 | (b & 0x33) << 2;
            b = (b & 0xAA) >> 1 | (b & 0x55) << 1;
            return b;
        }

        private abstract class Sprite {

            protected static final int POSITION_COUNTER_DIVISOR = 4;

            private int horizontalMotion;
            public boolean hMoving;

            protected int positionCounterPhase;
            protected int positionCounter;

            protected int startCounter;
            protected int scanCounter;
            protected int scanCounterIncrementDivisorCounter;

            protected boolean pixel;

            protected void resetHorizontalPosition() {
                this.positionCounterPhase = isHBlank() ? 2 : 0;
                this.positionCounter = 0;
            }

            protected void setHorizontalMotion(int value) {
                this.horizontalMotion = (value & 0xF) ^ 8;
            }

            protected void applyHorizontalMotion() {
                this.hMoving = true;
            }

            protected void clearHorizontalMotion() {
                this.horizontalMotion = 8;
            }

            protected void tick() {
                boolean clock = !isHBlank();
                if (getHSyncPhase() == STUFF_PHASE) {
                    if (this.hMoving) {
                        if (hMoveCounter == (this.horizontalMotion ^ 0xF)) {
                            this.hMoving = false;
                        } else {
                            clock = true;
                        }
                    }
                }
                if (clock) {
                    this.clock();
                }
            }

            protected abstract void clock();

            protected boolean getPixel() {
                return this.pixel;
            }

        }

        private class Player extends Sprite {

            private int oldGraphics;
            private int newGraphics;

            private int numberSize;

            private boolean verticalDelay;
            private boolean reflectGraphics;

            private boolean drawingCopy;

            private Player() {
                this.scanCounter = 8;
            }

            private void setGraphics(int graphics) {
                this.newGraphics = graphics & 0xFF;
            }

            private void setNumberSize(int value) {
                this.numberSize = value & 0b111;
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
                this.positionCounterPhase++;
                if (this.positionCounterPhase >= POSITION_COUNTER_DIVISOR) {
                    this.positionCounterPhase = 0;

                    boolean firstCopy = this.numberSize == 1 || this.numberSize == 3;
                    boolean secondCopy = this.numberSize == 2 || this.numberSize == 3 || this.numberSize == 6;
                    boolean thirdCopy = this.numberSize == 4 || this.numberSize == 6;

                    if (firstCopy && this.positionCounter == 3) {
                        this.drawingCopy = true;
                        this.start();
                    } else if (secondCopy && this.positionCounter == 7) {
                        this.drawingCopy = true;
                        this.start();
                    } else if (thirdCopy && this.positionCounter == 15) {
                        this.drawingCopy = true;
                        this.start();
                    } else if (this.positionCounter == 39) {
                        this.start();
                    }

                    this.positionCounter++;
                    if (this.positionCounter >= 40) {
                        this.drawingCopy = false;
                        this.positionCounter = 0;
                    }
                }

                if (this.startCounter > 0) {
                    this.startCounter--;
                    if (this.startCounter <= 0) {
                        this.scanCounter = 0;
                        this.scanCounterIncrementDivisorCounter = 0;
                    }
                }

                this.pixel = false;

                if (this.scanCounter < 8) {
                    int graphics = this.verticalDelay ? this.oldGraphics : this.newGraphics;
                    int bit = this.reflectGraphics ? this.scanCounter : (7 - this.scanCounter);
                    this.pixel = (graphics & (1 << bit)) != 0;

                    this.scanCounterIncrementDivisorCounter++;
                    if (this.scanCounterIncrementDivisorCounter >= this.getWidth()) {
                        this.scanCounter++;
                        this.scanCounterIncrementDivisorCounter = 0;
                    }
                }

            }

            protected boolean isDrawingMiddleOfMainCopy() {
                return this.scanCounter == 4 && !this.drawingCopy;
            }

            private void start() {
                this.startCounter = this.getWidth() > 1 ? 8 : 7;
            }

            private int getWidth() {
                return switch (this.numberSize) {
                    case 5 -> 2;
                    case 7 -> 4;
                    default -> 1;
                };
            }

        }

        private class Missile extends Sprite {

            private final Player associatedPlayer;

            private int number;
            private int width;

            private boolean enabled;
            private boolean resetToPlayer;

            private Missile(Player associatedPlayer) {
                this.associatedPlayer = associatedPlayer;
                this.scanCounter = 1;
            }

            protected void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            private void setNumberSize(int value) {
                this.number = value & 0b111;
                this.width = (value >>> 4) & 0b11;
            }

            protected void resetToPlayer(boolean resetToPlayer) {
                this.resetToPlayer = resetToPlayer;
            }

            @Override
            protected void clock() {
                this.positionCounterPhase++;
                if (this.positionCounterPhase >= POSITION_COUNTER_DIVISOR) {
                    this.positionCounterPhase = 0;

                    boolean firstCopy = this.number == 1 || this.number == 3;
                    boolean secondCopy = this.number == 2 || this.number == 3 || this.number == 6;
                    boolean thirdCopy = this.number == 4 || this.number == 6;

                    if (firstCopy && this.positionCounter == 3) {
                        this.start();
                    } else if (secondCopy && this.positionCounter == 7) {
                        this.start();
                    } else if (thirdCopy && this.positionCounter == 15) {
                        this.start();
                    } else if (this.positionCounter == 39) {
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
                        this.scanCounter = 0;
                        this.scanCounterIncrementDivisorCounter = 0;
                    }
                }

                this.pixel = false;

                if (this.scanCounter < 1) {

                    this.pixel = !this.resetToPlayer && this.enabled;

                    this.scanCounterIncrementDivisorCounter++;
                    if (this.scanCounterIncrementDivisorCounter >= this.getWidth()) {
                        this.scanCounter++;
                        this.scanCounterIncrementDivisorCounter = 0;
                    }

                }

                if (this.resetToPlayer && this.associatedPlayer.isDrawingMiddleOfMainCopy()) {
                    switch (this.associatedPlayer.getWidth()) {
                        case 2 -> {
                            this.positionCounter = 1;
                            this.positionCounterPhase = 3;
                        }
                        case 4 -> {
                            this.positionCounter = 3;
                            this.positionCounterPhase = 1;
                        }
                        default -> {
                            this.positionCounter = 1;
                            this.positionCounterPhase = 0;
                        }
                    }
                }
            }

            private void start() {
                this.startCounter = 6;
            }

            private int getWidth() {
                return 1 << this.width;
            }

        }

        private class Ball extends Sprite {

            private int width;

            private boolean oldEnabled;
            private boolean newEnabled;

            private boolean verticalDelay;

            private Ball() {
                this.scanCounter = 1;
            }

            protected void setEnabled(boolean enabled) {
                this.newEnabled = enabled;
            }

            private void setWidth(int value) {
                this.width = value & 0b11;
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
                this.positionCounterPhase++;
                if (this.positionCounterPhase >= POSITION_COUNTER_DIVISOR) {
                    this.positionCounterPhase = 0;

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
                        this.scanCounter = 0;
                        this.scanCounterIncrementDivisorCounter = 0;
                    }
                }

                this.pixel = false;

                if (this.scanCounter < 1) {

                    this.pixel = this.verticalDelay ? this.oldEnabled : this.newEnabled;

                    this.scanCounterIncrementDivisorCounter++;
                    if (this.scanCounterIncrementDivisorCounter >= this.getWidth()) {
                        this.scanCounter++;
                        this.scanCounterIncrementDivisorCounter = 0;
                    }

                }

            }

            private void start() {
                this.startCounter = 6;
            }

            private int getWidth() {
                return 1 << this.width;
            }

        }

    }

}
