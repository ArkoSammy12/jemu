package io.github.arkosammy12.jemu.frontend.swing;

import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

public class SystemViewport {

    private final JPanel viewportPanel;
    private JPanel systemDisplayPanel;

    public SystemViewport() {
        MigLayout viewportPanelLayout = new MigLayout(new LC().insets("0"));
        this.viewportPanel = new JPanel(viewportPanelLayout);
        this.viewportPanel.setFocusable(true);
        this.viewportPanel.setBackground(Color.BLACK);
        this.viewportPanel.setPreferredSize(new Dimension(960, this.viewportPanel.getHeight()));
        this.viewportPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(viewportPanel::requestFocusInWindow);
            }

        });

    }

    @ApiStatus.Internal
    JPanel getJPanel() {
        return this.viewportPanel;
    }

    public void setSystemDisplayPanel(@Nullable Supplier<JPanel> displaySupplier) {
        SwingUtilities.invokeLater(() -> {
            if (this.systemDisplayPanel != null) {
                this.viewportPanel.remove(this.systemDisplayPanel);
            }
            if (displaySupplier != null) {
                this.systemDisplayPanel = displaySupplier.get();
                this.systemDisplayPanel.setFocusable(true);
                this.systemDisplayPanel.setOpaque(true);
                this.systemDisplayPanel.setBackground(Color.BLACK);
                this.systemDisplayPanel.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        SwingUtilities.invokeLater(systemDisplayPanel::requestFocusInWindow);
                    }

                });
                this.viewportPanel.add(this.systemDisplayPanel, "grow, push");
                SwingUtilities.invokeLater(this.systemDisplayPanel::requestFocusInWindow);
            }
            this.viewportPanel.revalidate();
            this.viewportPanel.repaint();
        });
    }

}
