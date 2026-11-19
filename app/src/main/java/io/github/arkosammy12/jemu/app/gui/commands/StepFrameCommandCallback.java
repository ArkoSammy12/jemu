package io.github.arkosammy12.jemu.app.gui.commands;

public non-sealed interface StepFrameCommandCallback extends EmulatorCommandCallback {

    void onStepFrame(StepFrameEmulatorCommand stepFrameEmulatorCommand);

}
