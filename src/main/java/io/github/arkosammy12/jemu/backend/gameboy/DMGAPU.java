package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.Bus;
import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.arkosammy12.jemu.backend.gameboy.GameBoyMMIOBus.*;

public class DMGAPU<E extends GameBoyEmulator> extends AudioGenerator<E> implements Bus {

    private static final int UNUSED_BITS_NR10 = 0b10000000;
    private static final int UNUSED_BITS_NRX1 = 0b00111111;
    private static final int UNUSED_BITS_NRX4 = 0b10111111;
    private static final int UNUSED_BITS_NR30 = 0b01111111;
    private static final int UNUSED_BITS_NR32 = 0b10011111;
    private static final int UNUSED_BITS_NR41 = 0b11000000;
    private static final int UNUSED_BITS_NR52 = 0b01110000;

    private static final int[][] DUTY_CYCLES = {
            {0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 1, 1},
            {1, 0, 0, 0, 0, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 0, 0}
    };

    private final Channel1 channel1 = new Channel1();
    private final Channel2 channel2 = new Channel2();

    private int channel3DacEnable = UNUSED_BITS_NR30; // NR30
    private int channel3LengthTimer = UNUSED_BITS_NRX1; // NR31
    private int channel3OutputLevel = UNUSED_BITS_NR32; // NR32
    private int channel3PeriodLow; // NR33
    private int channel3PeriodHighAndControl = UNUSED_BITS_NRX4; //NR34

    private int channel4LengthTimer = UNUSED_BITS_NR41; // NR41
    private int channel4VolumeAndEnvelope; // NR42
    private int channel4FrequencyAndRandomness; // NR43
    private int channel4Control = UNUSED_BITS_NRX4; // NR44

    private int nr50; // NR50
    private int nr51 = UNUSED_BITS_NRX1; // NR51
    private int nr52 = UNUSED_BITS_NR52; // NR52


    /*
    // TODO randomize this, but this is one sample from my hardware. This is from @dtabacaru in emudev discord
    wave_ram = {0xE2, 0xB7, 0x10, 0x95, 0xC8, 0x6B, 0x0A, 0xF7, 0x02, 0xF6, 0x63, 0xCB, 0x59, 0xE3, 0x90, 0x2F};
     */

    private final byte[] leftChannelSamples = new byte[GameBoyEmulator.T_CYCLES_PER_FRAME];
    private final byte[] rightChannelSamples = new byte[GameBoyEmulator.T_CYCLES_PER_FRAME];
    private int currentSampleIndex = 0;

    private int lengthControlCycle;
    private int volumeEnvelopeCycle;
    private int sweepCycle;

    private int channel1WaveDutyIndex;
    private int channel1WavePeriodTimer;

    private int channel1EnvelopePeriodTimer;
    private int channel1EnvelopeCurrentVolume;

    private int channel1LengthTimer;

    private final int[] waveRam = new int[16];

    public DMGAPU(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            return this.waveRam[address - WAVERAM_START];
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
                case NR30_ADDR -> this.channel3DacEnable | UNUSED_BITS_NR30;
                case NR31_ADDR -> 0xFF;
                case NR32_ADDR -> this.channel3OutputLevel | UNUSED_BITS_NR32;
                case NR33_ADDR -> 0xFF;
                case NR34_ADDR -> this.channel3PeriodHighAndControl | UNUSED_BITS_NRX4;
                case NR41_ADDR -> 0xFF;
                case NR42_ADDR -> this.channel4VolumeAndEnvelope;
                case NR43_ADDR -> this.channel4FrequencyAndRandomness;
                case NR44_ADDR -> this.channel4Control | UNUSED_BITS_NRX4;
                case NR50_ADDR -> this.nr50;
                case NR51_ADDR -> this.nr51 | UNUSED_BITS_NRX1;
                case NR52_ADDR -> this.nr52 | UNUSED_BITS_NR52;
                default -> throw new EmulatorException("Invalid address \"%04X\" for GameBoy APU".formatted(address));
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            this.waveRam[address - WAVERAM_START] = value & 0xFF;
        } else {
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
                case NR30_ADDR -> this.channel3DacEnable = value & 0xF;
                case NR31_ADDR -> this.channel3LengthTimer = value & 0xFF;
                case NR32_ADDR -> this.channel3OutputLevel = value & 0xFF;
                case NR33_ADDR -> this.channel3PeriodLow = value & 0xFF;
                case NR34_ADDR -> this.channel3PeriodHighAndControl = value & 0xFF;
                case NR41_ADDR -> this.channel4LengthTimer = value & 0xFF;
                case NR42_ADDR -> this.channel4VolumeAndEnvelope = value & 0xFF;
                case NR43_ADDR -> this.channel4FrequencyAndRandomness = value & 0xFF;
                case NR44_ADDR -> this.channel4Control = value & 0xFF;
                case NR50_ADDR -> this.nr50 = value & 0xFF;
                case NR51_ADDR -> this.nr51 = value & 0xFF;
                case NR52_ADDR -> this.nr52 = (value & 0b11110000);
                default -> throw new EmulatorException("Invalid address \"%04X\" for GameBoy APU".formatted(address));
            }
        }
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
            double frac = pos - index;

            int nextIndex = Math.min(index + 1, GameBoyEmulator.T_CYCLES_PER_FRAME - 1);

            float l1 = this.leftChannelSamples[index];
            float l2 = this.leftChannelSamples[nextIndex];
            float leftSample = (float) (l1 + (l2 - l1) * frac);

            float r1 = this.rightChannelSamples[index];
            float r2 = this.rightChannelSamples[nextIndex];
            float rightSample = (float) (r1 + (r2 - r1) * frac);

            out[i * 2] = (byte) leftSample;
            out[(i * 2) + 1] = (byte) rightSample;

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
            float ch1Dac = this.channel1.tick();
            float ch2Dac = this.channel2.tick();

            byte leftSample = 0;
            byte rightSample = 0;

            if (this.getMasterAudioEnable()) {
                if (this.currentSampleIndex >= 0 && this.currentSampleIndex < this.leftChannelSamples.length) {
                    leftSample = this.mixLeft(ch1Dac, ch2Dac, 0, 0);
                    rightSample = this.mixRight(ch1Dac, ch2Dac, 0, 0);
                }
            }

            if (this.currentSampleIndex >= 0 && this.currentSampleIndex < this.leftChannelSamples.length) {
                this.leftChannelSamples[this.currentSampleIndex] = leftSample;
                this.rightChannelSamples[this.currentSampleIndex] = rightSample;
                this.currentSampleIndex++;
            }

        }

    }

    private void tickFrameSequencer() {
        this.lengthControlCycle++;
        if (this.lengthControlCycle >= 2) {
            this.lengthControlCycle = 0;
            this.channel1.clockLength();
            this.channel2.clockLength();
        }

        this.volumeEnvelopeCycle++;
        if (this.volumeEnvelopeCycle >= 8) {
            this.volumeEnvelopeCycle = 0;
            this.channel1.clockEnvelope();
            this.channel2.clockEnvelope();
        }

        this.sweepCycle++;
        if (this.sweepCycle >= 4) {
            this.sweepCycle = 0;
            this.channel1.clockSweep();
        }
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

    private byte mixLeft(float ch1Dac, float ch2Dac, float ch3Dac, float ch4Dac) {
        float sample = 0;
        if (this.channel1.getLeft()) {
            sample += ch1Dac;
        }
        if (this.channel2.getLeft()) {
            sample += ch2Dac;
        }
        float scaled = sample / 4.0f;
        return (byte)(scaled * 127.0f);
    }

    private byte mixRight(float ch1Dac, float ch2Dac, float ch3Dac, float ch4Dac) {
        float sample = 0;
        if (this.channel1.getRight()) {
            sample += ch1Dac;
        }
        if (this.channel2.getRight()) {
            sample += ch2Dac;
        }
        float scaled = sample / 4.0f;   // normalize channels
        return (byte)(scaled * 127.0f);
    }

    private abstract static class AudioChannel {

        protected int nrx1;
        protected int nrx2;
        protected int nrx3;
        protected int nrx4;

        protected int lengthTimer;

        abstract void setEnabled(boolean enable);

        abstract boolean getEnabled();

        abstract boolean getLeft();

        abstract boolean getRight();

        void setNRX1(int value) {
            this.nrx1 = value & 0xFF;
        }

        int getNRX1() {
            return this.nrx1;
        }

        void setNRX2(int value) {
            this.nrx2 = value & 0xFF;
        }

        int getNRX2() {
            return this.nrx2;
        }

        void setNRX3(int value) {
            this.nrx3 = value & 0xFF;
        }

        private int getNRX3() {
            return this.nrx3;
        }

        void setNRX4(int value) {
            this.nrx4 = value & 0xFF;
            if (this.getTrigger()) {
                this.trigger();
            }
        }

        int getNRX4() {
            return this.nrx4;
        }

        abstract boolean getLengthEnable();

        abstract float tick();

        boolean getTrigger() {
            return (this.nrx4 & (1 << 7)) != 0;
        }

        abstract void trigger();

        void clockLength() {
            if (this.getLengthEnable()) {
                this.lengthTimer--;
                if (this.lengthTimer <= 0) {
                    this.setEnabled(false);
                }
            }
        }

    }

    private class Channel2 extends AudioChannel {

        private int waveDutyIndex;
        protected int wavePeriodTimer;

        private int envelopePeriodTimer;
        private int envelopeCurrentVolume;

        @Override
        void setEnabled(boolean enable) {
            if (enable) {
                nr52 |= 1 << 1;
            } else {
                nr52 &= ~(1 << 1);
            }
        }

        @Override
        boolean getEnabled() {
            return (nr52 & (1 << 1)) != 0;
        }

        @Override
        boolean getLeft() {
            return (nr51 & (1 << 5)) != 0;
        }

        @Override
        boolean getRight() {
            return (nr51 & (1 << 1)) != 0;
        }

        private int getWaveDuty() {
            return (this.nrx1 >>> 6) & 0b11;
        }

        private int getInitialLengthTimer() {
            return this.nrx1 & 0b111111;
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

        @Override
        boolean getLengthEnable() {
            return (this.nrx4 & (1 << 6)) != 0;
        }

        private int getPeriodHigh() {
            return this.nrx4 & 0b111;
        }

        int getPeriodFull() {
            return (this.getPeriodHigh() << 8) | this.nrx3;
        }

        @Override
        float tick() {
            if (!this.getEnabled()) {
                return 0;
            }
            this.wavePeriodTimer--;
            if (this.wavePeriodTimer <= 0) {
                this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 4;
                this.waveDutyIndex = (this.waveDutyIndex + 1) % 8;
            }
            int amplitude = DUTY_CYCLES[this.getWaveDuty()][this.waveDutyIndex];
            int dacInput = amplitude * this.envelopeCurrentVolume;
            return (dacInput / 15.0f) * 2.0f - 1.0f;
        }

        @Override
        void trigger() {
            this.setEnabled(true);
            if (this.lengthTimer == 0) {
                this.lengthTimer = 64 - this.getInitialLengthTimer();
            }
            this.wavePeriodTimer = (2048 - this.getPeriodFull()) * 4;
            this.envelopePeriodTimer = this.getEnvelopeSweepPace();
            this.envelopeCurrentVolume = this.getInitialVolume();
            this.waveDutyIndex = 0;
        }

        void clockEnvelope() {
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
                }
            }
        }

    }

    private class Channel1 extends Channel2 {

        private int nr10;

        private boolean sweepEnable;
        private int shadowFrequency;
        private int sweepTimer;

        @Override
        void setEnabled(boolean enable) {
            if (enable) {
                nr52 |= 1;
            } else {
                nr52 &= ~1;
            }
        }

        @Override
        boolean getEnabled() {
            return (nr52 & 1) != 0;
        }

        @Override
        boolean getLeft() {
            return (nr51 & (1 << 4)) != 0;
        }

        @Override
        boolean getRight() {
            return (nr51 & 1) != 0;
        }

        private void setNR10(int value) {
            this.nr10 = value & 0xFF;
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
        void trigger() {
            super.trigger();
            this.shadowFrequency = this.getPeriodFull();
            this.sweepTimer = this.getSweepFrequencyPace();
            if (this.sweepTimer == 0) {
                this.sweepTimer = 8;
            }
            this.sweepEnable = this.getSweepFrequencyPace() != 0 || this.getSweepIndividualStep() != 0;
            if (this.getSweepIndividualStep() != 0) {
                // Overflow check
                this.calculateFrequency();
            }
        }

        private void clockSweep() {
            if (!this.getEnabled()) {
                return;
            }

            if (this.sweepTimer > 0) {
                this.sweepTimer--;
            }
            if (this.sweepTimer == 0) {
                this.sweepTimer = this.getSweepFrequencyPace();
                if (this.sweepTimer == 0) {
                    this.sweepTimer = 8;
                }
            }

            if (this.sweepEnable && this.getSweepFrequencyPace() > 0) {
                int newFrequency = this.calculateFrequency();
                if (newFrequency <= 2047 && this.getSweepIndividualStep() > 0) {
                    this.wavePeriodTimer = (2048 - newFrequency) * 4;
                    this.shadowFrequency = newFrequency;

                    this.nrx3 = newFrequency & 0xFF;
                    this.nrx4 = (this.nrx4 & 0b11111000) | ((newFrequency >>> 8) & 0b111);

                    // Overflow check
                    this.calculateFrequency();
                }
            }
        }

        private int calculateFrequency() {

            int newFrequency = this.shadowFrequency >>> this.getSweepIndividualStep();

            if (this.getSweepDirection()) {
                newFrequency = this.shadowFrequency - newFrequency;
            } else {
                newFrequency = this.shadowFrequency + newFrequency;
            }

            if (newFrequency > 2047) {
                this.setEnabled(false);
            }
            return newFrequency;
        }

    }

    /*
    private void setChannel4Enable(boolean enable) {
        if (enable) {
            this.nr52 |= 1 << 3;
        } else {
            this.nr52 &= ~(1 << 3);
        }
    }

    private boolean getChannel4Enable() {
        return (this.nr52 & (1 << 3)) != 0;
    }

    private void setChannel3Enable(boolean enable) {
        if (enable) {
            this.nr52 |= 1 << 2;
        } else {
            this.nr52 &= ~(1 << 2);
        }
    }

    private boolean getChannel3Enable() {
        return (this.nr52 & (1 << 2)) != 0;
    }


    private boolean getChannel4Left() {
        return (this.nr51 & (1 << 7)) != 0;
    }

    private boolean getChannel3Left() {
        return (this.nr51 & (1 << 6)) != 0;
    }

    private boolean getChannel4Right() {
        return (this.nr51 & (1 << 3)) != 0;
    }

    private boolean getChannel3Right() {
        return (this.nr51 & (1 << 2)) != 0;
    }

     */


}
