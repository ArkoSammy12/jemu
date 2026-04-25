package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2A03.*;

// TODO: PAL implementation
public class NESAPU<E extends NESEmulator> extends AudioGenerator<E> implements Bus {

    private final byte[] sampleBuffer;
    private int currentSampleIndex;

    private final PulseChannel1 pulseChannel1 = new PulseChannel1();
    private final PulseChannel2 pulseChannel2 = new PulseChannel2();
    private final TriangleChannel triangleChannel = new TriangleChannel();
    private final NoiseChannel noiseChannel = new NoiseChannel();
    private final DMCChannel dmcChannel = new DMCChannel();

    private int frameCounterControlUpdateCountdown;
    private int joy2UpdateValueBuffer;

    private int clockHalfFrameSignalCountdown;
    private int clockQuarterFrameSignalCountdown;

    private int clearFrameInterruptFlagCountdown;

    private int frameCounterCycleCounter;

    private boolean frameInterruptFlag;
    private FrameCounterStepMode frameCounterStepMode = FrameCounterStepMode.STEP_4;
    private boolean frameCounterInterruptInhibitFlag;

    public NESAPU(E emulator, int samplesPerFrame) {
        super(emulator);
        this.sampleBuffer = new byte[samplesPerFrame];
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
    public Optional<byte[]> getSampleFrame() {
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }

        AudioDriver audioDriver = optionalAudioDriver.get();
        int samplesPerFrame = audioDriver.getSamplesPerFrame();

        byte[] out = new byte[samplesPerFrame];
        double step = (double) this.sampleBuffer.length / samplesPerFrame;
        double pos = 0.0;

        for (int i = 0; i < samplesPerFrame; i++) {
            int index = Math.toIntExact(Math.round(pos));
            int nextIndex = Math.min(index + 1, this.sampleBuffer.length - 1);
            out[i] = this.sampleBuffer[nextIndex];
            pos += step;
        }

        this.currentSampleIndex = 0;
        return Optional.of(out);
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case SQ1_VOL_ADDR, SQ1_SWEEP_ADDR, SQ1_LO_ADDR, SQ1_HI_ADDR, SQ2_VOL_ADDR, SQ2_SWEEP_ADDR, SQ2_LO_ADDR,
                 SQ2_HI_ADDR, TRI_LINEAR_ADDR, TRI_LO_ADDR, TRI_HI_ADDR, NOISE_VOL_ADDR, NOISE_LO_ADDR, NOISE_HI_ADDR,
                 DMC_FREQ_ADDR, DMC_RAW_ADDR, DMC_START_ADDR, DMC_LEN_ADDR -> -1;

            case SND_CHN_ADDR -> {
                // TODO: If an interrupt flag was set at the same moment of the read, it will read back as 1 but it will not be cleared.
                int ret = this.dmcChannel.getInterruptFlag() ? 1 << 7 : 0;
                ret |= this.frameInterruptFlag ? 1 << 6 : 0;
                ret |= this.dmcChannel.isActive() ? 1 << 4 : 0;
                ret |= this.noiseChannel.getLengthCounter() > 0 ? 1 << 3 : 0;
                ret |= this.triangleChannel.getLengthCounter() > 0 ? 1 << 2 : 0;
                ret |= this.pulseChannel2.getLengthCounter() > 0 ? 1 << 1 : 0;
                ret |= this.pulseChannel1.getLengthCounter() > 0 ? 1 : 0;

                this.clearFrameInterruptFlagCountdown = switch (this.getCurrentApuHalfCycleType()) {
                    case GET -> 3;
                    case PUT -> 1;
                };

                yield ret;
            }
            default -> throw new EmulatorException("Invalid read address $%04X for NES APU!".formatted(address));
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case SQ1_VOL_ADDR -> this.pulseChannel1.setVolume(value);
            case SQ1_SWEEP_ADDR -> this.pulseChannel1.setSweep(value);
            case SQ1_LO_ADDR -> this.pulseChannel1.setLO(value);
            case SQ1_HI_ADDR -> this.pulseChannel1.setHI(value);
            case SQ2_VOL_ADDR -> this.pulseChannel2.setVolume(value);
            case SQ2_SWEEP_ADDR -> this.pulseChannel2.setSweep(value);
            case SQ2_LO_ADDR -> this.pulseChannel2.setLO(value);
            case SQ2_HI_ADDR -> this.pulseChannel2.setHI(value);
            case TRI_LINEAR_ADDR -> this.triangleChannel.setLinear(value);
            case TRI_LO_ADDR -> this.triangleChannel.setLO(value);
            case TRI_HI_ADDR -> this.triangleChannel.setHI(value);
            case NOISE_VOL_ADDR -> this.noiseChannel.setVolume(value);
            case NOISE_LO_ADDR -> this.noiseChannel.setLO(value);
            case NOISE_HI_ADDR -> this.noiseChannel.setHI(value);
            case DMC_FREQ_ADDR -> this.dmcChannel.setFreq(value);
            case DMC_RAW_ADDR -> this.dmcChannel.setRaw(value);
            case DMC_START_ADDR -> this.dmcChannel.setStart(value);
            case DMC_LEN_ADDR -> this.dmcChannel.setLength(value);
            case SND_CHN_ADDR -> {
                this.pulseChannel1.setEnabled((value & 1) != 0);
                this.pulseChannel2.setEnabled((value & (1 << 1)) != 0);
                this.triangleChannel.setEnabled((value & (1 << 2)) != 0);
                this.noiseChannel.setEnabled((value & (1 << 3)) != 0);
                this.dmcChannel.setEnabled((value & (1 << 4)) != 0);
                this.dmcChannel.clearInterruptFlag();
            }
            case JOY2_ADDR -> { // Frame counter control
                // If the write occurs during an APU cycle, the effects occur 3 CPU cycles after the $4017 write cycle, and if the write occurs between APU cycles, the effects occurs 4 CPU cycles after the write cycle.
                this.frameCounterControlUpdateCountdown = switch (this.getCurrentApuHalfCycleType()) {
                    case GET -> 3;
                    case PUT -> 4;
                };
                this.joy2UpdateValueBuffer = value & 0xFF;
            }
            default -> throw new EmulatorException("Invalid write address $%04X for NES APU!".formatted(address));
        }
    }

    private void signalHalfFrameClock() {
        this.clockHalfFrameSignalCountdown = 1;
    }

    private void signalQuarterFrameClock() {
        this.clockQuarterFrameSignalCountdown = 1;
    }

    private void setInterruptFlagIfApplicable() {
        if (!this.frameCounterInterruptInhibitFlag) {
            this.frameInterruptFlag = true;
        }
    }

    public void writeDmcDma(int value) {
        this.dmcChannel.writeDmcDma(value);
    }

    public void cycleHalf() {

        if (this.clockHalfFrameSignalCountdown > 0) {
            this.clockHalfFrameSignalCountdown--;
            if (this.clockHalfFrameSignalCountdown <= 0) {
                this.clockHalfFrame();
            }
        }

        if (this.clockQuarterFrameSignalCountdown > 0) {
            this.clockQuarterFrameSignalCountdown--;
            if (this.clockQuarterFrameSignalCountdown <= 0) {
                this.clockQuarterFrame();
            }
        }

        if (this.frameCounterControlUpdateCountdown > 0) {
            this.frameCounterControlUpdateCountdown--;
            if (this.frameCounterControlUpdateCountdown <= 0) {
                this.frameCounterStepMode = (this.joy2UpdateValueBuffer & (1 << 7)) != 0 ? FrameCounterStepMode.STEP_5 : FrameCounterStepMode.STEP_4;
                this.frameCounterInterruptInhibitFlag = (this.joy2UpdateValueBuffer & (1 << 6)) != 0;
                this.frameCounterCycleCounter = 0;

                if (this.frameCounterInterruptInhibitFlag) {
                    this.frameInterruptFlag = false;
                }

                if (this.frameCounterStepMode == FrameCounterStepMode.STEP_5) {
                    this.clockQuarterFrame();
                    this.clockHalfFrame();
                }
            }
        }

        if (this.clearFrameInterruptFlagCountdown > 0) {
            this.clearFrameInterruptFlagCountdown--;
            if (this.clearFrameInterruptFlagCountdown <= 0) {
                this.frameInterruptFlag = false;
            }
        }

        this.clockFrameCounter();

        this.triangleChannel.clockTimer();
        // Clock the noise and dmc channels' timers in both APU halves to line up with the CPU cycles period amount
        this.noiseChannel.clockTimer();
        this.dmcChannel.clockTimer();
        if (this.getCurrentApuHalfCycleType() == APUHalfCycleType.PUT) {
            this.pulseChannel1.clockTimer();
            this.pulseChannel2.clockTimer();
        }

        int pulse1 = this.pulseChannel1.getDigitalOutput();
        int pulse2 = this.pulseChannel2.getDigitalOutput();
        int triangle = this.triangleChannel.getDigitalOutput();
        int noise = this.noiseChannel.getDigitalOutput();
        int dmc = this.dmcChannel.getDigitalOutput();

        double pulseOut = 0;
        double tndOut = 0;

        int pulseGroupSum = pulse1 + pulse2;

        if (pulseGroupSum > 0) {
            pulseOut = 95.88 / (((double) 8128 / pulseGroupSum) + 100);
        }

        if (triangle != 0 || noise != 0 || dmc != 0) {
            tndOut = 159.79 / (((double) 1 / (((double) triangle / 8227) + ((double) noise / 12241) + ((double) dmc / 22638))) + 100);
        }

        double output = Math.clamp(pulseOut + tndOut, 0, 1.0);
        this.sampleBuffer[this.currentSampleIndex] = (byte) (((output * 2) - 1) * 127f);
        this.currentSampleIndex = (this.currentSampleIndex + 1) % this.sampleBuffer.length;
    }

    private void clockFrameCounter() {
        // TODO: PAL implementation with different apu cycle totals for signals

        switch (this.frameCounterStepMode) {
            case STEP_4 -> {
                switch (this.getCurrentApuHalfCycleType()) {
                    case GET -> {
                        switch (this.frameCounterCycleCounter) {
                            case 14914 -> {
                                this.setInterruptFlagIfApplicable();
                            }
                            case 14915 -> {
                                this.setInterruptFlagIfApplicable();
                                this.frameCounterCycleCounter = 0;
                            }
                        }
                    }
                    case PUT -> {
                        switch (this.frameCounterCycleCounter) {
                            case 3728 - 2, 11185 - 2 -> {
                                this.signalQuarterFrameClock();
                            }
                            case 7456 - 1 -> {
                                this.signalQuarterFrameClock();
                                this.signalHalfFrameClock();
                            }
                            case 14914 - 1 -> {
                                this.signalQuarterFrameClock();
                                this.signalHalfFrameClock();
                                this.setInterruptFlagIfApplicable();
                            }
                        }
                        this.frameCounterCycleCounter++;
                    }
                }
            }
            case STEP_5 -> {
                switch (this.getCurrentApuHalfCycleType()) {
                    case GET -> {
                        if (this.frameCounterCycleCounter == 18641) {
                            this.frameCounterCycleCounter = 0;
                        }
                    }
                    case PUT -> {
                        switch (this.frameCounterCycleCounter) {
                            case 3728, 11185 -> {
                                this.signalQuarterFrameClock();
                            }
                            case 7456 - 1, 18640 - 1 -> {
                                this.signalQuarterFrameClock();
                                this.signalHalfFrameClock();
                            }
                        }
                        this.frameCounterCycleCounter++;
                    }
                }
            }
        }
    }

    private void clockQuarterFrame() {
        this.pulseChannel1.clockEnvelope();
        this.pulseChannel2.clockEnvelope();
        this.triangleChannel.clockLinearCounter();
        this.noiseChannel.clockEnvelope();
    }

    private void clockHalfFrame() {
        this.pulseChannel1.clockLength();
        this.pulseChannel2.clockLength();
        this.triangleChannel.clockLength();
        this.noiseChannel.clockLength();

        this.pulseChannel1.clockSweep();
        this.pulseChannel2.clockSweep();
    }

    public boolean getIRQSignal() {
        return this.frameInterruptFlag || this.dmcChannel.getInterruptFlag();
    }

    private APUHalfCycleType getCurrentApuHalfCycleType() {
        return this.emulator.getRicohCore().getCurrentApuHalfCycleType();
    }

    private enum FrameCounterStepMode {
        STEP_4,
        STEP_5
    }

    private abstract static class AudioChannel {

        private boolean enabled;

        abstract protected void clockTimer();

        abstract protected int getDigitalOutput();

        protected void setEnabled(boolean value) {
            this.enabled = value;
        }

        protected boolean isEnabled() {
            return this.enabled;
        }

    }

    private abstract static class WaveformChannel extends AudioChannel {

        protected final int[] LENGTH_COUNTER_LUT = {
                10, 254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
                12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
        };

        protected int volume;
        protected int lo;
        protected int hi;

        private int lengthCounter;

        protected void setVolume(int value) {
            this.volume = value & 0xFF;
        }

        protected void setLO(int value) {
            this.lo = value & 0xFF;
        }

        protected void setHI(int value) {
            this.hi = value & 0xFF;
            if (this.isEnabled()) {
                this.lengthCounter = LENGTH_COUNTER_LUT[this.getLengthCounterLoad()];
            }
        }

        @Override
        protected void setEnabled(boolean value) {
            super.setEnabled(value);
            if (!this.isEnabled()) {
                this.lengthCounter = 0;
            }
        }

        protected boolean haltLengthCounter() {
            return (this.volume & (1 << 5)) != 0;
        }

        protected int getLengthCounterLoad() {
            return (this.hi >>> 3) & 0b11111;
        }

        protected void clockLength() {
            if (this.lengthCounter > 0 && !this.haltLengthCounter()) {
                this.lengthCounter--;
            }
        }

        protected int getLengthCounter() {
            return this.lengthCounter;
        }

    }

    private static class PulseChannel1 extends WaveformChannel {

        private static final int[][] DUTY_CYCLES = {
                {0, 0, 0, 0, 0, 0, 0, 1},
                {0, 0, 0, 0, 0, 0, 1, 1},
                {0, 0, 0, 0, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 0, 0}
        };

        private int sweep;

        protected int timer;
        private int sequencerStep;

        private boolean envelopeStartFlag;
        private int envelopeDivider;
        private int envelopeDecayCounter;

        private boolean sweepReloadFlag;
        protected int sweepTargetPeriod;
        private int sweepDividerCounter;

        protected int getTimerReload() {
            return this.lo | ((this.hi & 0b111) << 8);
        }

        private int getDutyCycle() {
            return (this.volume >>> 6) & 0b11;
        }

        private boolean getConstantVolumeFlag() {
            return (this.volume & (1 << 4)) != 0;
        }

        private int getEnvelopeDividerPeriod() {
            return this.volume & 0b1111;
        }

        private boolean getSweepEnableFlag() {
            return (this.sweep & (1 << 7)) != 0;
        }

        private int getSweepDividerPeriod() {
            return (this.sweep >>> 4) & 0b111;
        }

        protected boolean getSweepNegateFlag() {
            return (this.sweep & (1 << 3)) != 0;
        }

        protected int getSweepShiftCount() {
            return this.sweep & 0b111;
        }

        @Override
        protected void setHI(int value) {
            super.setHI(value);
            this.envelopeStartFlag = true;
            this.sequencerStep = 0;
        }

        protected void setSweep(int value) {
            this.sweep = value & 0xFF;
            this.sweepReloadFlag = true;
        }

        @Override
        protected void clockTimer() {
            this.timer--;
            if (this.timer < 0) {
                this.timer = this.getTimerReload();
                this.sequencerStep = (this.sequencerStep - 1) & 0b111;
            }
        }

        protected void clockEnvelope() {
            if (!this.envelopeStartFlag) {
                if (this.envelopeDivider > 0) {
                    this.envelopeDivider--;
                } else {
                    this.envelopeDivider = this.getEnvelopeDividerPeriod();
                    if (this.envelopeDecayCounter > 0) {
                        this.envelopeDecayCounter--;
                    } else if (this.haltLengthCounter()) {
                        this.envelopeDecayCounter = 15;
                    }
                }
            } else {
                this.envelopeStartFlag = false;
                this.envelopeDecayCounter = 15;
                this.envelopeDivider = this.getEnvelopeDividerPeriod();
            }
        }

        protected void clockSweep() {
            this.calculateSweepTargetPeriod();
            if (this.sweepDividerCounter > 0) {
                this.sweepDividerCounter--;
            } else {
                if (this.getSweepEnableFlag() && this.getSweepShiftCount() != 0 && this.sweepTargetPeriod <= 0x7FF) {
                    this.lo = this.sweepTargetPeriod & 0xFF;
                    this.hi = (this.hi & ~0b111) | ((this.sweepTargetPeriod >>> 8) & 0b111);
                }
                this.sweepDividerCounter = this.getSweepDividerPeriod();
            }
            if (this.sweepReloadFlag) {
                this.sweepDividerCounter = this.getSweepDividerPeriod();
                this.sweepReloadFlag = false;
            }
        }

        protected void calculateSweepTargetPeriod() {
            int changeAmount = this.getTimerReload() >>> this.getSweepShiftCount();
            if (this.getSweepNegateFlag()) {
                changeAmount = (changeAmount * -1) - 1;
            }
            this.sweepTargetPeriod = Math.max(0, this.getTimerReload() + changeAmount);
        }

        @Override
        protected int getDigitalOutput() {
            if (this.getLengthCounter() <= 0) {
                return 0;
            }
            if (this.getTimerReload() < 8) {
                return 0;
            }
            if (this.sweepTargetPeriod > 0x7FF) {
                return 0;
            }
            return DUTY_CYCLES[this.getDutyCycle()][this.sequencerStep] != 0 ? (this.getConstantVolumeFlag() ? this.getEnvelopeDividerPeriod() : this.envelopeDecayCounter) : 0;
        }

    }

    private static class PulseChannel2 extends PulseChannel1 {

        @Override
        protected void calculateSweepTargetPeriod() {
            int changeAmount = this.getTimerReload() >>> this.getSweepShiftCount();
            if (this.getSweepNegateFlag()) {
                changeAmount *= -1;
            }
            this.sweepTargetPeriod = Math.max(0, this.getTimerReload() + changeAmount);
        }

    }

    private static class TriangleChannel extends WaveformChannel {

        private static final int[] TRIANGLE_WAVEFORM_LUT = {
                15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
                0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
        };

        private int linear;

        private int timer;
        private int sequencerStep;

        private boolean linearCounterReloadFlag;
        private int linearCounter;

        @Override
        protected boolean haltLengthCounter() {
            return (this.linear & (1 << 7)) != 0;
        }

        protected int getTimerReload() {
            return this.lo | ((this.hi & 0b111) << 8);
        }

        private boolean getControlFlag() {
            return this.haltLengthCounter();
        }

        private int getLinearCounterReload() {
            return this.linear & 0b1111111;
        }

        @Override
        protected void setHI(int value) {
            super.setHI(value);
            this.linearCounterReloadFlag = true;
        }

        protected void setLinear(int value) {
            this.linear = value & 0xFF;
        }

        @Override
        protected void clockTimer() {
            this.timer--;
            if (this.timer < 0) {
                if (this.getLengthCounter() > 0 && this.linearCounter > 0) {
                    this.sequencerStep = (this.sequencerStep + 1) & 0x1F;
                }
                this.timer = this.getTimerReload() + 1;
            }
        }

        protected void clockLinearCounter() {
            if (this.linearCounterReloadFlag) {
                this.linearCounter = this.getLinearCounterReload();
            } else if (this.linearCounter > 0) {
                this.linearCounter--;
            }
            if (!this.getControlFlag()) {
                this.linearCounterReloadFlag = false;
            }
        }

        @Override
        protected int getDigitalOutput() {
            if (this.getLengthCounter() <= 0) {
                return 0;
            }
            return TRIANGLE_WAVEFORM_LUT[this.sequencerStep];
        }

    }

    private static class NoiseChannel extends WaveformChannel {

        // TODO: PAL support
        private static final int[] NTSC_TIMER_PERIOD_LUT = {
                // Values are in CPU cycles!
                4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        };

        private final int[] cpuCyclesPeriodLut;

        private int lfsr;

        private int timer;

        private boolean envelopeStartFlag;
        private int envelopeDivider;
        private int envelopeDecayCounter;

        protected NoiseChannel() {
            // Value on power-up
            this.lfsr = 1;
            this.cpuCyclesPeriodLut = NTSC_TIMER_PERIOD_LUT;
        }

        private boolean getConstantVolumeFlag() {
            return (this.volume & (1 << 4)) != 0;
        }

        private int getEnvelopeDividerPeriod() {
            return this.volume & 0b1111;
        }

        private boolean getMode() {
            return (this.lo & (1 << 7)) != 0;
        }

        private int getTimerPeriod() {
            return this.lo & 0b1111;
        }

        @Override
        protected void setHI(int value) {
            super.setHI(value);
            this.envelopeStartFlag = true;
        }

        @Override
        protected void clockTimer() {
            this.timer--;
            if (this.timer < 0) {
                this.timer = this.cpuCyclesPeriodLut[this.getTimerPeriod()];
                boolean feedback = ((this.lfsr & 1) ^ (this.getMode() ? ((this.lfsr >>> 6) & 1) : ((this.lfsr >>> 1) & 1))) != 0;
                this.lfsr >>>= 1;
                this.lfsr |= feedback ? (1 << 14) : 0;
                this.lfsr &= 0x7FFF;
            }
        }

        protected void clockEnvelope() {
            if (!this.envelopeStartFlag) {
                if (this.envelopeDivider > 0) {
                    this.envelopeDivider--;
                } else {
                    this.envelopeDivider = this.getEnvelopeDividerPeriod();
                    if (this.envelopeDecayCounter > 0) {
                        this.envelopeDecayCounter--;
                    } else if (this.haltLengthCounter()) {
                        this.envelopeDecayCounter = 15;
                    }
                }
            } else {
                this.envelopeStartFlag = false;
                this.envelopeDecayCounter = 15;
                this.envelopeDivider = this.getEnvelopeDividerPeriod();
            }
        }

        @Override
        protected int getDigitalOutput() {
            if (this.getLengthCounter() <= 0) {
                return 0;
            }
            return (this.lfsr & 1) == 0 ? (this.getConstantVolumeFlag() ? this.getEnvelopeDividerPeriod() : this.envelopeDecayCounter) : 0;
        }

    }

    private class DMCChannel extends AudioChannel {

        // TODO: Add PAL support
        private static final int[] NTSC_RATE_PERIOD_LUT = {
            // Values are in CPU cycles!
            428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
        };

        private final int[] ratePeriodLut;

        private int freq;
        private int start;
        private int length;

        private boolean interruptFlag;

        private int timer;

        private boolean sampleBufferEmpty = true;
        private boolean resetSampleFlag;

        private int sampleBuffer;
        private boolean silenceFlag;
        private int shiftRegister;
        private int outputLevel;

        private int bitsRemainingCounter;
        private int currentAddress;
        private int bytesRemainingCounter;

        protected DMCChannel() {
            this.ratePeriodLut = NTSC_RATE_PERIOD_LUT;
        }

        @Override
        protected void setEnabled(boolean value) {
            super.setEnabled(value);
            if (this.isEnabled()) {
                if (this.bytesRemainingCounter <= 0) {
                    this.resetSample();
                }
                this.checkStartDmcDma(DmcDmaType.LOAD);
            } else {
                this.bytesRemainingCounter = 0;
            }
        }

        private void resetSample() {
            this.currentAddress = this.getSampleAddress();
            this.bytesRemainingCounter = this.getSampleLength();
        }

        protected void setFreq(int value) {
            this.freq = value & 0xFF;
            if (!this.getIRQEnabledFlag()) {
                this.interruptFlag = false;
            }
        }

        protected void setRaw(int value) {
            this.setOutputLevel(value);
        }

        protected void setStart(int value) {
            this.start = value & 0xFF;
        }

        protected void setLength(int value) {
            this.length = value & 0xFF;
        }

        private boolean getIRQEnabledFlag() {
            return (this.freq & (1 << 7)) != 0;
        }

        private boolean getLoopFlag() {
            return (this.freq & (1 << 6)) != 0;
        }

        private int getRateIndex() {
            return this.freq & 0b1111;
        }

        private int getSampleAddress() {
            return 0xC000 + (this.start * 64);
        }

        private int getSampleLength() {
            return (this.length * 16) + 1;
        }

        protected void clearInterruptFlag() {
            this.interruptFlag = false;
        }

        protected boolean getInterruptFlag() {
            return this.interruptFlag;
        }

        protected boolean isActive() {
            return this.bytesRemainingCounter > 0;
        }

        private void setOutputLevel(int value) {
            this.outputLevel = value & 0x7F;
        }

        @Override
        protected void clockTimer() {
            if (this.timer > 0) {
                this.timer--;
            } else {
                this.timer = this.ratePeriodLut[this.getRateIndex()];
                if (!this.silenceFlag) {
                    if ((this.shiftRegister & 1) != 0) {
                        if (this.outputLevel <= 125) {
                            this.setOutputLevel(this.outputLevel + 2);
                        }
                    } else {
                        if (this.outputLevel >= 2) {
                            this.setOutputLevel(this.outputLevel - 2);
                        }
                    }
                }

                this.shiftRegister >>>= 1;
                this.bitsRemainingCounter--;
                if (this.bitsRemainingCounter <= 0) {
                    this.bitsRemainingCounter = 8;
                    if (this.sampleBufferEmpty) {
                        this.silenceFlag = true;
                    } else {
                        this.silenceFlag = false;
                        this.sampleBufferEmpty = true;
                        this.shiftRegister = this.sampleBuffer;
                        this.checkStartDmcDma(DmcDmaType.RELOAD);
                    }
                }
            }
        }

        private void checkStartDmcDma(DmcDmaType dmcDmaType) {
            if (this.sampleBufferEmpty && this.bytesRemainingCounter > 0) {
                emulator.getRicohCore().triggerDmcDma(dmcDmaType, this.currentAddress);
            }
        }

        protected void writeDmcDma(int value) {
            this.sampleBuffer = value & 0xFF;
            this.sampleBufferEmpty = false;
            this.currentAddress++;
            if (this.currentAddress > 0xFFFF) {
                this.currentAddress = 0x8000;
            }
            this.bytesRemainingCounter--;
            if (this.bytesRemainingCounter <= 0) {
                if (this.getLoopFlag()) {
                    this.resetSample();
                } else if (this.getIRQEnabledFlag()) {
                    this.interruptFlag = true;
                }
            }
        }


        @Override
        protected int getDigitalOutput() {
            return this.outputLevel;
        }

    }

    public enum DmcDmaType {
        LOAD,
        RELOAD
    }

}
