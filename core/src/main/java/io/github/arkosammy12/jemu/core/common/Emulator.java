package io.github.arkosammy12.jemu.core.common;

import io.github.arkosammy12.jemu.core.disassembler.Disassembler;
import org.jetbrains.annotations.Nullable;

public interface Emulator extends AutoCloseable {

    SystemHost getHost();

    Processor getCpu();

    VideoGenerator<?> getVideoGenerator();

    AudioGenerator<?> getAudioGenerator();

    SystemController<?> getSystemController();

    BusView getBusView();

    /*
    @Nullable
    DebuggerSchema getDebuggerSchema();
     */

    /*
    @Nullable
    Disassembler getDisassembler();
     */

    void executeFrame();

    void executeCycle();

    int getFramerate();

}
