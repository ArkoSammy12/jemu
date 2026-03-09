package io.github.arkosammy12.jemu.frontend.gui.swing.events;

public record StopEmulatorCommand() implements EmulatorCommand {

    public non-sealed interface Callback extends EmulatorCommand.Callback {

        void onStop(StopEmulatorCommand stopEmulatorCommand);

    }

}
