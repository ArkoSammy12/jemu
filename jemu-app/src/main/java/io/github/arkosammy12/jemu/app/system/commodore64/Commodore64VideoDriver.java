package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.drivers.DefaultSystemVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.GlueVideoDriver;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.frontend.events.VideoSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.util.StatusBar;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class Commodore64VideoDriver extends JPanel implements GlueVideoDriver {

    private final DefaultSystemVideoDriver systemVideoDriver;
    private final JLabel tapeCounterLabel;
    private final JPanel tapeIndicatorPanel;
    private int lastTapeCounterValue;
    private boolean lastMotorActive;

    public Commodore64VideoDriver(Jemu jemu, VideoGenerator videoGenerator) {
        super(new MigLayout(new LC().insets("0").fill().gridGap("0", "0")));

        this.systemVideoDriver = new DefaultSystemVideoDriver(jemu, videoGenerator);
        this.systemVideoDriver.setFocusable(false);
        StatusBar statusBar = new StatusBar();
        this.tapeCounterLabel = statusBar.createLabel(new CC().gap("5"));

        this.tapeIndicatorPanel = new JPanel();
        this.tapeIndicatorPanel.setBackground(Color.BLACK);
        this.tapeIndicatorPanel.setForeground(Color.BLACK);
        this.tapeIndicatorPanel.setSize(10, 10);
        this.tapeIndicatorPanel.setPreferredSize(this.tapeIndicatorPanel.getSize());
        this.tapeIndicatorPanel.setMinimumSize(this.tapeIndicatorPanel.getSize());
        this.tapeIndicatorPanel.setMaximumSize(this.tapeIndicatorPanel.getSize());
        statusBar.add(this.tapeIndicatorPanel, new CC().gap("0"));

        statusBar.add(new JLabel(), new CC().growX().pushX());

        this.add(this.systemVideoDriver, new CC().grow().push().wrap());
        this.add(statusBar, new CC().grow().pushX().dockSouth().height("28!"));
        this.tapeCounterLabel.setVisible(true);


        this.tapeCounterLabel.setText("Tape: %03d".formatted(0));
    }

    @Override
    public void outputFrame(int[] frameBuffer) {
        this.systemVideoDriver.outputFrame(frameBuffer);
    }

    @Override
    public int getSystemDisplayWidth() {
        return this.systemVideoDriver.getSystemDisplayWidth();
    }

    @Override
    public int getSystemDisplayHeight() {
        return this.systemVideoDriver.getSystemDisplayHeight();
    }

    @Override
    public double getSystemAspectRatio() {
        return this.systemVideoDriver.getSystemAspectRatio();
    }

    @Override
    @NotNull
    public Component getComponent() {
        return this;
    }

    @Override
    public void requestFrame() {
        this.systemVideoDriver.requestFrame();
    }

    @Override
    public void onVideoSettingChangedEvent(VideoSettingChangedEvent videoSettingChangedEvent) {
        this.systemVideoDriver.onVideoSettingChangedEvent(videoSettingChangedEvent);
    }

    public void setTapeCounter(int counter) {
        counter %= 1000;
        if (counter != this.lastTapeCounterValue) {
            this.lastTapeCounterValue = counter;
            SwingUtilities.invokeLater(() -> this.tapeCounterLabel.setText("Tape: %03d".formatted(this.lastTapeCounterValue)));
        }
    }

    public void setMotorActive(boolean motorActive) {
        if (motorActive != this.lastMotorActive) {
            this.lastMotorActive = motorActive;
            SwingUtilities.invokeLater(() -> {
                Color color = motorActive ? Color.GREEN : Color.BLACK;
                this.tapeIndicatorPanel.setBackground(color);
                this.tapeIndicatorPanel.setForeground(color);
            });
        }
    }

    @Override
    public void close() {
        this.systemVideoDriver.close();
    }

}
