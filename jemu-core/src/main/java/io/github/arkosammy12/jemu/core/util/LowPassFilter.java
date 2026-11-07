package io.github.arkosammy12.jemu.core.util;

/**
 * Courtesy of <a href="https://github.com/L-Spiro">Shawn (L. Spiro) Wilcoxen</a>
 */
public class LowPassFilter {

    /** The last output sample. */
    protected double lastSample;

    /** The last input. */
    protected double lastInput;

    /** The gain control (a0). */
    protected double gain;

    /** The corner frequency (b1). */
    protected double cornerFreq;

    /** Filter enablement state. */
    protected boolean enabled;

    // Internal state for caching parameters to avoid redundant calculations
    protected double lastFc;
    protected double lastSampleRate;

    public LowPassFilter() {
        this.lastSample = 0.0;
        this.lastInput = 0.0;
        this.gain = 0.0;
        this.cornerFreq = 0.0;
        this.enabled = true;

        // Initialize cache with invalid values to ensure the first calculation triggers
        this.lastFc = -1.0f;
        this.lastSampleRate = -1.0f;
    }

    /**
     * Checks if the parameters have changed, necessitating a recalculation.
     */
    private boolean isDirty(double fc, double sampleRate) {
        if (this.lastFc != fc || this.lastSampleRate != sampleRate) {
            this.lastFc = fc;
            this.lastSampleRate = sampleRate;
            return true;
        }
        return false;
    }

    /**
     * Sets the cut-off given a non-normalized cut-off frequency for an LPF and sample rate.
     *
     * @param fc The normalized cut-off frequency.
     * @param sampleRate The input/output sample rate.
     */
    public void createLpf(double fc, double sampleRate) {
        if (this.isDirty(fc, sampleRate)) {
            this.enabled = true;
            this.cornerFreq = 2.0 * Math.PI * ((double) fc / sampleRate);
            this.gain = 1.0 - this.cornerFreq;
            this.cornerFreq *= 0.5;

            if (fc >= sampleRate / 2.0f) {
                this.enabled = false;
            }
        }
    }

    /**
     * Processes a single sample.
     *
     * @param sample The sample to process.
     * @return Returns the filtered sample.
     */
    public double process(double sample) {
        if (this.enabled) {
            this.lastSample = this.lastSample * this.gain + (sample + this.lastInput) * this.cornerFreq;
            this.lastInput = sample;
            return this.lastSample;
        }
        return sample;
    }

    /**
     * Resets the state of the LPF without resetting the parameters.
     */
    public void resetState() {
        this.lastSample = 0.0;
        this.lastInput = 0.0;
    }

}