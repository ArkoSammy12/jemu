package io.github.arkosammy12.jemu.frontend.swing.menus;

import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;

public abstract class MenuBarMenu {

    protected final JMenu jMenu;

    public MenuBarMenu() {
        jMenu = new JMenu();
    }

    @ApiStatus.Internal
    public JMenu getJMenu() {
        return this.jMenu;
    }

}
