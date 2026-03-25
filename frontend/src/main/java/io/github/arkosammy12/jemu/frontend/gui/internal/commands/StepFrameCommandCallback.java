package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

import io.github.arkosammy12.jemu.frontend.gui.swing.commands.StepFrameEmulatorCommand;

public non-sealed interface StepFrameCommandCallback extends EmulatorCommandCallback {

    void onStepFrame(StepFrameEmulatorCommand stepFrameEmulatorCommand);

}
