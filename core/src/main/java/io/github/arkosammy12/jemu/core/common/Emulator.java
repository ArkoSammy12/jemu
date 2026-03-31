package io.github.arkosammy12.jemu.core.common;


public interface Emulator extends AutoCloseable {

    SystemHost getHost();

    VideoGenerator<?> getVideoGenerator();

    AudioGenerator<?> getAudioGenerator();

    SystemController<?> getSystemController();

    void executeFrame();

    void executeCycle();

    int getFramerate();

}
