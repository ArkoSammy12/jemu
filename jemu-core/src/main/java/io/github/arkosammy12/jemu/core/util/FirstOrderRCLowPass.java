package io.github.arkosammy12.jemu.core.util;

/**
 * Courtesy of <a href="https://github.com/dtabacaru">dtabacaru</a>
 */
public class FirstOrderRCLowPass {
    private double cornerFreq; // Hz
    private double sampleRate; // Hz

    private double k; // Blend
    private double lastOut;

    public void computeConstants(double cornerFreq, double sampleRate) {
        if (sampleRate == this.sampleRate && cornerFreq == this.cornerFreq) {
            return;
        }

        this.cornerFreq = cornerFreq;
        this.sampleRate = sampleRate;
        this.computeBlendConstant();
    }

    public double filter(double in) {
        double out = this.lastOut + k * (in - lastOut);
        this.lastOut = out;
        return out;
    }

    private void computeBlendConstant() {
        this.k = 1.0 - Math.exp(-2.0 * Math.PI * cornerFreq / sampleRate);
    }

}