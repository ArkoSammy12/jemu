package io.github.arkosammy12.jemu.app;

import io.github.arkosammy12.jemu.app.adapters.AbstractSystemAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.CLIArgs;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.GitProperties;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.audio.AudioChannels;
import io.github.arkosammy12.jemu.frontend.audio.AudioEngine;
import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.gui.swing.PendingEmulatorCommand;
import io.github.arkosammy12.jemu.frontend.gui.swing.commands.*;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.MuteEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.VolumeChangedEvent;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.HelpManager;
import net.harawata.appdirs.AppDirsFactory;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public final class Jemu {

    private final MainWindow mainWindow;
    private final AudioEngine audioEngine;
    private final Path appDataDirectory;

    private final Thread emulatorCommandListenerThread;
    private final Thread uiEventListenerThread;

    private volatile AbstractSystemAdapter currentSystem = null;
    private volatile State currentState = State.STOPPED;

    private final Object systemLock = new Object();
    private volatile boolean running;
    private volatile boolean shutdownStarted;

    public Jemu(@Nullable CLIArgs cliArgs) throws Exception {
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                Logger.error(throwable, "Uncaught exception in thread {}", thread.getName());
                this.shutdown();
            });

            this.appDataDirectory = this.tryAcquireAndCreateDataDirectory();

            this.mainWindow = new MainWindow(MavenProperties.ARTIFACT_ID, this.getAppDataDirectory().orElse(null), Arrays.stream(System.values()).toList());
            this.initMainWindow();

            this.audioEngine = new AudioEngine("%s-audio-thread".formatted(MavenProperties.ARTIFACT_ID));
            this.initAudioEngine();

            this.emulatorCommandListenerThread = new Thread(this::emulatorCommandListenerLoop, "%s-emulator-command-listener-thread".formatted(MavenProperties.ARTIFACT_ID));
            this.uiEventListenerThread = new Thread(this::eventListenerLoop, "%s-event-listener-thread".formatted(MavenProperties.ARTIFACT_ID));

            if (cliArgs != null) {
                cliArgs.getRomPath().ifPresent(romPath -> this.mainWindow.getFileManager().loadFile(romPath));
                cliArgs.getSystem().ifPresent(system -> {
                    this.mainWindow.getEmulatorManager().setCurrentSystemDescriptor(system);
                    this.mainWindow.submitEmulatorCommand(new PowerCycleCommand(system, false));
                });
            }

            this.mainWindow.show();
        } catch (Exception e) {
            Logger.error("Failed to initialize %s: ".formatted(MavenProperties.ARTIFACT_ID), e);
            this.shutdown();
            throw new RuntimeException(e);
        }
    }

    public AudioEngine getAudioEngine() {
        return this.audioEngine;
    }

    private Optional<Path> getAppDataDirectory() {
        return Optional.ofNullable(this.appDataDirectory);
    }

    public Optional<Path> getSavesDirectory() {
        return Optional.ofNullable(this.appDataDirectory).map(path -> path.resolve("saves"));
    }

    public void start() {
        this.running = true;
        if (this.uiEventListenerThread != null) {
            this.uiEventListenerThread.start();
        }
        if (this.emulatorCommandListenerThread != null) {
            this.emulatorCommandListenerThread.start();
        }
    }

    private void eventListenerLoop() {
        while (this.running) {
            try {
                Event uiEvent = this.mainWindow.waitEvent();
                switch (uiEvent) {
                    case MuteEvent(boolean mute) -> this.audioEngine.setMuted(mute);
                    case VolumeChangedEvent(int newVolume) -> this.audioEngine.setVolume(newVolume);
                    case SampleRateChangedEvent(SampleRate sampleRate) -> this.audioEngine.setSampleRate(sampleRate);
                    case null, default -> {}
                }
            } catch (InterruptedException _) {

            } catch (Exception e) {
                Logger.error("Unexpected error in event listener loop: {}", e);
            }
        }
    }

    private void emulatorCommandListenerLoop() {
        while (this.running) {
            try {
                PendingEmulatorCommand enqueuedEmulatorCommand = this.mainWindow.waitEmulatorCommand();
                if (enqueuedEmulatorCommand == null) {
                    continue;
                }
                synchronized (this.systemLock) {
                    State enqueuedState;
                    try {
                        enqueuedState = switch (enqueuedEmulatorCommand.getEmulatorCommand()) {
                            case PowerCycleCommand powerCycleCommand -> this.onEmulatorPowerCycleCommand(powerCycleCommand);
                            case ResetEmulatorCommand resetEmulatorCommand -> this.onEmulatorResetCommand(resetEmulatorCommand);
                            case StopEmulatorCommand _ -> this.onEmulatorStopCommand();
                            case PauseEmulatorCommand pauseEmulatorCommand -> this.onEmulatorPauseCommand(pauseEmulatorCommand);
                            case StepFrameEmulatorCommand _ -> this.onEmulatorStepFrameCommand();
                            case StepCycleEmulatorCommand _ -> this.onEmulatorStepCycleCommand();
                            case null -> null;
                        };
                    } finally {
                        enqueuedEmulatorCommand.acknowledge();
                    }
                    if (enqueuedState == null) {
                        continue;
                    }
                    this.currentState = enqueuedState;
                }
            } catch (EmulatorException e) {
                Logger.error("Error initializing emulator: {}", e);
                this.onEmulatorException(e);
            } catch (InterruptedException _) {

            } catch (Exception e) {
                Logger.error("Unexpected error while processing emulator command: {}", e);
                this.onEmulatorException(new EmulatorException("Unexpected error while processing emulator command!", e));
            }
        }
    }

    private void onEmulatorIdle() {
    }

    private void onEmulatorRunning() {
        if (currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().executeFrame();
        this.mainWindow.getTitleManager().update(this.currentSystem.getRomTitle().orElse("No title"));
    }

    private void onEmulatorSteppingFrame() {
        if (this.currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().executeFrame();
        this.currentState = State.PAUSED;
    }

    private void onEmulatorSteppingCycle() {
        if (this.currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().executeCycle();
        this.currentState = State.PAUSED;
    }

    private State onEmulatorPowerCycleCommand(PowerCycleCommand powerCycleCommand) throws Exception {
        this.initializeEmulator(powerCycleCommand.systemDescriptor() instanceof System system ? system : null);
        boolean powerCycleIntoPaused = powerCycleCommand.powerCycleIntoPaused();
        this.audioEngine.setPaused(powerCycleIntoPaused);
        return powerCycleIntoPaused ? State.PAUSED : State.RUNNING;
    }

    private State onEmulatorResetCommand(ResetEmulatorCommand resetEmulatorCommand) throws Exception {
        if (this.currentSystem == null) {
            return null;
        }
        Optional<SystemDescriptor> currentSystemDescriptor = resetEmulatorCommand.getSystemDescriptor();
        if (currentSystemDescriptor.isPresent() && currentSystemDescriptor.get() instanceof System system && system != this.currentSystem.getSystem()) {
            this.initializeEmulator(system);
        } else {
            this.currentSystem.reset(this.createEmulatorInitializer(this.currentSystem.getSystem()));
        }
        boolean resetIntoPaused = resetEmulatorCommand.resetIntoPaused();
        this.audioEngine.setPaused(resetIntoPaused);
        return resetIntoPaused ? State.PAUSED : State.RUNNING;
    }

    private State onEmulatorStopCommand() {
        if (this.currentSystem != null) {
            this.currentSystem.close();
            this.currentSystem = null;
        }
        this.audioEngine.stop();
        this.audioEngine.setPaused(true);
        return State.STOPPED;
    }

    private State onEmulatorPauseCommand(PauseEmulatorCommand pauseEmulatorCommand) {
        boolean stopped = this.currentSystem == null;
        if (pauseEmulatorCommand.pause()) {
            this.audioEngine.setPaused(true);
            return stopped ? State.PAUSE_STOPPED : State.PAUSED;
        } else {
            if (stopped) {
                return State.STOPPED;
            } else {
                this.audioEngine.setPaused(false);
                return State.RUNNING;
            }
        }
    }

    private State onEmulatorStepFrameCommand() {
        return State.STEPPING_FRAME;
    }

    private State onEmulatorStepCycleCommand() {
        return State.STEPPING_CYCLE;
    }

    private Path tryAcquireAndCreateDataDirectory() {
        Path appDataDirectory = null;
        try {
            appDataDirectory = Path.of(AppDirsFactory.getInstance().getUserDataDir(MavenProperties.ARTIFACT_ID, null, null));
        } catch (Exception e) {
            Logger.error("Error obtaining data directory path: {}", e);
        }

        if (appDataDirectory != null && (!Files.exists(appDataDirectory) || !Files.isDirectory(appDataDirectory))) {
            try {
                Files.createDirectory(appDataDirectory);
            } catch (Exception e) {
                Logger.error("Failed to create app data directory!", e);
            }
        }
        return appDataDirectory;
    }

    private void initMainWindow() {
        this.mainWindow.setClosingHook(this::shutdown);
        HelpManager helpManager = this.mainWindow.getHelpManager();
        helpManager.setProjectName(MavenProperties.ARTIFACT_ID);
        helpManager.setAuthorString(MavenProperties.AUTHOR);
        helpManager.setVersionString(MavenProperties.VERSION);
        helpManager.setCommitIDString(GitProperties.COMMIT_ID);
        helpManager.setBuildDateString(MavenProperties.BUILD_DATE);
        helpManager.setProjectSourceLink("https://github.com/ArkoSammy12/jemu");
        helpManager.setProjectBugReportLink("https://github.com/ArkoSammy12/jemu/issues");
    }

    private void initAudioEngine() {
        this.audioEngine.setSampleFrameCallback(() -> {
            try {
                synchronized (this.systemLock) {
                    if (this.currentSystem == null) {
                        return null;
                    }

                    switch (this.currentState) {
                        case STOPPED, PAUSED, PAUSE_STOPPED -> this.onEmulatorIdle();
                        case RUNNING -> this.onEmulatorRunning();
                        case STEPPING_FRAME -> this.onEmulatorSteppingFrame();
                        case STEPPING_CYCLE -> this.onEmulatorSteppingCycle();
                    }

                    this.currentSystem.onFrame();
                    return this.currentSystem.getAudioDriver().map(DefaultAudioRendererDriver::getSampleFrame).orElse(null);
                }
            } catch (Exception e) {
                Logger.error("Unexpected error while running emulator: {}", e);
                this.onEmulatorException(new EmulatorException("Unexpected error while running emulator!", e));
                return null;
            }
        });
    }

    private void onEmulatorException(Exception e) {
        this.mainWindow.showCoreError(e);
        synchronized (this.systemLock) {
            if (this.currentSystem != null) {
                try {
                    this.currentSystem.close();
                } catch (Exception _) {
                }
                this.mainWindow.getSystemViewport().setSystemDisplay(null);
                this.mainWindow.getSystemViewport().setSystemKeyListener(null);
                this.currentSystem = null;
            }
        }
        this.mainWindow.submitEmulatorCommand(new StopEmulatorCommand());
    }

    private void initializeEmulator(System system) throws Exception {
        if (this.currentSystem != null) {
            this.currentSystem.close();
        }

        this.currentSystem = System.getSystemAdapter(this, this.createEmulatorInitializer(system));
        this.audioEngine.setFramerate(this.currentSystem.getEmulator().getFramerate());
        this.audioEngine.setAudioChannels(this.currentSystem.getEmulator().getAudioGenerator().isStereo() ? AudioChannels.STEREO : AudioChannels.MONO);
        this.audioEngine.start();
        this.mainWindow.getSystemViewport().setSystemDisplay(() -> this.currentSystem.createAWTComponentVideoDriver());
        this.mainWindow.getSystemViewport().setSystemKeyListener(this.currentSystem.getSystemKeyListener());
    }

    private EmulatorInitializer createEmulatorInitializer(@Nullable System system) {
        return new EmulatorInitializer() {

            @Override
            public Optional<Path> getRomPath() {
                return mainWindow.getFileManager().getSelectedRomPath();
            }

            @Override
            public Optional<byte[]> getRawRom() {
                return this.getRomPath().map(SystemAdapter::readRawRom);
            }

            @Override
            public Optional<System> getSystem() {
                return Optional.ofNullable(system);
            }

        };
    }

    private void shutdown() {
        this.running = false;
        if (this.shutdownStarted) {
            return;
        }
        this.shutdownStarted = true;

        try {
            if (this.audioEngine != null) {
                this.audioEngine.close();
            }

            if (this.mainWindow != null) {
                this.mainWindow.close();
            }

            this.emulatorCommandListenerThread.interrupt();
            tryJoinSafely(this.emulatorCommandListenerThread);

            this.uiEventListenerThread.interrupt();
            tryJoinSafely(this.uiEventListenerThread);

            synchronized (this.systemLock) {
                if (this.currentSystem != null) {
                    this.currentSystem.close();
                    this.currentSystem = null;
                }
            }
        } catch (Throwable t) {
            this.forceShutdown(t);
        }
    }

    private void forceShutdown(@Nullable Throwable t) {
        if (t == null) {
            Logger.error("Forcing shutdown...");
        } else {
            Logger.error(t, "Forcing shutdown because of {}", t.getCause());
        }
        java.lang.System.exit(1);
    }

    public static void tryJoinSafely(@Nullable Thread thread) {
        tryJoinSafely(thread, 0);
    }

    public static void tryJoinSafely(@Nullable Thread thread, int timeout) {
        if (thread != null && !Thread.currentThread().equals(thread) && thread.isAlive()) {
            try {
                thread.join(timeout);
            } catch (InterruptedException e) {}
        }
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