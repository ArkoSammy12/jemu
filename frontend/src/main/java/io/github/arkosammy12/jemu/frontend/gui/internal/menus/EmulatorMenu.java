package io.github.arkosammy12.jemu.frontend.gui.internal.menus;

import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.InternalFileLoadedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.InternalROMEjectedEvent;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.PauseCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.PowerCycleCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.StopCommandCallback;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.swing.commands.*;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.EmulatorManager;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EmulatorMenu extends MenuBarMenu implements EmulatorManager {

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
        this.automaticItem.addActionListener(_ -> this.setSystemDescriptor(null));
        this.automaticItem.setSelected(true);
        buttonGroup.add(this.automaticItem);
        systemMenu.add(this.automaticItem);

        this.systemDescriptorButtonMap = new HashMap<>();

        for (SystemDescriptor systemDescriptor : mainWindow.getSystemDescriptors()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(systemDescriptor.getName());
            item.addActionListener(_ -> this.setSystemDescriptor(systemDescriptor));
            buttonGroup.add(item);
            systemMenu.add(item);
            this.systemDescriptorButtonMap.put(systemDescriptor, item);
        }

        JMenuItem powerCycleButton = new JMenuItem("Power Cycle");
        powerCycleButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK, true));
        powerCycleButton.setEnabled(true);
        powerCycleButton.addActionListener(_ -> this.submitPowerCycle());

        JMenuItem resetButton = new JMenuItem("Reset");
        resetButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
        resetButton.setEnabled(false);
        resetButton.addActionListener(_ -> this.submitReset());

        this.pauseButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, true));
        this.pauseButton.setEnabled(true);
        this.pauseButton.setSelected(false);
        this.pauseButton.addActionListener(_ -> mainWindow.submitEmulatorCommand(new PauseEmulatorCommand(this.pauseButton.isSelected())));

        this.stopButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true));
        this.stopButton.setEnabled(false);
        this.stopButton.addActionListener(_ -> this.submitStop());

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

        this.jMenu.add(powerCycleButton);
        this.jMenu.add(resetButton);
        this.jMenu.add(pauseButton);
        this.jMenu.add(stopButton);
        this.jMenu.add(stepFrameButton);
        this.jMenu.add(stepCycleButton);

        this.jMenu.addSeparator();

        this.jMenu.add(systemMenu);

        mainWindow.getConfig().getInternalPreferenceSettings().getInternalEmulatorSettings().getSelectedSystemId().ifPresent(systemId -> {
            for (Map.Entry<SystemDescriptor, JRadioButtonMenuItem> button : this.systemDescriptorButtonMap.entrySet()) {
                if (button.getKey().getId().equals(systemId)) {
                    button.getValue().doClick();
                    break;
                }
            }
        });

        mainWindow.<PowerCycleCommandCallback>onEmulatorCommand(_ -> SwingUtilities.invokeLater(() -> {
            boolean paused = this.pauseButton.isSelected();
            resetButton.setEnabled(true);
            stopButton.setEnabled(true);
            stepFrameButton.setEnabled(paused);
            stepCycleButton.setEnabled(paused);
            emulatorStopped = false;
        }));

        mainWindow.<PauseCommandCallback>onEmulatorCommand(pauseCommand -> SwingUtilities.invokeLater(() -> {
            if (pauseCommand.pause()) {
                this.pauseButton.setSelected(true);
                if (emulatorStopped) {
                    this.stepFrameButton.setEnabled(false);
                    this.stepCycleButton.setEnabled(false);
                } else {
                    this.stopButton.setEnabled(true);
                    this.stepFrameButton.setEnabled(true);
                    this.stepCycleButton.setEnabled(true);
                }
            } else {
                this.pauseButton.setSelected(false);
                this.stopButton.setEnabled(!emulatorStopped);
                this.stepFrameButton.setEnabled(false);
                this.stepCycleButton.setEnabled(false);
            }
        }));

        mainWindow.<StopCommandCallback>onEmulatorCommand(_ -> SwingUtilities.invokeLater(() -> {
            resetButton.setEnabled(false);
            this.stopButton.setEnabled(false);
            this.pauseButton.setSelected(false);
            this.stepFrameButton.setEnabled(false);
            this.stepCycleButton.setEnabled(false);
            this.mainWindow.getSystemViewport().setSystemDisplay(null);
            mainWindow.getSystemViewport().setSystemKeyListener(null);
            emulatorStopped = true;
        }));

        mainWindow.onEvent(InternalFileLoadedEvent.class, _ -> {
            if (this.mainWindow.getConfig().getInternalPreferenceSettings().getInternalFileSettings().getResetOnROMFileSelect()) {
                this.restartEmulator();
            }
        });

        mainWindow.onEvent(InternalROMEjectedEvent.class, _ -> {
            this.submitStop();
        });

    }

    @Override
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

    private void setSystemDescriptor(@Nullable SystemDescriptor systemDescriptor) {
        this.currentSystemDescriptor = systemDescriptor;
        mainWindow.getConfig().getInternalPreferenceSettings().getInternalEmulatorSettings().setSelectedSystemId(systemDescriptor == null ? null : systemDescriptor.getId());
    }

    private void submitPowerCycle() {
        SystemDescriptorResult systemDescriptorResult = this.getCurrentSystemDescriptor();
        Optional<SystemDescriptor> systemDescriptor = systemDescriptorResult.getSystemDescriptor();
        if (systemDescriptor.isPresent()) {
            this.mainWindow.submitEmulatorCommand(new PowerCycleCommand(systemDescriptor.get(), this.pauseButton.isSelected()));
        } else {
            this.mainWindow.showDialog("Error attempting to power cycle", systemDescriptorResult.errorMessage(), MainWindow.DialogType.ERROR);
        }
    }

    private void restartEmulator() {
        if (this.emulatorStopped) {
            this.submitPowerCycle();
        } else {
            this.submitReset();
        }
    }

    private void submitReset() {
        mainWindow.submitEmulatorCommand(new ResetEmulatorCommand(this.getCurrentSystemDescriptor().systemDescriptor(), this.pauseButton.isSelected()));
    }

    private void submitStop() {
        this.mainWindow.submitEmulatorCommand(new StopEmulatorCommand());
    }

    private SystemDescriptorResult getCurrentSystemDescriptor() {
        SystemDescriptor systemDescriptor = this.currentSystemDescriptor;
        if (systemDescriptor != null) {
            return new SystemDescriptorResult(systemDescriptor, null);
        }
        Optional<Path> optionalRomPath = this.mainWindow.getMainMenuBar().getFileMenu().getSelectedRomPath();
        if (optionalRomPath.isEmpty()) {
            return new SystemDescriptorResult(null, "No selected ROM path to determine system from!");
        }
        String fileExtension = FilenameUtils.getExtension(optionalRomPath.get().toString());
        if (fileExtension.isBlank()) {
            return new SystemDescriptorResult(null, "The file extension of the selected ROM path is blank!");
        }
        outer: for (SystemDescriptor descriptor : this.mainWindow.getSystemDescriptors()) {
            for (String extension : descriptor.getFileExtensions()) {
                if (fileExtension.equals(extension)) {
                    systemDescriptor = descriptor;
                    break outer;
                }
            }
        }

        if (systemDescriptor == null) {
            return new SystemDescriptorResult(null, "File extension of selected ROM path does not match of system descriptors!");
        }

        return new SystemDescriptorResult(systemDescriptor, null);
    }

    private record SystemDescriptorResult(@Nullable SystemDescriptor systemDescriptor, @Nullable String errorMessage) {

        private Optional<SystemDescriptor> getSystemDescriptor() {
            return Optional.ofNullable(this.systemDescriptor);
        }

        @Override
        public String errorMessage() {
            return Optional.ofNullable(this.errorMessage).orElse("No error message provided!");
        }

    }

}
