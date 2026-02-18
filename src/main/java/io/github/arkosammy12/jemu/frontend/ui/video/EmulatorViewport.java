package io.github.arkosammy12.jemu.frontend.ui.video;

import io.github.arkosammy12.jemu.application.Jemu;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EmulatorViewport extends JPanel {

    private JPanel displayRenderer;

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

        jemu.addStateChangedListener((systemAdapter, _, newState) -> {
            if (systemAdapter == null || newState.isStopping()) {
                this.setDisplayRenderer(null);
            } else if (newState.isResetting()) {
                this.setDisplayRenderer(systemAdapter.getJPanelVideoDriver());
            }
        });

        jemu.addEmulatorFrameListener((systemAdapter, _) -> {
            if (systemAdapter != null) {
                systemAdapter.getJPanelVideoDriver().requestFrame();
            }
        });

        jemu.addShutdownListener(() -> this.setDisplayRenderer(null));
    }

    private JPanel getDisplayRenderer() {
        return this.displayRenderer;
    }

    private void setDisplayRenderer(JPanel newRenderer) {
        SwingUtilities.invokeLater(() -> {
            JPanel currentRenderer = this.getDisplayRenderer();
            if (currentRenderer != null) {
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
