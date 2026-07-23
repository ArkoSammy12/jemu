package io.github.arkosammy12.jemu.frontend.gui;

import io.github.arkosammy12.jemu.frontend.gui.internal.commands.PowerCycleCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.StopCommandCallback;

import javax.swing.*;

public class TitleManager {

    private final MainWindow mainWindow;
    private final JFrame appFrame;

    private volatile String mainTitle = "unknown";
    private volatile String programTitleString = "No title";
    private volatile String fpsString = "0 FPS (0 ms)";

    private long lastWindowTitleUpdate = 0;
    private long lastFrameTime = System.nanoTime();
    private int framesSinceLastUpdate = 0;
    private double totalFrameTimeSinceLastUpdate = 0;

    public TitleManager(MainWindow mainWindow, JFrame appFrame) {
        this.mainWindow = mainWindow;
        this.appFrame = appFrame;

        mainWindow.<StopCommandCallback>onEmulatorCommand(_ -> {
            lastWindowTitleUpdate = 0;
            lastFrameTime = System.nanoTime();
            framesSinceLastUpdate = 0;
            totalFrameTimeSinceLastUpdate = 0;
            SwingUtilities.invokeLater(() -> {
                this.programTitleString = "";
                this.fpsString = "";
                this.mainTitle = mainWindow.getTitle();
                appFrame.setTitle(this.mainTitle);
            });
        });

        mainWindow.<PowerCycleCommandCallback>onEmulatorCommand(_ -> {
            this.mainTitle = this.mainWindow.getTitle();
        });

    }

    public void update(String programTitle) {
        boolean updateTitleNow = !programTitle.equals(this.programTitleString);

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
            final String titleSnapshot = updateTitleNow ? programTitle : this.programTitleString;
            final String fpsSnapshot = updateStatsNow ? newFpsString : this.fpsString;
            final String fullTitle = this.mainTitle + " - " + titleSnapshot + " - " + fpsSnapshot;

            if (updateTitleNow) {
                this.programTitleString = titleSnapshot;
            }
            if (updateStatsNow) {
                this.fpsString = fpsSnapshot;
            }

            SwingUtilities.invokeLater(() -> this.appFrame.setTitle(fullTitle));
        }
    }

}
