package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MOS6581 implements AudioGenerator, Bus {

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
        return Optional.empty();
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return (outputSampleRate, outputSamplesPerFrame, inputSampleFrame) -> Optional.empty();
    }

    @Override
    public int readByte(int address) {
        address &= 0x1F;

        return 0;
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x1F;
    }

    public void cycle() {

    }

}
