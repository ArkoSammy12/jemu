package io.github.arkosammy12.jemu.frontend.gui.swing.menus;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.gui.internal.SerializedEntry;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.PauseCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.ResetCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.StopCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.swing.commands.*;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EmulatorMenu extends MenuBarMenu {

    private final MainWindow mainWindow;

    private final JRadioButtonMenuItem pauseButton = new JRadioButtonMenuItem("Pause");
    private final JMenuItem stopButton = new JMenuItem("Stop");
    private final JMenuItem stepFrameButton = new JMenuItem("Step Frame");
    private final JMenuItem stepCycleButton = new JMenuItem("Step Cycle");

    private final JRadioButtonMenuItem automaticItem;
    private final Map<SystemDescriptor, JRadioButtonMenuItem> systemDescriptorButtonMap;

    @Nullable
    private volatile SystemDescriptor currentSystemDescriptor;
    private volatile boolean emulatorStopped = true;

    public EmulatorMenu(MainWindow mainWindow) {

        this.mainWindow = mainWindow;

        this.jMenu.setText("Emulator");
        this.jMenu.setMnemonic(KeyEvent.VK_E);

        JMenu systemMenu = new JMenu("System");

        ButtonGroup buttonGroup = new ButtonGroup();
        this.automaticItem = new JRadioButtonMenuItem("Automatic");
        this.automaticItem.addChangeListener(_ -> currentSystemDescriptor = null);
        this.automaticItem.setSelected(true);
        buttonGroup.add(this.automaticItem);
        systemMenu.add(this.automaticItem);

        this.systemDescriptorButtonMap = new HashMap<>();

        for (SystemDescriptor systemDescriptor : mainWindow.getSystemDescriptors()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(systemDescriptor.getName());
            item.addChangeListener(_ -> this.currentSystemDescriptor = systemDescriptor);
            buttonGroup.add(item);
            systemMenu.add(item);
            this.systemDescriptorButtonMap.put(systemDescriptor, item);
        }

        JMenuItem resetButton = new JMenuItem("Reset");
        resetButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
        resetButton.setEnabled(true);
        resetButton.addActionListener(_ -> this.submitReset());

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

        mainWindow.registerSettingProperty(new SerializedEntry("settings.selected_system", () -> this.currentSystemDescriptor == null ? "" : this.currentSystemDescriptor.getId(), s -> {
            for (Map.Entry<SystemDescriptor, JRadioButtonMenuItem> button : this.systemDescriptorButtonMap.entrySet()) {
                if (button.getKey().getId().equals(s)) {
                    button.getValue().doClick();
                    break;
                }
            }
        }));

        mainWindow.<PauseCommandCallback>addEmulatorCommandCallback(pauseCommand -> SwingUtilities.invokeLater(() -> {
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
        }));

        mainWindow.<ResetCommandCallback>addEmulatorCommandCallback(_ -> SwingUtilities.invokeLater(() -> {
            boolean paused = this.pauseButton.isSelected();
            stopButton.setEnabled(true);
            stepFrameButton.setEnabled(paused);
            stepCycleButton.setEnabled(paused);
            emulatorStopped = false;
        }));

        mainWindow.<StopCommandCallback>addEmulatorCommandCallback(_ -> SwingUtilities.invokeLater(() -> {
            stopButton.setEnabled(false);
            pauseButton.setSelected(false);
            stepFrameButton.setEnabled(false);
            stepCycleButton.setEnabled(false);
            mainWindow.getSystemViewport().setSystemDisplayPanel(null);
            emulatorStopped = true;
        }));
    }

    public void setCurrentSystemDescriptor(@Nullable SystemDescriptor systemDescriptor) {
        SwingUtilities.invokeLater(() -> {
            if (systemDescriptor == null) {
                this.automaticItem.doClick();
                return;
            }
            for (Map.Entry<SystemDescriptor, JRadioButtonMenuItem> button : this.systemDescriptorButtonMap.entrySet()) {
                if (button.getKey().equals(systemDescriptor)) {
                    button.getValue().doClick();
                    break;
                }
            }
        });
    }

    void submitReset() {
        SystemDescriptor systemDescriptor = this.currentSystemDescriptor;
        if (systemDescriptor != null) {
            this.mainWindow.submitEmulatorCommand(new ResetEmulatorCommand(systemDescriptor, this.pauseButton.isSelected()));
            return;
        }

        Optional<Path> optionalRomPath = this.mainWindow.getMainMenuBar().getFileMenu().getSelectedRomPath();
        if (optionalRomPath.isEmpty()) {
            this.mainWindow.showDialog("Error attempting to restart", "No selected ROM path to determine system from!", MainWindow.DialogType.ERROR);
            return;
        }
        String fileExtension = FilenameUtils.getExtension(optionalRomPath.get().toString());
        if (fileExtension.isBlank()) {
            this.mainWindow.showDialog("Error attempting to restart", "The file extension of the selected ROM path is blank!", MainWindow.DialogType.ERROR);
            return;
        }

        outer: for (SystemDescriptor descriptor : this.mainWindow.getSystemDescriptors()) {
            Optional<String[]> optionalFileExtensions = descriptor.getFileExtensions();
            if (optionalFileExtensions.isEmpty()) {
                break;
            }
            String[] fileExtensions = optionalFileExtensions.get();
            for (String extension : fileExtensions) {
                if (fileExtension.equals(extension)) {
                    systemDescriptor = descriptor;
                    break outer;
                }
            }
        }

        if (systemDescriptor == null) {
            this.mainWindow.showDialog("Error attempting to restart", "File extension of selected ROM path does not match of system descriptors!", MainWindow.DialogType.ERROR);
            return;
        }

        this.mainWindow.submitEmulatorCommand(new ResetEmulatorCommand(systemDescriptor, this.pauseButton.isSelected()));
    }

}
