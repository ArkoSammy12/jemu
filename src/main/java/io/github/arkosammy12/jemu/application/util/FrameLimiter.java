package io.github.arkosammy12.jemu.application.util;

import java.lang.System;

/// Frame pacer implementation generously provided by @janitor-raus via [his implementation](https://github.com/janitor-raus/CubeChip/blob/master/include/components/FrameLimiter.hpp)
public final class FrameLimiter {

    private static final double SPIN_THRESHOLD = 2.3E+6;

    private boolean doneFirstRunSetup;
    private boolean forceInitialFrame;
    private final boolean allowMissedFrames;
    private boolean previousFrameSkip;

    private double targetFramePeriod;
    private double elapsedOverTarget;
    private double elapsedTimePeriod;

    private long previousFrameTime;
    private long validFrameCount;

    public FrameLimiter(double frameRate, boolean firstPass, boolean lostFrame) {
        this.setLimiterProperties(frameRate);
        this.forceInitialFrame = firstPass;
        this.allowMissedFrames = lostFrame;
    }

    public void setLimiterProperties(double frameRate) {
        this.targetFramePeriod = 1_000_000_000L / Math.clamp(frameRate, 0.5, 1000);
    }

    public boolean isFrameReady(boolean lazy) {
        if (this.hasTargetPeriodElapsed()) {
            return true;
        }
        if ((lazy && targetFramePeriod >= SPIN_THRESHOLD) || (this.getRemainderToTarget() >= SPIN_THRESHOLD)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {}
        } else {
            Thread.onSpinWait();
        }
        return false;
    }

    private boolean hasTargetPeriodElapsed() {
        long currentTimePoint = System.nanoTime();

        if (!this.doneFirstRunSetup) {
            this.previousFrameTime = currentTimePoint;
            this.doneFirstRunSetup = true;
        }

        if (this.forceInitialFrame) {
            this.forceInitialFrame = false;
            this.validFrameCount++;
            return true;
        }

        this.elapsedTimePeriod = this.elapsedOverTarget + (currentTimePoint - this.previousFrameTime);

        if (this.elapsedTimePeriod < this.targetFramePeriod) {
            return false;
        }

        if (this.allowMissedFrames) {
            this.previousFrameSkip = this.elapsedTimePeriod >= this.targetFramePeriod + 50000;
            this.elapsedOverTarget = this.elapsedTimePeriod % this.targetFramePeriod;
        } else {
            // Without frameskip, we carry over frame debt until caught up
            this.elapsedOverTarget = this.elapsedTimePeriod - this.targetFramePeriod;
        }

        this.previousFrameTime = currentTimePoint;
        this.validFrameCount++;
        return true;
    }

    private double getRemainderToTarget() {
        return this.targetFramePeriod - this.elapsedTimePeriod;
    }

}
