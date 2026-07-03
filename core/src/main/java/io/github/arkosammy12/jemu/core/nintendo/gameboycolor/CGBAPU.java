package io.github.arkosammy12.jemu.core.nintendo.gameboycolor;

import io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGAPU;

public class CGBAPU<E extends GameBoyColorEmulator> extends DMGAPU<E> {

    public static final int PCM12_ADDR = 0xFF76;
    public static final int PCM34_ADDR = 0xFF77;

    public CGBAPU(E emulator) {
        super(emulator);
    }

    @Override
    protected CGBAPU<?>.Channel3 createChannel3() {
        return this.new Channel3();
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case PCM12_ADDR -> (this.channel2.getDigitalOutput() << 4) | this.channel1.getDigitalOutput();
            case PCM34_ADDR -> (this.channel4.getDigitalOutput() << 4) | this.channel3.getDigitalOutput();
            default -> super.readByte(address);
        };
    }

    @Override
    protected void onAPUOn() {
        super.onAPUOn();
        this.channel1.lengthTimer = 0;
        this.channel2.lengthTimer = 0;
        this.channel3.lengthTimer = 0;
        this.channel4.lengthTimer = 0;
    }

    protected class Channel3 extends DMGAPU<?>.Channel3 {

        private boolean triggeredThisCycle = false;

        @Override
        protected int readWaveRAM(int address) {
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                return (int) this.waveRAM[((this.waveRamIndex - 1) & 31) / 2] & 0xFF;
            } else {
                return (int) this.waveRAM[address - WAVERAM_START] & 0xFF;
            }
        }

        @Override
        protected void writeWaveRAM(int address, int value) {
            this.firstFetchConsumed = this.fetchedFirstByte;
            if (this.getEnabled()) {
                this.waveRAM[((this.waveRamIndex - 1) & 31) / 2] = (byte) value;
            } else {
                this.waveRAM[address - WAVERAM_START] = (byte) value;
            }
        }

        @Override
        protected void checkWaveRamCorruption() {
            // No wave ram corruption on CGB
        }

        @Override
        protected void tick() {
            if (!this.getEnabled() || this.triggeredThisCycle) {
                this.triggeredThisCycle = false;
                return;
            }
            super.tick();
        }

        @Override
        protected void trigger() {
            this.triggeredThisCycle = true;
            super.trigger();
            this.wavePeriodTimer = 4;
        }

    }

}
