package io.github.arkosammy12.jemu.app;

import io.github.arkosammy12.jemu.app.adapters.DefaultSystemAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.frontend.gui.swing.commands.*;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.MuteEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.VolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.menus.HelpMenu;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import net.harawata.appdirs.AppDirsFactory;
import org.tinylog.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public final class Jemu {

    private static final Path APP_DIR = Path.of(AppDirsFactory.getInstance().getUserDataDir("jemu", null, null));

    private volatile DefaultSystemAdapter currentSystem = null;
    private volatile State currentState = State.STOPPED;

    private final Thread emulatorThread;
    private final Thread uiEventListenerThread;

    private MainWindow mainWindow;
    private boolean running = true;

    public Jemu() {
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                Logger.error("Uncaught exception in thread {}: {}", thread.getName(), throwable, throwable.getStackTrace());
            });

            this.mainWindow = new MainWindow("jemu " + Main.VERSION_STRING, APP_DIR, Arrays.stream(System.values()).toList());
            this.mainWindow.setClosingHook(() -> {
                this.running = false;
                try {
                    this.onShutdown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            HelpMenu helpMenu = this.mainWindow.getMainMenuBar().getHelpMenu();
            helpMenu.setAuthorName("ArkoSammy12");
            helpMenu.setProjectName("jemu");
            helpMenu.setVersionString(Main.VERSION_STRING);
            helpMenu.setProjectSourceLink("https://github.com/ArkoSammy12/jemu");
            helpMenu.setProjectBugReportLink("https://github.com/ArkoSammy12/jemu/issues");

            this.emulatorThread = new Thread(this::emulatorLoop, "jemu-emulator-thread");
            this.uiEventListenerThread = new Thread(this::eventListenerLoop, "jemu-event-listener-thread");

            this.mainWindow.show();
        } catch (Exception e) {
            if (this.mainWindow != null) {
                this.mainWindow.close();
            }
            throw new RuntimeException("Failed to initialize jemu: " + e);
        }
    }

    public Optional<AudioRenderer> getCurrentAudioRenderer() {
        return Optional.ofNullable(this.currentSystem).map(DefaultSystemAdapter::getAudioRenderer);
    }

    public void start() {
        this.uiEventListenerThread.start();
        this.emulatorThread.start();
    }

    private void eventListenerLoop() {
        while (this.running) {
            try {
                Event uiEvent = this.mainWindow.waitEvent();
                switch (uiEvent) {
                    case MuteEvent(boolean mute) -> this.getCurrentAudioRenderer().ifPresent(audioRenderer -> audioRenderer.setMuted(mute));
                    case VolumeChangedEvent(int newVolume) -> this.getCurrentAudioRenderer().ifPresent(audioRenderer -> audioRenderer.setVolume(newVolume));
                    case null, default -> {}
                }
            } catch (InterruptedException _) {}
        }
    }

    private void emulatorLoop() {
        while (this.running) {
            try {
                while (this.currentSystem == null) {
                    this.updateState(true);
                    this.processState(this.currentState);
                }

                if (this.currentSystem == null) {
                    continue;
                }

                if (!this.currentSystem.getAudioRenderer().needsFrame()) {
                    Thread.sleep(1);
                    continue;
                }

                this.updateState(false);
                this.processState(this.currentState);
                if (this.currentSystem != null) {
                    this.currentSystem.onFrame();
                }

            } catch (EmulatorException e) {
                Logger.error("Emulation error: {}", e);
                this.mainWindow.showCoreError(e);
                if (this.currentSystem != null) {
                    try {
                        this.currentSystem.close();
                    } catch (Exception _) {}
                    this.mainWindow.getSystemViewport().setSystemDisplayPanel(null);
                    this.currentSystem = null;
                }
                this.mainWindow.submitEmulatorCommand(new StopEmulatorCommand());
            } catch (InterruptedException _) {

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateState(boolean take) throws Exception {
        EmulatorCommand enqueuedEmulatorCommand = take ? this.mainWindow.waitEmulatorCommand() : this.mainWindow.pollEmulatorCommand();
        State enqueuedState = switch (enqueuedEmulatorCommand) {
            case ResetEmulatorCommand resetEvent -> {
                this.onResetting(resetEvent);
                this.mainWindow.getSystemViewport().setSystemDisplayPanel(this.currentSystem.getVideoDriver().orElse(null) instanceof JPanel jPanel ? () -> jPanel : null);
                yield resetEvent.resetIntoPaused() ? State.PAUSED : State.RUNNING;
            }
            case StopEmulatorCommand _ -> {
                this.onStopping();
                yield State.STOPPED;
            }
            case PauseEmulatorCommand(boolean pause) -> {
                boolean stopped = this.currentSystem == null;
                if (pause) {
                    yield stopped ? State.PAUSE_STOPPED : State.PAUSED;
                } else {
                    yield stopped ? State.STOPPED : State.RUNNING;
                }
            }
            case StepFrameEmulatorCommand _ -> State.STEPPING_FRAME;
            case StepCycleEmulatorCommand _ -> State.STEPPING_CYCLE;
            case null -> null;
        };
        if (enqueuedState == null) {
            return;
        }
        this.currentState = enqueuedState;
    }

    private void processState(State state) {
        switch (state) {
            case STOPPED, PAUSED, PAUSE_STOPPED -> onIdle();
            case RUNNING -> onRunning();
            case STEPPING_FRAME -> onSteppingFrame();
            case STEPPING_CYCLE -> onSteppingCycle();
        }
    }

    private void onIdle() {
        this.getCurrentAudioRenderer().ifPresent(renderer -> renderer.setPaused(true));
    }

    private void onRunning() {
        if (currentSystem == null) {
            return;
        }
        this.getCurrentAudioRenderer().ifPresent(renderer -> renderer.setPaused(false));
        this.currentSystem.getEmulator().executeFrame();
        this.mainWindow.getStatusBar().update(this.currentSystem.getRomTitle().orElse(""), this.currentSystem.getSystemName());
    }

    private void onSteppingFrame() {
        if (this.currentSystem == null) {
            return;
        }
        this.getCurrentAudioRenderer().ifPresent(renderer -> renderer.setPaused(true));
        this.currentSystem.getEmulator().executeFrame();
        this.currentState = State.PAUSED;
    }

    private void onSteppingCycle() {
        if (this.currentSystem == null) {
            return;
        }
        this.getCurrentAudioRenderer().ifPresent(renderer -> renderer.setPaused(true));
        this.currentSystem.getEmulator().executeCycle();
        this.currentState = State.PAUSED;
    }

    private void onResetting(ResetEmulatorCommand resetEvent) throws Exception {
        if (this.currentSystem != null) {
            this.currentSystem.close();
        }

        EmulatorInitializer emulatorInitializer = new EmulatorInitializer() {

            @Override
            public Optional<Path> getRomPath() {
                return mainWindow.getMainMenuBar().getFileMenu().getSelectedRomPath();
            }

            @Override
            public Optional<byte[]> getRawRom() {
                return this.getRomPath().map(SystemAdapter::readRawRom);
            }

            @Override
            public Optional<System> getSystem() {
                return Optional.ofNullable(resetEvent.getSystemDescriptor().orElse(null) instanceof System system ? system : null);
            }

        };

        this.initializeEmulator(emulatorInitializer);
    }

    private void onStopping() throws Exception {
        if (this.currentSystem != null) {
            this.currentSystem.close();
            this.currentSystem = null;
        }
    }

    private void initializeEmulator(EmulatorInitializer initializer) {
        this.currentSystem = System.getSystemAdapter(this, initializer);
        this.getCurrentAudioRenderer().ifPresent(audioRenderer -> {
            audioRenderer.setMuted(this.mainWindow.getMainMenuBar().getSettingsMenu().getMuted());
            audioRenderer.setVolume(this.mainWindow.getMainMenuBar().getSettingsMenu().getVolume());
        });
    }

    void onShutdown() throws Exception {
        try {
            this.emulatorThread.interrupt();
            this.emulatorThread.join();
        } catch (InterruptedException _) {}

        try {
            this.uiEventListenerThread.interrupt();
            this.uiEventListenerThread.join();
        } catch (InterruptedException _) {}

        if (this.currentSystem != null) {
            this.currentSystem.close();
            this.currentSystem = null;
        }

        if (this.mainWindow != null) {
            this.mainWindow.close();
        }
        //this.notifyShutdownListeners();
        //this.dataManager.save();
    }

    private enum State {
        STOPPED,
        PAUSE_STOPPED,
        RUNNING,
        PAUSED,
        STEPPING_FRAME,
        STEPPING_CYCLE
    }

}
