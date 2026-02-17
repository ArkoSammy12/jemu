package io.github.arkosammy12.jemu.application.ui.util;

import javax.swing.*;
import java.awt.*;

import static javax.swing.JSplitPane.VERTICAL_SPLIT;

public class ToggleableSplitPane extends JPanel {

    private static final String SPLIT = "split";
    private static final String SINGLE = "single";

    private final CardLayout layout = new CardLayout();
    private final JSplitPane splitPane;
    private final Component left;
    private final Component right;
    private double currentProportionalDividerLocation;
    private boolean firstTimeShown = false;

    public ToggleableSplitPane(int orientation, Component left, Component right, int dividerSize, double resizeWeight) {
        this.setLayout(layout);
        this.setFocusable(false);

        this.left = left;
        this.right = right;

        this.splitPane = new JSplitPane(orientation, null, right);
        this.splitPane.setDividerSize(dividerSize);
        this.splitPane.setResizeWeight(resizeWeight);
        this.splitPane.setContinuousLayout(true);
        this.splitPane.setFocusable(false);

        this.left.setVisible(true);
        this.right.setVisible(false);

        this.add(this.splitPane, SPLIT);
        this.add(this.left, SINGLE);

        this.layout.show(this.left.getParent(), SINGLE);
    }

    public void setAbsoluteDividerLocation(int location) {
        this.splitPane.setDividerLocation(location);
    }

    public int getAbsoluteDividerLocation() {
        return this.splitPane.getDividerLocation();
    }

    public void toggleShowSplit(boolean enabled) {
        if (enabled) {
            if (!this.isSplitVisible()) {
                this.showSplit();
            }
        } else {
            if (this.isSplitVisible()) {
                this.hideRightPanel();
            }
        }
    }

    private void showSplit() {
        this.splitPane.setLeftComponent(this.left);
        this.left.setVisible(true);
        this.layout.show(this, SPLIT);
        if (!this.firstTimeShown) {
            this.splitPane.resetToPreferredSizes();
            this.firstTimeShown = true;
        } else {
            this.splitPane.setDividerLocation(this.currentProportionalDividerLocation);
        }
        this.right.setVisible(true);
    }

    private void hideRightPanel() {
        this.currentProportionalDividerLocation = this.getProportionalDividerLocation();
        this.add(this.left, SINGLE);
        this.layout.show(this, SINGLE);
        this.right.setVisible(false);
    }

    private boolean isSplitVisible() {
        return this.splitPane.isShowing();
    }

    private double getProportionalDividerLocation() {
        if (this.splitPane.getOrientation() == VERTICAL_SPLIT) {
            return (double) this.splitPane.getDividerLocation() / (this.splitPane.getHeight() - this.splitPane.getDividerSize());
        } else {
            return (double) this.splitPane.getDividerLocation() / (this.splitPane.getWidth() - this.splitPane.getDividerSize());
        }
    }

}

