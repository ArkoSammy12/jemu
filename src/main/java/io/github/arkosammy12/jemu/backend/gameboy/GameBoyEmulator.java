package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.SystemHost;
import io.github.arkosammy12.jemu.backend.common.Bus;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.backend.disassembler.Disassembler;
import io.github.arkosammy12.jemu.backend.cores.SM83;
import org.jetbrains.annotations.Nullable;

import static io.github.arkosammy12.jemu.backend.cores.SM83.INSTRUCTION_FINISHED_FLAG;

public class GameBoyEmulator implements Emulator, SM83.SystemBus {

    private static final int FRAMERATE = 60;
    public static final int CLOCK_FREQUENCY = 4194304;
    public static final int T_CYCLES_PER_FRAME = 70224;
    private static final int M_CYCLES_PER_FRAME = T_CYCLES_PER_FRAME / 4;

    private final GameBoyHost host;
    private int currentInstructionsPerFrame;

    private final SM83 cpu;
    private final DMGBus bus;
    private final DMGPPU<?> ppu;
    private final DMGAPU<?> apu;
    private final GameBoyJoypad<?> joypad;

    private final GameBoyCartridge cartridge;
    private final DMGMMIOBus mmioController;
    private final GameBoyTimerController<?> timerController;
    private final DMGSerialController serialController;

    public GameBoyEmulator(GameBoyHost host) {
        this.host = host;

        this.joypad = new GameBoyJoypad<>(this);
        this.cpu = new SM83(this);
        this.bus = new DMGBus(this);
        this.ppu = new DMGPPU<>(this);
        this.apu = new DMGAPU<>(this);

        this.cartridge = GameBoyCartridge.getCartridge(this);
        this.mmioController = new DMGMMIOBus(this);
        this.timerController = new GameBoyTimerController<>(this);
        this.serialController = new DMGSerialController<>(this);
    }

    @Override
    public SystemHost getHost() {
        return this.host;
    }

    @Override
    public SM83 getCpu() {
        return this.cpu;
    }

    @Override
    public DMGBus getBusView() {
        return this.bus;
    }

    @Override
    public Bus getBus() {
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

    public DMGMMIOBus getMMIOController() {
        return this.mmioController;
    }

    public GameBoyTimerController<?> getTimerController() {
        return this.timerController;
    }

    public DMGSerialController<?> getSerialController() {
        return this.serialController;
    }

    /*
    @Override
    @Nullable
    public DebuggerSchema getDebuggerSchema() {
        return null;
    }
     */

    @Override
    @Nullable
    public Disassembler getDisassembler() {
        return null;
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

    private void runCycle() {
        int flags = this.cpu.cycle();
        boolean apuFrameSequencerTick = this.timerController.cycle();
        this.cpu.nextState();
        this.ppu.cycle();
        this.apu.cycle(apuFrameSequencerTick);
        this.serialController.cycle();
        this.cartridge.cycle();
        this.bus.cycle();

        if ((flags & INSTRUCTION_FINISHED_FLAG) != 0) {
            this.currentInstructionsPerFrame++;
        }
    }

    @Override
    public int getCurrentInstructionsPerFrame() {
        int ret = this.currentInstructionsPerFrame;
        this.currentInstructionsPerFrame = 0;
        return ret;
    }

    @Override
    public int getFramerate() {
        return FRAMERATE;
    }

    @Override
    public void close() {
        /*
        if (this.disassembler != null) {
            this.disassembler.close();
        }
         */
    }

    @Override
    public int getIE() {
        return this.mmioController.getIE();
    }

    @Override
    public void setIF(int value) {
        this.mmioController.setIF(value);
    }

    @Override
    public int getIF() {
        return this.mmioController.getIF();
    }


}
