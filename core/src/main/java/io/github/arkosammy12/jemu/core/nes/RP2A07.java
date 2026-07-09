package io.github.arkosammy12.jemu.core.nes;

public class RP2A07<E extends NESEmulator> extends RP2A03<E> {

    private boolean oamDMAStartPending;
    private boolean dmcDMAStartPending;

    public RP2A07(E emulator, int apuSampleBufferSize) {
        super(emulator, apuSampleBufferSize);
    }

    // TODO: Different controller clocking behavior.

    @Override
    public void cycleHalf() {
        switch (this.cpu.getHalfCyclePhase()) {
            case PHI_1 -> super.cycleHalf();
            case PHI_2 -> {

                // Place checks before cycling PHI2 of the CPU to halt it on its fetch cycle if a DMA is pending
                if (this.cpu.getSYNC()) {
                    if (this.oamDMAStartPending) {
                        this.oamDMAStartPending = false;
                        super.startOAMDMA();
                    }
                    if (this.dmcDMAStartPending) {
                        this.dmcDMAStartPending = false;
                        super.startDMCDMA();
                    }
                }

                super.onCPUPHI2();

            }
        }
    }

    @Override
    protected void startDMCDMA() {
        this.dmcDMAStartPending = true;
    }

    @Override
    protected void startOAMDMA() {
        this.oamDMAStartPending = true;
    }

}
