package io.github.arkosammy12.jemu.app.util;

import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class HelpDialog extends JPanel {

    public HelpDialog(JFrame applicationFrame) {
        this.setLayout(new MigLayout());

        Image appIcon = applicationFrame.getIconImages().getLast();
        if (appIcon != null) {
            this.add(new JLabel(new ImageIcon(appIcon)),
                 new CC().alignX(AlignX.CENTER).gapRight("5").cell(0, 0, 1, 2)
            );
        }

        this.add(new JLabel("<html><h1>%s</h1></html>".formatted(MavenProperties.ARTIFACT_ID)),
                new CC().alignX(AlignX.LEFT).cell(1, 0, 1, 1).gapBottom("0")
        );
        this.add(new JLabel("<html><p><strong>Version</strong>: %s<br><strong>Build Date</strong>: %s<br><strong>Commit ID</strong>: %s<br><strong>Runtime</strong>: %s<br><strong>Author</strong>: %s</p></html>".formatted(
                MavenProperties.VERSION,
                MavenProperties.BUILD_DATE,
                GitProperties.COMMIT_ID,
                "%s %s".formatted(System.getProperty("java.vm.name"), System.getProperty("java.version")),
                MavenProperties.AUTHOR
        )), new CC().growX().pushX().cell(1, 1, 1, 1));

    }

}
