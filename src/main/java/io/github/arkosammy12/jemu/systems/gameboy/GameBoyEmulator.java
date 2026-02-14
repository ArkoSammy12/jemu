package io.github.arkosammy12.jemu.systems.gameboy;

import io.github.arkosammy12.jemu.config.settings.EmulatorSettings;
import io.github.arkosammy12.jemu.config.settings.GameBoyEmulatorSettings;
import io.github.arkosammy12.jemu.systems.common.Bus;
import io.github.arkosammy12.jemu.systems.common.Emulator;
import io.github.arkosammy12.jemu.disassembler.Disassembler;
import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.common.SoundSystem;
import io.github.arkosammy12.jemu.ui.debugger.DebuggerSchema;
import io.github.arkosammy12.jemu.util.System;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyAdapter;
import java.util.List;

public class GameBoyEmulator implements Emulator, SM83.SystemBus {

    private static final int FRAMERATE = 60;
    private static final int CLOCK_FREQUENCY = 4194304;
    private static final int T_CYCLES_PER_FRAME = 70224;
    private static final int M_CYCLES_PER_FRAME = T_CYCLES_PER_FRAME / 4;

    private final Jemu jemu;
    private final GameBoyEmulatorSettings  emulatorSettings;
    private final System system;

    private final SM83 cpu;
    private final GameBoyBus bus;
    private final DMGPPU<?> ppu;
    private final DMGAPU apu;
    private final GameBoyJoypad joypad;

    private final GameBoyCartridge cartridge;
    private final GameBoyMMIOBus mmioController;
    private final GameBoyTimerController timerController;

    public GameBoyEmulator(GameBoyEmulatorSettings emulatorSettings) {
        this.jemu = emulatorSettings.getJemu();
        this.emulatorSettings = emulatorSettings;
        this.system = emulatorSettings.getSystem();

        this.joypad = new GameBoyJoypad(this);
        this.cpu = new SM83(this);
        this.bus = new GameBoyBus(this);
        this.ppu = new DMGPPU<>(this);
        this.apu = new DMGAPU(this);

        this.cartridge = GameBoyCartridge.getCartridge(this);
        this.mmioController = new GameBoyMMIOBus(this);
        this.timerController = new GameBoyTimerController(this);
    }

    @Override
    public SM83 getProcessor() {
        return this.cpu;
    }

    @Override
    public GameBoyBus getBusView() {
        return this.bus;
    }

    @Override
    public Bus getBus() {
        return this.bus;
    }

    @Override
    public DMGPPU<?> getDisplay() {
        return this.ppu;
    }

    public GameBoyJoypad getJoypad() {
        return this.joypad;
    }

    @Override
    public DMGAPU getSoundSystem() {
        return this.apu;
    }

    @Override
    public EmulatorSettings getEmulatorSettings() {
        return this.emulatorSettings;
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    @Override
    public List<KeyAdapter> getKeyAdapters() {
        return List.of(this.joypad);
    }

    public GameBoyCartridge getCartridge() {
        return this.cartridge;
    }

    public GameBoyMMIOBus getMMIOController() {
        return this.mmioController;
    }

    public GameBoyTimerController getTimerController() {
        return this.timerController;
    }

    @Override
    @Nullable
    public DebuggerSchema getDebuggerSchema() {
        return null;
    }

    @Override
    @Nullable
    public Disassembler getDisassembler() {
        return null;
    }

    @Override
    public void executeFrame() {
        for (int i = 0; i < M_CYCLES_PER_FRAME; i++) {
            this.cpu.cycle();
            this.bus.cycle();
            this.timerController.cycle();
            this.cpu.nextState();
            this.ppu.cycle();
        }
    }

    @Override
    public void executeCycle() {
        this.cpu.cycle();
        this.bus.cycle();
        this.timerController.cycle();
        this.cpu.nextState();
        this.ppu.cycle();
    }

    @Override
    public int getCurrentInstructionsPerFrame() {
        return 0;
    }

    @Override
    public int getFramerate() {
        return FRAMERATE;
    }

    @Override
    public void close() throws Exception {
        if (this.ppu != null) {
            this.ppu.close();
        }
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
