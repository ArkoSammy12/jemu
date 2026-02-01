package io.github.arkosammy12.jemu.ui.menus;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.DataManager;
import io.github.arkosammy12.jemu.config.Serializable;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.main.MainWindow;
import io.github.arkosammy12.jemu.ui.util.EnumMenu;
import io.github.arkosammy12.jemu.ui.util.NumberOnlyTextField;
import io.github.arkosammy12.jemu.util.System;
import io.github.arkosammy12.jemu.util.DisplayAngle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class EmulatorMenu extends JMenu implements EmulatorInitializerConsumer {

    private final MainWindow mainWindow;

    private final JMenuItem resetButton = new JMenuItem("Reset");
    private final JRadioButtonMenuItem pauseButton = new JRadioButtonMenuItem("Pause");
    private final JMenuItem stopButton = new JMenuItem("Stop");
    private final JMenuItem stepFrameButton = new JMenuItem("Step Frame");
    private final JMenuItem stepCycleButton = new JMenuItem("Step Cycle");

    private final EnumMenu<System> systemMenu;
    private final EnumMenu<DisplayAngle> displayAngleMenu;

    public EmulatorMenu(Jemu jemu, MainWindow mainWindow) {
        super("Emulator");
        this.mainWindow = mainWindow;

        this.setMnemonic(KeyEvent.VK_E);

        this.resetButton.addActionListener(_ -> jemu.reset(this.pauseButton.isSelected()));
        this.resetButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
        this.resetButton.setEnabled(true);

        this.pauseButton.addActionListener(_ -> jemu.setPaused(this.pauseButton.isSelected()));
        this.pauseButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, true));
        this.pauseButton.setEnabled(true);
        this.pauseButton.setSelected(false);

        this.stopButton.addActionListener(_ -> {
            jemu.setPaused(false);
            jemu.stop();
        });
        this.stopButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true));
        this.stopButton.setEnabled(false);

        this.stepFrameButton.addActionListener(_ -> {
            if (!this.pauseButton.isSelected()) {
                return;
            }
            jemu.stepFrame();
        });
        this.stepFrameButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, true));
        this.stepFrameButton.setEnabled(false);

        this.stepCycleButton.addActionListener(_ -> {
            if (!this.pauseButton.isSelected()) {
                return;
            }
            jemu.stepCycle();
        });
        this.stepCycleButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, true));
        this.stepCycleButton.setEnabled(false);

        this.systemMenu = new EnumMenu<>("System", System.class, true);
        this.systemMenu.setMnemonic(KeyEvent.VK_V);

        this.displayAngleMenu = new EnumMenu<>("Display Angle", DisplayAngle.class, true);
        this.displayAngleMenu.setMnemonic(KeyEvent.VK_D);

        this.add(resetButton);
        this.add(pauseButton);
        this.add(stopButton);
        this.add(stepFrameButton);
        this.add(stepCycleButton);

        this.addSeparator();

        this.add(systemMenu);
        this.add(displayAngleMenu);

        this.mainWindow.setTitleSection(2, "Stopped");

        jemu.addStateChangedListener(((_, _, newState) -> {
            SwingUtilities.invokeLater(() -> {
                switch (newState) {
                    case RUNNING -> { // Loaded rom state
                        this.stopButton.setEnabled(true);
                        this.stepFrameButton.setEnabled(false);
                        this.stepCycleButton.setEnabled(false);
                        mainWindow.setTitleSection(2, "Running");
                    }
                    case PAUSED -> { // Loaded rom state
                        this.stopButton.setEnabled(true);
                        this.stepFrameButton.setEnabled(true);
                        this.stepCycleButton.setEnabled(true);
                        mainWindow.setTitleSection(2, "Paused");
                    }
                    case PAUSED_STOPPED -> { // No loaded rom state
                        this.stepFrameButton.setEnabled(false);
                        this.stepCycleButton.setEnabled(false);
                        mainWindow.setTitleSection(2, "Stopped (Paused)");
                    }
                    case STOPPED -> { // No loaded rom state
                        this.stopButton.setEnabled(false);
                        this.pauseButton.setSelected(false);
                        this.stepFrameButton.setEnabled(false);
                        this.stepCycleButton.setEnabled(false);
                        mainWindow.setTitleSection(2, "Stopped");
                    }
                }
            });
        }));

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent(DataManager.SYSTEM, Serializable.serialize(this.getSystem().orElse(null)));
            dataManager.putPersistent(DataManager.DISPLAY_ANGLE, Serializable.serialize(this.getDisplayAngle().orElse(null)));
        });
    }

    public Optional<System> getSystem() {
        return this.systemMenu.getState();
    }

    public Optional<DisplayAngle> getDisplayAngle() {
        return this.displayAngleMenu.getState();
    }

    public void onBreakpoint() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (!this.pauseButton.isSelected()) {
                        this.pauseButton.doClick();
                    }
                });
            } catch (Exception e) {
                this.mainWindow.showExceptionDialog(e);
            }
        }
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        initializer.getSystem().ifPresent(this.systemMenu::setState);
        initializer.getDisplayAngle().ifPresent(this.displayAngleMenu::setState);
    }

}
