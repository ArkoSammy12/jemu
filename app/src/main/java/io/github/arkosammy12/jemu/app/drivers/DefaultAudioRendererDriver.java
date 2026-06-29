package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.frontend.audio.AudioChannels;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;

import static io.github.arkosammy12.jemu.app.Jemu.tryJoinSafely;

public abstract class DefaultAudioRendererDriver implements AudioDriver, Closeable {

    protected final Jemu jemu;
    protected final AudioGenerator audioGenerator;

    private final ArrayBlockingQueue<byte[]> outputSampleFrameBuffer = new ArrayBlockingQueue<>(2);
    private final ArrayBlockingQueue<AudioGenerator.SampleFrame> inputSampleFrameBuffer = new ArrayBlockingQueue<>(2);

    private final Thread audioThread;
    private volatile boolean running = true;

    public DefaultAudioRendererDriver(Jemu jemu, Emulator emulator) throws LineUnavailableException {
        this.jemu = jemu;
        this.audioGenerator = emulator.getAudioGenerator();
        this.jemu.getAudioEngine().setSampleFrameCallback(this.outputSampleFrameBuffer::poll);
        this.jemu.getAudioEngine().setFramerate(jemu.getMainWindow().getConfigurations().getSettings().getSpeedSettings().getSpeedMode().scaleFramerate(emulator.getFramerate()));
        this.jemu.getAudioEngine().setAudioChannels(this.audioGenerator.isStereo() ? AudioChannels.STEREO : AudioChannels.MONO);

        this.audioThread = new Thread(this::audioLoop, "%s-render-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.audioThread.setDaemon(true);
        this.audioThread.start();
    }

    @Override
    public int getSampleRate() {
        return this.jemu.getAudioEngine().getSampleRate();
    }

    @Override
    public int getSamplesPerFrame() {
        return this.jemu.getAudioEngine().getSamplesPerFrame();
    }

    public void onFrame() {
        this.audioGenerator.getSampleFrame().ifPresent(samples -> {
            try {
                this.inputSampleFrameBuffer.put(samples);
            } catch (InterruptedException _) {}
        });
    }

    protected abstract byte[] convertBitDepthIfNecessary(byte[] buf);

    public void clearAudioBuffers() {
        this.inputSampleFrameBuffer.clear();
        this.outputSampleFrameBuffer.clear();
    }

    private void audioLoop() {
        while (this.running) {
            try {
                AudioGenerator.SampleFrame sampleFrame = this.inputSampleFrameBuffer.take();
                if (!this.running) {
                    break;
                }
                Optional<byte[]> sampleBuffer = this.audioGenerator.getSampleFrameResampler().resample(this.getSampleRate(), this.getSamplesPerFrame(), sampleFrame);
                if (sampleBuffer.isPresent()) {
                    this.outputSampleFrameBuffer.put(this.convertBitDepthIfNecessary(sampleBuffer.get()));
                }
            } catch (InterruptedException _) {

            } catch (Exception e) {
                Logger.error("Unexpected error in audio thread loop: {}", e);
            }
        }
    }

    @Override
    public void close() {
        this.clearAudioBuffers();
        this.running = false;
        this.audioThread.interrupt();
        tryJoinSafely(this.audioThread);
    }

}
