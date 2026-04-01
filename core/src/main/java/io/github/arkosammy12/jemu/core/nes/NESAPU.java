package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.NESCPUMMIOBus.*;

public class NESAPU<E extends NESEmulator> extends AudioGenerator<E> implements Bus {

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
            case SQ1_VOL_ADDR -> 0xFF;
            case SQ1_SWEEP_ADDR -> 0xFF;
            case SQ1_LO_ADDR -> 0xFF;
            case SQ1_HI_ADDR -> 0xFF;
            case SQ2_VOL_ADDR -> 0xFF;
            case SQ2_SWEEP_ADDR -> 0xFF;
            case SQ2_LO_ADDR -> 0xFF;
            case SQ2_HI_ADDR -> 0xFF;
            case TRI_LINEAR_ADDR -> 0xFF;
            case TRI_LO_ADDR -> 0xFF;
            case TRI_HI_ADDR -> 0xFF;
            case NOISE_VOL_ADDR -> 0xFF;
            case NOISE_LO_ADDR -> 0xFF;
            case NOISE_HI_ADDR -> 0xFF;
            case DMC_FREQ_ADDR -> 0xFF;
            case DMC_RAW_ADDR -> 0xFF;
            case DMC_START_ADDR -> 0xFF;
            case DMC_LEN_ADDR -> 0xFF;
            case SND_CHN_ADDR -> 0xFF; // This channel does not return open bus on reads!
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
            case SND_CHN_ADDR -> {}
            case JOY2_ADDR -> {} // Frame counter control
            default -> throw new EmulatorException("Invalid write address $%04X for NES APU!".formatted(address));
        }
    }
}
