package io.github.arkosammy12.jemu.frontend.gui.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.util.SystemInfo;
import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.gui.internal.SerializedEntry;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.*;
import io.github.arkosammy12.jemu.frontend.gui.internal.events.InternalEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.commands.*;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.EmulatorManager;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.FileManager;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.HelpManager;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.SettingsManager;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

// TODO: The big GUI feature update
public class MainWindow implements Closeable {

    @Nullable
    private JFrame appFrame;

    @Nullable
    private MainMenuBar menuBar;

    @Nullable
    private SystemViewport systemViewport;

    @Nullable
    private TitleManager titleManager;

    private final BlockingQueue<EmulatorCommand> emulatorCommandQueue = new LinkedBlockingDeque<>();
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingDeque<>();

    private final Collection<EmulatorCommandCallback> emulatorCommandCallbacks = new CopyOnWriteArrayList<>();

    private final Collection<SystemDescriptor> systemDescriptors;

    private Rectangle unmaximizedBounds;

    @Nullable
    private final Path dataDirectory;
    private final Collection<PropertyEntry> stateProperties = new CopyOnWriteArrayList<>();
    private final Collection<PropertyEntry> settingProperties = new CopyOnWriteArrayList<>();

    public MainWindow(String title, @Nullable Path dataDirectory, Collection<? extends SystemDescriptor> systemDescriptors) throws InterruptedException, InvocationTargetException {

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
        if (this.dataDirectory == null) {
            Logger.warn("No data directory was provided to the UI. Settings and state will not be saved or restored!");
        }

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
            Toolkit.getDefaultToolkit()
                    .getSystemEventQueue()
                    .push(new SafeEventQueue());

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
            this.titleManager = new TitleManager(this, this.appFrame);

            this.appFrame.setJMenuBar(this.menuBar.getJMenuBar());
            this.appFrame.add(this.systemViewport.getJPanel(), new CC().grow().push().wrap());

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

            this.registerStateProperty(new SerializedEntry("frame.x", () -> Integer.toString(unmaximizedBounds.x), s -> tryParseInt(s).ifPresent(x -> appFrame.setLocation(x, appFrame.getY()))));
            this.registerStateProperty(new SerializedEntry("frame.y", () -> Integer.toString(unmaximizedBounds.y), s -> tryParseInt(s).ifPresent(y -> appFrame.setLocation(appFrame.getX(), y))));
            this.registerStateProperty(new SerializedEntry("frame.width", () -> Integer.toString(unmaximizedBounds.width), s -> tryParseInt(s).ifPresent(width -> appFrame.setSize(new Dimension(width, appFrame.getHeight())))));
            this.registerStateProperty(new SerializedEntry("frame.height", () -> Integer.toString(unmaximizedBounds.height), s -> tryParseInt(s).ifPresent(height -> appFrame.setSize(new Dimension(appFrame.getWidth(), height)))));
            this.registerStateProperty(new SerializedEntry("frame.extended_state", () -> Integer.toString(appFrame.getExtendedState()), s -> tryParseInt(s).ifPresent(extendedState -> appFrame.setExtendedState(extendedState))));

            this.readSwingStateAndSettings();

        });

    }

    public Collection<SystemDescriptor> getSystemDescriptors() {
        return this.systemDescriptors;
    }

    public Optional<Path> getDataDirectoryPath() {
        return Optional.ofNullable(this.dataDirectory);
    }

    public SystemViewport getSystemViewport() {
        return Objects.requireNonNull(this.systemViewport);
    }

    public FileManager getFileManager() {
        return this.getMainMenuBar().getFileMenu();
    }

    public EmulatorManager getEmulatorManager() {
        return this.getMainMenuBar().getEmulatorMenu();
    }

    public SettingsManager getSettingsManager() {
        return this.getMainMenuBar().getSettingsMenu();
    }

    public HelpManager getHelpManager() {
        return this.getMainMenuBar().getHelpMenu();
    }

    public TitleManager getTitleManager() {
        return Objects.requireNonNull(this.titleManager);
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
    public PendingEmulatorCommand pollEmulatorCommand() throws InterruptedException {
        return this.getEmulatorCommand(false);
    }

    public PendingEmulatorCommand waitEmulatorCommand() throws InterruptedException {
        return this.getEmulatorCommand(true);
    }

    public Event waitEvent() throws InterruptedException {
        return this.eventQueue.take();
    }

    @Nullable
    private PendingEmulatorCommand getEmulatorCommand(boolean wait) throws InterruptedException {
        EmulatorCommand emulatorCommand = wait ? this.emulatorCommandQueue.take() : this.emulatorCommandQueue.poll();
        if (emulatorCommand == null) {
            return null;
        }
        Runnable acknowledgeFunction = switch (emulatorCommand) {
            case PowerCycleCommand command -> () -> this.emulatorCommandCallbacks.forEach(c -> {
                if (c instanceof PowerCycleCommandCallback powerCycleCallback) {
                    powerCycleCallback.onReset(command);
                }
            });
            case ResetEmulatorCommand command -> () -> this.emulatorCommandCallbacks.forEach(c -> {
                if (c instanceof ResetEmulatorCommandCallback resetCallback) {
                    resetCallback.onReset(command);
                }
            });
            case PauseEmulatorCommand command -> () -> this.emulatorCommandCallbacks.forEach(c -> {
                if (c instanceof PauseCommandCallback pauseCallback) {
                    pauseCallback.onPause(command);
                }
            });
            case StepCycleEmulatorCommand command -> () -> this.emulatorCommandCallbacks.forEach(c -> {
                if (c instanceof StepCycleCommandCallback stepCycleCallback) {
                    stepCycleCallback.onStepCycle(command);
                }
            });
            case StepFrameEmulatorCommand command -> () -> this.emulatorCommandCallbacks.forEach(c -> {
                if (c instanceof StepFrameCommandCallback stepFrameCallback) {
                    stepFrameCallback.onStepFrame(command);
                }
            });
            case StopEmulatorCommand command -> () -> this.emulatorCommandCallbacks.forEach(c -> {
                if (c instanceof StopCommandCallback stopCallback) {
                    stopCallback.onStop(command);
                }
            });
        };
        return new PendingEmulatorCommandImpl(emulatorCommand, acknowledgeFunction);
    }

    @NotNull
    @ApiStatus.Internal
    JFrame getJFrame() {
        return Objects.requireNonNull(this.appFrame);
    }

    @ApiStatus.Internal
    public <T extends EmulatorCommandCallback> void addEmulatorCommandCallback(T callback) {
        this.emulatorCommandCallbacks.add(callback);
    }

    @ApiStatus.Internal
    public void pushEvent(InternalEvent internalEvent) {
        this.eventQueue.offer(internalEvent.getEvent());
    }

    @ApiStatus.Internal
    public void registerStateProperty(SerializedEntry serializedEntry) {
        this.stateProperties.add(new PropertyEntry(serializedEntry.key(), serializedEntry.serializer(), serializedEntry.deserializer()));
    }

    @ApiStatus.Internal
    public void registerSettingProperty(SerializedEntry serializedEntry) {
        this.settingProperties.add(new PropertyEntry(serializedEntry.key(), serializedEntry.serializer(), serializedEntry.deserializer()));
    }

    @ApiStatus.Internal
    public MainMenuBar getMainMenuBar() {
        return Objects.requireNonNull(this.menuBar);
    }

    @Override
    public void close() {
        Runnable closer = () -> {
            if (this.appFrame != null) {
                Path statePropertiesFile = this.dataDirectory.resolve("swing-ui-state.properties");
                try {
                    if (!Files.exists(this.dataDirectory)) {
                        Files.createDirectory(this.dataDirectory);
                    }
                    if (!Files.exists(statePropertiesFile)) {
                        Files.createFile(statePropertiesFile);
                    }
                    try (FileOutputStream output = new FileOutputStream(statePropertiesFile.toFile())) {
                        if (!Files.exists(this.dataDirectory)) {
                            Files.createDirectory(this.dataDirectory);
                        }
                        if (!Files.exists(statePropertiesFile)) {
                            Files.createFile(statePropertiesFile);
                        }
                        Properties stateProperties = new Properties();
                        for (PropertyEntry entry : this.stateProperties) {
                            stateProperties.setProperty(entry.key(), entry.serializer().get());
                        }
                        stateProperties.store(output, "Swing GUI state properties");
                    }
                } catch (IOException e) {
                    Logger.error("Error storing swing ui state to properties file: {}", e);
                }

                Path settingsPropertiesFile = this.dataDirectory.resolve("swing-ui-settings.properties");
                try {
                    if (!Files.exists(this.dataDirectory)) {
                        Files.createDirectory(this.dataDirectory);
                    }
                    if (!Files.exists(settingsPropertiesFile)) {
                        Files.createFile(settingsPropertiesFile);
                    }
                    try (FileOutputStream output = new FileOutputStream(settingsPropertiesFile.toFile())) {
                        Properties settingProperties = new Properties();
                        for (PropertyEntry entry : this.settingProperties) {
                            settingProperties.setProperty(entry.key(), entry.serializer().get());
                        }
                        settingProperties.store(output, "Swing GUI setting properties");
                    }
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

    private void readSwingStateAndSettings() {
        if (this.dataDirectory == null) {
            return;
        }

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
    }

    private record PropertyEntry(String key, Supplier<String> serializer, Consumer<String> deserializer) {}

    private class SafeEventQueue extends EventQueue {

        @Override
        protected void dispatchEvent(AWTEvent event) {
            try {
                super.dispatchEvent(event);
            } catch (Throwable t) {
                this.handleException(t);
            }
        }

        private void handleException(Throwable t) {
            Logger.error("Uncaught Swing exception", t);

            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            getJFrame(),
                            t.toString(),
                            "Uncaught Swing UI exception",
                            JOptionPane.ERROR_MESSAGE
                    )
            );
        }
    }

    static final class PendingEmulatorCommandImpl implements PendingEmulatorCommand {

        private final EmulatorCommand emulatorCommand;
        private final Runnable acknowledgeFunction;

        private PendingEmulatorCommandImpl(EmulatorCommand emulatorCommand, Runnable acknowledgeFunction) {
            this.emulatorCommand = emulatorCommand;
            this.acknowledgeFunction = acknowledgeFunction;
        }

        @Override
        public EmulatorCommand getEmulatorCommand() {
            return this.emulatorCommand;
        }

        @Override
        public void acknowledge() {
            this.acknowledgeFunction.run();
        }
    }

}
