package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Emulator;

import javax.sound.sampled.LineUnavailableException;

public class StereoAudioRendererDriver extends DefaultAudioRendererDriver {

    public StereoAudioRendererDriver(Jemu jemu, Emulator emulator) throws LineUnavailableException {
        super(jemu, emulator);
    }

    @Override
    protected byte[] convertBitDepthIfNecessary(byte[] buf) {
        return switch (this.audioGenerator.getBytesPerSample()) {
            case BYTES_1 -> {
                byte[] buf16 = new byte[this.jemu.getAudioEngine().getBytesPerFrame()];

                int frames = buf.length / 2;
                for (int i = 0; i < frames; i++) {
                    int sample16Left = ((int) buf[i * 2] & 0xFF) << 8;
                    int sample16Right = ((int) buf[(i * 2) + 1] & 0xFF) << 8;
                    buf16[i * 4] = (byte) (sample16Left & 0xFF);
                    buf16[(i * 4) + 1] = (byte) ((sample16Left & 0xFF00) >>> 8);
                    buf16[(i * 4) + 2] = (byte) (sample16Right & 0xFF);
                    buf16[(i * 4) + 3] = (byte) ((sample16Right & 0xFF00) >>> 8);
                }
                yield buf16;
            }
            case BYTES_2 -> buf;
        };
    }

}
