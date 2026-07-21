package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.gameboycolor.GameBoyColorEmulator;
import io.github.arkosammy12.jemu.core.util.HighPassFilter;
import io.github.arkosammy12.jemu.core.util.LowPassFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.common.SystemHost.intToByteArray;

public class DMGAPU<E extends GameBoyEmulator> implements AudioGenerator, Bus {

    public static final int NR10_ADDR = 0xFF10;
    public static final int NR11_ADDR = 0xFF11;
    public static final int NR12_ADDR = 0xFF12;
    public static final int NR13_ADDR = 0xFF13;
    public static final int NR14_ADDR = 0xFF14;

    public static final int NR21_ADDR = 0xFF16;
    public static final int NR22_ADDR = 0xFF17;
    public static final int NR23_ADDR = 0xFF18;
    public static final int NR24_ADDR = 0xFF19;
    public static final int NR30_ADDR = 0xFF1A;
    public static final int NR31_ADDR = 0xFF1B;
    public static final int NR32_ADDR = 0xFF1C;
    public static final int NR33_ADDR = 0xFF1D;
    public static final int NR34_ADDR = 0xFF1E;

    public static final int NR41_ADDR = 0xFF20;
    public static final int NR42_ADDR = 0xFF21;
    public static final int NR43_ADDR = 0xFF22;
    public static final int NR44_ADDR = 0xFF23;
    public static final int NR50_ADDR = 0xFF24;
    public static final int NR51_ADDR = 0xFF25;
    public static final int NR52_ADDR = 0xFF26;

    public static final int WAVERAM_START = 0xFF30;
    public static final int WAVERAM_END = 0xFF3F;

    private static final int UNUSED_BITS_NR10 = 0b10000000;
    private static final int UNUSED_BITS_NRX1 = 0b00111111;
    private static final int UNUSED_BITS_NRX4 = 0b10111111;
    private static final int UNUSED_BITS_NR30 = 0b01111111;
    private static final int UNUSED_BITS_NR32 = 0b10011111;
    private static final int UNUSED_BITS_NR52 = 0b01110000;

    private static final double MAX_VOLUME = 15.0f;
    private static final double SAMPLE_SCALE = Short.MAX_VALUE;

    private final E emulator;
    private final SampleFrameResampler resampler;

    private final double[] leftChannelSamples = new double[GameBoyEmulator.T_CYCLES_PER_FRAME];
    private final double[] rightChannelSamples = new double[GameBoyEmulator.T_CYCLES_PER_FRAME];
    private final boolean[] dacEnableSamples = new boolean[GameBoyEmulator.T_CYCLES_PER_FRAME];
    private int currentSampleIndex = 0;

    private int frameSequencerStep;

    protected int nr50;
    private int leftVolume;
    private int rightVolume;

    protected int nr51;

    private int nr52;
    private boolean masterAudioEnable;

    protected final Channel1 channel1 = new Channel1();
    protected final Channel2 channel2 = new Channel2();
    protected final DMGAPU<?>.Channel3 channel3;
    protected final Channel4 channel4 = new Channel4();

    public DMGAPU(E emulator) {
        this.emulator = emulator;
        this.channel3 = this.createChannel3();
        this.resampler = new SampleFrameResampler() {

            private final LowPassFilter leftLpf = new LowPassFilter();
            private final HighPassFilter leftHpf = new HighPassFilter();

            private final LowPassFilter rightLpf = new LowPassFilter();
            private final HighPassFilter rightHpf = new HighPassFilter();

            {
                this.leftLpf.createLpf(22000, GameBoyEmulator.T_CYCLES_PER_FRAME * emulator.getFramerate());
                this.rightLpf.createLpf(22000, GameBoyEmulator.T_CYCLES_PER_FRAME * emulator.getFramerate());
            }

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(double[] leftChannelSampleFrame, double[] rightChannelSampleFrame, boolean[] dacEnableSampleFrame)) {
                    for (int i = 0; i < GameBoyColorEmulator.T_CYCLES_PER_FRAME; i++) {
                        if (!dacEnableSampleFrame[i]) {
                            this.leftLpf.resetState();
                            this.rightLpf.resetState();
                        }
                        leftChannelSampleFrame[i] = this.leftLpf.process(leftChannelSampleFrame[i]);
                        rightChannelSampleFrame[i] = this.rightLpf.process(rightChannelSampleFrame[i]);
                    }

                    byte[] out = new byte[outputSamplesPerFrame * 4];
                    double step = (double) GameBoyEmulator.T_CYCLES_PER_FRAME / (double) outputSamplesPerFrame;
                    double pos = 0.0;

                    this.leftHpf.createHpf(28, outputSampleRate);
                    this.rightHpf.createHpf(28, outputSampleRate);

                    for (int i = 0; i < outputSamplesPerFrame; i++) {
                        int index = Math.toIntExact(Math.round(pos));
                        int nextIndex = Math.min(index + 1, GameBoyEmulator.T_CYCLES_PER_FRAME - 1);

                        if (!dacEnableSampleFrame[nextIndex]) {
                            this.leftHpf.resetState();
                            this.rightHpf.resetState();
                        }

                        short left = (short) Math.clamp((long) (this.leftHpf.process(leftChannelSampleFrame[nextIndex]) * SAMPLE_SCALE), -Short.MAX_VALUE, Short.MAX_VALUE);
                        short right = (short) Math.clamp((long) (this.rightHpf.process(rightChannelSampleFrame[nextIndex]) * SAMPLE_SCALE), -Short.MAX_VALUE, Short.MAX_VALUE);

                        out[i * 4] = (byte) ((int) left & 0xFF);
                        out[(i * 4) + 1] = (byte) (((int) left >>> 8) & 0xFF);
                        out[(i * 4) + 2] = (byte) ((int) right & 0xFF);
                        out[(i * 4) + 3] = (byte) (((int) right >>> 8) & 0xFF);

                        pos += step;
                    }
                    return Optional.of(out);
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    protected DMGAPU<?>.Channel3 createChannel3() {
        return new Channel3();
    }

    @Override
    public int readByte(int address) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            return this.channel3.readWaveRAM(address);
        } else {
            return switch (address) {
                case NR13_ADDR, NR23_ADDR, NR31_ADDR, NR33_ADDR, NR41_ADDR -> 0xFF;
                case NR10_ADDR -> this.channel1.getNR10() | UNUSED_BITS_NR10;
                case NR11_ADDR -> this.channel1.getNRX1() | UNUSED_BITS_NRX1;
                case NR12_ADDR -> this.channel1.getNRX2();
                case NR14_ADDR -> this.channel1.getNRX4() | UNUSED_BITS_NRX4;
                case NR21_ADDR -> this.channel2.getNRX1() | UNUSED_BITS_NRX1;
                case NR22_ADDR -> this.channel2.getNRX2();
                case NR24_ADDR -> this.channel2.getNRX4() | UNUSED_BITS_NRX4;
                case NR30_ADDR -> this.channel3.getNR30() | UNUSED_BITS_NR30;
                case NR32_ADDR -> this.channel3.getNRX2() | UNUSED_BITS_NR32;
                case NR34_ADDR -> this.channel3.getNRX4() | UNUSED_BITS_NRX4;
                case NR42_ADDR -> this.channel4.getNRX2();
                case NR43_ADDR -> this.channel4.getNRX3();
                case NR44_ADDR -> this.channel4.getNRX4() | UNUSED_BITS_NRX4;
                case NR50_ADDR -> this.nr50;
                case NR51_ADDR -> this.nr51;
                case NR52_ADDR -> this.nr52 | UNUSED_BITS_NR52;
                default -> throw new EmulatorException("Invalid address $%04X for GameBoy APU!".formatted(address));
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            this.channel3.writeWaveRAM(address, value);
        } else if (this.masterAudioEnable || address == NR52_ADDR || address == NR11_ADDR || address == NR21_ADDR || address == NR31_ADDR || address == NR41_ADDR) {
            switch (address) {
                case NR10_ADDR -> this.channel1.setNR10(value);
                case NR11_ADDR -> this.channel1.setNRX1(value);
                case NR12_ADDR -> this.channel1.setNRX2(value);
                case NR13_ADDR -> this.channel1.setNRX3(value);
                case NR14_ADDR -> this.channel1.setNRX4(value);
                case NR21_ADDR -> this.channel2.setNRX1(value);
                case NR22_ADDR -> this.channel2.setNRX2(value);
                case NR23_ADDR -> this.channel2.setNRX3(value);
                case NR24_ADDR -> this.channel2.setNRX4(value);
                case NR30_ADDR -> this.channel3.setNR30(value);
                case NR31_ADDR -> this.channel3.setNRX1(value);
                case NR32_ADDR -> this.channel3.setNRX2(value);
                case NR33_ADDR -> this.channel3.setNRX3(value);
                case NR34_ADDR -> this.channel3.setNRX4(value);
                case NR41_ADDR -> this.channel4.setNRX1(value);
                case NR42_ADDR -> this.channel4.setNRX2(value);
                case NR43_ADDR -> this.channel4.setNRX3(value);
                case NR44_ADDR -> this.channel4.setNRX4(value);
                case NR50_ADDR -> {
                    this.nr50 = value & 0xFF;
                    this.leftVolume = (value >>> 4) & 0b111;
                    this.rightVolume = value & 0b111;
                }
                case NR51_ADDR -> {
                    this.nr51 = value & 0xFF;
                    this.channel4.setLeft((value & (1 << 7)) != 0);
                    this.channel3.setLeft((value & (1 << 6)) != 0);
                    this.channel2.setLeft((value & (1 << 5)) != 0);
                    this.channel1.setLeft((value & (1 << 4)) != 0);

                    this.channel4.setRight((value & (1 << 3)) != 0);
                    this.channel3.setRight((value & (1 << 2)) != 0);
                    this.channel2.setRight((value & (1 << 1)) != 0);
                    this.channel1.setRight((value & 1) != 0);
                }
                case NR52_ADDR -> {
                    boolean oldApuPower = this.masterAudioEnable;
                    this.nr52 = (value & 0b10000000) | (this.nr52 & 0b00001111);
                    this.masterAudioEnable = (this.nr52 & (1 << 7)) != 0;
                    boolean newApuPower = this.masterAudioEnable;

                    if (!oldApuPower && newApuPower) {
                        this.onAPUOn();
                    } else if (oldApuPower && !newApuPower) {
                        this.onAPUOff();
                    }
                }
                default -> throw new EmulatorException("Invalid address $%04X for GameBoy APU!".formatted(address));
            }
        }
    }

    protected void onAPUOn() {
        this.emulator.getTimerController().onAPUPowerOn();
        this.frameSequencerStep = 0;
        this.channel1.waveDutyIndex = 0;
        this.channel2.waveDutyIndex = 0;

        this.channel3.waveSampleBuffer = 0;
        this.channel3.fetchedFirstByte = false;
        this.channel3.firstFetchConsumed = false;
    }

    protected void onAPUOff() {
        this.channel1.nr10 = 0;
        this.channel1.sweepFrequencyPace = 0;
        this.channel1.sweepDirection = false;
        this.channel1.sweepIndividualStep = 0;

        this.channel1.nrx1 = 0;
        this.channel1.waveDuty = 0;

        this.channel1.setNRX2(0);

        this.channel1.nrx3 = 0;
        this.channel1.period = 0;

        this.channel1.nrx4 = 0;
        this.channel1.lengthEnable = false;

        this.channel2.nrx1 = 0;
        this.channel2.waveDuty = 0;

        this.channel2.setNRX2(0);

        this.channel2.nrx3 = 0;
        this.channel2.period = 0;

        this.channel2.nrx4 = 0;
        this.channel2.lengthEnable = false;

        this.channel3.setNR30(0);
        this.channel3.nrx1 = 0;
        this.channel3.nrx2 = 0;
        this.channel3.nrx3 = 0;
        this.channel3.period = 0;
        this.channel3.nrx4 = 0;
        this.channel3.lengthEnable = false;

        this.channel4.nrx1 = 0;
        this.channel4.setNRX2(0);
        this.channel4.nrx3 = 0;
        this.channel4.clockShift = 0;
        this.channel4.lfsrWidth = false;
        this.channel4.clockDivider = 0;
        this.channel4.nrx4 = 0;
        this.channel4.lengthEnable = false;

        this.nr50 = 0;
        this.leftVolume = 0;
        this.rightVolume = 0;

        this.nr51 = 0;
        this.channel4.setLeft(false);
        this.channel3.setLeft(false);
        this.channel2.setLeft(false);
        this.channel1.setLeft(false);
        this.channel4.setRight(false);
        this.channel3.setRight(false);
        this.channel2.setRight(false);
        this.channel1.setRight(false);
    }

    @Override
    public boolean isStereo() {
        return true;
    }

    @Override
    public AudioGenerator.@NotNull SampleSize getBytesPerSample() {
        return SampleSize.BYTES_2;
    }

    @Override
    public Optional<AudioGenerator.SampleFrame> getSampleFrame() {
        this.currentSampleIndex = 0;
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SampleFrame(Arrays.copyOf(this.leftChannelSamples, this.leftChannelSamples.length), Arrays.copyOf(this.rightChannelSamples, this.rightChannelSamples.length), Arrays.copyOf(this.dacEnableSamples, this.dacEnableSamples.length)));
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.resampler;
    }

    public void cycle(boolean tickFrameSequencer) {
        if (tickFrameSequencer) {
            this.tickFrameSequencer();
        }
        for (int i = 0; i < 4; i++) {
            double ch1 = 0;
            double ch2 = 0;
            double ch3 = 0;
            double ch4 = 0;
            if (this.masterAudioEnable) {
                this.channel1.tick();
                this.channel2.tick();
                this.channel3.tick();
                this.channel4.tick();
                ch1 = this.channel1.getDigitalOutput();
                ch2 = this.channel2.getDigitalOutput();
                ch3 = this.channel3.getDigitalOutput();
                ch4 = this.channel4.getDigitalOutput();

                // Hack to avoid aliasing at extremely high frequencies for the square channels
                if (this.channel1.getPeriodFull() > 2046) {
                    ch1 = 0;
                }
                if (this.channel2.getPeriodFull() > 2046) {
                    ch2 = 0;
                }
            }

            ch1 = ((ch1 - ((double) this.channel1.envelopeCurrentVolume / 2.0)) / MAX_VOLUME);
            ch2 = ((ch2 - ((double) this.channel2.envelopeCurrentVolume / 2.0)) / MAX_VOLUME);

            ch3 /= MAX_VOLUME;
            ch4 = (ch4 - ((double) this.channel4.envelopeCurrentVolume / 2.0)) / MAX_VOLUME;

            double left = 0;
            double right = 0;

            if (this.channel1.getLeft()) {
                left += ch1;
            }
            if (this.channel2.getLeft()) {
                left += ch2;
            }
            if (this.channel3.getLeft()) {
                left += ch3;
            }
            if (this.channel4.getLeft()) {
                left += ch4;
            }

            if (this.channel1.getRight()) {
                right += ch1;
            }
            if (this.channel2.getRight()) {
                right += ch2;
            }
            if (this.channel3.getRight()) {
                right += ch3;
            }
            if (this.channel4.getRight()) {
                right += ch4;
            }

            left *= -1.0 * (this.leftVolume + 1.0) / 8.0f;
            right *= -1.0 * (this.rightVolume + 1.0) / 8.0f;

            left /= 4.0f;
            right /= 4.0f;

            boolean dacEnable = this.channel1.getDacEnable() || this.channel2.getDacEnable() || this.channel3.getDacEnable() || this.channel4.getDacEnable();

            this.leftChannelSamples[this.currentSampleIndex] = left;
            this.rightChannelSamples[this.currentSampleIndex] = right;
            this.dacEnableSamples[this.currentSampleIndex] = dacEnable;
            this.currentSampleIndex = (this.currentSampleIndex + 1) % GameBoyEmulator.T_CYCLES_PER_FRAME;
        }
    }

    private void tickFrameSequencer() {
        if (!this.masterAudioEnable) {
            return;
        }

        switch (this.frameSequencerStep) {
            case 0, 2, 4, 6 -> {
                this.channel1.clockLength();
                this.channel2.clockLength();
                this.channel3.clockLength();
                this.channel4.clockLength();
            }
        }

        if (this.frameSequencerStep == 7) {
            this.channel1.clockEnvelope();
            this.channel2.clockEnvelope();
            this.channel4.clockEnvelope();
        }

        if (this.frameSequencerStep == 2 || this.frameSequencerStep == 6) {
            this.channel1.clockSweep();
        }

        this.frameSequencerStep = (this.frameSequencerStep + 1) & 7;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isLengthClockStep() {
        return this.frameSequencerStep == 0 || this.frameSequencerStep == 2 || this.frameSequencerStep == 4 || this.frameSequencerStep == 6;
    }

    private boolean getMasterAudioEnable() {
        return this.masterAudioEnable;
    }

    private record SampleFrame(double[] leftChannelSampleFrame, double[] rightChannelSampleFrame, boolean[] dacEnableSampleFrame) implements AudioGenerator.SampleFrame {}

    protected abstract class AudioChannel {

        protected int nrx1;
        protected int nrx2;
        protected int nrx3;
        protected int nrx4;

        protected boolean enabled;
        private boolean left;
        private boolean right;

        protected boolean lengthEnable;

        public int lengthTimer;

        protected void setEnabled(boolean enable) {
            this.enabled = enable;
        }

        protected boolean getEnabled() {
            return this.enabled;
        }

        protected void setLeft(boolean value) {
            this.left = value;
        }

        protected boolean getLeft() {
            return this.left;
        }

        protected void setRight(boolean value) {
            this.right = value;
        }

        protected boolean getRight() {
            return this.right;
        }

        protected void setNRX1(int value) {
            if (getMasterAudioEnable()) {
                this.nrx1 = value & 0xFF;
            }
        }

        protected int getNRX1() {
            return this.nrx1;
        }

        protected void setNRX2(int value) {
            this.nrx2 = value & 0xFF;
        }

        protected int getNRX2() {
            return this.nrx2;
        }

        protected void setNRX3(int value) {
            this.nrx3 = value & 0xFF;
        }

        protected int getNRX3() {
            return this.nrx3;
        }

        protected void setNRX4(int value) {
            boolean oldEnable = this.getLengthEnable();
            this.nrx4 = value & 0xFF;
            this.lengthEnable = (value & (1 << 6)) != 0;

            // TODO: Possible difference in hardware behavior in CGB. Consult https://gbdev.gg8.se/wiki/articles/Gameboy_sound_hardware#Differences
            if (!oldEnable && this.getLengthEnable() && !isLengthClockStep()) {
                this.clockLength();
            }

            if ((value & (1 << 7)) != 0) {
                this.trigger();
            }
        }

        protected int getNRX4() {
            return this.nrx4;
        }

        protected boolean getLengthEnable() {
            return this.lengthEnable;
        }

        abstract protected boolean getDacEnable();

        protected int getMaxLengthTimer() {
            return 64;
        }

        protected void trigger() {
            if (this.getDacEnable()) {
                this.setEnabled(true);
            }
            if (this.lengthTimer == 0) {
                this.lengthTimer = this.getMaxLengthTimer();
                if (this.getLengthEnable() && !isLengthClockStep()) {
                    this.clockLength();
                }
            }
        }

        abstract protected void tick();

        abstract public int getDigitalOutput();

        protected void clockLength() {
            if (!this.getLengthEnable()) {
                return;
            }
            if (this.lengthTimer <= 0) {
                return;
            }
            this.lengthTimer--;
            if (this.lengthTimer <= 0) {
                this.setEnabled(false);
            }
        }

    }

    protected class Channel2 extends AudioChannel {

        protected static final int[][] DUTY_CYCLES = {
                {0, 0, 0, 0, 0, 0, 0, 1},
                {0, 0, 0, 0, 0, 0, 1, 1},
                {1, 0, 0, 0, 0, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 0, 0}
        };

        protected int waveDuty;
        protected int waveDutyIndex;
        protected int wavePeriodTimer;
        protected int period;

        private int envelopePeriodTimer;
        protected int envelopeCurrentVolume;
        private boolean envelopeUpdating;

        private int initialVolume;
        private boolean envelopeDirection;
        private int envelopeSweepPace;

        @Override
        protected void setEnabled(boolean enable) {
            super.setEnabled(enable);
            if (enable) {
                nr52 |= 1 << 1;
            } else {
                nr52 &= ~(1 << 1);
            }
        }

        private int getWaveDuty() {
            return this.waveDuty;
        }

        @Override
        protected void setNRX1(int value) {
            super.setNRX1(value);
            this.waveDuty = (value >>> 6) & 0b11;
            this.lengthTimer = 64 - (value & 0x3F);
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public void setNRX2(int value) {
            int oldEnvelopeSweepPace = this.getEnvelopeSweepPace();
            boolean oldEnvelopeDirection = this.getEnvelopeDirection();

            super.setNRX2(value);

            // TODO: Possible difference in hardware behavior in CGB. Consult https://gbdev.gg8.se/wiki/articles/Gameboy_sound_hardware#Differences
            this.initialVolume = (value >>> 4) & 0b1111;
            this.envelopeDirection = (value & (1 << 3)) != 0;
            this.envelopeSweepPace = value & 0b111;

            if (!this.getDacEnable()) {
                this.setEnabled(false);
            }

            if (this.getEnabled()) {
                if (oldEnvelopeSweepPace == 0 && this.envelopeUpdating) {
                    this.envelopeCurrentVolume++;
                } else if (!oldEnvelopeDirection) {
                    this.envelopeCurrentVolume += 2;
                }
                if (oldEnvelopeDirection != this.getEnvelopeDirection()) {
                    this.envelopeCurrentVolume = 16 - this.envelopeCurrentVolume;
                }
                this.envelopeCurrentVolume &= 0xF;
            }
        }

        @Override
        protected boolean getDacEnable() {
            return (this.nrx2 & 0xF8) != 0;
        }

        private int getInitialVolume() {
            return this.initialVolume;
        }

        private boolean getEnvelopeDirection() {
            return this.envelopeDirection;
        }

        private int getEnvelopeSweepPace() {
            return this.envelopeSweepPace;
        }

        @Override
        protected void setNRX3(int value) {
            super.setNRX3(value);
            this.period = (this.period & 0b111_0000_0000) | (value & 0xFF);
        }

        @Override
        protected void setNRX4(int value) {
            this.period = (this.period & 0b000_1111_1111) | ((value & 0b111) << 8);
            super.setNRX4(value);
        }

        protected int getPeriodFull() {
            return this.period;
        }

        @Override
        protected void trigger() {
            super.trigger();
            this.wavePeriodTimer = Math.max(4, (2048 - this.getPeriodFull()) * 4);
            this.envelopePeriodTimer = this.getEnvelopeSweepPace();
            this.envelopeCurrentVolume = this.getInitialVolume();
            this.waveDutyIndex = 0;
            this.envelopeUpdating = true;
        }

        @Override
        protected void tick() {
            if (!this.getEnabled()) {
                return;
            }
            this.wavePeriodTimer--;
            if (this.wavePeriodTimer <= 0) {
                this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 4;
                this.waveDutyIndex = (this.waveDutyIndex + 1) % 8;
            }
        }

        @Override
        public int getDigitalOutput() {
            if (this.getEnabled()) {
                int amplitude = DUTY_CYCLES[this.getWaveDuty()][this.waveDutyIndex];
                return amplitude * this.envelopeCurrentVolume;
            } else if (this.getDacEnable()) {
                return 0xF;
            } else {
                return 0;
            }
        }

        protected void clockEnvelope() {
            if (!this.getEnabled()) {
                return;
            }
            if (this.getEnvelopeSweepPace() == 0) {
                return;
            }
            if (this.envelopePeriodTimer > 0) {
                this.envelopePeriodTimer--;
            }
            if (this.envelopePeriodTimer == 0) {
                this.envelopePeriodTimer = this.getEnvelopeSweepPace();
                if ((this.envelopeCurrentVolume < 0xF && this.getEnvelopeDirection()) || (this.envelopeCurrentVolume > 0x0 && !this.getEnvelopeDirection())) {
                    this.envelopeCurrentVolume += this.getEnvelopeDirection() ? 1 : -1;
                } else {
                    this.envelopeUpdating = false;
                }
            }
        }

    }

    protected class Channel1 extends Channel2 {

        protected int nr10;
        private int sweepFrequencyPace;
        private boolean sweepDirection;
        private int sweepIndividualStep;

        private boolean sweepEnable;
        private int sweepShadow;
        private int sweepTimer;
        private boolean sweepNegateUsedSinceTrigger;

        @Override
        protected void setEnabled(boolean enable) {
            this.enabled = enable;
            if (enable) {
                nr52 |= 1;
            } else {
                nr52 &= ~1;
            }
        }

        private void setNR10(int value) {
            boolean oldNegate = this.getSweepDirection();
            this.nr10 = value & 0xFF;
            this.sweepFrequencyPace = (value >>> 4) & 0b111;
            this.sweepDirection = (value & (1 << 3)) != 0;
            this.sweepIndividualStep = value & 0b111;
            if (oldNegate && !this.getSweepDirection() && sweepNegateUsedSinceTrigger) {
                this.setEnabled(false);
            }
        }

        private int getNR10() {
            return this.nr10;
        }

        private int getSweepFrequencyPace() {
            return this.sweepFrequencyPace;
        }

        private boolean getSweepDirection() {
            return this.sweepDirection;
        }

        private int getSweepIndividualStep() {
            return this.sweepIndividualStep;
        }

        @Override
        protected void trigger() {
            super.trigger();
            this.sweepShadow = this.getPeriodFull();
            this.sweepTimer = this.getSweepFrequencyPace() == 0 ? 8 : this.getSweepFrequencyPace();
            this.sweepEnable = this.getSweepFrequencyPace() != 0 || this.getSweepIndividualStep() != 0;
            this.sweepNegateUsedSinceTrigger = false;
            if (this.getSweepIndividualStep() != 0) {
                int newPeriod = this.calculateSweepNewPeriod();
                if (this.getSweepDirection()) {
                    this.sweepNegateUsedSinceTrigger = true;
                }
                if (newPeriod > 2047) {
                    this.setEnabled(false);
                }
            }
        }

        private void clockSweep() {
            if (!this.getEnabled()) {
                return;
            }
            if (!this.sweepEnable) {
                return;
            }
            if (this.sweepTimer > 0) {
                this.sweepTimer--;
            }
            if (this.sweepTimer == 0) {
                this.sweepTimer = (this.getSweepFrequencyPace() == 0) ? 8 : this.getSweepFrequencyPace();
                if (this.getSweepFrequencyPace() == 0) {
                    return;
                }
                int newPeriod = this.calculateSweepNewPeriod();
                if (this.getSweepDirection()) {
                    this.sweepNegateUsedSinceTrigger = true;
                }
                if (newPeriod > 2047) {
                    this.setEnabled(false);
                    return;
                }
                if (this.getSweepIndividualStep() != 0) {
                    this.sweepShadow = newPeriod;
                    this.period = newPeriod;
                    this.nrx3 = newPeriod & 0xFF;
                    this.nrx4 = (this.nrx4 & 0b11111000) | ((newPeriod >>> 8) & 0b111);
                    int check = this.calculateSweepNewPeriod();
                    if (check > 2047) {
                        this.setEnabled(false);
                    }
                }
            }
        }

        private int calculateSweepNewPeriod() {
            int delta = this.sweepShadow >>> this.getSweepIndividualStep();
            return this.getSweepDirection() ? this.sweepShadow - delta : this.sweepShadow + delta;
        }
    }

    protected class Channel3 extends AudioChannel {

        protected final byte[] waveRAM = intToByteArray(new int[] {
                0xE2, 0xB7, 0x10, 0x95,
                0xC8, 0x6B, 0x0A, 0xF7,
                0x02, 0xF6, 0x63, 0xCB,
                0x59, 0xE3, 0x90, 0x2F
        });

        protected int nr30;
        private boolean dacEnable;

        protected int waveSampleBuffer;
        protected int waveRamIndex;
        protected int wavePeriodTimer;
        protected int currentOutputLevel;
        protected int period;

        protected boolean fetchedFirstByte;
        protected boolean firstFetchConsumed;

        @Override
        protected void setEnabled(boolean enable) {
            super.setEnabled(enable);
            if (enable) {
                nr52 |= 1 << 2;
            } else {
                nr52 &= ~(1 << 2);
            }
        }

        @Override
        protected void setNRX1(int value) {
            super.setNRX1(value);
            this.lengthTimer = 256 - value;
        }

        public void setNR30(int value) {
            this.nr30 = value & 0xFF;
            this.dacEnable = (value & (1 << 7)) != 0;
            if (!this.dacEnable) {
                this.setEnabled(false);
            }
        }

        private int getNR30() {
            return this.nr30;
        }

        @Override
        protected boolean getDacEnable() {
            return this.dacEnable;
        }

        @Override
        protected int getMaxLengthTimer() {
            return 256;
        }

        protected int getOutputLevel() {
            return (this.nrx2 >>> 5) & 0b11;
        }

        @Override
        protected void setNRX3(int value) {
            super.setNRX3(value);
            this.period = (this.period & 0b111_0000_0000) | (value & 0xFF);
        }

        @Override
        protected void setNRX4(int value) {
            this.period = (this.period & 0b000_1111_1111) | ((value & 0b111) << 8);
            super.setNRX4(value);
        }

        protected int getPeriodFull() {
            return this.period;
        }

        protected int readWaveRAM(int address) {
            boolean originalFirstFetchConsumed = this.firstFetchConsumed;
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                if (this.wavePeriodTimer <= 2 && originalFirstFetchConsumed) {
                    return (int) this.waveRAM[((this.waveRamIndex - 1) & 31) / 2] & 0xFF;
                } else {
                    return 0xFF;
                }
            } else {
                return (int) this.waveRAM[address - WAVERAM_START] & 0xFF;
            }
        }

        protected void writeWaveRAM(int address, int value) {
            boolean originalFirstFetchConsumed = this.firstFetchConsumed;
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                if (this.wavePeriodTimer <= 2 && originalFirstFetchConsumed) {
                    this.waveRAM[((this.waveRamIndex - 1) & 31) / 2] = (byte) value;
                }
            } else {
                this.waveRAM[address - WAVERAM_START] = (byte) value;
            }
        }

        @Override
        protected void trigger() {
            this.checkWaveRamCorruption();
            super.trigger();
            this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 2;
            this.currentOutputLevel = this.getOutputLevel();
            this.waveRamIndex = 0;
        }

        @Override
        protected void tick() {
            if (!this.getEnabled()) {
                return;
            }

            this.wavePeriodTimer--;
            if (this.wavePeriodTimer <= 0) {
                this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 2;
                this.waveRamIndex = (this.waveRamIndex + 1) % 32;
                this.waveSampleBuffer = (int) this.waveRAM[this.waveRamIndex / 2] & 0xFF;
                this.currentOutputLevel = this.getOutputLevel();
                this.fetchedFirstByte = true;
            }
        }

        @Override
        public int getDigitalOutput() {
            int amplitude;
            if (this.waveRamIndex % 2 == 0) {
                amplitude = (this.waveSampleBuffer >>> 4) & 0xF;
            } else {
                amplitude = this.waveSampleBuffer & 0xF;
            }
            return amplitude >>> this.getShiftAmount();
        }

        protected int getShiftAmount() {
            return switch (this.currentOutputLevel) {
                case 0 -> 4;
                case 1 -> 0;
                case 2 -> 1;
                case 3 -> 2;
                default -> throw new EmulatorException("Invalid CH3 output level \"%d\" for the GameBoy APU!".formatted(this.currentOutputLevel));
            };
        }

        protected void checkWaveRamCorruption() {
            if (this.getEnabled() && this.wavePeriodTimer == 4) {
                int coarseReadByteIndex = ((this.waveRamIndex - 1) & 31) / 2;
                if (coarseReadByteIndex <= 3) {
                    this.waveRAM[0] = this.waveRAM[coarseReadByteIndex];
                } else {
                    int beginIndex = coarseReadByteIndex & ~0b11;
                    for (int i = beginIndex, j = 0; i <= beginIndex + 3; i++, j++) {
                        this.waveRAM[j] = this.waveRAM[i];
                    }
                }
            }
        }

    }

    protected class Channel4 extends AudioChannel {

        private int envelopePeriodTimer;
        protected int envelopeCurrentVolume;
        private boolean envelopeUpdating;

        private int initialVolume;
        private boolean envelopeDirection;
        private int envelopeSweepPace;

        private int clockShift;
        private boolean lfsrWidth;
        private int clockDivider;

        private int wavePeriodTimer;
        private int lfsr;

        @Override
        protected void setEnabled(boolean enable) {
            super.setEnabled(enable);
            if (enable) {
                nr52 |= 1 << 3;
            } else {
                nr52 &= ~(1 << 3);
            }
        }

        private int getInitialVolume() {
            return this.initialVolume;
        }

        @Override
        public void setNRX1(int value) {
            super.setNRX1(value);
            this.lengthTimer = 64 - (value & 0x3F);
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public void setNRX2(int value) {
            int oldPeriod = this.getEnvelopeSweepPace();
            boolean oldIncrease = this.getEnvelopeDirection();

            super.setNRX2(value);

            // TODO: Possible difference in hardware behavior in CGB. Consult https://gbdev.gg8.se/wiki/articles/Gameboy_sound_hardware#Differences
            this.initialVolume = (value >>> 4) & 0b1111;
            this.envelopeDirection = (value & (1 << 3)) != 0;
            this.envelopeSweepPace = value & 0b111;

            if (!this.getDacEnable()) {
                this.setEnabled(false);
            }

            if (this.getEnabled()) {
                if (oldPeriod == 0 && this.envelopeUpdating) {
                    this.envelopeCurrentVolume++;
                } else if (!oldIncrease) {
                    this.envelopeCurrentVolume += 2;
                }
                if (oldIncrease != this.getEnvelopeDirection()) {
                    this.envelopeCurrentVolume = 16 - this.envelopeCurrentVolume;
                }
                this.envelopeCurrentVolume &= 0xF;
            }
        }

        @Override
        protected boolean getDacEnable() {
            return (this.nrx2 & 0xF8) != 0;
        }

        private boolean getEnvelopeDirection() {
            return this.envelopeDirection;
        }

        private int getEnvelopeSweepPace() {
            return this.envelopeSweepPace;
        }

        @Override
        protected void setNRX3(int value) {
            super.setNRX3(value);
            this.clockShift = (value >>> 4) & 0b1111;
            this.lfsrWidth = (value & (1 << 3)) != 0;
            this.clockDivider = value & 0b111;
        }

        private int getClockShift() {
            return this.clockShift;
        }

        private boolean getLFSRWidth() {
            return this.lfsrWidth;
        }

        private int getClockDivider() {
            return this.clockDivider;
        }

        @Override
        protected void trigger() {
            super.trigger();
            this.wavePeriodTimer = (this.getClockDivider() > 0 ? (this.getClockDivider() << 4) : 8) << this.getClockShift();
            this.envelopeCurrentVolume = this.getInitialVolume();
            this.lfsr = 0x7FFF;
            this.envelopeUpdating = true;
        }

        @Override
        protected void tick() {
            if (!this.getEnabled()) {
                return;
            }
            this.wavePeriodTimer--;
            if (this.wavePeriodTimer <= 0) {
                this.wavePeriodTimer = (this.getClockDivider() > 0 ? (this.getClockDivider() << 4) : 8) << this.getClockShift();
                int xorResult = (this.lfsr & 0b01) ^ ((this.lfsr & 0b10) >>> 1);
                this.lfsr = ((this.lfsr >>> 1) | (xorResult << 14)) & 0x7FFF;
                if (this.getLFSRWidth()) {
                    this.lfsr = (this.lfsr & (~(1 << 6))) & 0x7FFF;
                    this.lfsr = (this.lfsr | (xorResult << 6)) & 0x7FFF;
                }
            }
        }

        @Override
        public int getDigitalOutput() {
            if (this.getEnabled()) {
                return (~this.lfsr & 0x01) * this.envelopeCurrentVolume;
            } else if (this.getDacEnable()) {
                return 0xF;
            } else {
                return 0;
            }
        }

        protected void clockEnvelope() {
            if (!this.getEnabled()) {
                return;
            }
            if (this.getEnvelopeSweepPace() == 0) {
                return;
            }
            if (this.envelopePeriodTimer > 0) {
                this.envelopePeriodTimer--;
            }
            if (this.envelopePeriodTimer == 0) {
                this.envelopePeriodTimer = this.getEnvelopeSweepPace();
                if ((this.envelopeCurrentVolume < 0xF && this.getEnvelopeDirection()) || (this.envelopeCurrentVolume > 0x0 && !this.getEnvelopeDirection())) {
                    this.envelopeCurrentVolume += this.getEnvelopeDirection() ? 1 : -1;
                } else {
                    this.envelopeUpdating = false;
                }
            }
        }
    }

}