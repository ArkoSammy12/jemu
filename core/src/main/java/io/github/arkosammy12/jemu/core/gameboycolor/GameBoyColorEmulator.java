package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.cpu.SM83;
import io.github.arkosammy12.jemu.core.gameboy.*;

public class GameBoyColorEmulator extends GameBoyEmulator {

    private CGBBus<?> bus;
    private CGBPPU<?> ppu;

    private CGBMMMIOBus<?> mmioBus;

    public GameBoyColorEmulator(GameBoyHost host) {
        super(host);
    }

    public boolean isDmgCompatibilityMode() {
        return this.getMMIOBus().isDmgCompatibilityMode();
    }

    @Override
    protected DMGBus<?> createBus() {
        this.bus = new CGBBus<>(this);
        return this.bus;
    }

    @Override
    public CGBBus<?> getBus() {
        return this.bus;
    }

    @Override
    protected CGBPPU<?> createPpu() {
        this.ppu = new CGBPPU<>(this);
        return this.ppu;
    }

    @Override
    public CGBPPU<?> getVideoGenerator() {
        return this.ppu;
    }

    protected CGBMMMIOBus<?> createMmioBus() {
        this.mmioBus = new CGBMMMIOBus<>(this);
        return this.mmioBus;
    }

    @Override
    public CGBMMMIOBus<?> getMMIOBus() {
        return this.mmioBus;
    }

    @Override
    protected void runCycle() {
        if (this.getMMIOBus().getCpuSpeed() == CGBMMMIOBus.CPUSpeed.SINGLE_SPEED) {
            boolean haltCpu = this.getBus().isCopyingDma();
            if (!haltCpu) {
                this.getCpu().cycle();
            }
            boolean apuFrameSequencerTick = false;
            if (this.getCpu().getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick = this.getTimerController().cycle();
            }
            if (!haltCpu) {
                this.getCpu().nextState();
            }
            this.getVideoGenerator().cycle();
            this.getAudioGenerator().cycle(apuFrameSequencerTick);
            this.getSerialController().cycle();
            this.getCartridge().cycle();
            this.getBus().cycleOamDMA();
            this.getBus().cycleVDMA();
        } else {
            boolean haltCpu = this.getBus().isCopyingDma();
            if (!haltCpu) {
                this.getCpu().cycle();
            }
            boolean apuFrameSequencerTick = false;
            if (this.getCpu().getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick = this.getTimerController().cycle();
            }
            if (!haltCpu) {
                this.getCpu().nextState();
            }

            haltCpu = this.getBus().isCopyingDma();
            if (!haltCpu) {
                this.getCpu().cycle();
            }
            if (this.getCpu().getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick = this.getTimerController().cycle();
            }
            if (!haltCpu) {
                this.getCpu().nextState();
            }

            this.getVideoGenerator().cycle();
            this.getAudioGenerator().cycle(apuFrameSequencerTick);

            this.getSerialController().cycle();
            this.getSerialController().cycle();

            this.getCartridge().cycle();

            this.getBus().cycleOamDMA();
            this.getBus().cycleOamDMA();

            this.getBus().cycleVDMA();
        }
    }

}
