package io.github.arkosammy12.jemu.frontend.swing.menus;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.swing.events.ResetEvent;
import io.github.arkosammy12.jemu.frontend.swing.events.StopEvent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

public class EmulatorMenu extends MenuBarMenu {

    private final JMenuItem resetButton = new JMenuItem("Reset");
    private final JMenuItem stopButton = new JMenuItem("Stop");

    private final JMenu systemMenu = new JMenu("System");

    @Nullable
    private volatile SystemDescriptor currentSystemDescriptor;

    public EmulatorMenu(MainWindow mainWindow) {

        this.jMenu.setText("Emulator");
        this.jMenu.setMnemonic(KeyEvent.VK_E);

        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem unspecifiedItem = new JRadioButtonMenuItem("Unspecified");
        unspecifiedItem.addActionListener(_ -> currentSystemDescriptor = null);
        unspecifiedItem.setSelected(true);
        buttonGroup.add(unspecifiedItem);
        systemMenu.add(unspecifiedItem);

        for (SystemDescriptor systemDescriptor : mainWindow.getSystemDescriptors()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(systemDescriptor.getName());
            item.addActionListener(_ -> this.currentSystemDescriptor = systemDescriptor);
            buttonGroup.add(item);
            systemMenu.add(item);
        }

        this.resetButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
        this.resetButton.setEnabled(true);
        this.resetButton.addActionListener(_ -> {
            ResetEvent resetEvent = new ResetEvent(this.currentSystemDescriptor, false) {

                @Override
                public void onCompleted(Supplier<JPanel> panelSupplier) {
                    stopButton.setEnabled(true);
                    mainWindow.getSystemViewport().setSystemDisplayPanel(panelSupplier);
                    //stepFrameButton.setEnabled(false);
                    //stepCycleButton.setEnabled(false);
                }

            };

            mainWindow.offerEvent(resetEvent);

        });

        this.stopButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true));
        this.stopButton.setEnabled(false);
        this.stopButton.addActionListener(_ -> {
            this.stopButton.setEnabled(false);
            //this.pauseButton.setSelected(false);
            //this.stepFrameButton.setEnabled(false);
            //this.stepCycleButton.setEnabled(false);
            mainWindow.offerEvent(new StopEvent() {

                @Override
                public void onCompleted() {
                    mainWindow.getSystemViewport().setSystemDisplayPanel(null);
                }

            });
        });

        this.jMenu.add(resetButton);
        this.jMenu.add(stopButton);

        this.jMenu.addSeparator();

        this.jMenu.add(systemMenu);
    }

}
