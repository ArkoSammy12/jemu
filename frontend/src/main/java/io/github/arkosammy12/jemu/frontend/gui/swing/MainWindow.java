package io.github.arkosammy12.jemu.frontend.gui.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.util.SystemInfo;
import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.*;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.QuitStrategy;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

public class MainWindow implements Closeable {

    @Nullable
    private JFrame appFrame;

    @Nullable
    private MainMenuBar menuBar;

    @Nullable
    private SystemViewport systemViewport;

    @Nullable
    private StatusBar statusBar;

    private final CC infoBarConstraints = new CC().grow().pushX().dockSouth().height("18!");

    private final BlockingQueue<EmulatorCommand> emulatorCommandQueue = new LinkedBlockingDeque<>();
    private final Collection<PauseEmulatorCommand.Callback> pauseCallbacks = new CopyOnWriteArrayList<>();
    private final Collection<ResetEmulatorCommand.Callback> resetCallbacks = new CopyOnWriteArrayList<>();
    private final Collection<StepCycleEmulatorCommand.Callback> stepCycleCallbacks = new CopyOnWriteArrayList<>();
    private final Collection<StepFrameEmulatorCommand.Callback> stepFrameCallbacks = new CopyOnWriteArrayList<>();
    private final Collection<StopEmulatorCommand.Callback> stopCallbacks = new CopyOnWriteArrayList<>();

    private final Collection<SystemDescriptor> systemDescriptors;

    public MainWindow(String title, Collection<? extends SystemDescriptor> systemDescriptors) throws InterruptedException, InvocationTargetException {

        this.systemDescriptors = List.copyOf(systemDescriptors);

        if (SystemInfo.isMacOS) {
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.application.name", "jemu");

            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
                desktop.setQuitHandler((_, response) -> response.performQuit());
            }
        }

        System.setProperty("sun.awt.noerasebackground", Boolean.TRUE.toString());
        System.setProperty("flatlaf.uiScale.allowScaleDown", Boolean.TRUE.toString());
        System.setProperty("flatlaf.menuBarEmbedded", Boolean.FALSE.toString());

        SwingUtilities.invokeAndWait(() -> {
            FlatDarkLaf.setup();

            UIManager.put("TitlePane.useWindowDecorations", false);
            UIManager.put("Component.hideMnemonics", false);
            UIManager.put("FileChooser.readOnly", true);
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);

            ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
            toolTipManager.setLightWeightPopupEnabled(false);
            toolTipManager.setInitialDelay(700);
            toolTipManager.setReshowDelay(700);
            toolTipManager.setDismissDelay(4000);

            JFrame.setDefaultLookAndFeelDecorated(false);
            JDialog.setDefaultLookAndFeelDecorated(false);
            Toolkit.getDefaultToolkit().setDynamicLayout(true);

            this.appFrame = new JFrame(title);
            MigLayout appFrameLayout = new MigLayout(new LC().insets("0"), new AC(), new AC().gap("0"));
            this.appFrame.setLayout(appFrameLayout);
            appFrame.setBackground(Color.BLACK);
            this.appFrame.getRootPane().putClientProperty("apple.awt.fullscreenable", true);

            this.systemViewport = new SystemViewport();
            this.menuBar = new MainMenuBar(this, this.appFrame);
            this.statusBar = new StatusBar(this);

            this.appFrame.setJMenuBar(this.menuBar.getJMenuBar());
            this.appFrame.add(this.systemViewport.getJPanel(), new CC().grow().push().wrap());
            this.appFrame.add(this.statusBar.getJPanel(), this.infoBarConstraints);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            appFrame.requestFocusInWindow();
            appFrame.setResizable(true);
            appFrame.setPreferredSize(new Dimension((int) (screenSize.getWidth() / 1.5), (int) (screenSize.getHeight() / 1.5)));
            appFrame.pack();
            appFrame.setLocationRelativeTo(null);

        });

    }

    public Collection<SystemDescriptor> getSystemDescriptors() {
        return this.systemDescriptors;
    }

    public SystemViewport getSystemViewport() {
        return Objects.requireNonNull(this.systemViewport);
    }

    public MainMenuBar getMainMenuBar() {
        return Objects.requireNonNull(this.menuBar);
    }

    public StatusBar getStatusBar() {
        return Objects.requireNonNull(this.statusBar);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> this.getJFrame().setVisible(true));
    }

    public void setClosingHook(Runnable runnable) {
        this.getJFrame().addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                runnable.run();
            }

        });
    }

    public void setStatusBarEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            this.getStatusBar().getJPanel().setVisible(enabled);
            this.infoBarConstraints.setHideMode(enabled ? 0 : 3);
            this.getJFrame().revalidate();
            this.getJFrame().repaint();
        });
    }

    public void showCoreError(Throwable e) {
        this.showDialog("Emulation error: %s".formatted(e.getClass().getSimpleName()), e.getMessage(), DialogType.ERROR);
    }

    public void showDialog(String title, String message, DialogType dialogType) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this.getJFrame(), message, title, dialogType.getjOptionPaneMessageTypeId()));
    }

    public void submitEmulatorCommand(EmulatorCommand emulatorCommand) {
        this.emulatorCommandQueue.offer(emulatorCommand);
    }

    @Nullable
    public EmulatorCommand pollEmulatorCommand() throws InterruptedException {
        return this.getEmulatorCommand(false);
    }

    public EmulatorCommand waitEmulatorCommand() throws InterruptedException {
        return this.getEmulatorCommand(true);
    }

    @Nullable
    private EmulatorCommand getEmulatorCommand(boolean wait) throws InterruptedException {
        EmulatorCommand emulatorCommand = wait ? this.emulatorCommandQueue.take() : this.emulatorCommandQueue.poll();
        switch (emulatorCommand) {
            case null -> {}
            case PauseEmulatorCommand command -> this.pauseCallbacks.forEach(c -> c.onPause(command));
            case ResetEmulatorCommand command -> this.resetCallbacks.forEach(c -> c.onReset(command));
            case StepCycleEmulatorCommand command -> this.stepCycleCallbacks.forEach(c -> c.onStepCycle(command));
            case StepFrameEmulatorCommand command -> this.stepFrameCallbacks.forEach(c -> c.onStepFrame(command));
            case StopEmulatorCommand command -> this.stopCallbacks.forEach(c -> c.onStop(command));
        }
        return emulatorCommand;
    }

    @NotNull
    @ApiStatus.Internal
    JFrame getJFrame() {
        return Objects.requireNonNull(this.appFrame);
    }

    @ApiStatus.Internal
    public <T extends EmulatorCommand.Callback> void addEmulatorCommandCallback(T callback) {
        switch (callback) {
            case PauseEmulatorCommand.Callback c -> this.pauseCallbacks.add(c);
            case ResetEmulatorCommand.Callback c -> this.resetCallbacks.add(c);
            case StepCycleEmulatorCommand.Callback c -> this.stepCycleCallbacks.add(c);
            case StepFrameEmulatorCommand.Callback c -> this.stepFrameCallbacks.add(c);
            case StopEmulatorCommand.Callback c -> this.stopCallbacks.add(c);
        }
    }

    @Override
    public void close() throws IOException {
        SwingUtilities.invokeLater(() -> {
            if (this.appFrame != null) {
                this.appFrame.dispose();
            }
        });
    }

    public enum DialogType {
        INFORMATION(JOptionPane.INFORMATION_MESSAGE),
        WARNING(JOptionPane.WARNING_MESSAGE),
        ERROR(JOptionPane.ERROR_MESSAGE);

        private final int jOptionPaneMessageTypeId;

        DialogType(int jOptionPaneMessageTypeId) {
            this.jOptionPaneMessageTypeId = jOptionPaneMessageTypeId;
        }

        private int getjOptionPaneMessageTypeId() {
            return this.jOptionPaneMessageTypeId;
        }

    }

}
