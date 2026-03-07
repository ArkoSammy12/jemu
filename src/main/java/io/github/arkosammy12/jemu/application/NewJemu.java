package io.github.arkosammy12.jemu.application;

import io.github.arkosammy12.jemu.application.adapters.DefaultSystemAdapter;
import io.github.arkosammy12.jemu.application.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.util.KeyboardLayout;
import io.github.arkosammy12.jemu.application.util.System;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.frontend.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.swing.events.Event;
import io.github.arkosammy12.jemu.frontend.swing.events.PauseEvent;
import io.github.arkosammy12.jemu.frontend.swing.events.ResetEvent;
import io.github.arkosammy12.jemu.frontend.swing.events.StopEvent;
import org.tinylog.Logger;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public final class NewJemu {

    private volatile DefaultSystemAdapter currentSystem = null;
    private volatile State currentState = State.STOPPED;

    private final Thread emulatorThread;

    private MainWindow mainWindow;
    private boolean running = true;

    public NewJemu() throws IOException {
        try {
            this.mainWindow = new io.github.arkosammy12.jemu.frontend.swing.MainWindow("jemu " + Main.VERSION_STRING, Arrays.stream(System.values()).toList());
            this.mainWindow.setClosingHook(() -> {
                this.running = false;
                try {
                    this.onShutdown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            this.emulatorThread = new Thread(this::emulatorLoop, "jemu-emulator-thread");
            this.mainWindow.show();
        } catch (Exception e) {
            if (this.mainWindow != null) {
                this.mainWindow.close();
            }
            throw new RuntimeException("Failed to initialize jemu: " + e);
        }
    }

    public Optional<AudioRenderer> getAudioRenderer() {
        return Optional.ofNullable(this.currentSystem).map(DefaultSystemAdapter::getAudioRenderer);
    }

    public void start() {
        this.emulatorThread.start();
    }

    public void emulatorLoop() {
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
                Logger.error("Exception while running emulator: {}", e);
                //this.mainWindow.showExceptionDialog();
                this.mainWindow.offerEvent(new StopEvent());
            } catch (InterruptedException _) {

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateState(boolean take) throws Exception {
        Event enqueuedEvent = take ? this.mainWindow.waitEvent() : this.mainWindow.pollEvent();
        State enqueuedState = switch (enqueuedEvent) {
            case ResetEvent resetEvent -> {
                this.onResetting(resetEvent);
                resetEvent.onCompleted(this.currentSystem.getVideoDriver().orElse(null) instanceof JPanel jPanel ? () -> jPanel : null);
                yield resetEvent.resetIntoPaused() ? State.PAUSED : State.RUNNING;
            }
            case StopEvent stopEvent -> {
                this.onStopping();
                stopEvent.onCompleted();
                yield State.STOPPED;
            }
            case PauseEvent pauseEvent -> {
                boolean stopped = this.currentSystem == null;
                pauseEvent.onCompleted(stopped);
                if (pauseEvent.getPause()) {
                    yield stopped ? State.PAUSE_STOPPED : State.PAUSED;
                } else {
                    yield stopped ? State.STOPPED : State.RUNNING;
                }
            }
            case null, default -> null;
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
        }
    }

    private void onIdle() {
        this.getAudioRenderer().ifPresent(renderer -> renderer.setPaused(true));
    }

    private void onRunning() {
        if (currentSystem == null) {
            return;
        }
        this.currentSystem.getAudioRenderer().setPaused(false);
        this.currentSystem.getEmulator().executeFrame();
    }

    private void onResetting(ResetEvent resetEvent) throws Exception {
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

            @Override
            public Optional<KeyboardLayout> getKeyboardLayout() {
                return Optional.empty();
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
        /*
        if (this.muteAudio) {
            this.systemAdapter.getAudioRenderer().setMuted(true);
        }
         */
        this.currentSystem.getAudioRenderer().setVolume(50);
    }

    void onShutdown() throws Exception {
        try {
            this.emulatorThread.interrupt();
            this.emulatorThread.join();
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
    }

}
