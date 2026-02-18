package io.github.arkosammy12.jemu.frontend.audio;

import javax.sound.sampled.AudioFormat;

public class MonoAudioRenderer extends AudioRenderer {

    private static final int BYTES_PER_OUTPUT_SAMPLE = 2;

    public MonoAudioRenderer(int frameRate) {
        super(frameRate);
    }

    @Override
    protected AudioFormat getAudioFormat() {
        return new AudioFormat(SAMPLE_RATE, BYTES_PER_OUTPUT_SAMPLE * 8, 1, true, true);
    }

    @Override
    protected int getBytesPerOutputSample() {
        return BYTES_PER_OUTPUT_SAMPLE;
    }

    @Override
    protected byte[] ensureBufferLength(byte[] buf) {
        if (buf.length == this.bytesPerFrame) {
            return buf;
        }
        byte[] actualBuf = new byte[this.bytesPerFrame];
        System.arraycopy(buf, 0, actualBuf, 0, buf.length);

        byte lastSample = buf[buf.length - 1];
        for (int i = buf.length; i < actualBuf.length; i++) {
            actualBuf[i] = lastSample;
        }
        return actualBuf;
    }


}
