package io.github.arkosammy12.jemu.frontend.gui;

import io.github.arkosammy12.jemu.frontend.gui.internal.commands.PowerCycleCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.StopCommandCallback;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class TitleManager {

    private final MainWindow mainWindow;
    private final JFrame appFrame;

    private volatile String mainTitle = "unknown";

    @Nullable
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
            this.lastWindowTitleUpdate = 0;
            this.lastFrameTime = System.nanoTime();
            this.framesSinceLastUpdate = 0;
            this.totalFrameTimeSinceLastUpdate = 0;
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

    public void onFrame(@Nullable String programTitle) {
        boolean updateTitleNow = !Objects.equals(programTitle, this.programTitleString);

        long now = System.nanoTime();
        double lastFrameDuration = (double) (now - this.lastFrameTime);
        this.lastFrameTime = now;
        this.totalFrameTimeSinceLastUpdate += lastFrameDuration;
        this.framesSinceLastUpdate++;

        boolean updateStatsNow = false;
        String newFpsString = null;

        long deltaTime = now - lastWindowTitleUpdate;
        if (deltaTime >= 1_000_000_000L) {
            updateStatsNow = true;
            double fps = (double) this.framesSinceLastUpdate / ((double) deltaTime / 1_000_000_000.0);
            double avgMs = (this.totalFrameTimeSinceLastUpdate / (double) this.framesSinceLastUpdate) / 1_000_000.0;
            newFpsString = "%.2f FPS (%.2f ms)".formatted(fps, avgMs);

            this.framesSinceLastUpdate = 0;
            this.totalFrameTimeSinceLastUpdate = 0;
            this.lastWindowTitleUpdate = now;
        }

        if (updateTitleNow || updateStatsNow) {
            String titleSnapshot = updateTitleNow ? programTitle : this.programTitleString;
            String fpsSnapshot = updateStatsNow ? newFpsString : this.fpsString;
            String fullTitle = this.mainTitle;
            if (titleSnapshot != null) {
                fullTitle += " - " + titleSnapshot;
            }
            fullTitle += " - " + fpsSnapshot;
            final String fullTitleSnapshot = fullTitle;

            if (updateTitleNow) {
                this.programTitleString = titleSnapshot;
            }
            if (updateStatsNow) {
                this.fpsString = fpsSnapshot;
            }

            SwingUtilities.invokeLater(() -> this.appFrame.setTitle(fullTitleSnapshot));
        }
    }

}
