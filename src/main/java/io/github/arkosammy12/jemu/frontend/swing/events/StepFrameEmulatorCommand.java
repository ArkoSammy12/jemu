package io.github.arkosammy12.jemu.frontend.swing.events;

public record StepFrameEmulatorCommand() implements EmulatorCommand {

    public non-sealed interface Callback extends EmulatorCommand.Callback {

        void onStepFrame(StepFrameEmulatorCommand stepFrameEmulatorCommand);

    }

}
