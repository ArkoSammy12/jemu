package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.gameboy.DMGAPU;

public class CGBAPU<E extends GameBoyColorEmulator> extends DMGAPU<E> {

    public CGBAPU(E emulator) {
        super(emulator);
    }

    @Override
    protected void onApuOn() {
        super.onApuOn();
        this.channel1.lengthTimer = 0;
        this.channel2.lengthTimer = 0;
        this.channel3.lengthTimer = 0;
        this.channel4.lengthTimer = 0;
    }

    /*
    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void onApuOff() {
        this.channel1.nr10 = 0;
        this.channel1.nrx1 = 0;
        this.channel1.setNRX2(0);
        this.channel1.nrx3 = 0;
        this.channel1.nrx4 = 0;

        this.channel2.nrx1 = 0;
        this.channel2.setNRX2(0);
        this.channel2.nrx3 = 0;
        this.channel2.nrx4 = 0;

        this.channel3.setNR30(0);
        this.channel3.nrx1 = 0;
        this.channel3.nrx2 = 0;
        this.channel3.nrx3 = 0;
        this.channel3.nrx4 = 0;

        this.channel4.nrx1 = 0;
        this.channel4.setNRX2(0);
        this.channel4.nrx3 = 0;
        this.channel4.nrx4 = 0;

        this.nr50 = 0;
        this.nr51 = 0;
    }
     */

}
