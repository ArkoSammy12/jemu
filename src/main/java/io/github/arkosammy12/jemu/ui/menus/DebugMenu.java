package io.github.arkosammy12.jemu.ui.menus;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.DataManager;
import io.github.arkosammy12.jemu.config.initializers.ApplicationInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.main.MainWindow;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class DebugMenu extends JMenu implements EmulatorInitializerConsumer {

    private final JRadioButtonMenuItem showDebuggerButton;
    private final JRadioButtonMenuItem showDisassemblerButton;

    public DebugMenu(Jemu jemu, MainWindow mainWindow) {
        super("Debug");

        this.setMnemonic(KeyEvent.VK_D);

        this.showDebuggerButton = new JRadioButtonMenuItem("Show debugger");
        this.showDebuggerButton.addChangeListener(_ -> mainWindow.setDebuggerEnabled(this.showDebuggerButton.isSelected()));

        this.showDisassemblerButton = new JRadioButtonMenuItem("Show disassembler");
        this.showDisassemblerButton.addChangeListener(_ -> mainWindow.setDisassemblerEnabled(this.showDisassemblerButton.isSelected()));

        this.add(this.showDebuggerButton);
        this.add(this.showDisassemblerButton);

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent(DataManager.SHOW_DEBUGGER, String.valueOf(this.showDebuggerButton.isSelected()));
            dataManager.putPersistent(DataManager.SHOW_DISASSEMBLER, String.valueOf(this.showDisassemblerButton.isSelected()));

        });
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        if (initializer instanceof ApplicationInitializer applicationInitializer) {
            applicationInitializer.getShowingDebugger().ifPresent(this.showDebuggerButton::setSelected);
            applicationInitializer.getShowingDisassembler().ifPresent(this.showDisassemblerButton::setSelected);
        }
    }
}
