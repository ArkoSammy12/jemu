package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class WindowSettings extends MenuBarMenu {

    private boolean fullScreen = false;

    private Rectangle windowBounds;
    private int windowExtendedState;

    public WindowSettings(MainWindow mainWindow, JFrame appFrame) {
        this.getJMenu().setText("Window");

        JRadioButtonMenuItem alwaysOnTopButton = new JRadioButtonMenuItem("Always on Top");
        alwaysOnTopButton.addActionListener(_ -> {
            boolean alwaysOnTop = alwaysOnTopButton.isSelected();
            appFrame.setAlwaysOnTop(alwaysOnTop);
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalWindowSettings().setAlwaysOnTop(alwaysOnTop);
        });

        JMenuItem fullScreenButton = new JMenuItem("Toggle Fullscreen");
        fullScreenButton.addActionListener(_ -> this.toggleFullScreen(appFrame, false));
        fullScreenButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0, true));

        JRadioButtonMenuItem startInFullScreenButton = new JRadioButtonMenuItem("Start in Fullscreen");
        startInFullScreenButton.addActionListener(_ -> mainWindow.getConfig().getInternalPreferenceSettings().getInternalWindowSettings().setStartInFullscreen(startInFullScreenButton.isSelected()));

        this.getJMenu().add(alwaysOnTopButton);
        this.getJMenu().add(fullScreenButton);
        this.getJMenu().add(startInFullScreenButton);

        if (mainWindow.getConfig().getInternalPreferenceSettings().getInternalWindowSettings().getAlwaysOnTop()) {
            alwaysOnTopButton.doClick();
        }

        if (mainWindow.getConfig().getInternalPreferenceSettings().getInternalWindowSettings().getStartInFullscreen()) {
            startInFullScreenButton.doClick();
            SwingUtilities.invokeLater(() -> this.toggleFullScreen(appFrame, true));
        }
    }

    private void toggleFullScreen(JFrame jFrame, boolean forceFullScreen) {
        this.fullScreen = forceFullScreen || !this.fullScreen;
        if (this.fullScreen) {
            this.windowBounds = jFrame.getBounds();
            this.windowExtendedState = jFrame.getExtendedState();
            jFrame.dispose();
            jFrame.setUndecorated(true);
            jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            jFrame.setVisible(true);
        } else {
            jFrame.dispose();
            jFrame.setUndecorated(false);
            jFrame.setBounds(this.windowBounds);
            jFrame.setExtendedState(this.windowExtendedState);
            jFrame.setVisible(true);
        }
    }

}
