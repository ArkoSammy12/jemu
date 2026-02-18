package io.github.arkosammy12.jemu.frontend.ui.menus;

import com.formdev.flatlaf.util.SystemInfo;
import io.github.arkosammy12.jemu.application.Main;
import io.github.arkosammy12.jemu.frontend.ui.MainWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.URI;

public class HelpMenu extends JMenu {

    public HelpMenu(MainWindow mainWindow) {
        super("Help");

        this.setMnemonic(KeyEvent.VK_H);

        Runnable showAboutDialog = () -> JOptionPane.showMessageDialog(
                mainWindow,
                String.format("Jemu\nVersion %s\n\nBy ArkoSammy12", Main.VERSION_STRING),
                "About Jemu",
                JOptionPane.INFORMATION_MESSAGE);

        Runnable addAboutItem = () -> {
            JMenuItem aboutItem = new JMenuItem("About");
            aboutItem.setMnemonic(KeyEvent.VK_A);
            aboutItem.addActionListener(_ -> showAboutDialog.run());
            this.add(aboutItem);
        };

        if (SystemInfo.isMacOS) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(_ -> showAboutDialog.run());
            } else {
                addAboutItem.run();
            }
        } else {
            addAboutItem.run();
        }

        JMenuItem sourceItem = new JMenuItem("Source");
        sourceItem.setMnemonic(KeyEvent.VK_S);
        sourceItem.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/ArkoSammy12/jemu"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainWindow, "Unable to open source link.");
            }
        });

        JMenuItem reportItem = new JMenuItem("Report a Bug");
        reportItem.setMnemonic(KeyEvent.VK_R);
        reportItem.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/ArkoSammy12/jchip/issues"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainWindow, "Unable to open issues link.");
            }
        });

        this.add(sourceItem);
        this.add(reportItem);

    }

}
