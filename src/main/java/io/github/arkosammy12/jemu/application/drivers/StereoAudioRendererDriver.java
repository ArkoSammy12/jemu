package io.github.arkosammy12.jemu.application.drivers;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.frontend.audio.StereoAudioRenderer;

import java.io.IOException;
import java.util.Optional;

public class StereoAudioRendererDriver extends DefaultAudioRendererDriver {

    public StereoAudioRendererDriver(Jemu jemu, AudioGenerator<?> audioGenerator, StereoAudioRenderer audioRenderer) {
        super(jemu, audioGenerator, audioRenderer);
    }

    @Override
    public int getSampleRate() {
        return this.audioRenderer.getSampleRate();
    }

    @Override
    public int getSamplesPerFrame() {
        return this.audioRenderer.getSamplesPerFrame();
    }

    @Override
    protected void onFrame() {
        Optional<byte[]> optionalSamples = this.audioGenerator.getSampleFrame();
        if (optionalSamples.isEmpty()) {
            this.audioRenderer.pushSampleFrame(null);
            return;
        }
        byte[] samples = optionalSamples.get();
        this.audioRenderer.pushSampleFrame(this.resampleIfNecessary(samples));
    }

    private byte[] resampleIfNecessary(byte[] buf) {
        return switch (this.audioGenerator.getBytesPerSample()) {
            case BYTES_1 -> {
                byte[] buf16 = new byte[this.audioRenderer.getBytesPerFrame()];

                int frames = buf.length / 2;
                for (int i = 0; i < frames; i++) {
                    int sample16Left = buf[i * 2] * 256;
                    int sample16Right = buf[(i * 2) + 1] * 256;

                    buf16[i * 4] = (byte) ((sample16Left & 0xFF00) >>> 8);
                    buf16[(i * 4) + 1] = (byte) (sample16Left & 0xFF);
                    buf16[(i * 4) + 2] = (byte) ((sample16Right & 0xFF00) >>> 8);
                    buf16[(i * 4) + 3] = (byte) (sample16Right & 0xFF);
                }
                yield buf16;
            }
            case BYTES_2 -> buf;
        };
    }

    @Override
    public void close() throws IOException {
        if (this.audioRenderer != null) {
            this.audioRenderer.close();
        }
    }

}
