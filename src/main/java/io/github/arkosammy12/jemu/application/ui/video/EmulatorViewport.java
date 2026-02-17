package io.github.arkosammy12.jemu.application.ui.video;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EmulatorViewport extends JPanel {

    private final DisplayRenderer displayRenderer = new DisplayRenderer();

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
                this.clearDisplayRenderer();
            } else if (newState.isResetting()) {
                this.initializeDisplayRenderer(emulator);
            }
        });
        jemu.addEmulatorFrameListener((emulator, _) -> {
            if (emulator != null) {
                this.displayRenderer.requestFrame();
            }
        });
        jemu.addShutdownListener(this::clearDisplayRenderer);

        this.displayRenderer.setFocusable(true);
        this.displayRenderer.setOpaque(true);
        this.displayRenderer.setBackground(Color.BLACK);
        this.displayRenderer.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(displayRenderer::requestFocusInWindow);
            }

        });
        SwingUtilities.invokeLater(this.displayRenderer::requestFocusInWindow);
        this.revalidate();
        this.repaint();
    }

    public DisplayRenderer getDisplayRenderer() {
        return this.displayRenderer;
    }

    private void initializeDisplayRenderer(Emulator emulator) {
        this.displayRenderer.setVideoGenerator(emulator.getVideoGenerator());
        this.displayRenderer.setSystemController(emulator.getSystemController());
        this.add(displayRenderer, "grow, push");
        SwingUtilities.invokeLater(() -> {
            this.displayRenderer.requestFocusInWindow();
            this.revalidate();
            this.repaint();
        });
    }

    private void clearDisplayRenderer() {
        this.displayRenderer.setVideoGenerator(null);
        this.displayRenderer.setSystemController(null);
        this.remove(displayRenderer);
        SwingUtilities.invokeLater(() -> {
            this.revalidate();
            this.repaint();
        });
    }

}
