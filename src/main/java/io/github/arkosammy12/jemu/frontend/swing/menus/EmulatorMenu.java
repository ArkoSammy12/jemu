package io.github.arkosammy12.jemu.frontend.swing.menus;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.swing.events.PauseEvent;
import io.github.arkosammy12.jemu.frontend.swing.events.ResetEvent;
import io.github.arkosammy12.jemu.frontend.swing.events.StopEvent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

public class EmulatorMenu extends MenuBarMenu {

    private final JMenuItem resetButton = new JMenuItem("Reset");
    private final JRadioButtonMenuItem pauseButton = new JRadioButtonMenuItem("Pause");
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
            boolean paused = this.pauseButton.isSelected();
            mainWindow.offerEvent(new ResetEvent(this.currentSystemDescriptor, paused) {

                @Override
                public void onCompleted(Supplier<JPanel> panelSupplier) {
                    stopButton.setEnabled(true);
                    mainWindow.getSystemViewport().setSystemDisplayPanel(panelSupplier);
                    //stepFrameButton.setEnabled(paused);
                    //stepCycleButton.setEnabled(paused);
                }

            });
        });

        this.pauseButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, true));
        this.pauseButton.setEnabled(true);
        this.pauseButton.setSelected(false);
        this.pauseButton.addActionListener(_ -> {
            boolean pause = this.pauseButton.isSelected();
            mainWindow.offerEvent(new PauseEvent(pause) {

                @Override
                public void onCompleted(boolean coreStopped) {
                    onPause(pause, coreStopped);
                }

            });
        });

        this.stopButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true));
        this.stopButton.setEnabled(false);
        this.stopButton.addActionListener(_ -> {
            mainWindow.offerEvent(new StopEvent() {

                @Override
                public void onCompleted() {
                    stopButton.setEnabled(false);
                    pauseButton.setSelected(false);
                    //stepFrameButton.setEnabled(false);
                    //stepCycleButton.setEnabled(false);
                    mainWindow.getSystemViewport().setSystemDisplayPanel(null);
                }

            });

        });

        this.jMenu.add(resetButton);
        this.jMenu.add(pauseButton);
        this.jMenu.add(stopButton);

        this.jMenu.addSeparator();

        this.jMenu.add(systemMenu);
    }

    private void onPause(boolean pause, boolean coreStopped) {
        if (pause) {
            if (coreStopped) {
                //this.stepFrameButton.setEnabled(false);
                //this.stepCycleButton.setEnabled(false);
            } else {
                stopButton.setEnabled(true);
                //this.stepFrameButton.setEnabled(true);
                //this.stepCycleButton.setEnabled(true);
            }

        } else {
            if (coreStopped) {
                stopButton.setEnabled(false);
                pauseButton.setSelected(false);
                //stepFrameButton.setEnabled(false);
                //stepCycleButton.setEnabled(false);
            } else {
                stopButton.setEnabled(true);
                //stepFrameButton.setEnabled(false);
                //stepCycleButton.setEnabled(false);
            }
        }
    }

}
