package io.github.arkosammy12.jemu.frontend.gui.swing;

import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;

public abstract class MenuBarMenu {

    protected final JMenu jMenu;

    public MenuBarMenu() {
        jMenu = new JMenu();
    }

    @ApiStatus.Internal
    protected JMenu getJMenu() {
        return this.jMenu;
    }

}
