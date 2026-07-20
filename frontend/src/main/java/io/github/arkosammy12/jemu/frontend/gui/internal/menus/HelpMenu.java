package io.github.arkosammy12.jemu.frontend.gui.internal.menus;

import com.formdev.flatlaf.util.SystemInfo;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.managers.HelpManager;
import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;

public class HelpMenu extends MenuBarMenu implements HelpManager {

    @Nullable
    private volatile Function<? super JFrame, ? extends @Nullable JPanel> helpDialogContentsSupplier;

    @NotNull
    private String projectSourceLink = "unknown";

    @NotNull
    private String projectBugReportLink = "unknown";

    public HelpMenu(MainWindow mainWindow, JFrame appFrame) {

        this.getJMenu().setText("Help");
        this.getJMenu().setMnemonic(KeyEvent.VK_H);

        Runnable showAboutDialog = () -> {
            JDialog jDialog = new JDialog(appFrame, "About - %s".formatted(mainWindow.getTitle()), true);
            jDialog.setLayout(new MigLayout());

            Image appFrameIconImage = appFrame.getIconImage();
            if (appFrameIconImage != null) {
                jDialog.setIconImage(appFrameIconImage);
            }

            jDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            jDialog.setResizable(false);

            JButton okButton = new JButton("Ok");
            okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            okButton.addActionListener(_ -> jDialog.dispose());

            JPanel contents = null;
            Function<? super JFrame, ? extends @Nullable JPanel> contentsSupplier = this.helpDialogContentsSupplier;
            if (contentsSupplier != null) {
                contents = contentsSupplier.apply(appFrame);
            }
            if (contents != null) {
                jDialog.add(contents, new CC().grow().push().wrap());
            }

            jDialog.add(okButton, new CC().alignX(AlignX.CENTER));

            jDialog.pack();
            jDialog.setLocationRelativeTo(appFrame);
            jDialog.setVisible(true);
        };

        JMenuItem sourceItem = new JMenuItem("Source");
        sourceItem.setMnemonic(KeyEvent.VK_S);
        sourceItem.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(new URI(this.projectSourceLink));
            } catch (Exception ex) {
                mainWindow.showDialog("Unable to open source link", ex.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        JMenuItem reportItem = new JMenuItem("Report a Bug");
        reportItem.setMnemonic(KeyEvent.VK_R);
        reportItem.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(new URI(this.projectBugReportLink));
            } catch (Exception ex) {
                mainWindow.showDialog("Unable to open bug report link", ex.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        this.getJMenu().add(sourceItem);
        this.getJMenu().add(reportItem);

        Runnable addAboutItem = () -> {
            JMenuItem aboutItem = new JMenuItem("About");
            aboutItem.setMnemonic(KeyEvent.VK_A);
            aboutItem.addActionListener(_ -> showAboutDialog.run());
            this.getJMenu().addSeparator();
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

    }

    @Override
    public void setHelpDialogContentsSupplier(@Nullable Function<? super JFrame, ? extends @Nullable JPanel> helpDialogContentsSupplier) {
        this.helpDialogContentsSupplier = helpDialogContentsSupplier;
    }

    @Override
    public void setProjectSourceLink(@NotNull String projectSourceLink) {
        this.projectSourceLink = projectSourceLink;
    }

    @Override
    public void setProjectBugReportLink(@NotNull String projectBugReportLink) {
        this.projectBugReportLink = projectBugReportLink;
    }

}
