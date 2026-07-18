package io.github.arkosammy12.jemu.frontend.gui;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface SystemDisplayComponent {

    int getSystemDisplayWidth();

    int getSystemDisplayHeight();

    @NotNull
    Component getComponent();

}
