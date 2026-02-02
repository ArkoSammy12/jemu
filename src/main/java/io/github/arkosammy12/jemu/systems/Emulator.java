package io.github.arkosammy12.jemu.systems;

import io.github.arkosammy12.jemu.config.settings.EmulatorSettings;
import io.github.arkosammy12.jemu.cpu.Processor;
import io.github.arkosammy12.jemu.disassembler.Disassembler;
import io.github.arkosammy12.jemu.ui.debugger.DebuggerSchema;
import io.github.arkosammy12.jemu.util.System;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyAdapter;
import java.util.List;

public interface Emulator extends AutoCloseable {

    Processor getProcessor();

    Bus getBus();

    Display<?> getDisplay();

    SoundSystem getSoundSystem();

    EmulatorSettings getEmulatorSettings();

    System getSystem();

    List<KeyAdapter> getKeyAdapters();

    @Nullable
    DebuggerSchema getDebuggerSchema();

    @Nullable
    Disassembler getDisassembler();

    void executeFrame();

    void executeCycle();

    int getCurrentInstructionsPerFrame();

    int getFramerate();

}
