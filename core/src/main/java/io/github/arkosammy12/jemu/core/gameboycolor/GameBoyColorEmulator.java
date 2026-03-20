package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.cpu.CGBSM83;
import io.github.arkosammy12.jemu.core.cpu.SM83;
import io.github.arkosammy12.jemu.core.gameboy.*;

public class GameBoyColorEmulator extends GameBoyEmulator implements CGBSM83.SystemBus {

    private CGBSM83<?> cpu;
    private CGBBus<?> bus;
    private CGBPPU<?> ppu;
    private CGBAPU<?> apu;

    private CGBMMMIOBus<?> mmioBus;
    private CGBTimerController<?> timerController;

    public GameBoyColorEmulator(GameBoyHost host) {
        super(host);
    }

    public boolean isDmgCompatibilityMode() {
        return this.getMMIOBus().isDmgCompatibilityMode();
    }

    protected CGBSM83<?> createCpu() {
        this.cpu = new CGBSM83<>(this);
        return this.cpu;
    }

    public CGBSM83<?> getCpu() {
        return this.cpu;
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

    protected CGBAPU<?> createApu() {
        this.apu = new CGBAPU<>(this);
        return this.apu;
    }

    public CGBAPU<?> getAudioGenerator() {
        return this.apu;
    }

    protected CGBMMMIOBus<?> createMmioBus() {
        this.mmioBus = new CGBMMMIOBus<>(this);
        return this.mmioBus;
    }

    @Override
    public CGBMMMIOBus<?> getMMIOBus() {
        return this.mmioBus;
    }


    protected CGBTimerController<?> createTimerController() {
        this.timerController = new CGBTimerController<>(this);
        return this.timerController;
    }

    public CGBTimerController<?> getTimerController() {
        return this.timerController;
    }

    @Override
    protected void runCycle() {
        if (this.getMMIOBus().getCpuSpeed() == CGBMMMIOBus.CPUSpeed.SINGLE_SPEED) {
            boolean haltCpu = this.getBus().haltCpu();
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
            boolean haltCpu = this.getBus().haltCpu();
            if (!haltCpu) {
                this.getCpu().cycle();
            }
            boolean apuFrameSequencerTick = false;
            if (this.getCpu().getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick |= this.getTimerController().cycle();
            }
            if (!haltCpu) {
                this.getCpu().nextState();
            }

            haltCpu = this.getBus().haltCpu();
            if (!haltCpu) {
                this.getCpu().cycle();
            }
            if (this.getCpu().getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick |= this.getTimerController().cycle();
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

    @Override
    public boolean isSpeedSwitchRequested() {
        return this.getMMIOBus().isSwitchSpeedArmed();
    }

    @Override
    public void onStopInstructionWithSpeedSwitch(boolean resetDiv) {
        this.onStopInstruction(resetDiv);
        if (this.getMMIOBus().isSwitchSpeedArmed()) {
            this.getMMIOBus().switchCpuSpeed();
        }
    }
}
