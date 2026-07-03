package io.github.arkosammy12.jemu.frontend.gui.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.util.SystemInfo;
import io.github.arkosammy12.jemu.frontend.config.ConfigurationManager;
import io.github.arkosammy12.jemu.frontend.config.Configurations;
import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.config.internal.InternalConfigurations;
import io.github.arkosammy12.jemu.frontend.events.internal.ExposableEvent;
import io.github.arkosammy12.jemu.frontend.gui.internal.commands.*;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.commands.*;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.EmulatorManager;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.FileManager;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.HelpManager;
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

public class MainWindow implements Closeable {

    @Nullable
    private final Path dataDirectory;
    private final ConfigurationManager configurationManager;

    private final Collection<EmulatorCommandCallback> emulatorCommandCallbacks = new CopyOnWriteArrayList<>();
    private final Collection<PushedEventConsumer<?>> pushedEventConsumers = new CopyOnWriteArrayList<>();
    private final Collection<SystemDescriptor> systemDescriptors;

    private final BlockingQueue<EmulatorCommand> emulatorCommandQueue = new LinkedBlockingDeque<>();
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingDeque<>();

    @Nullable
    private JFrame appFrame;

    @Nullable
    private MainMenuBar menuBar;

    @Nullable
    private SystemViewport systemViewport;

    @Nullable
    private TitleManager titleManager;

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

        this.configurationManager = new ConfigurationManager(dataDirectory);
        this.configurationManager.read();

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
            Toolkit.getDefaultToolkit().getSystemEventQueue().push(new SafeEventQueue());

            UIManager.put("TitlePane.useWindowDecorations", false);
            UIManager.put("Component.hideMnemonics", false);
            UIManager.put("FileChooser.readOnly", true);
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("MenuBar.itemMargins", new Insets(5, 10, 5, 10));
            UIManager.put("MenuItem.margin", new Insets(4, 8, 4, 8));

            FlatDarkLaf.setup();

            ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
            toolTipManager.setLightWeightPopupEnabled(false);
            toolTipManager.setInitialDelay(700);
            toolTipManager.setReshowDelay(700);
            toolTipManager.setDismissDelay(4000);

            JFrame.setDefaultLookAndFeelDecorated(false);
            JDialog.setDefaultLookAndFeelDecorated(false);
            Toolkit.getDefaultToolkit().setDynamicLayout(true);

            this.appFrame = new JFrame(title);
            this.appFrame.setLayout(new MigLayout(new LC().insets("0"), new AC(), new AC().gap("0")));
            this.appFrame.setBackground(Color.BLACK);
            this.appFrame.getRootPane().putClientProperty("apple.awt.fullscreenable", true);

            this.systemViewport = new SystemViewport(this);
            this.menuBar = new MainMenuBar(this);
            this.titleManager = new TitleManager(this);

            this.appFrame.setJMenuBar(this.menuBar.getJMenuBar());
            this.appFrame.add(this.systemViewport.getJPanel(), new CC().grow().push().wrap());

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            this.appFrame.requestFocusInWindow();
            this.appFrame.setResizable(true);
            this.appFrame.setPreferredSize(new Dimension((int) (screenSize.getWidth() / 1.5), (int) (screenSize.getHeight() / 1.5)));
            this.appFrame.pack();
            this.appFrame.setLocationRelativeTo(null);

            this.appFrame.addWindowStateListener(e -> {
                this.getConfig().getState().getWindowState().setExtendedState(e.getNewState());
                if ((e.getNewState() & Frame.MAXIMIZED_BOTH) == 0) {
                    this.getConfig().getState().getWindowState().getBounds().setFromBounds(appFrame.getBounds());
                }
            });

            this.appFrame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentMoved(ComponentEvent e) {
                    if ((appFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                        getConfig().getState().getWindowState().getBounds().setFromBounds(appFrame.getBounds());
                    }
                }

                @Override
                public void componentResized(ComponentEvent e) {
                    if ((appFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                        getConfig().getState().getWindowState().getBounds().setFromBounds(appFrame.getBounds());
                    }
                }
            });

            this.getConfig().getState().getWindowState().getBounds().getX().ifPresent(x -> this.appFrame.setLocation(x, this.appFrame.getY()));
            this.getConfig().getState().getWindowState().getBounds().getY().ifPresent(y -> this.appFrame.setLocation(this.appFrame.getX(), y));
            this.getConfig().getState().getWindowState().getBounds().getWidth().ifPresent(width -> this.appFrame.setSize(width, this.appFrame.getHeight()));
            this.getConfig().getState().getWindowState().getBounds().getHeight().ifPresent(height -> this.appFrame.setSize(this.appFrame.getWidth(), height));
            this.getConfig().getState().getWindowState().getExtendedState().ifPresent(extendedState -> this.appFrame.setExtendedState(extendedState));

        });

    }

    public Configurations getConfigurations() {
        return this.configurationManager.getConfig();
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

    public HelpManager getHelpManager() {
        return this.getMainMenuBar().getHelpMenu();
    }

    public TitleManager getTitleManager() {
        return Objects.requireNonNull(this.titleManager);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> this.getJFrame().setVisible(true));
    }

    public void setIcons(List<Image> icons) {
        SwingUtilities.invokeLater(() -> {
            if (this.appFrame != null) {
                this.appFrame.setIconImages(icons);
            }
        });
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
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this.getJFrame(), message, title, dialogType.getJOptionPaneMessageTypeId()));
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

    @ApiStatus.Internal
    public InternalConfigurations getConfig() {
        return this.configurationManager.getConfig();
    }

    @NotNull
    @ApiStatus.Internal
    JFrame getJFrame() {
        return Objects.requireNonNull(this.appFrame);
    }

    @ApiStatus.Internal
    public <T extends EmulatorCommandCallback> void onEmulatorCommand(T callback) {
        this.emulatorCommandCallbacks.add(callback);
    }

    @ApiStatus.Internal
    public void pushEvent(InternalEvent internalEvent) {
        if (internalEvent instanceof ExposableEvent exposableEvent) {
            this.eventQueue.offer(exposableEvent.getEvent());
        }
        for (PushedEventConsumer<?> pushedEventConsumer : this.pushedEventConsumers) {
            if (pushedEventConsumer.eventType().isInstance(internalEvent)) {
                this.dispatchToConsumer(pushedEventConsumer, internalEvent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends InternalEvent> void dispatchToConsumer(PushedEventConsumer<T> consumer, InternalEvent event) {
        consumer.action().accept((T) event);
    }

    @ApiStatus.Internal
    public <T extends InternalEvent> void onEvent(Class<T> eventType, Consumer<T> action) {
        this.pushedEventConsumers.add(new PushedEventConsumer<>(eventType, action));
    }

    @ApiStatus.Internal
    public MainMenuBar getMainMenuBar() {
        return Objects.requireNonNull(this.menuBar);
    }

    @Override
    public void close() {
        Runnable closer = () -> {
            this.configurationManager.save();
            JFrame frame = this.appFrame;
            if (frame != null) {
                frame.dispose();
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

        private int getJOptionPaneMessageTypeId() {
            return this.jOptionPaneMessageTypeId;
        }

    }

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

    private record PushedEventConsumer<T extends InternalEvent>(Class<T> eventType, Consumer<T> action) {}

}
