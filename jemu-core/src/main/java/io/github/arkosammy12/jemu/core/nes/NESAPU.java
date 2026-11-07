package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.util.ActionSignalDispatcher;
import io.github.arkosammy12.jemu.core.util.HighPassFilter;
import io.github.arkosammy12.jemu.core.util.LowPassFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.IntConsumer;

import static io.github.arkosammy12.jemu.core.nes.RP2A03.*;

public class NESAPU<E extends NESEmulator> implements AudioGenerator, Bus {

    private static final double OUTPUT_GAIN = Short.MAX_VALUE;

    private static final double[] PULSE_TABLE = new double[31];
    private static final double[] TND_TABLE = new double [203];

    static {
        for (int i = 0; i < PULSE_TABLE.length; i++) {
            PULSE_TABLE[i] = 95.52 / (8128.0 / (double) i + 100);
        }
        for (int i = 0; i < TND_TABLE.length; i++) {
            TND_TABLE[i] = 163.67 / (24329.0 / (double) i + 100);
        }
    }

    private final E emulator;
    private final SampleFrameResampler sampleFrameResampler;

    private final double[] sampleBuffer;
    private int currentSampleIndex;

    private final PulseChannel1 pulseChannel1;
    private final PulseChannel2 pulseChannel2;
    private final TriangleChannel triangleChannel;
    private final NoiseChannel noiseChannel;
    private final DMCChannel dmcChannel;

    // =============================================================
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // If adding or changing the initial value of a field here,
    // consider adding the correspond initialization code in reset()
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // =============================================================

    private int frameCounterCycleCounter;

    private final ActionSignalDispatcher signalDispatcher = new ActionSignalDispatcher();
    private final int clockHalfFrameSignalId;
    private final int clockQuarterFrameSignalId;
    private final int frameCounterControlUpdateOnGetCycleId;
    private final int frameCounterControlUpdateOnPutCycleId;
    private final int clearFrameInterruptFlagSignalIdOnGetCycleId;
    private final int clearFrameInterruptFlagSignalIdOnPutCycleId;

    private final Runnable frameCounterClocker;

    private boolean frameInterruptFlag;
    private boolean frameInterruptFlagForIRQSignal;
    private FrameCounterStepMode frameCounterStepMode = FrameCounterStepMode.STEP_4;
    private boolean frameCounterInterruptInhibitFlag;

    public NESAPU(E emulator, int samplesPerFrame) {
        this.emulator = emulator;
        this.sampleBuffer = new double[samplesPerFrame];

        IntConsumer frameCounterControlUpdateAction = newJoy2Value -> {
            this.frameCounterStepMode = (newJoy2Value & (1 << 7)) != 0 ? FrameCounterStepMode.STEP_5 : FrameCounterStepMode.STEP_4;
            this.frameCounterInterruptInhibitFlag = (newJoy2Value & (1 << 6)) != 0;
            this.frameCounterCycleCounter = 0;
            if (this.frameCounterInterruptInhibitFlag) {
                this.frameInterruptFlag = false;
                this.frameInterruptFlagForIRQSignal = false;
            }
        };

        this.frameCounterControlUpdateOnGetCycleId = this.signalDispatcher.addSignal(5, frameCounterControlUpdateAction);
        this.frameCounterControlUpdateOnPutCycleId = this.signalDispatcher.addSignal(4, frameCounterControlUpdateAction);

        IntConsumer clearFrameInterruptFlagAction = _ -> {
            this.frameInterruptFlag = false;
            this.frameInterruptFlagForIRQSignal = false;
        };

        this.clearFrameInterruptFlagSignalIdOnGetCycleId = this.signalDispatcher.addSignal(2, clearFrameInterruptFlagAction);
        this.clearFrameInterruptFlagSignalIdOnPutCycleId = this.signalDispatcher.addSignal(1, clearFrameInterruptFlagAction);

        this.clockHalfFrameSignalId = this.signalDispatcher.addSignal(1, _ -> this.clockHalfFrame());
        this.clockQuarterFrameSignalId = this.signalDispatcher.addSignal(1, _ -> this.clockQuarterFrame());

        this.pulseChannel1 = new PulseChannel1();
        this.pulseChannel2 = new PulseChannel2();
        this.triangleChannel = new TriangleChannel();
        this.noiseChannel = new NoiseChannel();
        this.dmcChannel = new DMCChannel();

        if (emulator.getTVSystem() == NESEmulator.TVSystem.PAL) {
            this.frameCounterClocker = () -> {
                switch (this.frameCounterStepMode) {
                    case STEP_4 -> {
                        switch (this.getCurrentAPUHalfCycleType()) {
                            case GET -> {
                                switch (this.frameCounterCycleCounter) {
                                    case 16626 -> {
                                        this.forceSetFrameCounterIRQFlag();
                                        this.frameInterruptFlagForIRQSignal = !this.frameCounterInterruptInhibitFlag;
                                    }
                                    case 16627 -> {
                                        this.trySetFrameCounterIRQFlag();
                                        this.frameCounterCycleCounter = 0;
                                    }
                                }
                            }
                            case PUT -> {
                                switch (this.frameCounterCycleCounter) {
                                    case 4156 - 2, 12469 - 2 -> this.signalQuarterFrameClock();
                                    case 8313 - 1 -> {
                                        this.signalQuarterFrameClock();
                                        this.signalHalfFrameClock();
                                    }
                                    case 16626 - 1 -> {
                                        this.signalQuarterFrameClock();
                                        this.signalHalfFrameClock();
                                        this.forceSetFrameCounterIRQFlag();
                                    }
                                    case 16626 -> this.trySetFrameCounterIRQFlag();
                                }
                                this.frameCounterCycleCounter++;
                            }
                        }
                    }
                    case STEP_5 -> {
                        switch (this.getCurrentAPUHalfCycleType()) {
                            case GET -> {
                                if (this.frameCounterCycleCounter == 20783) {
                                    this.frameCounterCycleCounter = 0;
                                }
                            }
                            case PUT -> {
                                switch (this.frameCounterCycleCounter) {
                                    case 4156, 12469 -> this.signalQuarterFrameClock();
                                    case 8313 - 1, 20782 - 1 -> {
                                        this.signalQuarterFrameClock();
                                        this.signalHalfFrameClock();
                                    }
                                }
                                this.frameCounterCycleCounter++;
                            }
                        }
                    }
                }
            };
        } else {
            this.frameCounterClocker = () -> {
                switch (this.frameCounterStepMode) {
                    case STEP_4 -> {
                        switch (this.getCurrentAPUHalfCycleType()) {
                            case GET -> {
                                switch (this.frameCounterCycleCounter) {
                                    case 14914 -> {
                                        this.forceSetFrameCounterIRQFlag();
                                        this.frameInterruptFlagForIRQSignal = !this.frameCounterInterruptInhibitFlag;
                                    }
                                    case 14915 -> {
                                        this.trySetFrameCounterIRQFlag();
                                        this.frameCounterCycleCounter = 0;
                                    }
                                }
                            }
                            case PUT -> {
                                switch (this.frameCounterCycleCounter) {
                                    case 3728 - 2, 11185 - 2 -> this.signalQuarterFrameClock();
                                    case 7456 - 1 -> {
                                        this.signalQuarterFrameClock();
                                        this.signalHalfFrameClock();
                                    }
                                    case 14914 - 1 -> {
                                        this.signalQuarterFrameClock();
                                        this.signalHalfFrameClock();
                                        this.forceSetFrameCounterIRQFlag();
                                    }
                                    case 14914 -> this.trySetFrameCounterIRQFlag();
                                }
                                this.frameCounterCycleCounter++;
                            }
                        }
                    }
                    case STEP_5 -> {
                        switch (this.getCurrentAPUHalfCycleType()) {
                            case GET -> {
                                if (this.frameCounterCycleCounter == 18641) {
                                    this.frameCounterCycleCounter = 0;
                                }
                            }
                            case PUT -> {
                                switch (this.frameCounterCycleCounter) {
                                    case 3728, 11185 -> this.signalQuarterFrameClock();
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
            };
        }

        this.sampleFrameResampler = new SampleFrameResampler() {

            private final LowPassFilter lpf = new LowPassFilter();
            private final HighPassFilter hpf0 = new HighPassFilter();
            private final HighPassFilter hpf1 = new HighPassFilter();
            private final HighPassFilter hpf2 = new HighPassFilter();

            {
                this.lpf.createLpf(17000.0, samplesPerFrame * emulator.getFramerate());
            }

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(double[] sampleFrame)) {
                    for (int i = 0; i < sampleFrame.length; i++) {
                        sampleFrame[i] = this.lpf.process(sampleFrame[i]);
                    }

                    this.hpf0.createHpf(285.17092929859564, outputSampleRate);
                    this.hpf1.createHpf(85.509330674952423, outputSampleRate);
                    this.hpf2.createHpf(7.3617262313390981, outputSampleRate);

                    byte[] out = new byte[outputSamplesPerFrame * 2];
                    double step = (double) sampleFrame.length / (double) outputSamplesPerFrame;
                    double pos = 0.0;

                    for (int i = 0; i < outputSamplesPerFrame; i++) {
                        int index = Math.min((int) Math.round(pos), sampleFrame.length - 1);
                        short sample = (short) Math.clamp((long)(this.hpf2.process(this.hpf1.process(this.hpf0.process(sampleFrame[index]))) * OUTPUT_GAIN), -Short.MAX_VALUE, Short.MAX_VALUE);
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

        this.reset();

    }

    public void reset() {
        // =============================================================
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // If changing the reset value of a field here, consider changing
        // it in the field declaration and initialization up in the fields
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // =============================================================

        // Values on reset provided explicitly by the nesdev wiki in https://www.nesdev.org/wiki/CPU_power_up_state

        // See TriangleChannel class for the reasoning as to why we reset the sequencer step to 15 instead of 0
        this.triangleChannel.sequencerStep = 15;
        this.dmcChannel.outputLevel &= 1;

        this.pulseChannel1.setEnabled(false);
        this.pulseChannel2.setEnabled(false);
        this.triangleChannel.setEnabled(false);
        this.noiseChannel.setEnabled(false);
        this.dmcChannel.setEnabled(false);

        this.frameCounterCycleCounter = 0;

        // Internal state resetting

        this.signalDispatcher.reset();

        // The frame counter IRQ flag being clear is expected by blargg's apu_reset tests
        this.frameInterruptFlag = false;
        this.frameInterruptFlagForIRQSignal = false;
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

                switch (this.getCurrentAPUHalfCycleType()) {
                    case GET -> this.signalDispatcher.trigger(this.clearFrameInterruptFlagSignalIdOnGetCycleId, 0);
                    case PUT -> this.signalDispatcher.trigger(this.clearFrameInterruptFlagSignalIdOnPutCycleId, 0);
                }

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

                switch (this.getCurrentAPUHalfCycleType()) {
                    case GET -> this.signalDispatcher.trigger(this.frameCounterControlUpdateOnGetCycleId, value & 0xFF);
                    case PUT -> this.signalDispatcher.trigger(this.frameCounterControlUpdateOnPutCycleId, value & 0xFF);
                }

                if ((value & 0x80) != 0) {
                    this.clockHalfFrame();
                    this.clockQuarterFrame();
                }

            }
            default -> throw new EmulatorException("Invalid write address $%04X for NES APU!".formatted(address));
        }
    }

    void writeDmcDma(int value) {
        this.dmcChannel.writeDmcDma(value);
    }
	
	DmcDmaType getPendingImplicitDmcDmaType() {
		return this.dmcChannel.pendingImplicitDmaType;
	}

	int getPendingImplicitDmcDmaAddress() {
		return this.dmcChannel.pendingImplicitDmaAddress;
	}

	void clearPendingImplicitDmcDma() {
		this.dmcChannel.pendingImplicitDmaType = null;
	}

    public void cycleHalf() {

        this.signalDispatcher.tick();

        this.frameCounterClocker.run();

        this.pulseChannel1.onAPUHalfCycle();
        this.pulseChannel2.onAPUHalfCycle();
        this.triangleChannel.onAPUHalfCycle();
        this.noiseChannel.onAPUHalfCycle();
        this.dmcChannel.onAPUHalfCycle();

        this.triangleChannel.clockTimer();
        // Clock the noise and dmc channels' timers in both APU halves to line up with the CPU cycles period amount
        this.noiseChannel.clockTimer();
        this.dmcChannel.clockTimer();
        if (this.getCurrentAPUHalfCycleType() == APUHalfCycleType.PUT) {
            this.pulseChannel1.clockTimer();
            this.pulseChannel2.clockTimer();
        }

        int pulse1 = this.pulseChannel1.getDigitalOutput();
        int pulse2 = this.pulseChannel2.getDigitalOutput();
        int triangle = this.triangleChannel.getDigitalOutput();
        int noise = this.noiseChannel.getDigitalOutput();
        int dmc = this.dmcChannel.getDigitalOutput();

        double output = PULSE_TABLE[pulse1 + pulse2] + TND_TABLE[3 * triangle + 2 * noise + dmc];

        // > But, when Sunsoft 5B mixes itself with the APU audio, it should invert the incoming APU audio.
        //
        // Shawn (L. Spiro) Wilcoxen, 2026
        output = this.emulator.getCartridge().mixAPUAudio(output);

        // Apparently my final waveform is inverted compared to hardware, so we revert it right back here
        output *= -1;
        this.sampleBuffer[this.currentSampleIndex] = output;
        this.currentSampleIndex = (this.currentSampleIndex + 1) % this.sampleBuffer.length;
    }

    private void signalHalfFrameClock() {
        this.signalDispatcher.trigger(this.clockHalfFrameSignalId, 0);
    }

    private void signalQuarterFrameClock() {
        this.signalDispatcher.trigger(this.clockQuarterFrameSignalId, 0);
    }

    private void trySetFrameCounterIRQFlag() {
        this.frameInterruptFlag = !this.frameCounterInterruptInhibitFlag;
    }

    private void forceSetFrameCounterIRQFlag() {
        this.frameInterruptFlag = true;
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
        return this.dmcChannel.getInterruptFlag() || (this.frameInterruptFlagForIRQSignal && !this.frameCounterInterruptInhibitFlag);
    }

    private APUHalfCycleType getCurrentAPUHalfCycleType() {
        return this.emulator.getRicohCore().getCurrentApuHalfCycleType();
    }

    private enum FrameCounterStepMode {
        STEP_4,
        STEP_5
    }

    private record SampleFrame(double[] sampleFrame) implements AudioGenerator.SampleFrame {}

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

        protected void onAPUHalfCycle() {

        }

    }

    private abstract static class WaveformChannel extends AudioChannel {

        protected final int[] LENGTH_COUNTER_LUT = {
                10, 254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
                12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
        };

        // VOL registers
        private boolean haltLengthCounter;

        // HI registers
        private int lengthCounterReload;

        private int lengthCounter;
        private boolean lengthCounterReloadPending;
        private int lengthCounterReloadValue;

        // TODO: Delay on the halt flag. Writes to $4000 and equivalent for pulse1, pulse2, triangle and noise have a delay when changing the halt flag

        protected void setVolume(int value) {
            this.haltLengthCounter = (value & (1 << 5)) != 0;
        }

        protected void setLO(int value) {

        }

        protected void setHI(int value) {
            this.lengthCounterReload = (value >>> 3) & 0b11111;
            if (this.isEnabled()) {
                this.lengthCounterReloadPending = true;
                this.lengthCounterReloadValue = LENGTH_COUNTER_LUT[this.getLengthCounterLoad()];
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
            return this.haltLengthCounter;
        }

        protected int getLengthCounterLoad() {
            return this.lengthCounterReload;
        }

        protected void clockLength() {
            boolean didClock = false;
            if (this.lengthCounter > 0 && !this.haltLengthCounter()) {
                this.lengthCounter--;
                didClock = true;
            }

            if (this.lengthCounterReloadPending) {
                this.lengthCounterReloadPending = false;
                if (!didClock || this.lengthCounter == 0) {
                    this.lengthCounter = this.lengthCounterReloadValue;
                }
            }
        }

        protected int getLengthCounter() {
            return this.lengthCounter;
        }

        @Override
        protected void onAPUHalfCycle() {
            if (this.lengthCounterReloadPending) {
                this.lengthCounterReloadPending = false;
                this.lengthCounter = this.lengthCounterReloadValue;
            }
        }

    }

    private static class PulseChannel1 extends WaveformChannel {

        private static final int[][] DUTY_CYCLES = {
                {0, 0, 0, 0, 0, 0, 0, 1},
                {0, 0, 0, 0, 0, 0, 1, 1},
                {0, 0, 0, 0, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 0, 0}
        };

        // VOL registers
        private int dutyCycle;
        private boolean constantVolumeFlag;
        private int envelopeDividerPeriod;

        // SWEEP registers
        private boolean sweepEnableFlag;
        private int sweepDividerPeriod;
        private boolean sweepNegateFlag;
        private int sweepShiftCount;

        private int timerReload;

        protected int timer;
        private int sequencerStep;

        private boolean envelopeStartFlag;
        private int envelopeDivider;
        private int envelopeDecayCounter;

        private boolean sweepReloadFlag;
        protected int sweepTargetPeriod;
        private int sweepDividerCounter;

        protected int getTimerReload() {
            return this.timerReload;
        }

        private int getDutyCycle() {
            return this.dutyCycle;
        }

        private boolean getConstantVolumeFlag() {
            return this.constantVolumeFlag;
        }

        private int getEnvelopeDividerPeriod() {
            return this.envelopeDividerPeriod;
        }

        private boolean getSweepEnableFlag() {
            return this.sweepEnableFlag;
        }

        private int getSweepDividerPeriod() {
            return this.sweepDividerPeriod;
        }

        protected boolean getSweepNegateFlag() {
            return this.sweepNegateFlag;
        }

        protected int getSweepShiftCount() {
            return this.sweepShiftCount;
        }

        @Override
        protected void setVolume(int value) {
            super.setVolume(value);
            this.dutyCycle = (value >>> 6) & 0b11;
            this.constantVolumeFlag = (value & (1 << 4)) != 0;
            this.envelopeDividerPeriod = value & 0b1111;
        }

        @Override
        protected void setLO(int value) {
            super.setLO(value);
            this.timerReload = (this.timerReload & 0b111_0000_0000) | (value & 0xFF);
        }

        @Override
        protected void setHI(int value) {
            super.setHI(value);
            this.timerReload = (this.timerReload & 0b000_1111_1111) | ((value & 0b111) << 8);
            this.envelopeStartFlag = true;
            this.sequencerStep = 0;
        }

        protected void setSweep(int value) {
            this.sweepEnableFlag = (value & (1 << 7)) != 0;
            this.sweepDividerPeriod = (value >>> 4) & 0b111;
            this.sweepNegateFlag = (value & (1 << 3)) != 0;
            this.sweepShiftCount = value & 0b111;

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
                    this.timerReload = this.sweepTargetPeriod;
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
            if (this.getLengthCounter() <= 0 || this.getTimerReload() < 8 || this.sweepTargetPeriod > 0x7FF) {
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

        private int timerReload;

        // LINEAR registers
        private int linearCounterReload;
        private boolean haltLengthCounter;

        private int timer;

        // Initialize the sequencer step value to 15 in order to avoid immediately
        // outputting a non-zero digital output and avoiding the initial start up pop
        private int sequencerStep = 15;

        private boolean linearCounterReloadFlag;
        private int linearCounter;

        protected int getTimerReload() {
            return this.timerReload;
        }

        @Override
        protected boolean haltLengthCounter() {
            return this.haltLengthCounter;
        }

        private boolean getControlFlag() {
            return this.haltLengthCounter();
        }

        private int getLinearCounterReload() {
            return this.linearCounterReload;
        }

        @Override
        protected void setLO(int value) {
            super.setLO(value);
            this.timerReload = (this.timerReload & 0b111_0000_0000) | (value & 0xFF);
        }

        @Override
        protected void setHI(int value) {
            super.setHI(value);
            this.timerReload = (this.timerReload & 0b000_1111_1111) | ((value & 0b111) << 8);
            this.linearCounterReloadFlag = true;
        }

        protected void setLinear(int value) {
            this.linearCounterReload = value & 0b1111111;
            this.haltLengthCounter = (value & (1 << 7)) != 0;
        }

        @Override
        protected void clockTimer() {
            this.timer--;
            if (this.timer < 0) {
                // Also prevent the increase of the sequencer step if the timer reload value is less than 2
                // The hardware accurate workaround is to instead force the digital output to 7 if the timer reload value
                // is less than 2 and allow the sequencer step to advance, but this produces noticeable pops in audio,
                // especially at the start, due to the immediate discontinuity.
                // We choose instead to keep the sequencer step the same to keep the triangle channel's output constant, avoiding aliasing.
                if (this.getLengthCounter() > 0 && this.linearCounter > 0 && this.getTimerReload() >= 2) {
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
            return TRIANGLE_WAVEFORM_LUT[this.sequencerStep];
        }

    }

    private class NoiseChannel extends WaveformChannel {

        private static final int[] NTSC_TIMER_PERIOD_LUT = {
                // Values are in CPU cycles!
                4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        };

        private static final int[] PAL_TIMER_PERIOD_LUT = {
                // Values are in CPU cycles!
                4, 8, 14, 30, 60, 88, 118, 148, 188, 236, 354, 472, 708,  944, 1890, 3778
        };

        private final int[] cpuCyclesPeriodLut;

        // VOL registers
        private int envelopeDividerPeriod;
        private boolean constantVolumeFlag;

        // LO registers
        private boolean mode;
        private int timerPeriod;

        private int lfsr;

        private int timer;

        private boolean envelopeStartFlag;
        private int envelopeDivider;
        private int envelopeDecayCounter;

        protected NoiseChannel() {
            // Value on power-up
            this.lfsr = 1;
            if (emulator.getTVSystem() == NESEmulator.TVSystem.PAL) {
                this.cpuCyclesPeriodLut = PAL_TIMER_PERIOD_LUT;
            } else {
                this.cpuCyclesPeriodLut = NTSC_TIMER_PERIOD_LUT;
            }
        }

        private boolean getConstantVolumeFlag() {
            return this.constantVolumeFlag;
        }

        private int getEnvelopeDividerPeriod() {
            return this.envelopeDividerPeriod;
        }

        private boolean getMode() {
            return this.mode;
        }

        private int getTimerPeriod() {
            return this.timerPeriod;
        }

        @Override
        protected void setVolume(int value) {
            super.setVolume(value);
            this.envelopeDividerPeriod = value & 0b1111;
            this.constantVolumeFlag = (value & (1 << 4)) != 0;
        }

        @Override
        protected void setLO(int value) {
            super.setLO(value);
            this.timerPeriod = value & 0b1111;
            this.mode = (value & (1 << 7)) != 0;
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

        private static final int[] NTSC_RATE_PERIOD_LUT = {
                // Values are in CPU cycles!
                428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
        };

        private static final int[] PAL_RATE_PERIOD_LUT = {
                // Values are in CPU cycles!
                398, 354, 316, 298, 276, 236, 210, 198, 176, 148, 132, 118,  98,  78,  66,  50
        };

        private final int[] ratePeriodLut;

        // FREQ registers
        private boolean irqEnableFlag;
        private boolean loopFlag;
        private int rateIndex;

        // START register
        private int sampleAddress;

        // LENGTH register
        private int sampleLength;

        private boolean interruptFlag;

        private int timer;

        private boolean sampleBufferEmpty = true;

        private int sampleBuffer;
        private boolean silenceFlag;
        private int shiftRegister;
        private int outputLevel;

        private int bitsRemainingCounter;
        private int currentAddress;
        private int bytesRemainingCounter;
		private DmcDmaType pendingImplicitDmaType;
		private int pendingImplicitDmaAddress;

        protected DMCChannel() {
            if (emulator.getTVSystem() == NESEmulator.TVSystem.PAL) {
                this.ratePeriodLut = PAL_RATE_PERIOD_LUT;
            } else {
                this.ratePeriodLut = NTSC_RATE_PERIOD_LUT;
            }
        }

        @Override
        protected void setEnabled(boolean value) {
            boolean wasEnabled = this.isEnabled();

            super.setEnabled(value);

            if (this.isEnabled()) {
                this.checkImplicitStopDmcDma();

                if (this.bytesRemainingCounter <= 0) {
                    this.resetSample();
                }

                this.checkStartDmcDma(DmcDmaType.LOAD);
            } else {
                if (wasEnabled) {
                    this.checkExplicitStopDmcDma();
                }

                this.bytesRemainingCounter = 0;
            }
        }

        private void resetSample() {
            this.currentAddress = this.getSampleAddress();
            this.bytesRemainingCounter = this.getSampleLength();
        }

        protected void setFreq(int value) {
            this.irqEnableFlag = (value & (1 << 7)) != 0;
            this.loopFlag = (value & (1 << 6)) != 0;
            this.rateIndex = value & 0b1111;
            if (!this.getIRQEnabledFlag()) {
                this.interruptFlag = false;
            }
        }

        protected void setRaw(int value) {
            this.setOutputLevel(value);
        }

        protected void setStart(int value) {
            this.sampleAddress = 0xC000 + (value * 64);
        }

        protected void setLength(int value) {
            this.sampleLength = (value * 16) + 1;
        }

        private boolean getIRQEnabledFlag() {
            return this.irqEnableFlag;
        }

        private boolean getLoopFlag() {
            return this.loopFlag;
        }

        private int getRateIndex() {
            return this.rateIndex;
        }

        private int getSampleAddress() {
            return this.sampleAddress;
        }

        private int getSampleLength() {
            return this.sampleLength;
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
            this.timer--;
            if (this.timer <= 0) {
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

        private void checkExplicitStopDmcDma() {
            if (this.bytesRemainingCounter <= 0) {
                return;
            }

            int ratePeriod = this.ratePeriodLut[this.getRateIndex()];
            int cyclesUntilBufferEmpty = this.timer + ((this.bitsRemainingCounter - 1) * ratePeriod);
            int reloadWindow = ratePeriod * 8;

            DmcDmaType explicitAbortType = null;

            if (this.sampleBufferEmpty) {
                if (cyclesUntilBufferEmpty == reloadWindow - 2) {
                    explicitAbortType = DmcDmaType.EXPLICIT_ABORT_0;
                } else if (cyclesUntilBufferEmpty == reloadWindow - 1) {
                    explicitAbortType = DmcDmaType.EXPLICIT_ABORT_MINUS_1;
                } else if (cyclesUntilBufferEmpty == reloadWindow) {
                    explicitAbortType = DmcDmaType.EXPLICIT_ABORT_MINUS_2_OR_3;
                }
            } else if (cyclesUntilBufferEmpty == 1) {
                explicitAbortType = DmcDmaType.EXPLICIT_ABORT_MINUS_2_OR_3;
            }

            if (explicitAbortType != null) {
                emulator.getRicohCore().triggerExplicitStopDmcDma(explicitAbortType, this.currentAddress);
            }
        }

        private void checkImplicitStopDmcDma() {
            this.pendingImplicitDmaType = null;

            if (!this.sampleBufferEmpty || this.bytesRemainingCounter > 0 || this.getSampleLength() != 1) {
                return;
            }

            int ratePeriod = this.ratePeriodLut[this.getRateIndex()];
            int cyclesUntilBufferEmpty = this.timer + ((this.bitsRemainingCounter - 1) * ratePeriod);

            if (cyclesUntilBufferEmpty == 4 || cyclesUntilBufferEmpty == 5) {
                this.pendingImplicitDmaType = DmcDmaType.IMPLICIT_UNEXPECTED_RELOAD;
                this.pendingImplicitDmaAddress = this.getSampleAddress();
            } else if (cyclesUntilBufferEmpty == 6 || cyclesUntilBufferEmpty == 7) {
                this.pendingImplicitDmaType = this.getLoopFlag() ? DmcDmaType.IMPLICIT_UNEXPECTED_RELOAD : DmcDmaType.IMPLICIT_ABORT;
                this.pendingImplicitDmaAddress = this.getSampleAddress();
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
		RELOAD,
		EXPLICIT_ABORT_0,
		EXPLICIT_ABORT_MINUS_1,
		EXPLICIT_ABORT_MINUS_2_OR_3,
		IMPLICIT_UNEXPECTED_RELOAD,
		IMPLICIT_ABORT
	}

}