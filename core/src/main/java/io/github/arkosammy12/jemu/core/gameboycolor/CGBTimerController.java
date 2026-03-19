package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.gameboy.DMGTimerController;

public class CGBTimerController<E extends GameBoyColorEmulator> extends DMGTimerController<E> {

    protected static final int DIV_BIT_5_MASK = 1 << 13;

    protected boolean oldDivBit5 = false;
    private boolean oldFrequencyBit;

    public CGBTimerController(E emulator) {
        super(emulator);
    }

    @Override
    protected boolean cycleSystemClock() {

        this.systemClock = (this.systemClock + 1) & 0xFFFF;
        if (this.reloadDelay > 0) {
            this.reloadDelay--;

            if (this.reloadDelay <= 0) {
                this.timerCounter = this.timerModulo;
                this.triggerInterrupt();
                this.reloadOccurred = true;
            }

        }

        boolean frequencyBit = switch (this.timerControl & TAC_CLOCK_SELECT_MASK) {
            case 0 -> (this.systemClock & FREQ_0) != 0;
            case 1 -> (this.systemClock & FREQ_1) != 0;
            case 2 -> (this.systemClock & FREQ_2) != 0;
            case 3 -> (this.systemClock & FREQ_3) != 0;
            default -> throw new EmulatorException("Lower 2 bits of TAC is not in the range [0, 3]!");
        };

        if (this.oldFrequencyBit && !frequencyBit && (this.timerControl & TAC_ENABLE_BIT) != 0) {
            int newTimerCounter = this.timerCounter + 1;
            if (newTimerCounter > 0xFF) {
                this.reloadDelay = 4;
            }
            this.timerCounter = newTimerCounter & 0xFF;
        }

        this.oldFrequencyBit = frequencyBit;

        boolean divBit4 = (this.systemClock & DIV_BIT_4_MASK) != 0;
        boolean divBit5 = (this.systemClock & DIV_BIT_5_MASK) != 0;

        boolean apuFrameSequencerTick = switch (this.emulator.getMMIOBus().getCpuSpeed()) {
            case SINGLE_SPEED -> this.oldDivBit4 && !divBit4;
            case DOUBLE_SPEED -> this.oldDivBit5 && !divBit5;
        };

        this.oldDivBit4 = divBit4;
        this.oldDivBit5 = divBit5;
        return apuFrameSequencerTick;

    }

    public void onAPUPowerOn() {
        this.oldDivBit4 = (this.systemClock & DIV_BIT_4_MASK) != 0;
        this.oldDivBit5 = (this.systemClock & DIV_BIT_5_MASK) != 0;
    }

}
