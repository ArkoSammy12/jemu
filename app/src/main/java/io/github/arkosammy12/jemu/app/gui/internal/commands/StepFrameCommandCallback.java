package io.github.arkosammy12.jemu.app.gui.internal.commands;

import io.github.arkosammy12.jemu.app.gui.commands.StepFrameEmulatorCommand;

public non-sealed interface StepFrameCommandCallback extends EmulatorCommandCallback {

    void onStepFrame(StepFrameEmulatorCommand stepFrameEmulatorCommand);

}
