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
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.QuitStrategy;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private Rectangle unmaximizedBounds;
    private final Path dataDirectory;
    private final Collection<PropertyEntry> stateProperties = new CopyOnWriteArrayList<>();
    private final Collection<PropertyEntry> settingProperties = new CopyOnWriteArrayList<>();

    public MainWindow(String title, Path dataDirectory, Collection<? extends SystemDescriptor> systemDescriptors) throws InterruptedException, InvocationTargetException {

        List<? extends SystemDescriptor> descriptors = new ArrayList<>(systemDescriptors);

        for (int i = 0; i < descriptors.size(); i++) {
            SystemDescriptor currentDescriptor = descriptors.get(i);
            for (int j = 0; j < descriptors.size(); j++) {
                if (j == i) {
                    continue;
                }
                if (currentDescriptor.getId().equals(descriptors.get(j).getId())) {
                    throw new IllegalArgumentException("Duplicated system descriptor ID \"%s\"!".formatted(currentDescriptor.getId()));
                }
            }
        }

        this.systemDescriptors = List.copyOf(systemDescriptors);
        this.dataDirectory = dataDirectory;

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

            unmaximizedBounds = appFrame.getBounds();

            appFrame.addWindowStateListener(e -> {
                if ((e.getNewState() & Frame.MAXIMIZED_BOTH) == 0) {
                    unmaximizedBounds = appFrame.getBounds();
                }
            });

            appFrame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentMoved(ComponentEvent e) {
                    if ((appFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                        unmaximizedBounds = appFrame.getBounds();
                    }
                }

                @Override
                public void componentResized(ComponentEvent e) {
                    if ((appFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                        unmaximizedBounds = appFrame.getBounds();
                    }
                }
            });

            this.registerStateProperty("frame.x", () -> Integer.toString(unmaximizedBounds.x), s -> tryParseInt(s).ifPresent(x -> appFrame.setLocation(x, appFrame.getY())));
            this.registerStateProperty("frame.y", () -> Integer.toString(unmaximizedBounds.y), s -> tryParseInt(s).ifPresent(y -> appFrame.setLocation(appFrame.getX(), y)));
            this.registerStateProperty("frame.width", () -> Integer.toString(unmaximizedBounds.width), s -> tryParseInt(s).ifPresent(width -> appFrame.setSize(new Dimension(width, appFrame.getHeight()))));
            this.registerStateProperty("frame.height", () -> Integer.toString(unmaximizedBounds.height), s -> tryParseInt(s).ifPresent(height -> appFrame.setSize(new Dimension(appFrame.getWidth(), height))));
            this.registerStateProperty("frame.extended_state", () -> Integer.toString(appFrame.getExtendedState()), s -> tryParseInt(s).ifPresent(extendedState -> appFrame.setExtendedState(extendedState)));

            try (FileInputStream input = new FileInputStream(this.dataDirectory.resolve("swing-ui-state.properties").toFile())) {
                Properties stateProperties = new Properties();
                stateProperties.load(input);
                for (PropertyEntry entry : this.stateProperties) {
                    String property = stateProperties.getProperty(entry.key());
                    if (property != null) {
                        entry.deserializer().accept(property);
                    }
                }
            } catch (FileNotFoundException e) {
                Logger.warn("swing-state-ui.properties file not found!");
            } catch (IOException e) {
                Logger.error("Error restoring swing ui state from properties file: {}", e);
            }

            try (FileInputStream input = new FileInputStream(this.dataDirectory.resolve("swing-ui-settings.properties").toFile())) {
                Properties settingProperties = new Properties();
                settingProperties.load(input);
                for (PropertyEntry entry : this.settingProperties) {
                    String property = settingProperties.getProperty(entry.key());
                    if (property != null) {
                        entry.deserializer().accept(property);
                    }
                }
            } catch (FileNotFoundException e) {
                Logger.warn("swing-state-settings.properties file not found!");
            } catch (IOException e) {
                Logger.error("Error restoring swing ui settings from properties file: {}", e);
            }

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

    @ApiStatus.Internal
    public void registerStateProperty(String key, Supplier<String> serializer, Consumer<String> deserializer) {
        this.stateProperties.add(new PropertyEntry(key, serializer, deserializer));
    }

    @ApiStatus.Internal
    public void registerSettingProperty(String key, Supplier<String> serializer, Consumer<String> deserializer) {
        this.settingProperties.add(new PropertyEntry(key, serializer, deserializer));
    }

    @Override
    public void close() {
        Runnable closer = () -> {
            if (this.appFrame != null) {
                try (FileOutputStream output = new FileOutputStream(this.dataDirectory.resolve("swing-ui-state.properties").toFile())) {
                    Properties stateProperties = new Properties();
                    for (PropertyEntry entry : this.stateProperties) {
                        stateProperties.setProperty(entry.key(), entry.serializer().get());
                    }
                    stateProperties.store(output, "Swing GUI state properties");
                } catch (IOException e) {
                    Logger.error("Error storing swing ui state to properties file: {}", e);
                }

                try (FileOutputStream output = new FileOutputStream(this.dataDirectory.resolve("swing-ui-settings.properties").toFile())) {
                    Properties settingProperties = new Properties();
                    for (PropertyEntry entry : this.settingProperties) {
                        settingProperties.setProperty(entry.key(), entry.serializer().get());
                    }
                    settingProperties.store(output, "Swing GUI setting properties");
                } catch (IOException e) {
                    Logger.error("Error storing swing ui settings to properties file: {}", e);
                }
                this.appFrame.dispose();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            closer.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(closer);
            } catch (Exception e) {
                Logger.error("Failed to properly close Main Window object: {}", e);
            }
        }

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

    public static Optional<Integer> tryParseInt(String s) {
        try {
            return Optional.of(Integer.valueOf(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private record PropertyEntry(String key, Supplier<String> serializer, Consumer<String> deserializer) {}

}
