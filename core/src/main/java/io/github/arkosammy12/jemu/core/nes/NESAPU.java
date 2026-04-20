package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2A03.*;

public class NESAPU<E extends NESEmulator> extends AudioGenerator<E> implements Bus {

    private int channelStatus;

    private boolean frameInterruptFlag;
    private FrameCounterStepMode frameCounterStepMode = FrameCounterStepMode.STEP_4;
    private boolean frameCounterInterruptInhibitFlag;

    public NESAPU(E emulator) {
        super(emulator);
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
        return Optional.empty();
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case SQ1_VOL_ADDR, SQ1_SWEEP_ADDR, SQ1_LO_ADDR, SQ1_HI_ADDR, SQ2_VOL_ADDR, SQ2_SWEEP_ADDR, SQ2_LO_ADDR,
                 SQ2_HI_ADDR, TRI_LINEAR_ADDR, TRI_LO_ADDR, TRI_HI_ADDR, NOISE_VOL_ADDR, NOISE_LO_ADDR, NOISE_HI_ADDR,
                 DMC_FREQ_ADDR, DMC_RAW_ADDR, DMC_START_ADDR, DMC_LEN_ADDR -> -1;

            case SND_CHN_ADDR -> {
                // TODO: Implement DMC active, and length counter not halted flags
                int ret = (this.getDmcInterruptFlag() ? 1 << 7 : 0) | (this.frameInterruptFlag ? 1 << 6 : 0);
                this.frameInterruptFlag = false;
                yield ret;
            }
            default -> throw new EmulatorException("Invalid read address $%04X for NES APU!".formatted(address));
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case SQ1_VOL_ADDR -> {}
            case SQ1_SWEEP_ADDR -> {}
            case SQ1_LO_ADDR -> {}
            case SQ1_HI_ADDR -> {}
            case SQ2_VOL_ADDR -> {}
            case SQ2_SWEEP_ADDR -> {}
            case SQ2_LO_ADDR -> {}
            case SQ2_HI_ADDR -> {}
            case TRI_LINEAR_ADDR -> {}
            case TRI_LO_ADDR -> {}
            case TRI_HI_ADDR -> {}
            case NOISE_VOL_ADDR -> {}
            case NOISE_LO_ADDR -> {}
            case NOISE_HI_ADDR -> {}
            case DMC_FREQ_ADDR -> {}
            case DMC_RAW_ADDR -> {}
            case DMC_START_ADDR -> {}
            case DMC_LEN_ADDR -> {}
            case SND_CHN_ADDR -> this.channelStatus = value & 0b11111;
            case JOY2_ADDR -> { // Frame counter control
                // TODO: If the write occurs during an APU cycle, the effects occur 3 CPU cycles after the $4017 write cycle, and if the write occurs between APU cycles, the effects occurs 4 CPU cycles after the write cycle.
                this.frameCounterStepMode = (value & (1 << 7)) != 0 ? FrameCounterStepMode.STEP_5 : FrameCounterStepMode.STEP_4;
                this.frameCounterInterruptInhibitFlag = (value & (1 << 6)) != 0;
            }
            default -> throw new EmulatorException("Invalid write address $%04X for NES APU!".formatted(address));
        }
    }

    public void cycleHalf() {

    }

    public boolean getIRQSignal() {
        return this.frameInterruptFlag;
    }

    private boolean getDmcChannelEnable() {
        return (this.channelStatus & (1 << 4)) != 0;
    }

    private boolean getNoiseChannelEnable() {
        return (this.channelStatus & (1 << 3)) != 0;
    }

    private boolean getTriangleChannelEnable() {
        return (this.channelStatus & (1 << 2)) != 0;
    }

    private boolean getPulseChannel2Enable() {
        return (this.channelStatus & (1 << 1)) != 0;
    }

    private boolean getPulseChannel1Enable() {
        return (this.channelStatus & 1) != 0;
    }

    private boolean getDmcInterruptFlag() {
        return false;
    }

    private boolean getFrameInterruptFlag() {
        return false;
    }

    private enum FrameCounterStepMode {
        STEP_4,
        STEP_5

    }

}
