package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.cpu.SM83;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

public class GameBoyEmulator implements Emulator, SM83.SystemBus {

    private static final int FRAMERATE = 60;
    public static final int CLOCK_FREQUENCY = 4194304;
    public static final int T_CYCLES_PER_FRAME = 70224;
    public static final int M_CYCLES_PER_FRAME = T_CYCLES_PER_FRAME / 4;

    private final GameBoyHost host;

    private final SM83 cpu;
    private final DMGBus<?> bus;
    private final DMGPPU<?> ppu;
    private final DMGAPU<?> apu;
    private final GameBoyJoypad<?> joypad;

    private final DMGMMIOBus mmioBus;
    private final DMGTimerController<?> timerController;
    private final DMGSerialController<?> serialController;

    private final GameBoyCartridge cartridge;

    public GameBoyEmulator(GameBoyHost host) {
        this.host = host;

        this.joypad = new GameBoyJoypad<>(this);
        this.cpu = this.createCpu();
        this.bus = this.createBus();
        this.ppu = this.createPpu();
        this.apu = this.createApu();

        this.mmioBus = this.createMmioBus();
        this.timerController = this.createTimerController();
        this.serialController = this.createSerialController();

        this.cartridge = GameBoyCartridge.getCartridge(this);
    }

    protected SM83 createCpu() {
        return new SM83(this);
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

    protected DMGMMIOBus createMmioBus() {
        return new DMGMMIOBus(this);
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

    @Override
    public SM83 getCpu() {
        return this.cpu;
    }

    @Override
    public DMGBus<?> getBusView() {
        return this.bus;
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

    public DMGMMIOBus getMMIOBus() {
        return this.mmioBus;
    }

    public DMGTimerController<?> getTimerController() {
        return this.timerController;
    }

    public DMGSerialController<?> getSerialController() {
        return this.serialController;
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
        this.cpu.cycle();
        boolean apuFrameSequencerTick = this.timerController.cycle();
        this.cpu.nextState();
        this.ppu.cycle();
        this.apu.cycle(apuFrameSequencerTick);
        this.serialController.cycle();
        this.cartridge.cycle();
        this.bus.cycle();
    }

    @Override
    public int getFramerate() {
        return FRAMERATE;
    }

    @Override
    public void close() {
        try {
        /*
        if (this.disassembler != null) {
            this.disassembler.close();
        }
         */
            if (this.cartridge != null) {
                this.cartridge.save();
            }
        } catch (Exception e) {
            throw new EmulatorException("Error releasing emulator resources: ", e);
        }
    }

    @Override
    public int getIE() {
        return this.mmioBus.getIE();
    }

    @Override
    public void setIF(int value) {
        this.mmioBus.setIF(value);
    }

    @Override
    public void onStopInstruction() {

    }

    @Override
    public int getIF() {
        return this.mmioBus.getIF();
    }


}
