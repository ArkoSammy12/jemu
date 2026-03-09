package io.github.arkosammy12.jemu.frontend.gui.swing;

import io.github.arkosammy12.jemu.frontend.gui.swing.events.StopEmulatorCommand;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;
import java.util.Objects;

public class StatusBar {

    private final JPanel jPanel;

    private final JTextField systemField = createField();
    private final JTextField romTitleField = createField();
    private final JTextField frameTimeField = createField();
    private final JTextField fpsField = createField();

    private long lastWindowTitleUpdate = 0;
    private long lastFrameTime = System.nanoTime();
    private int framesSinceLastUpdate = 0;
    private double totalFrameTimeSinceLastUpdate = 0;

    public StatusBar(MainWindow mainWindow) {
        MigLayout migLayout = new MigLayout(new LC().insets("1"), new AC(), new AC());
        this.jPanel = new JPanel(migLayout);

        this.jPanel.add(createPanel(systemField, "The system used by the currently running ROM."), new CC().growX());
        this.jPanel.add(createPanel(romTitleField, "The name or file name of the currently running ROM."), new CC().growX());
        this.jPanel.add(createPanel(frameTimeField, "The current frame time value average, in milliseconds."), new CC().growX());
        this.jPanel.add(createPanel(fpsField, "The current frames per second value average."), new CC().growX());

        mainWindow.<StopEmulatorCommand.Callback>addEmulatorCommandCallback(_ -> {
            this.lastWindowTitleUpdate = 0;
            this.lastFrameTime = System.nanoTime();
            this.framesSinceLastUpdate = 0;
            this.totalFrameTimeSinceLastUpdate = 0;
            SwingUtilities.invokeLater(() -> {
                this.systemField.setText("");
                this.romTitleField.setText("");
                this.frameTimeField.setText("");
                this.fpsField.setText("");
                this.jPanel.revalidate();
                this.jPanel.repaint();
            });
        });

    }

    private static JPanel createPanel(JTextField field, String tooltip) {
        field.setToolTipText(tooltip);
        JPanel panel = new JPanel(new MigLayout(new LC().insets("0"), new AC().grow(), new AC().align("center")));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground")));
        panel.add(field, new CC().alignY("center"));
        return panel;
    }

    private static JTextField createField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBorder(null);
        field.setFocusable(false);
        field.setBackground(null);
        return field;
    }

    @ApiStatus.Internal
    public JPanel getJPanel() {
        return this.jPanel;
    }

    public void update(String romTitle, String systemName) {

        boolean updateTitleNow = false;
        boolean updateStatsNow = false;

        if (!Objects.equals(this.romTitleField.getText(), romTitle) || !Objects.equals(this.systemField.getText(), systemName)) {
            updateTitleNow = true;
        }

        long now = System.nanoTime();
        double lastFrameDuration = now - lastFrameTime;
        lastFrameTime = now;
        totalFrameTimeSinceLastUpdate += lastFrameDuration;
        framesSinceLastUpdate++;

        long deltaTime = now - lastWindowTitleUpdate;

        double fps = 0;
        double averageFrameTimeMs = 0;

        if (deltaTime >= 1_000_000_000L) {
            updateStatsNow = true;

            fps = framesSinceLastUpdate / (deltaTime / 1_000_000_000.0);
            averageFrameTimeMs = (totalFrameTimeSinceLastUpdate / framesSinceLastUpdate) / 1_000_000.0;

            framesSinceLastUpdate = 0;
            totalFrameTimeSinceLastUpdate = 0;
            lastWindowTitleUpdate = now;
        }

        if (updateTitleNow || updateStatsNow) {
            final boolean fUpdateTitle = updateTitleNow;
            final boolean fUpdateStats = updateStatsNow;
            final String fRomTitle = romTitle;
            final String fVariantName = systemName;
            final double fFps = fps;
            final double fAverageFrameTimeMs = averageFrameTimeMs;

            SwingUtilities.invokeLater(() -> {
                if (fUpdateTitle) {
                    romTitleField.setText(fRomTitle);
                    systemField.setText(fVariantName);
                }

                if (fUpdateStats) {
                    this.frameTimeField.setText("Frame time: " + String.format("%.2f ms", fAverageFrameTimeMs));
                    this.fpsField.setText("FPS: " + String.format("%.2f", fFps));
                }

                this.jPanel.revalidate();
                this.jPanel.repaint();
            });
        }
    }

}
