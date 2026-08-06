package io.github.arkosammy12.jemu.core.appleii;

import io.github.arkosammy12.jemu.core.common.*;

public class AppleIIEmulator implements Emulator {

    private static final int MPU_CLOCK_SPEED_HZ = 1_020_500;

    private final AppleIISystemHost systemHost;

    public AppleIIEmulator(AppleIISystemHost systemHost) {
        this.systemHost = systemHost;
    }

    @Override
    public SystemHost getHost() {
        return this.systemHost;
    }

    @Override
    public VideoGenerator getVideoGenerator() {
        return null;
    }

    @Override
    public AudioGenerator getAudioGenerator() {
        return null;
    }

    @Override
    public SystemController getSystemController() {
        return null;
    }

    @Override
    public void executeFrame() {

    }

    @Override
    public void executeCycle() {

    }

    @Override
    public int getFramerate() {
        return 60;
    }

    @Override
    public void close() throws Exception {

    }
}
