package io.github.arkosammy12.jemu.core.util;

/**
 * Courtesy of <a href="https://github.com/dtabacaru">dtabacaru</a>
 */
public class FirstOrderRCHighPass {

    private double cornerFreq;
    private double sampleRate;

    private double k; // Pole
    private double lastOut;
    private double lastIn;

    public void computeConstants(double cornerFreq, double sampleRate) {
        if (sampleRate == this.sampleRate && cornerFreq == this.cornerFreq) {
            return;
        }

        this.cornerFreq = cornerFreq;
        this.sampleRate = sampleRate;
        this.computePoleConstant();
    }

    public double filter(double in) {
        double out = k * (this.lastOut + in - this.lastIn);
        this.lastIn  = in;
        this.lastOut = out;
        return out;
    }

    private void computePoleConstant() {
        this.k = Math.exp(-2.0 * Math.PI * cornerFreq / sampleRate);
    }

}