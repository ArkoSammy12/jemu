package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.Bus;
import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.util.Optional;

import static io.github.arkosammy12.jemu.backend.gameboy.GameBoyMMIOBus.*;

public class DMGAPU<E extends GameBoyEmulator> extends AudioGenerator<E> implements Bus {

    private static final int UNUSED_BITS_NR10 = 0b10000000;
    private static final int UNUSED_BITS_NRX1 = 0b00111111;
    private static final int UNUSED_BITS_NRX4 = 0b10111111;
    private static final int UNUSED_BITS_NR30 = 0b01111111;
    private static final int UNUSED_BITS_NR32 = 0b10011111;
    private static final int UNUSED_BITS_NR52 = 0b01110000;

    private static final float MAX_VOLUME = 15.0f;
    private static final int VOLUME_DIVISOR = 7;
    private static final int CHANNEL_COUNT = 4;
    private static final float SAMPLE_SCALE = 127.0f;

    private static final int[][] DUTY_CYCLES = {
            {0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 1, 1},
            {1, 0, 0, 0, 0, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 0, 0}
    };

    private final byte[] leftChannelSamples = new byte[GameBoyEmulator.T_CYCLES_PER_FRAME];
    private final byte[] rightChannelSamples = new byte[GameBoyEmulator.T_CYCLES_PER_FRAME];
    private int currentSampleIndex = 0;

    private int frameSequencerStep;

    private int nr50;
    private int nr51;
    private int nr52;

    private final Channel1 channel1 = new Channel1();
    private final Channel2 channel2 = new Channel2();
    private final Channel3 channel3 = new Channel3();
    private final Channel4 channel4 = new Channel4();


    public DMGAPU(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            return this.channel3.readWaveRam(address);
        } else {
            return switch (address) {
                case NR10_ADDR -> this.channel1.getNR10() | UNUSED_BITS_NR10;
                case NR11_ADDR -> this.channel1.getNRX1() | UNUSED_BITS_NRX1;
                case NR12_ADDR -> this.channel1.getNRX2();
                case NR13_ADDR -> 0xFF;
                case NR14_ADDR -> this.channel1.getNRX4() | UNUSED_BITS_NRX4;
                case NR21_ADDR -> this.channel2.getNRX1() | UNUSED_BITS_NRX1;
                case NR22_ADDR -> this.channel2.getNRX2();
                case NR23_ADDR -> 0xFF;
                case NR24_ADDR -> this.channel2.getNRX4() | UNUSED_BITS_NRX4;
                case NR30_ADDR -> this.channel3.getNR30() | UNUSED_BITS_NR30;
                case NR31_ADDR -> 0xFF;
                case NR32_ADDR -> this.channel3.getNRX2() | UNUSED_BITS_NR32;
                case NR33_ADDR -> 0xFF;
                case NR34_ADDR -> this.channel3.getNRX4() | UNUSED_BITS_NRX4;
                case NR41_ADDR -> 0xFF;
                case NR42_ADDR -> this.channel4.getNRX2();
                case NR43_ADDR -> this.channel4.getNRX3();
                case NR44_ADDR -> this.channel4.getNRX4() | UNUSED_BITS_NRX4;
                case NR50_ADDR -> this.nr50;
                case NR51_ADDR -> this.nr51;
                case NR52_ADDR -> this.nr52 | UNUSED_BITS_NR52;
                default -> throw new EmulatorException("Invalid address \"%04X\" for GameBoy APU".formatted(address));
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            this.channel3.writeWaveRam(address, value);
        } else if (this.getMasterAudioEnable() || address == NR52_ADDR || address == NR11_ADDR || address == NR21_ADDR || address == NR31_ADDR || address == NR41_ADDR) {
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
                case NR50_ADDR -> this.nr50 = value & 0xFF;
                case NR51_ADDR -> this.nr51 = value & 0xFF;
                case NR52_ADDR -> {
                    boolean oldPower = this.getMasterAudioEnable();
                    this.nr52 = (value & 0b10000000) | (this.nr52 & 0b00001111);
                    boolean newPower = this.getMasterAudioEnable();

                    if (!oldPower && newPower) {
                        this.onPowerOn();
                    }
                    if (oldPower && !newPower) {
                        this.onPowerOff();
                    }
                }
                default -> throw new EmulatorException("Invalid address \"%04X\" for GameBoy APU".formatted(address));
            }
        }
    }

    private void onPowerOn() {
        this.emulator.getTimerController().onAPUPowerOn();
        this.frameSequencerStep = 0;
        this.channel1.waveDutyIndex = 0;
        this.channel2.waveDutyIndex = 0;

        this.channel3.waveSampleBuffer = 0;
        this.channel3.fetchedFirstByte = false;
        this.channel3.firstFetchConsumed = false;
    }

    private void onPowerOff() {
        this.channel1.nr10 = 0;
        this.channel1.nrx1 = 0;
        this.channel1.setNRX2(0);
        this.channel1.nrx3 = 0;
        this.channel1.nrx4 = 0;

        this.channel2.nrx1 = 0;
        this.channel2.setNRX2(0);
        this.channel2.nrx3 = 0;
        this.channel2.nrx4 = 0;

        this.channel3.setNR30(0);
        this.channel3.nrx1 = 0;
        this.channel3.nrx2 = 0;
        this.channel3.nrx3 = 0;
        this.channel3.nrx4 = 0;

        this.channel4.nrx1 = 0;
        this.channel4.setNRX2(0);
        this.channel4.nrx3 = 0;
        this.channel4.nrx4 = 0;

        this.nr50 = 0;
        this.nr51 = 0;
    }

    @Override
    public boolean isStereo() {
        return true;
    }

    @Override
    public AudioGenerator.@NotNull SampleSize getBytesPerSample() {
        return SampleSize.BYTES_1;
    }

    @Override
    public Optional<byte[]> getSampleFrame() {
        Optional<AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }

        AudioDriver audioDriver = optionalAudioDriver.get();
        int samplesPerFrame = audioDriver.getSamplesPerFrame();

        byte[] out = new byte[samplesPerFrame * 2];
        double step = (double) GameBoyEmulator.T_CYCLES_PER_FRAME / samplesPerFrame;
        double pos = 0.0;

        for (int i = 0; i < samplesPerFrame; i++) {
            int index = (int) pos;
            int nextIndex = Math.min(index + 1, GameBoyEmulator.T_CYCLES_PER_FRAME - 1);

            out[i * 2] = this.leftChannelSamples[nextIndex];
            out[(i * 2) + 1] = this.rightChannelSamples[nextIndex];

            pos += step;
        }

        this.currentSampleIndex = 0;
        return Optional.of(out);
    }


    public void cycle(boolean tickFrameSequencer) {
        if (tickFrameSequencer) {
            this.tickFrameSequencer();
        }

        for (int i = 0; i < 4; i++) {

            int ch1 = 0;
            int ch2 = 0;
            int ch3 = 0;
            int ch4 = 0;

            if (this.getMasterAudioEnable()) {
                ch1 = this.channel1.tick();
                ch2 = this.channel2.tick();
                ch3 = this.channel3.tick();
                ch4 = this.channel4.tick();
            }

            this.mixChannels(ch1, ch2, ch3, ch4);

        }

    }

    private void mixChannels(float ch1, float ch2, float ch3, float ch4) {

        ch1 = (float) ((ch1 - (this.channel1.envelopeCurrentVolume / 2.0)) / MAX_VOLUME);
        ch2 = (float) ((ch2 - (this.channel2.envelopeCurrentVolume / 2.0)) / MAX_VOLUME);

        int scale = this.channel3.getShiftAmount() == 4 ? 1 : 1 << this.channel3.getShiftAmount();
        ch3 = (float) ((ch3 - (this.channel3.dcOffset / scale)) / MAX_VOLUME);
        ch4 = (float) ((ch4 - (this.channel4.envelopeCurrentVolume / 2.0)) / MAX_VOLUME);

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

        left /= CHANNEL_COUNT;
        right /= CHANNEL_COUNT;

        left *= (float) this.getLeftVolume() / VOLUME_DIVISOR;
        right *= (float) this.getRightVolume() / VOLUME_DIVISOR;

        this.leftChannelSamples[this.currentSampleIndex] = (byte) (left * SAMPLE_SCALE);
        this.rightChannelSamples[this.currentSampleIndex] = (byte) (right * SAMPLE_SCALE);
        this.currentSampleIndex = (this.currentSampleIndex + 1) % GameBoyEmulator.T_CYCLES_PER_FRAME;
    }

    private void tickFrameSequencer() {
        if (!this.getMasterAudioEnable()) {
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
        return (this.nr52 & (1 << 7)) != 0;
    }

    private int getLeftVolume() {
        return (this.nr50 >>> 4) & 0b111;
    }

    private int getRightVolume() {
        return this.nr50 & 0b111;
    }

    private abstract class AudioChannel {

        protected int nrx1;
        protected int nrx2;
        protected int nrx3;
        protected int nrx4;

        protected int lengthTimer;

        abstract protected void setEnabled(boolean enable);

        abstract protected boolean getEnabled();

        abstract protected boolean getLeft();

        abstract protected boolean getRight();

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

            boolean oldEnable = getLengthEnable();
            this.nrx4 = value & 0xFF;
            boolean newEnable = getLengthEnable();

            if (!oldEnable && newEnable && !isLengthClockStep()) {
                this.clockLength();
            }

            if (this.getTrigger()) {
                this.trigger();
            }
        }

        protected int getNRX4() {
            return this.nrx4;
        }

        protected boolean getTrigger() {
            return (this.nrx4 & (1 << 7)) != 0;
        }

        protected boolean getLengthEnable() {
            return (this.nrx4 & (1 << 6)) != 0;
        }

        abstract protected boolean getDacEnable();

        protected int getMaxLengthTimer() {
            return 64;
        }

        abstract protected int tick();

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

    private class Channel2 extends AudioChannel {

        int waveDutyIndex;
        protected int wavePeriodTimer;

        private int envelopePeriodTimer;
        int envelopeCurrentVolume;
        private boolean envelopeUpdating;

        @Override
        protected void setEnabled(boolean enable) {
            if (enable) {
                nr52 |= 1 << 1;
            } else {
                nr52 &= ~(1 << 1);
            }
        }

        @Override
        protected boolean getEnabled() {
            return (nr52 & (1 << 1)) != 0;
        }

        @Override
        protected boolean getLeft() {
            return (nr51 & (1 << 5)) != 0;
        }

        @Override
        protected boolean getRight() {
            return (nr51 & (1 << 1)) != 0;
        }

        private int getWaveDuty() {
            return (this.nrx1 >>> 6) & 0b11;
        }

        @Override
        protected void setNRX1(int value) {
            super.setNRX1(value);
            this.lengthTimer = 64 - (value & 0x3F);
        }

        protected void setNRX2(int value) {

            int oldPeriod = this.getEnvelopeSweepPace();
            boolean oldIncrease = this.getEnvelopeDirection();

            super.setNRX2(value);
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

        private int getInitialVolume() {
            return (this.nrx2 >>> 4) & 0b1111;
        }

        private boolean getEnvelopeDirection() {
            return (this.nrx2 & (1 << 3)) != 0;
        }

        private int getEnvelopeSweepPace() {
            return this.nrx2 & 0b111;
        }

        private int getPeriodHigh() {
            return this.nrx4 & 0b111;
        }

        int getPeriodFull() {
            return (this.getPeriodHigh() << 8) | this.nrx3;
        }

        @Override
        protected int tick() {
            if (!this.getEnabled()) {
                return 0;
            }
            this.wavePeriodTimer--;
            if (this.wavePeriodTimer <= 0) {
                this.wavePeriodTimer = Math.max(4, (2048 - this.getPeriodFull()) * 4);
                this.waveDutyIndex = (this.waveDutyIndex + 1) % 8;
            }
            int amplitude = DUTY_CYCLES[this.getWaveDuty()][this.waveDutyIndex];
            return amplitude * this.envelopeCurrentVolume;
        }

        @Override
        protected void trigger() {
            super.trigger();
            this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 4;
            this.envelopePeriodTimer = this.getEnvelopeSweepPace();
            this.envelopeCurrentVolume = this.getInitialVolume();
            this.waveDutyIndex = 0;
            this.envelopeUpdating = true;
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
                boolean isUpwards = this.getEnvelopeDirection();
                int adjustment;
                if ((this.envelopeCurrentVolume < 0xF && isUpwards) || (this.envelopeCurrentVolume > 0x0 && !isUpwards)) {
                    if (isUpwards) {
                        adjustment = 1;
                    } else {
                        adjustment = -1;
                    }
                    this.envelopeCurrentVolume += adjustment;
                } else {
                    this.envelopeUpdating = false;
                }
            }
        }

    }

    private class Channel1 extends Channel2 {

        private int nr10;

        private boolean sweepEnable;
        private int sweepShadow;
        private int sweepTimer;
        private boolean sweepNegateUsedSinceTrigger;

        @Override
        protected void setEnabled(boolean enable) {
            if (enable) {
                nr52 |= 1;
            } else {
                nr52 &= ~1;
            }
        }

        @Override
        protected boolean getEnabled() {
            return (nr52 & 1) != 0;
        }

        @Override
        protected boolean getLeft() {
            return (nr51 & (1 << 4)) != 0;
        }

        @Override
        protected boolean getRight() {
            return (nr51 & 1) != 0;
        }

        private void setNR10(int value) {
            boolean oldNegate = this.getSweepDirection();
            this.nr10 = value & 0xFF;
            if (oldNegate && !this.getSweepDirection() && sweepNegateUsedSinceTrigger) {
                this.setEnabled(false);
            }
        }

        private int getNR10() {
            return this.nr10;
        }

        private int getSweepFrequencyPace() {
            return (this.nr10 >>> 4) & 0b111;
        }

        private boolean getSweepDirection() {
            return (this.nr10 & (1 << 3)) != 0;
        }

        private int getSweepIndividualStep() {
            return this.nr10 & 0b111;
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

    private class Channel3 extends AudioChannel {

        private final int[] waveRam = {
                0xE2, 0xB7, 0x10, 0x95,
                0xC8, 0x6B, 0x0A, 0xF7,
                0x02, 0xF6, 0x63, 0xCB,
                0x59, 0xE3, 0x90, 0x2F
        };

        private int nr30;

        private int waveSampleBuffer;
        private int waveRamIndex;
        private int wavePeriodTimer;
        private int currentOutputLevel;
        private double dcOffset;

        private boolean fetchedFirstByte;
        private boolean firstFetchConsumed;

        @Override
        protected void setEnabled(boolean enable) {
            if (enable) {
                nr52 |= 1 << 2;
            } else {
                nr52 &= ~(1 << 2);
            }
        }

        @Override
        protected boolean getEnabled() {
            return (nr52 & (1 << 2)) != 0;
        }

        @Override
        protected boolean getLeft() {
            return (nr51 & (1 << 6)) != 0;
        }

        @Override
        protected boolean getRight() {
            return (nr51 & (1 << 2)) != 0;
        }

        @Override
        protected void setNRX1(int value) {
            super.setNRX1(value);
            this.lengthTimer = 256 - value;
        }

        private void setNR30(int value) {
            this.nr30 = value & 0xFF;
            if (!this.getDacEnable()) {
                this.setEnabled(false);
            }
        }

        private int getNR30() {
            return this.nr30;
        }

        @Override
        protected boolean getDacEnable() {
            return (this.nr30 & (1 << 7)) != 0;
        }

        protected int getMaxLengthTimer() {
            return 256;
        }

        private int getOutputLevel() {
            return (this.nrx2 >>> 5) & 0b11;
        }

        private int getPeriodHigh() {
            return this.nrx4 & 0b111;
        }

        int getPeriodFull() {
            return (this.getPeriodHigh() << 8) | this.nrx3;
        }

        private int readWaveRam(int address) {
            boolean originalFirstFetchConsumed = this.firstFetchConsumed;
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                if (this.wavePeriodTimer <= 2 && originalFirstFetchConsumed) {
                    return this.waveRam[((this.waveRamIndex - 1) & 31) / 2];
                } else {
                    return 0xFF;
                }
            } else {
                return this.waveRam[address - WAVERAM_START];
            }
        }

        private void writeWaveRam(int address, int value) {
            boolean originalFirstFetchConsumed = this.firstFetchConsumed;
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                if (this.wavePeriodTimer <= 2 && originalFirstFetchConsumed) {
                    this.waveRam[((this.waveRamIndex - 1) & 31) / 2] = value & 0xFF;
                }
            } else {
                this.waveRam[address - WAVERAM_START] = value & 0xFF;
            }
        }

        @Override
        protected int tick() {
            if (!this.getEnabled()) {
                return 0;
            }

            this.wavePeriodTimer--;
            if (this.wavePeriodTimer <= 0) {
                this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 2;
                this.waveRamIndex = (this.waveRamIndex + 1) % 32;
                this.waveSampleBuffer = this.waveRam[this.waveRamIndex / 2];
                this.currentOutputLevel = this.getOutputLevel();
                this.fetchedFirstByte = true;
            }

            int amplitude;
            if (this.waveRamIndex % 2 == 0) {
                amplitude = (this.waveSampleBuffer >>> 4) & 0xF;
            } else {
                amplitude = this.waveSampleBuffer & 0xF;
            }

            return amplitude >>> this.getShiftAmount();
        }

        int getShiftAmount() {
            return switch (this.currentOutputLevel) {
                case 0 -> 4;
                case 1 -> 0;
                case 2 -> 1;
                case 3 -> 2;
                default -> throw new EmulatorException("Invalid CH3 output level \"%d\" for the GameBoy!".formatted(this.currentOutputLevel));
            };
        }


        @Override
        protected void trigger() {
            if (this.getEnabled() && this.wavePeriodTimer == 4) {
                int coarseReadByteIndex = ((this.waveRamIndex - 1) & 31) / 2;
                if (coarseReadByteIndex <= 3) {
                    this.waveRam[0] = this.waveRam[coarseReadByteIndex];
                } else {
                    int beginIndex = coarseReadByteIndex & ~3;
                    for (int i = beginIndex, j = 0; i <= beginIndex + 3; i++, j++) {
                        this.waveRam[j] = this.waveRam[i];
                    }
                }
            }

            super.trigger();
            this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 2;
            this.currentOutputLevel = this.getOutputLevel();
            this.waveRamIndex = 0;

            double sum = 0;
            for (int element : this.waveRam) {
                sum += (element >>> 4) & 0xF;
                sum += element & 0xF;
            }
            this.dcOffset = sum / (this.waveRam.length * 2);
        }

    }

    private class Channel4 extends AudioChannel {

        private int envelopePeriodTimer;
        private int envelopeCurrentVolume;
        private boolean envelopeUpdating;

        private int wavePeriodTimer;
        private int lfsr;

        @Override
        protected void setEnabled(boolean enable) {
            if (enable) {
                nr52 |= 1 << 3;
            } else {
                nr52 &= ~(1 << 3);
            }
        }

        @Override
        protected boolean getEnabled() {
            return (nr52 & (1 << 3)) != 0;
        }

        @Override
        protected boolean getLeft() {
            return (nr51 & (1 << 7)) != 0;
        }

        @Override
        protected boolean getRight() {
            return (nr51 & (1 << 3)) != 0;
        }

        private int getInitialVolume() {
            return (this.nrx2 >>> 4) & 0b1111;
        }

        @Override
        protected void setNRX1(int value) {
            super.setNRX1(value);
            this.lengthTimer = 64 - (value & 0x3F);
        }

        @Override
        protected void setNRX2(int value) {

            int oldPeriod = this.getEnvelopeSweepPace();
            boolean oldIncrease = this.getEnvelopeDirection();

            super.setNRX2(value);
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
            return (this.nrx2 & (1 << 3)) != 0;
        }

        private int getEnvelopeSweepPace() {
            return this.nrx2 & 0b111;
        }

        private int getClockShift() {
            return (this.nrx3 >>> 4) & 0b1111;
        }

        private boolean getLFSRWidth() {
            return (this.nrx3 & (1 << 3)) != 0;
        }

        private int getClockDivider() {
            return this.nrx3 & 0b111;
        }

        @Override
        protected int tick() {
            if (!this.getEnabled()) {
                return 0;
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

            int amplitude = ~this.lfsr & 0x01;
            return amplitude * this.envelopeCurrentVolume;
        }

        @Override
        protected void trigger() {
            super.trigger();
            this.wavePeriodTimer = (this.getClockDivider() > 0 ? (this.getClockDivider() << 4) : 8) << this.getClockShift();
            this.envelopeCurrentVolume = this.getInitialVolume();
            this.lfsr = 0x7FFF;
            this.envelopeUpdating = true;
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
                boolean isUpwards = this.getEnvelopeDirection();
                int adjustment;
                if ((this.envelopeCurrentVolume < 0xF && isUpwards) || (this.envelopeCurrentVolume > 0x0 && !isUpwards)) {
                    if (isUpwards) {
                        adjustment = 1;
                    } else {
                        adjustment = -1;
                    }
                    this.envelopeCurrentVolume += adjustment;
                } else {
                    this.envelopeUpdating = false;
                }
            }
        }
    }

}
