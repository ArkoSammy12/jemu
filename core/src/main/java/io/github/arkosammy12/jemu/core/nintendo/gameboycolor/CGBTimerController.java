package io.github.arkosammy12.jemu.core.nintendo.gameboycolor;

import io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGTimerController;

public class CGBTimerController<E extends GameBoyColorEmulator> extends DMGTimerController<E> {

    protected static final int DIV_BIT_5_MASK = 1 << 13;
    protected boolean oldDivBit5 = false;

    public CGBTimerController(E emulator) {
        super(emulator);
    }

    @Override
    protected boolean getAPUFrameSequencerTick() {
        boolean divBit4 = (this.systemClock & DIV_BIT_4_MASK) != 0;
        boolean divBit5 = (this.systemClock & DIV_BIT_5_MASK) != 0;

        boolean tick = switch (this.emulator.getCPUSpeed()) {
            case SINGLE_SPEED -> this.oldDivBit4 && !divBit4;
            case DOUBLE_SPEED -> this.oldDivBit5 && !divBit5;
        };

        this.oldDivBit4 = divBit4;
        this.oldDivBit5 = divBit5;
        return tick;
    }

    @Override
    public void onAPUPowerOn() {
        super.onAPUPowerOn();
        this.oldDivBit5 = (this.systemClock & DIV_BIT_5_MASK) != 0;
    }

}
