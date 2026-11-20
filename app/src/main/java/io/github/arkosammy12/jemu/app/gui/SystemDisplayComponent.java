package io.github.arkosammy12.jemu.app.gui;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface SystemDisplayComponent {

    int getSystemDisplayWidth();

    int getSystemDisplayHeight();

    double getSystemAspectRatio();

    @NotNull
    Component getComponent();

}
