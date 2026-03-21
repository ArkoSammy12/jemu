package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.gameboy.DMGAPU;

import static io.github.arkosammy12.jemu.core.gameboy.DMGMMIOBus.WAVERAM_START;

public class CGBAPU<E extends GameBoyColorEmulator> extends DMGAPU<E> {

    public CGBAPU(E emulator) {
        super(emulator);
    }

    @Override
    protected CGBAPU<?>.Channel3 createChannel3() {
        return new CGBAPU<?>.Channel3();
    }

    @Override
    protected void onApuOn() {
        super.onApuOn();
        this.channel1.lengthTimer = 0;
        this.channel2.lengthTimer = 0;
        this.channel3.lengthTimer = 0;
        this.channel4.lengthTimer = 0;
    }

    protected class Channel3 extends DMGAPU<?>.Channel3 {

        @Override
        protected int readWaveRam(int address) {
            //boolean originalFirstFetchConsumed = this.firstFetchConsumed;
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                //if (originalFirstFetchConsumed) {
                    return this.waveRam[((this.waveRamIndex - 1) & 31) / 2];
                //} else {
                    //return this.waveRam[0];
                //}
            } else {
                return this.waveRam[address - WAVERAM_START];
            }
        }

        @Override
        protected void writeWaveRam(int address, int value) {
            //boolean originalFirstFetchConsumed = this.firstFetchConsumed;
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                //if (originalFirstFetchConsumed) {
                    this.waveRam[((this.waveRamIndex - 1) & 31) / 2] = value & 0xFF;
                //} else {
                    //this.waveRam[0] = value & 0xFF;
                //}
            } else {
                this.waveRam[address - WAVERAM_START] = value & 0xFF;
            }
        }

        @Override
        protected void checkWaveRamCorruption() {
            // No wave ram corruption on CGB
        }

    }

}
