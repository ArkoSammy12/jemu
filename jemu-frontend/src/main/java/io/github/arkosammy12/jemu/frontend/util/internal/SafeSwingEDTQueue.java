package io.github.arkosammy12.jemu.frontend.util.internal;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import org.tinylog.Supplier;

import javax.swing.*;
import java.awt.*;

public class SafeSwingEDTQueue extends EventQueue {

    private final Supplier<JFrame> jFrameSupplier;

    public SafeSwingEDTQueue(@NotNull Supplier<JFrame> jFrameSupplier) {
        this.jFrameSupplier = jFrameSupplier;
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        try {
            super.dispatchEvent(event);
        } catch (Throwable t) {
            this.handleException(t);
        }
    }

    private void handleException(Throwable t) {
        Logger.error("Uncaught Swing exception", t);

        JFrame appFrame = this.jFrameSupplier.get();

        if (appFrame != null) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            appFrame,
                            t.toString(),
                            "Uncaught Swing UI exception",
                            JOptionPane.ERROR_MESSAGE
                    )
            );
        }
    }

}