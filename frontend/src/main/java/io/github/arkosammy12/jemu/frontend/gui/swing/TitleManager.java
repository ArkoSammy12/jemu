package io.github.arkosammy12.jemu.frontend.gui.swing;

import io.github.arkosammy12.jemu.frontend.gui.internal.commands.PowerCycleCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.StopCommandCallback;

import javax.swing.*;

public class TitleManager {

    private final MainWindow mainWindow;

    private volatile String projectNameString = "unknown";
    private volatile String romTitleString = "No title";
    private volatile String fpsString = "0 FPS (0 ms)";

    private long lastWindowTitleUpdate = 0;
    private long lastFrameTime = System.nanoTime();
    private int framesSinceLastUpdate = 0;
    private double totalFrameTimeSinceLastUpdate = 0;

    public TitleManager(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        mainWindow.<StopCommandCallback>onEmulatorCommand(_ -> {
            lastWindowTitleUpdate = 0;
            lastFrameTime = System.nanoTime();
            framesSinceLastUpdate = 0;
            totalFrameTimeSinceLastUpdate = 0;
            SwingUtilities.invokeLater(() -> {
                this.romTitleString = "";
                this.fpsString = "";
                this.projectNameString = this.mainWindow.getMainMenuBar().getHelpMenu().getProjectName();
                this.mainWindow.getJFrame().setTitle(this.projectNameString);
            });
        });

        mainWindow.<PowerCycleCommandCallback>onEmulatorCommand(_ -> {
            this.projectNameString = this.mainWindow.getMainMenuBar().getHelpMenu().getProjectName();
        });

    }

    public void update(String romTitle) {
        boolean updateTitleNow = !romTitle.equals(this.romTitleString);

        long now = System.nanoTime();
        double lastFrameDuration = (double) (now - lastFrameTime);
        lastFrameTime = now;
        totalFrameTimeSinceLastUpdate += lastFrameDuration;
        framesSinceLastUpdate++;

        boolean updateStatsNow = false;
        String newFpsString = null;

        long deltaTime = now - lastWindowTitleUpdate;
        if (deltaTime >= 1_000_000_000L) {
            updateStatsNow = true;
            double fps = (double) framesSinceLastUpdate / ((double) deltaTime / 1_000_000_000.0);
            double avgMs = (totalFrameTimeSinceLastUpdate / (double) framesSinceLastUpdate) / 1_000_000.0;
            newFpsString = "%.2f FPS (%.2f ms)".formatted(fps, avgMs);

            framesSinceLastUpdate = 0;
            totalFrameTimeSinceLastUpdate = 0;
            lastWindowTitleUpdate = now;
        }

        if (updateTitleNow || updateStatsNow) {
            final String titleSnapshot = updateTitleNow ? romTitle : this.romTitleString;
            final String fpsSnapshot = updateStatsNow ? newFpsString : this.fpsString;
            final String fullTitle = this.projectNameString + " - " + titleSnapshot + " - " + fpsSnapshot;

            if (updateTitleNow) {
                this.romTitleString = titleSnapshot;
            }
            if (updateStatsNow) {
                this.fpsString = fpsSnapshot;
            }

            SwingUtilities.invokeLater(() -> this.mainWindow.getJFrame().setTitle(fullTitle));
        }
    }

}
