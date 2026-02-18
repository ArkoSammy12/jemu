package io.github.arkosammy12.jemu.application;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import io.github.arkosammy12.jemu.application.adapters.DefaultSystemAdapter;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.frontend.ui.MainWindow;
import io.github.arkosammy12.jemu.application.io.CLIArgs;
import io.github.arkosammy12.jemu.application.io.DataManager;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.application.util.System;
import io.github.arkosammy12.jemu.application.util.FrameLimiter;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import static io.github.arkosammy12.jemu.application.Main.MAIN_FRAMERATE;

public class Jemu {

    private MainWindow mainWindow;
    private volatile DefaultSystemAdapter systemAdapter;

    private final List<StateChangedListener> stateChangedListeners = new CopyOnWriteArrayList<>();
    private final List<FrameListener> frameListeners = new CopyOnWriteArrayList<>();
    private final List<FrameListener> emulatorFrameListeners = new CopyOnWriteArrayList<>();
    private final List<ShutdownListener> shutdownListeners = new CopyOnWriteArrayList<>();

    private volatile State currentState = State.STOPPED;

    private final FrameLimiter pacer = new FrameLimiter(MAIN_FRAMERATE, true, true);
    private final DataManager dataManager = new DataManager();

    private final Thread emulatorThread;
    private final BlockingQueue<State> queuedStates = new LinkedBlockingDeque<>();

    private volatile boolean running = true;
    private volatile boolean muteAudio = false;
    private int volume = 50;

    Jemu(String[] args) {
        this.installEDTExceptionHandler();
        try {

            CLIArgs cliArgs;
            if (args.length > 0) {
                cliArgs = new CLIArgs(args);
                int exitCode = cliArgs.getExitCode();
                if (exitCode >= 0) {
                    java.lang.System.exit(exitCode);
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
                this.initializeEmulator(cliArgs);
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

    public Optional<AudioRenderer> getAudioRenderer() {
        return Optional.ofNullable(this.systemAdapter).map(DefaultSystemAdapter::getAudioRenderer);
    }

    public void setMuted(boolean muted) {
        this.muteAudio = muted;
        this.getAudioRenderer().ifPresent(audioRenderer ->  audioRenderer.setMuted(muted));
    }

    public void setVolume(int volume) {
        this.volume = volume;
        this.getAudioRenderer().ifPresent(audioRenderer -> audioRenderer.setVolume(volume));
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
                while (this.systemAdapter == null) {
                    State oldState = this.updateState(true);
                    State newState = this.getState();
                    this.processState(oldState, newState);
                }

                if (this.systemAdapter == null) {
                    continue;
                }

                if (!this.systemAdapter.getAudioRenderer().needsFrame()) {
                    Thread.sleep(1);
                    continue;
                }

                State oldState = this.updateState(false);
                State newState = this.getState();
                this.processState(oldState, newState);
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

    private void processState(State oldState, State newState) throws Exception {
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
    }

    private void initializeEmulator(EmulatorInitializer initializer) {
        this.systemAdapter = System.getSystemAdapter(this, initializer);
        if (this.muteAudio) {
            this.systemAdapter.getAudioRenderer().setMuted(true);
        }
        this.systemAdapter.getAudioRenderer().setVolume(this.volume);
    }

    public void reset(boolean startPaused) {
        this.enqueueState(startPaused ? State.RESETTING_AND_PAUSING : State.RESETTING_AND_RUNNING);
    }

    public void setPaused(boolean paused) {
        if (paused) {
            this.enqueueState(this.systemAdapter == null ? State.PAUSED_STOPPED : State.PAUSED);
        } else {
            this.enqueueState(this.systemAdapter == null ? State.STOPPED : State.RUNNING);
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
        this.getAudioRenderer().ifPresent(renderer -> renderer.setPaused(true));
    }

    private void onStopping() throws Exception {
        if (this.systemAdapter != null) {
            this.systemAdapter.close();
            this.systemAdapter = null;
        }
        this.enqueueState(State.STOPPED);
    }

    private void onResetting(boolean resetAndPause) throws Exception {
        if (this.systemAdapter != null) {
            this.systemAdapter.close();
        }
        this.initializeEmulator(this.mainWindow.getSettingsBar());
        this.enqueueState(resetAndPause ? State.PAUSED : State.RUNNING);
    }

    private void onRunning() {
        if (systemAdapter == null) {
            return;
        }
        this.getAudioRenderer().ifPresent(renderer -> renderer.setPaused(false));
        this.systemAdapter.getEmulator().executeFrame();
    }

    private void onSteppingFrame() {
        if (systemAdapter == null) {
            return;
        }
        this.getAudioRenderer().ifPresent(renderer -> renderer.setPaused(true));
        this.systemAdapter.getEmulator().executeFrame();
        this.enqueueState(State.PAUSED);
    }

    private void onSteppingCycle() {
        if (this.systemAdapter == null) {
            return;
        }
        this.getAudioRenderer().ifPresent(renderer -> renderer.setPaused(true));
        this.systemAdapter.getEmulator().executeCycle();
        this.enqueueState(State.PAUSED);
    }

    // TODO
    /*
    public void onBreakpoint() {
        this.mainWindow.onBreakpoint();
    }
     */

    void onShutdown() throws Exception {
        try {
            this.emulatorThread.interrupt();
            this.emulatorThread.join();
        } catch (InterruptedException _) {}
        if (this.systemAdapter != null) {
            this.systemAdapter.close();
            this.systemAdapter = null;
        }
        if (this.mainWindow != null) {
            this.mainWindow.close();
        }
        this.notifyShutdownListeners();
        this.dataManager.save();
    }

    private State updateState(boolean take) throws InterruptedException {
        State enqueuedState = take ? queuedStates.take() : queuedStates.poll();
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
            l.onStateChanged(this.systemAdapter, oldState, newState);
        }
    }

    private void notifyFrameListeners() {
        for (FrameListener l : this.frameListeners) {
            l.onFrame(this.systemAdapter, this.currentState);
        }
    }

    private void notifyEmulatorFrameListeners() {
        for (FrameListener l : this.emulatorFrameListeners) {
            l.onFrame(this.systemAdapter, this.currentState);
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

        void onFrame(@Nullable DefaultSystemAdapter systemAdapter, State currentState);

    }

    public interface StateChangedListener {

        void onStateChanged(@Nullable DefaultSystemAdapter systemAdapter, State oldState, State newState);

    }

    public interface ShutdownListener {

        void onShutdown();

    }

}
