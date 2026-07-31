package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.panel;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SettingsWindow extends JFrame {

    public SettingsWindow(MainWindow mainWindow, JFrame appFrame) {
        super("Settings");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTabbedPane jTabbedPane = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);

        jTabbedPane.add(new AudioSettingsPanel(mainWindow), "Audio");
        jTabbedPane.add(new VideoSettingsPanel(mainWindow), "Video");

        for (SystemDescriptor systemDescriptor : mainWindow.getSystemCatalog().getSystemDescriptors()) {
            systemDescriptor.getSettingsWindowContents().ifPresent(contentsSupplier -> jTabbedPane.add(contentsSupplier.apply(mainWindow), systemDescriptor.getName()));
        }

        this.add(jTabbedPane);

        appFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                SettingsWindow.this.dispose();
            }

        });
    }

}
