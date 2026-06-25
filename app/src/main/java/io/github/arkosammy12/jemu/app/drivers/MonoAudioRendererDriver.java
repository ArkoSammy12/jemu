package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Emulator;

import javax.sound.sampled.LineUnavailableException;

public class MonoAudioRendererDriver extends DefaultAudioRendererDriver {

    public MonoAudioRendererDriver(Jemu jemu, Emulator emulator) throws LineUnavailableException {
        super(jemu, emulator);
    }

    @Override
    protected byte[] convertBitDepthIfNecessary(byte[] buf) {
        return switch (this.audioGenerator.getBytesPerSample()) {
            case BYTES_1 -> {
                byte[] buf16 = new byte[this.jemu.getAudioEngine().getBytesPerFrame()];
                for (int i = 0; i < buf.length; i++) {
                    int sample16 = ((int) buf[i] & 0xFF) * 256;
                    buf16[i * 2] = (byte) (sample16 & 0xFF);
                    buf16[(i * 2) + 1] = (byte) ((sample16 & 0xFF00) >>> 8);
                }
                yield buf16;
            }
            case BYTES_2 -> buf;
        };
    }

}
