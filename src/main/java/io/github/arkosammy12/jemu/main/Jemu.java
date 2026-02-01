package io.github.arkosammy12.jemu.main;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import io.github.arkosammy12.jemu.config.CLIArgs;
import io.github.arkosammy12.jemu.config.DataManager;
import io.github.arkosammy12.jemu.systems.Emulator;
import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.util.System;
import io.github.arkosammy12.jemu.util.FrameLimiter;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.arkosammy12.jemu.main.Main.MAIN_FRAMERATE;

public class Jemu {

    private MainWindow mainWindow;
    private volatile Emulator currentEmulator;

    private final List<StateChangedListener> stateChangedListeners = new CopyOnWriteArrayList<>();
    private final List<FrameListener> frameListeners = new CopyOnWriteArrayList<>();
    private final List<FrameListener> emulatorFrameListeners = new CopyOnWriteArrayList<>();
    private final List<ShutdownListener> shutdownListeners = new CopyOnWriteArrayList<>();

    private final Queue<State> queuedStates = new ConcurrentLinkedQueue<>();
    private volatile State currentState = State.STOPPED;
    private volatile boolean running = true;

    private final FrameLimiter pacer = new FrameLimiter(MAIN_FRAMERATE, true, true);
    private final DataManager dataManager = new DataManager();
    private final DefaultAudioRenderer audioRenderer = new DefaultAudioRenderer(this);

    private final Thread emulatorThread;

    Jemu(String[] args) {
        this.installEDTExceptionHandler();
        try {
            CLIArgs cliArgs;
            if (args.length > 0) {
                cliArgs = new CLIArgs();
                CommandLine cli = new CommandLine(cliArgs);
                CommandLine.ParseResult parseResult = cli.parseArgs(args);
                Integer executeHelpResult = CommandLine.executeHelpRequest(parseResult);
                int exitCodeOnUsageHelp = cli.getCommandSpec().exitCodeOnUsageHelp();
                int exitCodeOnVersionHelp = cli.getCommandSpec().exitCodeOnVersionHelp();
                if (executeHelpResult != null) {
                    if (executeHelpResult == exitCodeOnUsageHelp) {
                        java.lang.System.exit(exitCodeOnUsageHelp);
                    } else if (executeHelpResult == exitCodeOnVersionHelp) {
                        java.lang.System.exit(exitCodeOnVersionHelp);
                    }
                }
            } else {
                cliArgs = null;
            }

            SwingUtilities.invokeAndWait(() -> {
                FlatOneDarkIJTheme.setup();

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

                this.mainWindow = new MainWindow(this);
                this.mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                this.mainWindow.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        try {
                            running = false;
                            onShutdown();
                        } catch (Exception ex) {
                            Logger.error("Failed to release application resources: {}", ex);
                        }
                    }

                });
                this.mainWindow.accept(this.dataManager);
            });

            if (cliArgs != null) {
                SwingUtilities.invokeAndWait(() -> this.mainWindow.accept(cliArgs));
                this.currentEmulator = io.github.arkosammy12.jemu.util.System.getEmulator(this, cliArgs);
                this.reset(false);
            }
            SwingUtilities.invokeLater(() -> this.mainWindow.setVisible(true));
            this.emulatorThread = new Thread(this::emulatorLoop, "jemu-emulator-thread");
        } catch (Exception e) {
            if (this.mainWindow != null) {
                SwingUtilities.invokeLater(() -> this.mainWindow.dispose());
            }
            throw new RuntimeException("Failed to initialize jemu: " + e);
        }
    }

    public MainWindow getMainWindow() {
        return this.mainWindow;
    }

    public AudioRenderer getAudioRenderer() {
        return this.audioRenderer;
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    public void addStateChangedListener(StateChangedListener l) {
        this.stateChangedListeners.add(l);
    }

    public void addFrameListener(FrameListener l) {
        this.frameListeners.add(l);
    }

    public void addEmulatorFrameListener(FrameListener l) {
        this.emulatorFrameListeners.add(l);
    }

    public void addShutdownListener(ShutdownListener l) {
        this.shutdownListeners.add(l);
    }

    public State getState() {
        return this.currentState;
    }

    public void start() throws Exception {
        this.emulatorThread.start();
        while (this.running) {
            if (!this.pacer.isFrameReady(true)) {
                continue;
            }
            this.notifyFrameListeners();
        }
    }

    private void emulatorLoop() {
        while (this.running) {
            try {
                if (!this.audioRenderer.needsFrame()) {
                    Thread.sleep(1);
                    continue;
                }
                State oldState = this.updateState();
                State newState = this.getState();
                switch (newState) {
                    case STOPPED, PAUSED, PAUSED_STOPPED -> onIdle();
                    case RESETTING_AND_RUNNING -> onResetting(false);
                    case RESETTING_AND_PAUSING -> onResetting(true);
                    case STOPPING -> onStopping();
                    case RUNNING -> onRunning();
                    case STEPPING_FRAME -> onSteppingFrame();
                    case STEPPING_CYCLE -> onSteppingCycle();
                }
                this.notifyStateChangedListeners(oldState, newState);
                this.notifyEmulatorFrameListeners();
            } catch (EmulatorException e) {
                Logger.error("Exception while running emulator: {}", e);
                this.mainWindow.showExceptionDialog(e);
                this.stop();
            } catch (InterruptedException _) {}
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void reset(boolean startPaused) {
        this.enqueueState(startPaused ? State.RESETTING_AND_PAUSING : State.RESETTING_AND_RUNNING);
    }

    public void setPaused(boolean paused) {
        if (paused) {
            this.enqueueState(this.currentEmulator == null ? State.PAUSED_STOPPED : State.PAUSED);
        } else {
            this.enqueueState(this.currentEmulator == null ? State.STOPPED : State.RUNNING);
        }
    }

    public void stop() {
        this.enqueueState(State.STOPPING);
    }

    public void stepFrame() {
        this.enqueueState(State.STEPPING_FRAME);
    }

    public void stepCycle() {
        this.enqueueState(State.STEPPING_CYCLE);
    }

    private void onIdle() {
        this.audioRenderer.setPaused(true);
    }

    private void onStopping() throws Exception {
        if (this.currentEmulator != null) {
            this.currentEmulator.close();
            this.currentEmulator = null;
        }
        this.audioRenderer.setPaused(true);
        this.audioRenderer.setFramerate(MAIN_FRAMERATE);
        this.enqueueState(State.STOPPED);
    }

    private void onResetting(boolean resetAndPause) throws Exception {
        if (this.currentEmulator != null) {
            this.currentEmulator.close();
        }
        this.audioRenderer.setPaused(true);
        this.currentEmulator = System.getEmulator(this, this.mainWindow.getSettingsBar());
        this.audioRenderer.setFramerate(this.currentEmulator.getFramerate());
        this.enqueueState(resetAndPause ? State.PAUSED : State.RUNNING);
    }

    private void onRunning() {
        if (currentEmulator == null) {
            return;
        }
        this.audioRenderer.setPaused(false);
        this.currentEmulator.executeFrame();
    }

    private void onSteppingFrame() {
        if (currentEmulator == null) {
            return;
        }
        this.audioRenderer.setPaused(true);
        this.currentEmulator.executeFrame();
        this.enqueueState(State.PAUSED);
    }

    private void onSteppingCycle() {
        if (this.currentEmulator == null) {
            return;
        }
        this.audioRenderer.setPaused(true);
        this.currentEmulator.executeCycle();
        this.enqueueState(State.PAUSED);
    }

    public void onBreakpoint() {
        this.mainWindow.onBreakpoint();
    }

    void onShutdown() throws Exception {
        try {
            this.emulatorThread.join();
        } catch (InterruptedException _) {}
        if (this.currentEmulator != null) {
            this.currentEmulator.close();
            this.currentEmulator = null;
        }
        if (this.mainWindow != null) {
            this.mainWindow.close();
        }
        this.audioRenderer.close();
        this.notifyShutdownListeners();
        this.dataManager.save();
    }

    private State updateState() {
        State enqueuedState = queuedStates.poll();
        if (enqueuedState == null) {
            return this.getState();
        }
        State oldState = this.currentState;
        this.currentState = enqueuedState;
        return oldState;
    }

    private void notifyStateChangedListeners(State oldState, State newState) {
        if (oldState == newState) {
            return;
        }
        for (StateChangedListener l : this.stateChangedListeners) {
            l.onStateChanged(this.currentEmulator, oldState, newState);
        }
    }

    private void notifyFrameListeners() {
        for (FrameListener l : this.frameListeners) {
            l.onFrame(this.currentEmulator);
        }
    }

    private void notifyEmulatorFrameListeners() {
        for (FrameListener l : this.emulatorFrameListeners) {
            l.onFrame(this.currentEmulator);
        }
    }

    private void notifyShutdownListeners() {
        for (ShutdownListener l : this.shutdownListeners) {
            l.onShutdown();
        }
    }

    private void enqueueState(State newState) {
        this.queuedStates.offer(newState);
    }

    private void installEDTExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (SwingUtilities.isEventDispatchThread()) {
                Logger.error("Uncaught exception on EDT: {}", throwable);
                if (this.mainWindow != null) {
                    this.mainWindow.showExceptionDialog(throwable);
                }
            } else {
                Logger.error("Uncaught exception on thread " + thread.getName() + ": {}", throwable);
            }
        });
    }

    public enum State {
        RUNNING,
        STOPPING,
        RESETTING_AND_RUNNING,
        RESETTING_AND_PAUSING,
        PAUSED,
        STEPPING_FRAME,
        STEPPING_CYCLE,
        STOPPED,
        PAUSED_STOPPED;

        public boolean isRunning() {
            return this == RUNNING;
        }

        public boolean isStopping() {
            return this == STOPPING;
        }

        public boolean isStopped() {
            return this == STOPPED || this == PAUSED_STOPPED;
        }

        public boolean isResetting() {
            return this == RESETTING_AND_RUNNING || this == RESETTING_AND_PAUSING;
        }

        public boolean isStepping() {
            return this == STEPPING_CYCLE || this == STEPPING_FRAME;
        }

    }

    public interface FrameListener {

        void onFrame(@Nullable Emulator emulator);

    }

    public interface StateChangedListener {

        void onStateChanged(@Nullable Emulator emulator, State oldState, State newState);

    }

    public interface ShutdownListener {

        void onShutdown();

    }

}
