package io.github.arkosammy12.jemu.frontend.gui.swing.menus;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class EmulatorMenu extends MenuBarMenu {

    private final JRadioButtonMenuItem pauseButton = new JRadioButtonMenuItem("Pause");
    private final JMenuItem stopButton = new JMenuItem("Stop");
    private final JMenuItem stepFrameButton = new JMenuItem("Step Frame");
    private final JMenuItem stepCycleButton = new JMenuItem("Step Cycle");

    @Nullable
    private volatile SystemDescriptor currentSystemDescriptor;
    private volatile boolean emulatorStopped = true;

    public EmulatorMenu(MainWindow mainWindow) {

        this.jMenu.setText("Emulator");
        this.jMenu.setMnemonic(KeyEvent.VK_E);

        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem unspecifiedItem = new JRadioButtonMenuItem("Unspecified");
        unspecifiedItem.addActionListener(_ -> currentSystemDescriptor = null);
        unspecifiedItem.setSelected(true);
        buttonGroup.add(unspecifiedItem);
        JMenu systemMenu = new JMenu("System");
        systemMenu.add(unspecifiedItem);

        Map<SystemDescriptor, JRadioButtonMenuItem> buttonMap = new HashMap<>();

        for (SystemDescriptor systemDescriptor : mainWindow.getSystemDescriptors()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(systemDescriptor.getName());
            item.addChangeListener(_ -> this.currentSystemDescriptor = systemDescriptor);
            buttonGroup.add(item);
            systemMenu.add(item);
            buttonMap.put(systemDescriptor, item);
        }

        JMenuItem resetButton = new JMenuItem("Reset");
        resetButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
        resetButton.setEnabled(true);
        resetButton.addActionListener(_ -> mainWindow.submitEmulatorCommand(new ResetEmulatorCommand(this.currentSystemDescriptor, this.pauseButton.isSelected())));

        this.pauseButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, true));
        this.pauseButton.setEnabled(true);
        this.pauseButton.setSelected(false);
        this.pauseButton.addActionListener(_ -> mainWindow.submitEmulatorCommand(new PauseEmulatorCommand(this.pauseButton.isSelected())));

        this.stopButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true));
        this.stopButton.setEnabled(false);
        this.stopButton.addActionListener(_ -> mainWindow.submitEmulatorCommand(new StopEmulatorCommand()));

        this.stepFrameButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, true));
        this.stepFrameButton.setEnabled(false);
        this.stepFrameButton.addActionListener(_ -> {
            if (!this.pauseButton.isSelected()) {
                return;
            }
            mainWindow.submitEmulatorCommand(new StepFrameEmulatorCommand());
        });

        this.stepCycleButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, true));
        this.stepCycleButton.setEnabled(false);
        this.stepCycleButton.addActionListener(_ -> {
            if (!this.pauseButton.isSelected()) {
                return;
            }
            mainWindow.submitEmulatorCommand(new StepCycleEmulatorCommand());
        });

        this.jMenu.add(resetButton);
        this.jMenu.add(pauseButton);
        this.jMenu.add(stopButton);
        this.jMenu.add(stepFrameButton);
        this.jMenu.add(stepCycleButton);

        this.jMenu.addSeparator();

        this.jMenu.add(systemMenu);

        mainWindow.registerSettingProperty("settings.selected_system", () -> this.currentSystemDescriptor == null ? "" : this.currentSystemDescriptor.getId(), s -> {
            for (Map.Entry<SystemDescriptor, JRadioButtonMenuItem> button : buttonMap.entrySet()) {
                if (button.getKey().getId().equals(s)) {
                    button.getValue().setSelected(true);
                    break;
                }
            }
        });

        mainWindow.<PauseEmulatorCommand.Callback>addEmulatorCommandCallback(pauseCommand -> {
            SwingUtilities.invokeLater(() -> {
                if (pauseCommand.pause()) {
                    if (emulatorStopped) {
                        this.stepFrameButton.setEnabled(false);
                        this.stepCycleButton.setEnabled(false);
                    } else {
                        stopButton.setEnabled(true);
                        this.stepFrameButton.setEnabled(true);
                        this.stepCycleButton.setEnabled(true);
                    }

                } else {
                    if (emulatorStopped) {
                        stopButton.setEnabled(false);
                        pauseButton.setSelected(false);
                        stepFrameButton.setEnabled(false);
                        stepCycleButton.setEnabled(false);
                    } else {
                        stopButton.setEnabled(true);
                        stepFrameButton.setEnabled(false);
                        stepCycleButton.setEnabled(false);
                    }
                }
            });
        });

        mainWindow.<ResetEmulatorCommand.Callback>addEmulatorCommandCallback(resetCommand -> {
            SwingUtilities.invokeLater(() -> {
                boolean paused = this.pauseButton.isSelected();
                stopButton.setEnabled(true);
                stepFrameButton.setEnabled(paused);
                stepCycleButton.setEnabled(paused);
                emulatorStopped = false;
            });
        });

        mainWindow.<StopEmulatorCommand.Callback>addEmulatorCommandCallback(stopCommand -> {
            SwingUtilities.invokeLater(() -> {
                stopButton.setEnabled(false);
                pauseButton.setSelected(false);
                stepFrameButton.setEnabled(false);
                stepCycleButton.setEnabled(false);
                mainWindow.getSystemViewport().setSystemDisplayPanel(null);
                emulatorStopped = true;
            });
        });
    }

}
