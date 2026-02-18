package io.github.arkosammy12.jemu.backend.common;

import io.github.arkosammy12.jemu.backend.disassembler.Disassembler;
import io.github.arkosammy12.jemu.frontend.ui.debugger.DebuggerSchema;
import org.jetbrains.annotations.Nullable;

public interface Emulator extends AutoCloseable {

    SystemHost getHost();

    Processor getCpu();

    VideoGenerator<?> getVideoGenerator();

    AudioGenerator<?> getAudioGenerator();

    SystemController<?> getSystemController();

    BusView getBusView();

    @Nullable
    DebuggerSchema getDebuggerSchema();

    @Nullable
    Disassembler getDisassembler();

    void executeFrame();

    void executeCycle();

    int getCurrentInstructionsPerFrame();

    int getFramerate();

}
