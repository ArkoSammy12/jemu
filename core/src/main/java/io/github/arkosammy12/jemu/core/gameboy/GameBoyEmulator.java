package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.hardware.SM83;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

public class GameBoyEmulator implements Emulator, SM83.SystemBus {

    private static final int FRAMERATE = 60;
    public static final int CLOCK_FREQUENCY = 4194304;
    public static final int T_CYCLES_PER_FRAME = 70224;
    public static final int M_CYCLES_PER_FRAME = T_CYCLES_PER_FRAME / 4;

    private final GameBoyHost host;

    private final SM83<?> cpu;
    private final DMGBus<?> bus;
    private final DMGPPU<?> ppu;
    private final DMGAPU<?> apu;
    private final GameBoyJoypad<?> joypad;

    private final DMGTimerController<?> timerController;
    private final DMGSerialController<?> serialController;

    private final GameBoyCartridge cartridge;

    protected int mCycleDot;
    protected boolean cpuOnBus;
    protected int cpuMCycleDotBase;
    protected int cpuMCycleDotSpan = 4;

    public GameBoyEmulator(GameBoyHost host) {
        this.host = host;

        this.joypad = new GameBoyJoypad<>(this);
        this.cpu = this.createCpu();
        this.bus = this.createBus();
        this.ppu = this.createPpu();
        this.apu = this.createApu();

        this.timerController = this.createTimerController();
        this.serialController = this.createSerialController();

        this.cartridge = GameBoyCartridge.getCartridge(this);
    }

    protected SM83<?> createCpu() {
        return new SM83<>(this);
    }

    protected DMGBus<?> createBus() {
        return new DMGBus<>(this);
    }

    protected DMGPPU<?> createPpu() {
        return new DMGPPU<>(this);
    }

    protected DMGAPU<?> createApu() {
        return new DMGAPU<>(this);
    }

    protected DMGTimerController<?> createTimerController() {
        return new DMGTimerController<>(this);
    }

    protected DMGSerialController<?> createSerialController() {
        return new DMGSerialController<>(this);
    }

    @Override
    public GameBoyHost getHost() {
        return this.host;
    }

    public SM83<?> getCpu() {
        return this.cpu;
    }

    @Override
    public DMGBus<?> getBus() {
        return this.bus;
    }

    @Override
    public DMGPPU<?> getVideoGenerator() {
        return this.ppu;
    }

    @Override
    public GameBoyJoypad<?> getSystemController() {
        return this.joypad;
    }

    @Override
    public DMGAPU<?> getAudioGenerator() {
        return this.apu;
    }

    public GameBoyCartridge getCartridge() {
        return this.cartridge;
    }

    public DMGTimerController<?> getTimerController() {
        return this.timerController;
    }

    public DMGSerialController<?> getSerialController() {
        return this.serialController;
    }

    public void setDMGPalette(DMGPPU.Palette palette) {
        this.ppu.setDMGPalette(palette);
    }

    @Override
    public void executeFrame() {
        for (int i = 0; i < M_CYCLES_PER_FRAME; i++) {
            this.runCycle();
        }
    }

    @Override
    public void executeCycle() {
        this.runCycle();
    }

    protected void runCycle() {
        this.mCycleDot = 0;
        this.cpuMCycleDotBase = 0;
        this.cpuOnBus = true;
        this.cpu.cycle();
        this.cpuOnBus = false;
        boolean apuFrameSequencerTick = false;
        if (this.cpu.getMode() != SM83.Mode.STOPPED) {
            apuFrameSequencerTick = this.timerController.cycle();
        }
        this.cpu.nextState();
        this.syncPPUToDot(4);
        this.apu.cycle(apuFrameSequencerTick);
        this.serialController.cycle();
        this.cartridge.cycle();
        this.bus.cycleOAMDMA();
    }

    protected void syncPPUToDot(int targetDot) {
        while (this.mCycleDot < targetDot) {
            this.ppu.cycleDot(this.mCycleDot);
            this.mCycleDot++;
        }
    }

    // CPU reads latch at the end of the CPU M-cycle: run its dots first
    void syncPPUForCPURead() {
        if (this.cpuOnBus) {
            this.syncPPUToDot(this.cpuMCycleDotBase + this.cpuMCycleDotSpan);
        }
    }

    // CPU writes to PPU registers commit halfway into the CPU M-cycle, so the
    // remaining dots see the new value
    void syncPPUForCPUPPURegisterWrite() {
        if (this.cpuOnBus) {
            this.syncPPUToDot(this.cpuMCycleDotBase + this.cpuMCycleDotSpan / 2);
        }
    }

    @Override
    public int getFramerate() {
        return FRAMERATE;
    }

    @Override
    public void close() {
        try {
            if (this.cartridge != null) {
                this.cartridge.save();
            }
        } catch (Exception e) {
            throw new EmulatorException("Error releasing emulator resources!", e);
        }
    }

    @Override
    public int getIE() {
        return this.bus.getIE();
    }

    @Override
    public int getIF() {
        return this.bus.getIF();
    }

    @Override
    public void setIF(int value) {
        this.bus.setIF(value);
    }

    @Override
    public boolean isButtonHeld() {
        return this.joypad.isButtonHeld();
    }

    @Override
    public void onStopInstruction(boolean resetDiv) {
        if (resetDiv) {
            this.timerController.resetDiv();
        }
    }

    @Override
    public void onIDURead(int originalValue) {
        this.ppu.checkArmOAMBugRead(originalValue);
    }

    @Override
    public void onIDUWrite(int originalValue) {
        this.ppu.checkArmOAMBugWrite(originalValue);
    }

}
