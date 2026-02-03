package io.github.arkosammy12.jemu.systems;

import io.github.arkosammy12.jemu.config.settings.EmulatorSettings;
import io.github.arkosammy12.jemu.config.settings.GameBoyEmulatorSettings;
import io.github.arkosammy12.jemu.systems.bus.DMGBus;
import io.github.arkosammy12.jemu.systems.cpu.Processor;
import io.github.arkosammy12.jemu.systems.cpu.SM83;
import io.github.arkosammy12.jemu.disassembler.Disassembler;
import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.sound.DMGAPU;
import io.github.arkosammy12.jemu.systems.sound.SoundSystem;
import io.github.arkosammy12.jemu.systems.video.DMGPPU;
import io.github.arkosammy12.jemu.systems.video.Display;
import io.github.arkosammy12.jemu.ui.debugger.DebuggerSchema;
import io.github.arkosammy12.jemu.util.System;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyAdapter;
import java.util.List;

public class GameBoyEmulator implements Emulator, SystemBus {

    private static final int FRAMERATE = 60;
    private static final int M_CYCLE_FREQUENCY = 4194304;
    private static final int M_CYCLES_PER_FRAME = 70224;

    private final Jemu jemu;
    private final GameBoyEmulatorSettings  emulatorSettings;
    private final System system;

    private final SM83 processor;
    private final DMGBus bus;
    private final DMGPPU<?> ppu;
    private final DMGAPU apu;

    public GameBoyEmulator(GameBoyEmulatorSettings emulatorSettings) {
        this.jemu = emulatorSettings.getJemu();
        this.emulatorSettings = emulatorSettings;
        this.system = emulatorSettings.getSystem();

        this.processor = new SM83(this);
        this.bus = new DMGBus(this);
        this.ppu = new DMGPPU<>(this);
        this.apu = new DMGAPU(this);
    }

    @Override
    public Processor getProcessor() {
        return this.processor;
    }

    @Override
    public DMGBus getBus() {
        return this.bus;
    }

    @Override
    public Display<?> getDisplay() {
        return this.ppu;
    }

    @Override
    public SoundSystem getSoundSystem() {
        return this.apu;
    }

    @Override
    public EmulatorSettings getEmulatorSettings() {
        return null;
    }

    @Override
    public System getSystem() {
        return null;
    }

    @Override
    public List<KeyAdapter> getKeyAdapters() {
        return List.of();
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

    }

    @Override
    public void executeCycle() {

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

    }
}
