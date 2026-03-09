package io.github.arkosammy12.jemu.frontend.gui.swing.menus;

import com.formdev.flatlaf.util.SystemInfo;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.URI;

public class HelpMenu extends MenuBarMenu {

    @Nullable
    private String projectName;

    @Nullable
    private String authorName;

    @Nullable
    private String versionString;

    @Nullable
    private String projectSourceLink;

    @Nullable
    private String projectBugReportLink;

    public HelpMenu(MainWindow mainWindow) {

        this.getJMenu().setText("Help");
        this.getJMenu().setMnemonic(KeyEvent.VK_H);

        Runnable showAboutDialog = () -> {

            String name = this.projectName == null ? "{project}" : this.projectName;
            String author = this.authorName == null ? "{author}" : this.authorName;
            String version = this.versionString == null ? "{version}" : this.versionString;

            mainWindow.showDialog("About %s".formatted(this.projectName), "%s\nVersion %s\n\nBy %s".formatted(name, version, author), MainWindow.DialogType.INFORMATION);

        };

        Runnable addAboutItem = () -> {
            JMenuItem aboutItem = new JMenuItem("About");
            aboutItem.setMnemonic(KeyEvent.VK_A);
            aboutItem.addActionListener(_ -> showAboutDialog.run());
            this.getJMenu().add(aboutItem);
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
                if (this.projectSourceLink == null) {
                    throw new IllegalStateException("No project source link specified!");
                }
                Desktop.getDesktop().browse(new URI(this.projectSourceLink));
            } catch (Exception ex) {
                mainWindow.showDialog("Unable to open source link", ex.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        JMenuItem reportItem = new JMenuItem("Report a Bug");
        reportItem.setMnemonic(KeyEvent.VK_R);
        reportItem.addActionListener(_ -> {
            try {
                if (this.projectBugReportLink == null) {
                    throw new IllegalStateException("No project bug report link specified!");
                }
                Desktop.getDesktop().browse(new URI(this.projectBugReportLink));
            } catch (Exception ex) {
                mainWindow.showDialog("Unable to open bug report link", ex.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        this.getJMenu().add(sourceItem);
        this.getJMenu().add(reportItem);

    }

    public void setAuthorName(@Nullable String authorName) {
        this.authorName = authorName;
    }

    public void setProjectName(@Nullable String projectName) {
        this.projectName = projectName;
    }

    public void setVersionString(@Nullable String versionString) {
        this.versionString = versionString;
    }

    public void setProjectSourceLink(@Nullable String projectSourceLink) {
        this.projectSourceLink = projectSourceLink;
    }

    public void setProjectBugReportLink(@Nullable String projectBugReportLink) {
        this.projectBugReportLink = projectBugReportLink;
    }

}
