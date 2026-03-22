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
        CGBSM83<?> cpu = this.getCpu();
        CGBPPU<?> ppu = this.getVideoGenerator();
        CGBAPU<?> apu = this.getAudioGenerator();
        GameBoyCartridge cartridge = this.getCartridge();
        CGBBus<?> bus = this.getBus();
        CGBTimerController<?> timerController = this.getTimerController();
        CGBMMMIOBus<?> mmio = this.getMMIOBus();
        DMGSerialController<?> serialController = this.getSerialController();

        if (mmio.getCpuSpeed() == CGBMMMIOBus.CPUSpeed.SINGLE_SPEED) {
            boolean haltCpu = bus.haltCpu();
            if (!haltCpu) {
                cpu.cycle();
            }
            boolean apuFrameSequencerTick = false;
            if (cpu.getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick = timerController.cycle();
            }
            if (!haltCpu) {
                cpu.nextState();
            }

            ppu.cycle();
            apu.cycle(apuFrameSequencerTick);
            serialController.cycle();
            cartridge.cycle();
            bus.cycleOamDMA();
            bus.cycleVDMA();
        } else {
            boolean haltCpu = bus.haltCpu();
            if (!haltCpu) {
                cpu.cycle();
            }
            boolean apuFrameSequencerTick = false;
            if (cpu.getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick |= timerController.cycle();
            }
            if (!haltCpu) {
                cpu.nextState();
            }

            haltCpu = bus.haltCpu();
            if (!haltCpu) {
                cpu.cycle();
            }
            if (cpu.getMode() != SM83.Mode.STOPPED) {
                apuFrameSequencerTick |= this.getTimerController().cycle();
            }
            if (!haltCpu) {
                cpu.nextState();
            }

            ppu.cycle();
            apu.cycle(apuFrameSequencerTick);

            serialController.cycle();
            serialController.cycle();

            cartridge.cycle();

            bus.cycleOamDMA();
            bus.cycleOamDMA();

            bus.cycleVDMA();
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
