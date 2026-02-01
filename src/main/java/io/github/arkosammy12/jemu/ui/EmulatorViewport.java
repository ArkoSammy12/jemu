package io.github.arkosammy12.jemu.ui;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.video.DisplayRenderer;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EmulatorViewport extends JPanel {

    private DisplayRenderer displayRenderer;

    public EmulatorViewport(Jemu jemu) {
        MigLayout migLayout = new MigLayout(new LC().insets("0"));
        super(migLayout);
        this.setFocusable(true);
        this.setBackground(Color.BLACK);
        this.setPreferredSize(new Dimension(960, this.getHeight()));
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e){
                SwingUtilities.invokeLater(() -> requestFocusInWindow());
            }

        });
        jemu.addStateChangedListener((emulator, _, newState) -> {
            if (emulator == null || newState.isStopping()) {
                this.setDisplayRenderer(null);
            } else if (newState.isResetting()) {
                this.setDisplayRenderer(emulator.getDisplay().getDisplayRenderer());
            }
        });
        jemu.addEmulatorFrameListener(emulator -> {
            if (emulator != null) {
                emulator.getDisplay().getDisplayRenderer().requestFrame();
            }
        });
        jemu.addShutdownListener(() -> this.setDisplayRenderer(null));
    }

    private DisplayRenderer getDisplayRenderer() {
        return this.displayRenderer;
    }

    private void setDisplayRenderer(DisplayRenderer newRenderer) {
        SwingUtilities.invokeLater(() -> {
            DisplayRenderer currentRenderer = this.getDisplayRenderer();
            if (currentRenderer != null) {
                currentRenderer.close();
                this.remove(currentRenderer);
            }
            this.displayRenderer = newRenderer;

            if (newRenderer != null) {
                newRenderer.setFocusable(true);
                newRenderer.setOpaque(true);
                newRenderer.setBackground(Color.BLACK);
                newRenderer.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        SwingUtilities.invokeLater(newRenderer::requestFocusInWindow);
                    }

                });

                this.add(newRenderer, "grow, push");
                SwingUtilities.invokeLater(newRenderer::requestFocusInWindow);
            }
            this.revalidate();
            this.repaint();
        });
    }

}
