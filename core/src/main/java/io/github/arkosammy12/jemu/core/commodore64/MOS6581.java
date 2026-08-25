package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.util.HighPassFilter;
import io.github.arkosammy12.jemu.core.util.LowPassFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

/// Implementation of analog stage and certain parts of the digital logic for voices
/// derived from the [sidera](https://docs.rs/crate/sidera/1.1.0/source/SID_ANALOG_SPEC.md) Commodore 64 SID emulator project.
public class MOS6581<E extends Commodore64Emulator> implements AudioGenerator, Bus {

    private static final int VOICE_1_FREQ_LO = 0x00;
    private static final int VOICE_1_FREQ_HI = 0x01;
    private static final int VOICE_1_PW_LO = 0x02;
    private static final int VOICE_1_PW_HI = 0x03;
    private static final int VOICE_1_CONTROL = 0x04;
    private static final int VOICE_1_ATTACK_DECAY = 0x05;
    private static final int VOICE_1_SUSTAIN_RELEASE = 0x06;
    private static final int VOICE_2_FREQ_LO = 0x07;
    private static final int VOICE_2_FREQ_HI = 0x08;
    private static final int VOICE_2_PW_LO = 0x09;
    private static final int VOICE_2_PW_HI = 0x0A;
    private static final int VOICE_2_CONTROL = 0x0B;
    private static final int VOICE_2_ATTACK_DECAY = 0x0C;
    private static final int VOICE_2_SUSTAIN_RELEASE = 0x0D;
    private static final int VOICE_3_FREQ_LO = 0x0E;
    private static final int VOICE_3_FREQ_HI = 0x0F;
    private static final int VOICE_3_PW_LO = 0x10;
    private static final int VOICE_3_PW_HI = 0x11;
    private static final int VOICE_3_CONTROL = 0x12;
    private static final int VOICE_3_ATTACK_DECAY = 0x13;
    private static final int VOICE_3_SUSTAIN_RELEASE = 0x14;
    private static final int FC_LO = 0x15;
    private static final int FC_HI = 0x16;
    private static final int RES_FILT = 0x17;
    private static final int MODE_VOL = 0x18;
    private static final int POTX = 0x19;
    private static final int POTY = 0x1A;
    private static final int OSC3 = 0x1B;
    private static final int ENV3 = 0x1C;

    private static final int MIXER_DC = -453;
    private static final long W0_K = 6_588_397;
    private static final long FC_CEIL_HZ = 16_000;
    private static final int OUT_DIV = ((0xFFF * 0xFF) >> 7) * 3 * 15 * 2 / (1 << 16);
    private static final int W0_LP = 104_858;
    private static final int W0_HP = 105;
    private static final int[][] PTS = {
            {0, 220},
            {128, 235},
            {256, 250},
            {384, 300},
            {512, 430},
            {640, 760},
            {768, 1600},
            {896, 3200},
            {1023, 6000},
            {1024, 4600},
            {1152, 7000},
            {1408, 12000},
            {1664, 16000},
            {2047, 18000},
    };

    private final E emulator;
    private final Voice1 voice1 = new Voice1();
    private final Voice2 voice2 = new Voice2();
    private final Voice3 voice3 = new Voice3();

    private final SampleFrameResampler sampleFrameResampler;
    private final short[] sampleBuffer;
    private int currentSampleIndex;

    private int filterCutoff; // 11 bits
    private int filterResonance; // 4 bits
    private boolean filterVoice1;
    private boolean filterVoice2;
    private boolean filterVoice3;
    private boolean filterExternal;
    private int volume;
    private boolean selectLowPassOutput;
    private boolean selectBandPassOutput;
    private boolean selectHighPassOutput;
    private boolean disableVoice3DirectOutput;

    private int vlp;
    private int vbp;
    private int vhp;

    private long w0Ceil;
    private long q1024;

    private int externalVlp;
    private int externalVhp;
    private long hpRemainder;

    public MOS6581(E emulator, int samplesPerFrame) {
        this.emulator = emulator;
        this.sampleBuffer = new short[samplesPerFrame];
        this.sampleFrameResampler = new SampleFrameResampler() {

            private final LowPassFilter lpf = new LowPassFilter();
            private final HighPassFilter hpf = new HighPassFilter();

            {
                this.lpf.createLpf(16000.0, samplesPerFrame * emulator.getFramerate());
            }

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(short[] sampleFrame)) {
                    /*
                    for (int i = 0; i < sampleFrame.length; i++) {
                        sampleFrame[i] = (short) Math.clamp(Math.round(this.lpf.process(sampleFrame[i])), Short.MIN_VALUE, Short.MAX_VALUE);
                    }
                     */
                    this.hpf.createHpf(16.0, outputSampleRate);

                    byte[] out = new byte[outputSamplesPerFrame * 2];
                    double step = (double) sampleFrame.length / (double) outputSamplesPerFrame;
                    double pos = 0.0;

                    for (int i = 0; i < outputSamplesPerFrame; i++) {
                        int index = Math.min((int) Math.round(pos), sampleFrame.length - 1);
                        short sample = sampleFrame[index];
                        //short sample = (short) Math.clamp(Math.round(this.hpf.process(sampleFrame[index])), Short.MIN_VALUE, Short.MAX_VALUE);
                        out[i * 2] = (byte) ((int) sample & 0xFF);
                        out[i * 2 + 1] = (byte) (((int) sample >>> 8) & 0xFF);
                        pos += step;
                    }

                    return Optional.of(out);
                } else {
                    return Optional.empty();
                }
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
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SampleFrame(Arrays.copyOf(this.sampleBuffer, this.sampleBuffer.length)));
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.sampleFrameResampler;
    }

    @Override
    public int readByte(int address) {
        // TODO: Unused bits and addresses return decaying open bus mutated by SID register accesses, between 4K and 5K cycles
        address &= 0x1F;
        return switch (address) {
            case POTX -> 0;
            case POTY -> 0;
            case OSC3 -> this.voice3.readOSC3();
            case ENV3 -> this.voice3.readENV3();
            default -> 0;
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x1F;
        switch (address) {
            case VOICE_1_FREQ_LO -> this.voice1.setFrequencyLow(value);
            case VOICE_1_FREQ_HI -> this.voice1.setFrequencyHigh(value);
            case VOICE_1_PW_LO -> this.voice1.setPulseWidthLow(value);
            case VOICE_1_PW_HI -> this.voice1.setPulseWidthHigh(value);
            case VOICE_1_CONTROL -> this.voice1.setControl(value);
            case VOICE_1_ATTACK_DECAY -> this.voice1.setAttackDecay(value);
            case VOICE_1_SUSTAIN_RELEASE -> this.voice1.setSustainRelease(value);
            case VOICE_2_FREQ_LO -> this.voice2.setFrequencyLow(value);
            case VOICE_2_FREQ_HI -> this.voice2.setFrequencyHigh(value);
            case VOICE_2_PW_LO -> this.voice2.setPulseWidthLow(value);
            case VOICE_2_PW_HI -> this.voice2.setPulseWidthHigh(value);
            case VOICE_2_CONTROL -> this.voice2.setControl(value);
            case VOICE_2_ATTACK_DECAY -> this.voice2.setAttackDecay(value);
            case VOICE_2_SUSTAIN_RELEASE -> this.voice2.setSustainRelease(value);
            case VOICE_3_FREQ_LO -> this.voice3.setFrequencyLow(value);
            case VOICE_3_FREQ_HI -> this.voice3.setFrequencyHigh(value);
            case VOICE_3_PW_LO -> this.voice3.setPulseWidthLow(value);
            case VOICE_3_PW_HI -> this.voice3.setPulseWidthHigh(value);
            case VOICE_3_CONTROL -> this.voice3.setControl(value);
            case VOICE_3_ATTACK_DECAY -> this.voice3.setAttackDecay(value);
            case VOICE_3_SUSTAIN_RELEASE -> this.voice3.setSustainRelease(value);
            case FC_LO -> {
                this.filterCutoff = (this.filterCutoff & 0b11111111000) | (value & 0b111);
                this.updateW0();
            }
            case FC_HI -> {
                this.filterCutoff = ((value & 0xFF) << 3) | (this.filterCutoff & 0b00000000111);
                this.updateW0();
            }
            case RES_FILT -> {
                this.filterVoice1 = (value & 1) != 0;
                this.filterVoice2 = (value & (1 << 1)) != 0;
                this.filterVoice3 = (value & (1 << 2)) != 0;
                this.filterExternal = (value & (1 << 3)) != 0;
                this.filterResonance = (value >>> 4) & 0xF;
                this.updateQ();
            }
            case MODE_VOL -> {
                this.volume = value & 0xF;
                this.selectLowPassOutput = (value & (1 << 4)) != 0;
                this.selectBandPassOutput = (value & (1 << 5)) != 0;
                this.selectHighPassOutput = (value & (1 << 6)) != 0;
                this.disableVoice3DirectOutput = (value & (1 << 7)) != 0;
            }
        }
    }

    public void cycle() {
        this.voice1.cycle();
        this.voice2.cycle();
        this.voice3.cycle();

        this.voice1.checkSync();
        this.voice2.checkSync();
        this.voice3.checkSync();

        this.voice1.clockEnvelope();
        this.voice2.clockEnvelope();
        this.voice3.clockEnvelope();

        int v1 = this.voice1.getOutput() >>> 7;
        int v2 = this.voice2.getOutput() >>> 7;
        int v3;
        if (this.disableVoice3DirectOutput && !this.filterVoice3) {
            v3 = 0;
        } else {
            v3 = this.voice3.getOutput() >>> 7;
        }

        int vi = 0;
        int vnf = 0;
        if (this.filterVoice1) {
            vi += v1;
        } else {
            vnf += v1;
        }

        if (this.filterVoice2) {
            vi += v2;
        } else {
            vnf += v2;
        }

        if (this.filterVoice3) {
            vi += v3;
        } else {
            vnf += v3;
        }
        int vnf1 = vnf;

        int dVbp = (int) ((this.w0Ceil * this.vhp) >> 20);
        int dVlp = (int) ((this.w0Ceil * this.vbp) >> 20);
        this.vbp -= dVbp;
        this.vlp -= dVlp;
        this.vhp = (int) (((long) this.vbp * this.q1024) >> 10) - this.vlp - vi;

        int vf = 0;
        if (this.selectLowPassOutput) {
            vf += this.vlp;
        }
        if (this.selectBandPassOutput) {
            vf += this.vbp;
        }
        if (this.selectHighPassOutput) {
            vf += this.vhp;
        }
        int mixedFilterOutput = (vnf1 + vf + MIXER_DC) * this.volume;

        int dVlpExternal = ((W0_LP >>> 8) * (mixedFilterOutput - this.externalVlp)) >> 12;
        long num = this.hpRemainder + (long) W0_HP * (this.externalVlp - this.externalVhp);
        long dVhpExternal = num >> 20;
        this.hpRemainder = num - (dVhpExternal << 20);
        int externalVo = this.externalVlp - this.externalVhp;
        this.externalVlp += dVlpExternal;
        this.externalVhp += (int) dVhpExternal;

        this.sampleBuffer[this.currentSampleIndex] = (short) Math.clamp(externalVo / OUT_DIV, Short.MIN_VALUE, Short.MAX_VALUE);
        this.currentSampleIndex = (this.currentSampleIndex + 1) % this.sampleBuffer.length;
    }

    private void updateW0() {
        long f0 = this.getCutoffHz(this.filterCutoff);
        long f0Ceil = Math.min(f0, FC_CEIL_HZ);
        this.w0Ceil = f0Ceil * W0_K / 1_000_000;
    }

    private void updateQ() {
        long qMilli = 707 + (1000L * this.filterResonance) / 15;
        this.q1024 = 1_024_000 / qMilli;
    }

    private long getCutoffHz(int filterCutoff) {
        int i = 0;
        while (i + 1 < PTS.length && PTS[i + 1][0] <= filterCutoff) {
            i++;
        }
        if (i + 1 >= PTS.length) {
            return PTS[PTS.length - 1][1];
        }
        int x0 = PTS[i][0];
        int y0 = PTS[i][1];
        int x1 = PTS[i + 1][0];
        int y1 = PTS[i + 1][1];
        if (x1 == x0) {
            return y1;
        }
        return y0 + (long) (y1 - y0) * (filterCutoff - x0) / (x1 - x0);
    }

    private record SampleFrame(short[] sampleFrame) implements AudioGenerator.SampleFrame {}

    private abstract static class Voice {

        private static final int WAVEFORM_ZERO = 0x380;
        private static final int DC_OFFSET = 0x800 * 0xFF;

        private static final int[] ADSR_RATES_LUT = {
                9, 32, 63, 95, 149, 220, 267, 313, 392, 977, 1954, 3126, 3907, 11720, 19532, 31251
        };

        private int frequency; // 16 bits
        private int pulseWidth; // 12 bits
        private boolean gate;
        private boolean sync;
        private boolean ringModulate;
        private boolean test;
        private boolean selectTriangle;
        private boolean selectSawtooth;
        private boolean selectPulse;
        private boolean selectNoise;
        private int decay; // 4 bits
        private int attack; // 4 bits
        private int release; // 4 bits
        private int sustain; // 4 bits

        protected int oscillator; // 24 bits;
        private int noiseLFSR = 0x7FFFF8; // 23 bits
        private boolean oscillatorMSBRisingEdge;

        protected int envelopeCounter; // 8 bits;
        private boolean envelopeCountDirection;
        private boolean envelopeCounterEnable;
        private int envelopeRateCounter; // 15 bits
        private int exponentialPeriod = 1;
        private int envelopeExponentialCounter; // 5 bits

        protected void setFrequencyLow(int value) {
            this.frequency = (this.frequency & 0xFF00) | (value & 0xFF);
        }

        protected void setFrequencyHigh(int value) {
            this.frequency = ((value & 0xFF) << 8) | (this.frequency & 0x00FF);
        }

        protected void setPulseWidthLow(int value) {
            this.pulseWidth = (this.pulseWidth & 0xF00) | (value & 0xFF);
        }

        protected void setPulseWidthHigh(int value) {
            this.pulseWidth = ((value & 0xF) << 8) | (this.pulseWidth & 0x0FF);
        }

        protected void setControl(int value) {
            boolean oldGate = this.gate;
            boolean oldTest = this.test;
            this.gate = (value & 1) != 0;
            this.sync = (value & (1 << 1)) != 0;
            this.ringModulate = (value & (1 << 2)) != 0;
            this.test = (value & (1 << 3)) != 0;
            this.selectTriangle = (value & (1 << 4)) != 0;
            this.selectSawtooth = (value & (1 << 5)) != 0;
            this.selectPulse = (value & (1 << 6)) != 0;
            this.selectNoise = (value & (1 << 7)) != 0;

            if (this.test) {
                this.oscillator = 0;
                this.noiseLFSR = 0;
            } else if (oldTest) {
                this.noiseLFSR = 0x7FFFF8;
            }

            if (!oldGate && this.gate) {
                this.envelopeCountDirection = true;
                this.envelopeCounterEnable = true;
            } else if (oldGate && !this.gate) {
                this.envelopeCountDirection = false;
            }
        }

        protected void setAttackDecay(int value) {
            this.decay = value & 0xF;
            this.attack = (value >>> 4) & 0xF;
        }

        protected void setSustainRelease(int value) {
            this.release = value & 0xF;
            this.sustain = (value >>> 4) & 0xF;
        }

        abstract protected Voice getSyncRingModeSource();

        protected void cycle() {
            boolean oldBit19 = (this.oscillator & (1 << 19)) != 0;
            boolean oldBit23 = (this.oscillator & (1 << 23)) != 0;
            if (!this.test) {
                this.oscillator = (this.oscillator + this.frequency) & 0xFFFFFF;
            }

            if (!oldBit19 && (this.oscillator & (1 << 19)) != 0) {
                int bit0 = ((this.noiseLFSR >>> 22) ^ (this.noiseLFSR >>> 17)) & 1;
                this.noiseLFSR = ((this.noiseLFSR << 1) | bit0) & 0x7FFFFF;
            }

            this.oscillatorMSBRisingEdge = !oldBit23 && (this.oscillator & (1 << 23)) != 0;
        }

        protected void checkSync() {
            Voice source = this.getSyncRingModeSource();
            if (this.sync && source.oscillatorMSBRisingEdge && !(source.sync && source.getSyncRingModeSource().oscillatorMSBRisingEdge)) {
                this.oscillator = 0;
            }
        }

        protected void clockEnvelope() {
            this.envelopeRateCounter = (this.envelopeRateCounter + 1) & 0x7FFF;
            if (this.envelopeRateCounter == this.getEnvelopeRatePeriod()) {
                this.envelopeRateCounter = 0;
                if (this.envelopeCountDirection) {
                    this.envelopeExponentialCounter = 0;
                    if (this.envelopeCounterEnable) {
                        this.envelopeCounter = (this.envelopeCounter + 1) & 0xFF;
                        if (this.envelopeCounter == 0xFF) {
                            this.envelopeCountDirection = false;
                        }
                        this.updateEnvelopeExponentialPeriod();
                    }
                } else {
                    this.envelopeExponentialCounter = (this.envelopeExponentialCounter + 1) & 0b11111;
                    if (this.envelopeExponentialCounter == this.exponentialPeriod) {
                        this.envelopeExponentialCounter = 0;
                        if (this.envelopeCounterEnable) {
                            if (!this.gate || this.envelopeCounter != (this.sustain | (this.sustain << 4))) {
                                this.envelopeCounter = (this.envelopeCounter - 1) & 0xFF;
                            }
                            this.updateEnvelopeExponentialPeriod();
                        }
                    }
                }
            }
        }

        private void updateEnvelopeExponentialPeriod() {
            this.exponentialPeriod = switch (this.envelopeCounter) {
                case 0xFF -> 1;
                case 0x5D -> 2;
                case 0x36 -> 4;
                case 0x1A -> 8;
                case 0x0E -> 16;
                case 0x06 -> 30;
                case 0x00 -> {
                    this.envelopeCounterEnable = false;
                    yield 1;
                }
                default -> this.exponentialPeriod;
            };
        }

        protected int getOutput() {
            return (this.getWaveformSelectorOutput() - WAVEFORM_ZERO) * this.envelopeCounter + DC_OFFSET;
        }

        private int getPulseOutput() {
            return this.test || ((this.oscillator >>> 12) & 0xFFF) >= this.pulseWidth ? 0xFFF : 0x000;
        }

        private int getTriangleOutput() {
            boolean msb = (this.oscillator & (1 << 23)) != 0;
            if (this.ringModulate) {
                msb ^= (this.getSyncRingModeSource().oscillator & (1 << 23)) != 0;
            }
            return ((msb ? ~this.oscillator : this.oscillator) >>> 11) & 0xFFF;
        }

        private int getSawtoothOutput() {
            return (this.oscillator >>> 12) & 0xFFF;
        }

        private int getNoiseOutput() {
            return ((this.noiseLFSR & 0x400000) >>> 11)
                    | ((this.noiseLFSR & 0x100000) >>> 10)
                    | ((this.noiseLFSR & 0x010000) >>> 7)
                    | ((this.noiseLFSR & 0x002000) >>> 5)
                    | ((this.noiseLFSR & 0x000800) >>> 4)
                    | ((this.noiseLFSR & 0x000080) >> 1)
                    | ((this.noiseLFSR & 0x000010) << 1)
                    | ((this.noiseLFSR & 0x000004) << 2);
        }

        protected int getWaveformSelectorOutput() {
            boolean waveformSelected = this.selectTriangle || this.selectSawtooth || this.selectPulse;
            if (this.selectNoise) {
                if (waveformSelected) {
                    return 0x000;
                } else {
                    return this.getNoiseOutput();
                }
            } else if (waveformSelected) {
                int output = 0xFFF;
                if (this.selectTriangle) {
                    output &= this.getTriangleOutput();
                }
                if (this.selectSawtooth) {
                    output &= this.getSawtoothOutput();
                }
                if (this.selectPulse) {
                    output &= this.getPulseOutput();
                }
                if (this.selectNoise) {
                    output &= this.getNoiseOutput();
                }
                return output;
            } else {
                return 0x000;
            }
        }

        private int getEnvelopeRatePeriod() {
            int activeADRNibble;
            if (this.gate) {
                if (this.envelopeCountDirection) {
                    activeADRNibble = this.attack;
                } else {
                    activeADRNibble = this.decay;
                }
            } else {
                activeADRNibble = this.release;
            }
            return ADSR_RATES_LUT[activeADRNibble];
        }

    }

    private class Voice1 extends Voice {

        @Override
        protected Voice getSyncRingModeSource() {
            return voice3;
        }

    }

    private class Voice2 extends Voice {

        @Override
        protected Voice getSyncRingModeSource() {
            return voice1;
        }

    }

    private class Voice3 extends Voice {

        @Override
        protected Voice getSyncRingModeSource() {
            return voice2;
        }

        private int readOSC3() {
            return (this.getWaveformSelectorOutput() >>> 4) & 0xFF;
        }

        private int readENV3() {
            return this.envelopeCounter;
        }

    }

}
