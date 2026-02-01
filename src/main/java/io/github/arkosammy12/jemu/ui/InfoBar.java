package io.github.arkosammy12.jemu.ui;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.Emulator;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class InfoBar extends JPanel {

    private final JTextField systemField = createField();
    private final JTextField romTitleField = createField();
    private final JTextField ipfField = createField();
    private final JTextField mipsField = createField();
    private final JTextField frameTimeField = createField();
    private final JTextField fpsField = createField();

    private long lastWindowTitleUpdate = 0;
    private long lastFrameTime = System.nanoTime();
    private int framesSinceLastUpdate = 0;
    private long totalIpfSinceLastUpdate = 0;
    private double totalFrameTimeSinceLastUpdate = 0;

    public InfoBar(Jemu jemu) {
        MigLayout migLayout = new MigLayout(new LC().insets("1"), new AC().gap("5").gap("5").gap("5").gap("5").gap("5"), new AC());
        super(migLayout);

        this.add(createScrollPanel(systemField, "The system used by the currently running ROM."), new CC().grow().push());
        this.add(createScrollPanel(romTitleField, "The name or file name of the currently running ROM."), new CC().grow().push());
        this.add(createScrollPanel(ipfField, "The current IPF value average."), new CC().grow().push());
        this.add(createScrollPanel(mipsField, "The current MIPS value average."), new CC().grow().push());
        this.add(createScrollPanel(frameTimeField, "The current frame time value average, in milliseconds."), new CC().grow().push());
        this.add(createScrollPanel(fpsField, "The current frames per second value average."), new CC().grow().push());

        jemu.addStateChangedListener((emulator, _, newState) -> {
            if (emulator == null || newState.isStopping()) {
                this.onStopping();
            }
        });
        jemu.addEmulatorFrameListener(this::onFrame);
    }

    private static JTextField createField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBorder(null);
        field.setFocusable(false);
        field.setBackground(null);
        return field;
    }

    private static JScrollPane createScrollPanel(JTextField field, String tooltip) {
        field.setToolTipText(tooltip);
        return new JScrollPane(field, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private void onStopping() {
        this.lastWindowTitleUpdate = 0;
        this.lastFrameTime = System.nanoTime();
        this.framesSinceLastUpdate = 0;
        this.totalIpfSinceLastUpdate = 0;
        this.totalFrameTimeSinceLastUpdate = 0;
        SwingUtilities.invokeLater(() -> {
            this.systemField.setText("");

            this.romTitleField.setText("");

            this.ipfField.setText("");
            this.mipsField.setText("");
            this.frameTimeField.setText("");
            this.fpsField.setText("");
            this.revalidate();
            this.repaint();
        });
    }

    private void onFrame(@Nullable Emulator emulator) {
        if (emulator == null) {
            return;
        }
        String romTitle = emulator.getEmulatorSettings().getRomTitle().orElse("N/A");
        String variantName = emulator.getSystem().getDisplayName();

        boolean updateTitleNow = false;
        boolean updateStatsNow = false;

        if (!Objects.equals(this.romTitleField.getText(), romTitle) || !Objects.equals(this.systemField.getText(), variantName)) {
            updateTitleNow = true;
        }

        this.totalIpfSinceLastUpdate += emulator.getCurrentInstructionsPerFrame();
        long now = System.nanoTime();
        double lastFrameDuration = now - lastFrameTime;
        lastFrameTime = now;
        totalFrameTimeSinceLastUpdate += lastFrameDuration;
        framesSinceLastUpdate++;

        long deltaTime = now - lastWindowTitleUpdate;

        double fps = 0;
        long averageIpf = 0;
        double averageFrameTimeMs = 0;
        double mips = 0;

        if (deltaTime >= 1_000_000_000L) {
            updateStatsNow = true;

            fps = framesSinceLastUpdate / (deltaTime / 1_000_000_000.0);
            averageIpf = totalIpfSinceLastUpdate / framesSinceLastUpdate;
            averageFrameTimeMs = (totalFrameTimeSinceLastUpdate / framesSinceLastUpdate) / 1_000_000.0;
            mips = (averageIpf * fps) / 1_000_000.0;

            framesSinceLastUpdate = 0;
            totalIpfSinceLastUpdate = 0;
            totalFrameTimeSinceLastUpdate = 0;
            lastWindowTitleUpdate = now;
        }

        if (updateTitleNow || updateStatsNow) {
            final boolean fUpdateTitle = updateTitleNow;
            final boolean fUpdateStats = updateStatsNow;
            final String fRomTitle = romTitle;
            final String fVariantName = variantName;
            final double fFps = fps;
            final long fAverageIpf = averageIpf;
            final double fAverageFrameTimeMs = averageFrameTimeMs;
            final double fMips = mips;

            SwingUtilities.invokeLater(() -> {
                if (fUpdateTitle) {
                    romTitleField.setText(fRomTitle);
                    systemField.setText(fVariantName);
                }

                if (fUpdateStats) {
                    this.ipfField.setText("IPF: " + fAverageIpf);
                    this.mipsField.setText("MIPS: " + String.format("%.2f", fMips));
                    this.frameTimeField.setText("Frame time: " + String.format("%.2f ms", fAverageFrameTimeMs));
                    this.fpsField.setText("FPS: " + String.format("%.2f", fFps));
                }

                this.revalidate();
                this.repaint();
            });
        }
    }

}
