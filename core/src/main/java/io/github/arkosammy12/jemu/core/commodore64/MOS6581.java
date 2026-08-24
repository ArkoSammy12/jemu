package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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

    private final E emulator;
    private final Voice1 voice1 = new Voice1();
    private final Voice2 voice2 = new Voice2();
    private final Voice3 voice3 = new Voice3();

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
    private boolean disableVoice3Output;

    public MOS6581(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public boolean isStereo() {
        return false;
    }

    @Override
    public @NotNull SampleSize getBytesPerSample() {
        return SampleSize.BYTES_1;
    }

    @Override
    public Optional<? extends SampleFrame> getSampleFrame() {
        return Optional.of(new SampleFrame() {

        });
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return (outputSampleRate, outputSamplesPerFrame, inputSampleFrame) -> Optional.empty();
    }

    @Override
    public int readByte(int address) {
        // TODO: Unused bits and addresses return decaying open bus mutated by SID register accesses, between 4K and 5K cycles
        address &= 0x1F;
        return switch (address) {
            case POTX -> 0;
            case POTY -> 0;
            case OSC3 -> this.voice3.readOSC3();
            case ENV3 -> 0;
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
            case FC_LO -> this.filterCutoff = (this.filterCutoff & 0b11111111000) | (value & 0b111);
            case FC_HI -> this.filterCutoff = ((value & 0xFF) << 3) | (this.filterCutoff & 0b00000000111);
            case RES_FILT -> {
                this.filterVoice1 = (value & 1) != 0;
                this.filterVoice2 = (value & (1 << 1)) != 0;
                this.filterVoice3 = (value & (1 << 2)) != 0;
                this.filterExternal = (value & (1 << 3)) != 0;
                this.filterResonance = (value >>> 4) & 0xF;
            }
            case MODE_VOL -> {
                this.volume = value & 0xF;
                this.selectLowPassOutput = (value & (1 << 4)) != 0;
                this.selectBandPassOutput = (value & (1 << 5)) != 0;
                this.selectHighPassOutput = (value & (1 << 6)) != 0;
                this.disableVoice3Output = (value & (1 << 7)) != 0;
            }
        }
    }

    public void cycle() {
        this.voice1.cycle();
        this.voice2.cycle();
        this.voice3.cycle();
    }

    private abstract static class Voice {

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
        private int noiseLFSR = 0x7FFFFF; // 23 bits
        private int envelopeCounter; // 8 bits;
        private int envelopeLFSR15; // 15 bits
        private int envelopeLFSR5; // 5 bits
        private boolean envelopeCountDirection;
        private boolean envelopeCounterEnable;

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
            this.gate = (value & 1) != 0;
            this.sync = (value & (1 << 1)) != 0;
            this.ringModulate = (value & (1 << 2)) != 0;
            this.test = (value & (1 << 3)) != 0;
            this.selectTriangle = (value & (1 << 4)) != 0;
            this.selectSawtooth = (value & (1 << 5)) != 0;
            this.selectPulse = (value & (1 << 6)) != 0;
            this.selectNoise = (value & (1 << 7)) != 0;

            // TODO: Should we check for 0 -> 1 transition instead?
            if (this.test) {
                this.oscillator = 0;
                this.noiseLFSR = 0x7FFFFF;
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
            if (!this.test) {
                this.oscillator = (this.oscillator + this.frequency) & 0xFFFFFF;
            }
            boolean newBit19 = (this.oscillator & (1 << 19)) != 0;

            if (!oldBit19 && newBit19) {
                boolean bit17 = (this.noiseLFSR & (1 << 17)) != 0;
                boolean bit22 = (this.noiseLFSR & (1 << 22)) != 0;
                this.noiseLFSR = ((this.noiseLFSR << 1) | (bit17 ^ bit22 ? 1 : 0)) & 0x7FFFFF;
            }

            // TODO: USE 15 AND 5 BIT LFSRS TO CLOCK ENVELOPE COUNTER BASED ON COUNTER DIRECTION
            if (this.envelopeCounterEnable) {
                if (this.envelopeCountDirection) {
                    this.envelopeCounter = (this.envelopeCounter + 1) & 0xFF;
                    if (this.envelopeCounter == 0xFF) {
                        this.envelopeCountDirection = false;
                    }
                } else {
                    if (!this.gate || this.envelopeCounter != (this.sustain | (this.sustain << 4))) {
                        this.envelopeCounter = (this.envelopeCounter - 1) & 0xFF;
                    }
                    if (this.envelopeCounter == 0x00) {
                        this.envelopeCounterEnable = false;
                    }
                }
            }
        }

        private int getPulseOutput() {
            return this.test ? 0xFFF : ((this.oscillator >>> 12) & 0xFFF) >= this.pulseWidth ? 0xFFF : 0x000;
        }

        private int getTriangleOutput() {
            return this.getTriangleXorOutputs() << 1;
        }

        private int getSawtoothOutput() {
            return this.getTriangleXorOutputs() | ((this.oscillator & (1 << 23)) >>> 12);
        }

        private int getNoiseOutput() {
            return (this.noiseLFSR & 1
                    | ((this.noiseLFSR & (1 << 2)) >>> 1)
                    | ((this.noiseLFSR & (1 << 5)) >>> 3)
                    | ((this.noiseLFSR & (1 << 9)) >>> 6)
                    | ((this.noiseLFSR & (1 << 11)) >>> 7)
                    | ((this.noiseLFSR & (1 << 14)) >>> 9)
                    | ((this.noiseLFSR & (1 << 18)) >>> 12)
                    | ((this.noiseLFSR & (1 << 20)) >>> 13)) << 4;

        }

        protected int getWaveformSelectorOutput() {
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
                output &=  this.getNoiseOutput();
            }
            return output;
        }

        private int getTriangleXorOutputs() {
            int oscillatorUpper11Bits = (this.oscillator >>> 12) & 0x7FF;
            return (this.oscillator & (1 << 23)) != 0 ? oscillatorUpper11Bits ^ 0x7FF : oscillatorUpper11Bits;
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

    }

}
