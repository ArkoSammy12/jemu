package io.github.arkosammy12.jemu.application.drivers;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.MonoAudioRenderer;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.Optional;

public class MonoAudioRendererDriver extends DefaultAudioRendererDriver {

    public MonoAudioRendererDriver(Jemu jemu, AudioGenerator<?> audioGenerator, MonoAudioRenderer audioRenderer) {
        super(jemu, audioGenerator, audioRenderer);
    }

    @Override
    public int getSampleRate() {
        return AudioRenderer.SAMPLE_RATE;
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
                for (int i = 0; i < buf.length; i++) {
                    int sample16 = buf[i] * 256;
                    buf16[i * 2] = (byte) ((sample16 & 0xFF00) >>> 8);
                    buf16[(i * 2) + 1] = (byte) (sample16 & 0xFF);
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
