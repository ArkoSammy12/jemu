package io.github.arkosammy12.jemu.core.util;

/**
 * Courtesy of <a href="https://github.com/L-Spiro">Shawn (L. Spiro) Wilcoxen</a>
 */
public class HighPassFilter {

    /** Alpha. */
    protected double alpha;

    /** The previous output sample. */
    protected double previousOutput;

    /** The previous input sample. */
    protected double prevInput;

    /** Delta. */
    protected double delta;

    /** The current filtered output sample. */
    protected double output;

    /** Filter enablement state. */
    protected boolean enabled;

    // Internal state for caching parameters to avoid redundant calculations
    protected double lastFc;
    protected double lastSampleRate;

    public HighPassFilter() {
        this.alpha = 0.0;
        this.previousOutput = 0.0;
        this.prevInput = 0.0;
        this.delta = 0.0;
        this.output = 0.0;
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
     * Sets the cut-off given a non-normalized cut-off frequency for an HPF and sample rate.
     *
     * @param fc The non-normalized cut-off frequency.
     * @param sampleRate The input/output sample rate.
     */
    public void createHpf(double fc, double sampleRate) {
        if (this.isDirty(fc, sampleRate)) {
            this.enabled = true;

            // Renamed local dDelta to deltaT to avoid shadowing the member variable 'delta'
            double deltaT = (sampleRate != 0.0f) ? (1.0 / sampleRate) : 0.0;
            double timeConstant = (fc != 0.0f) ? (1.0 / fc) : 0.0;

            this.alpha = ((timeConstant + deltaT) != 0.0) ? (timeConstant / (timeConstant + deltaT)) : 0.0;

            this.output = 0.0;
            this.previousOutput = this.output;
            this.prevInput = 0.0;
            this.delta = 0.0;

            if (fc >= sampleRate / 2.0f || fc < 0.0f) {
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
            this.previousOutput = this.output;
            this.delta = sample - this.prevInput;
            this.prevInput = sample;

            this.output = this.alpha * this.previousOutput + this.alpha * this.delta;
            return this.output;
        }
        return sample;
    }

    /**
     * Gets the Enabled flag.
     *
     * @return Returns true if the HPF is enabled.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Resets the state of the HPF without resetting the parameters.
     */
    public void resetState() {
        this.output = 0.0;
        this.previousOutput = 0.0;
        this.prevInput = 0.0;
        this.delta = 0.0;
    }

}
