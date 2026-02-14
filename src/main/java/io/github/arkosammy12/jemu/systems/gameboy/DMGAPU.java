package io.github.arkosammy12.jemu.systems.gameboy;

import io.github.arkosammy12.jemu.systems.common.Bus;
import io.github.arkosammy12.jemu.systems.common.SoundSystem;

import static io.github.arkosammy12.jemu.systems.gameboy.GameBoyMMIOBus.*;

public class DMGAPU implements SoundSystem, Bus {

    private static final int UNUSED_BITS_NR10 = 0b10000000;
    private static final int UNUSED_BITS_NRX1 = 0b00111111;
    private static final int UNUSED_BITS_NRX4 = 0b10111111;
    private static final int UNUSED_BITS_NR30 = 0b01111111;
    private static final int UNUSED_BITS_NR32 = 0b10011111;
    private static final int UNUSED_BITS_NR41 = 0b11000000;
    private static final int UNUSED_BITS_NR52 = 0b01110000;

    private int channel1Sweep = UNUSED_BITS_NR10; // NR10
    private int channel1LengthTimerAndDutyCycle = UNUSED_BITS_NRX1; // NR11
    private int channel1VolumeAndEnvelope; // NR12
    private int channel1PeriodLow; // NR13
    private int channel1PeriodHighAndControl = UNUSED_BITS_NRX4; // NR14

    private int channel2LengthTimerAndDutyCycle = UNUSED_BITS_NRX1; // NR21
    private int channel2VolumeAndEnvelope; // NR22
    private int channel2PeriodLow; // NR23
    private int channel2PeriodHighAndControl = UNUSED_BITS_NRX4; // NR24

    private int channel3DacEnable = UNUSED_BITS_NR30; // NR30
    private int channel3LengthTimer = UNUSED_BITS_NRX1; // NR31
    private int channel3OutputLevel = UNUSED_BITS_NR32; // NR32
    private int channel3PeriodLow; // NR33
    private int channel3PeriodHighAndControl = UNUSED_BITS_NRX4; //NR34

    private int channel4LengthTimer = UNUSED_BITS_NR41; // NR41
    private int channel4VolumeAndEnvelope; // NR42
    private int channel4FrequencyAndRandomness; // NR43
    private int channel4Control = UNUSED_BITS_NRX4; // NR44

    private int masterVolumeAndVinPanning; // NR50
    private int soundPanning = UNUSED_BITS_NRX1; // NR51
    private int soundToggle = UNUSED_BITS_NR52; // NR52


    /*
    // TODO randomize this, but this is one sample from my hardware. This is from @dtabacaru in emudev discord
    wave_ram = {0xE2, 0xB7, 0x10, 0x95, 0xC8, 0x6B, 0x0A, 0xF7, 0x02, 0xF6, 0x63, 0xCB, 0x59, 0xE3, 0x90, 0x2F};
     */

    private final int[] waveRam = new int[16];

    public DMGAPU(GameBoyEmulator emulator) {

    }


    @Override
    public int readByte(int address) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            return this.waveRam[address - WAVERAM_START];
        } else {
            return switch (address) {
                case NR10_ADDR -> this.channel1Sweep;
                case NR11_ADDR -> this.channel1LengthTimerAndDutyCycle;
                case NR12_ADDR -> this.channel1VolumeAndEnvelope;
                case NR13_ADDR -> 0xFF;
                case NR14_ADDR -> this.channel1PeriodHighAndControl;
                case NR21_ADDR -> this.channel2LengthTimerAndDutyCycle;
                case NR22_ADDR -> this.channel2VolumeAndEnvelope;
                case NR23_ADDR -> 0xFF;
                case NR24_ADDR -> this.channel2PeriodHighAndControl;
                case NR30_ADDR -> this.channel3DacEnable;
                case NR31_ADDR -> 0xFF;
                case NR32_ADDR -> this.channel3OutputLevel;
                case NR33_ADDR -> 0xFF;
                case NR34_ADDR -> this.channel3PeriodHighAndControl;
                case NR41_ADDR -> 0xFF;
                case NR42_ADDR -> this.channel4VolumeAndEnvelope;
                case NR43_ADDR -> this.channel4FrequencyAndRandomness;
                case NR44_ADDR -> this.channel4Control;
                case NR50_ADDR -> this.masterVolumeAndVinPanning;
                case NR51_ADDR -> this.soundPanning;
                case NR52_ADDR -> this.soundToggle;
                default -> throw new IllegalArgumentException("Invalid address \"%04X\" for GameBoy APU".formatted(address));
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= WAVERAM_START && address <= WAVERAM_END) {
            this.waveRam[address - WAVERAM_START] = value & 0xFF;
        } else {
            switch (address) {
                case NR10_ADDR -> this.channel1Sweep = (value & 0xFF) | UNUSED_BITS_NR10;
                case NR11_ADDR -> this.channel1LengthTimerAndDutyCycle = (value & 0xFF) | UNUSED_BITS_NRX1;
                case NR12_ADDR -> this.channel1VolumeAndEnvelope = value & 0xFF;
                case NR13_ADDR -> this.channel1PeriodLow = value & 0xFF;
                case NR14_ADDR -> this.channel1PeriodHighAndControl = (value & 0xFF) | UNUSED_BITS_NRX4;
                case NR21_ADDR -> this.channel2LengthTimerAndDutyCycle = (value & 0xFF) | UNUSED_BITS_NRX1;
                case NR22_ADDR -> this.channel2VolumeAndEnvelope = value & 0xFF;
                case NR23_ADDR -> this.channel2PeriodLow = value & 0xFF;
                case NR24_ADDR -> this.channel2PeriodHighAndControl = (value & 0xFF) | UNUSED_BITS_NRX4;
                case NR30_ADDR -> this.channel3DacEnable = (value & 0xFF) | UNUSED_BITS_NR30;
                case NR31_ADDR -> this.channel3LengthTimer = (value & 0xFF) | UNUSED_BITS_NRX1;
                case NR32_ADDR -> this.channel3OutputLevel = (value & 0xFF) | UNUSED_BITS_NR32;
                case NR33_ADDR -> this.channel3PeriodLow = value & 0xFF;
                case NR34_ADDR -> this.channel3PeriodHighAndControl = (value & 0xFF) | UNUSED_BITS_NRX4;
                case NR41_ADDR -> this.channel4LengthTimer = (value & 0xFF) | UNUSED_BITS_NR41;
                case NR42_ADDR -> this.channel4VolumeAndEnvelope = value & 0xFF;
                case NR43_ADDR -> this.channel4FrequencyAndRandomness = value & 0xFF;
                case NR44_ADDR -> this.channel4Control = (value & 0xFF) | UNUSED_BITS_NRX4;
                case NR50_ADDR -> this.masterVolumeAndVinPanning = value & 0xFF;
                case NR51_ADDR -> this.soundPanning = (value & 0xFF) | UNUSED_BITS_NRX1;
                case NR52_ADDR -> this.soundToggle = (value & 0xFF) | UNUSED_BITS_NR52;
                default -> throw new IllegalArgumentException("Invalid address \"%04X\" for GameBoy APU".formatted(address));
            };
        }
    }

    @Override
    public void pushSamples() {

    }

    // TODO: APU wave channel cycled every 2 T-cycles

}
