package io.github.arkosammy12.jemu.frontend.oldui;

import io.github.arkosammy12.jemu.application.OldJemu;
import io.github.arkosammy12.jemu.application.io.DataManager;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.frontend.oldui.disassembly.DisassemblyPanel;
import io.github.arkosammy12.jemu.frontend.oldui.util.ToggleableSplitPane;
import io.github.arkosammy12.jemu.frontend.oldui.video.EmulatorViewport;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import static io.github.arkosammy12.jemu.application.io.DataManager.tryOptional;

public class LeftPanel extends JPanel implements EmulatorInitializerConsumer {

    private final ToggleableSplitPane splitPane;

    public LeftPanel(OldJemu jemu, MainWindow mainWindow) {
        MigLayout migLayout = new MigLayout(new LC().insets("0"));
        super(migLayout);
        EmulatorViewport emulatorViewport = new EmulatorViewport(jemu);
        DisassemblyPanel disassemblyPanel = new DisassemblyPanel(jemu, mainWindow);
        this.splitPane = new ToggleableSplitPane(JSplitPane.VERTICAL_SPLIT, emulatorViewport, disassemblyPanel, 5, 0.75);

        this.add(this.splitPane, new CC().grow().push());

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent("ui.viewport_disassembler_divider_location", String.valueOf(this.splitPane.getAbsoluteDividerLocation()));
        });
    }

    public void setDisassemblerEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            this.splitPane.toggleShowSplit(enabled);
            this.revalidate();
            this.repaint();
        });
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        if (initializer instanceof DataManager dataManager) {
            dataManager.getPersistent("ui.viewport_disassembler_divider_location").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(this.splitPane::setAbsoluteDividerLocation);
        }
    }

}
