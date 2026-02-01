package io.github.arkosammy12.jemu.ui;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.DataManager;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.main.MainWindow;
import io.github.arkosammy12.jemu.ui.disassembly.DisassemblyPanel;
import io.github.arkosammy12.jemu.ui.util.ToggleableSplitPane;
import io.github.arkosammy12.jemu.ui.video.EmulatorViewport;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import static io.github.arkosammy12.jemu.config.DataManager.tryOptional;

public class LeftPanel extends JPanel implements EmulatorInitializerConsumer {

    private final ToggleableSplitPane splitPane;

    public LeftPanel(Jemu jemu, MainWindow mainWindow) {
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
