package io.github.arkosammy12.jemu.app;

import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.io.CLIArgs;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.util.HelpDialog;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.frontend.events.core.VideoSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.audio.AudioEngine;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.commands.*;
import io.github.arkosammy12.jemu.frontend.gui.PendingEmulatorCommand;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.managers.HelpManager;
import net.harawata.appdirs.AppDirsFactory;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Jemu {

    private final SystemRegistry systemRegistry;
    private final MainWindow mainWindow;
    private final AudioEngine audioEngine;
    private final Path appDataDirectory;

    private final Thread coreThread;
    private final Thread uiEventListenerThread;

    private volatile SystemAdapter currentSystem = null;
    private volatile State currentState = State.STOPPED;

    private final Object systemLock = new Object();
    private volatile boolean running;
    private volatile boolean shutdownStarted;

    public Jemu(String[] args) throws Exception {
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                Logger.error(throwable, "Uncaught exception in thread {}", thread.getName());
                this.shutdown();
            });

            this.systemRegistry = new SystemRegistry(this);

            CLIArgs cliArgs = null;
            if (args.length > 0) {
                cliArgs = new CLIArgs(args, this.systemRegistry);
                if (cliArgs.exitImmediately()) {
                    System.exit(0);
                }
            }

            this.appDataDirectory = this.tryAcquireAndCreateDataDirectory();

            this.mainWindow = new MainWindow(MavenProperties.ARTIFACT_ID, this.getAppDataDirectory().orElse(null), this.systemRegistry);
            this.initMainWindow();

            this.audioEngine = new AudioEngine("%s-audio-callback-thread".formatted(MavenProperties.ARTIFACT_ID));
            this.initAudioEngine();

            this.coreThread = new Thread(this::coreLoop, "%s-core-thread".formatted(MavenProperties.ARTIFACT_ID));
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
            Logger.error(e, "Failed to initialize %s: ".formatted(MavenProperties.ARTIFACT_ID));
            this.shutdown();
            throw new RuntimeException(e);
        }
    }

    public MainWindow getMainWindow() {
        return this.mainWindow;
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
        if (this.coreThread != null) {
            this.coreThread.start();
        }
    }

    private void eventListenerLoop() {
        while (this.running) {
            try {
                Event event = this.mainWindow.waitEvent();
                if (!this.running) {
                    break;
                }
                if (event instanceof CoreSettingChangeEvent coreSettingChangeEvent) {
                    this.systemRegistry.onCoreSettingChangedEvent(coreSettingChangeEvent);
                    SystemAdapter currentSystem = this.currentSystem;
                    if (currentSystem != null) {
                        currentSystem.onCoreSettingChangedEvent(coreSettingChangeEvent);
                    }
                }
                if (event instanceof AudioSettingChangeEvent audioSettingChangeEvent) {
                    this.audioEngine.onAudioSettingChanged(audioSettingChangeEvent);
                }
                if (event instanceof VideoSettingChangedEvent videoSettingChangedEvent) {
                    SystemAdapter currentSystem = this.currentSystem;
                    if (currentSystem != null) {
                        currentSystem.getVideoDriver().ifPresent(videoDriver -> videoDriver.onVideoSettingChangedEvent(videoSettingChangedEvent));
                    }
                }
            } catch (InterruptedException _) {

            } catch (Exception e) {
                Logger.error("Unexpected error in event listener loop: {}", e);
            }
        }
    }

    private void coreLoop() {
        while (this.running) {
            try {
                this.updateState(this.currentSystem == null);
                if (!this.running) {
                    break;
                }
                synchronized (this.systemLock) {
                    switch (this.currentState) {
                        case STOPPED, PAUSED, PAUSE_STOPPED -> this.onEmulatorIdle();
                        case RUNNING -> this.onEmulatorRunning();
                        case STEPPING_FRAME -> this.onEmulatorSteppingFrame();
                        case STEPPING_CYCLE -> this.onEmulatorSteppingCycle();
                    }
                    if (this.currentSystem != null) {
                        this.currentSystem.onFrame();
                    }
                }
            } catch (EmulatorException e) {
                Logger.error("Error initializing or running emulator: {}", e);
                this.onEmulatorException(e);
            } catch (InterruptedException _) {

            } catch (Exception e) {
                Logger.error("Unexpected error while initializing or running emulator: {}", e);
                this.onEmulatorException(new EmulatorException("Unexpected error while initializing or running emulator!", e));
            }
        }
    }

    private void updateState(boolean take) throws Exception {
        PendingEmulatorCommand enqueuedEmulatorCommand = take ? this.mainWindow.waitEmulatorCommand() : this.mainWindow.pollEmulatorCommand();
        if (!this.running) {
            return;
        }
        if (enqueuedEmulatorCommand == null) {
            return;
        }
        State enqueuedState;
        try {
            synchronized (this.systemLock) {
                enqueuedState = switch (enqueuedEmulatorCommand.getEmulatorCommand()) {
                    case PowerCycleCommand powerCycleCommand -> this.onEmulatorPowerCycleCommand(powerCycleCommand);
                    case ResetEmulatorCommand resetEmulatorCommand -> this.onEmulatorResetCommand(resetEmulatorCommand);
                    case StopEmulatorCommand _ -> this.onEmulatorStopCommand();
                    case PauseEmulatorCommand pauseEmulatorCommand -> this.onEmulatorPauseCommand(pauseEmulatorCommand);
                    case StepFrameEmulatorCommand _ -> this.onEmulatorStepFrameCommand();
                    case StepCycleEmulatorCommand _ -> this.onEmulatorStepCycleCommand();
                    case null -> null;
                };
            }
        } finally {
            enqueuedEmulatorCommand.acknowledge();
        }
        if (enqueuedState == null) {
            return;
        }
        this.currentState = enqueuedState;
    }

    private void onEmulatorIdle() {
    }

    private void onEmulatorRunning() {
        if (this.currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().ifPresent(Emulator::executeFrame);
        this.mainWindow.getTitleManager().update(this.currentSystem.getProgramTitle().orElse("No title"));
    }

    private void onEmulatorSteppingFrame() {
        if (this.currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().ifPresent(Emulator::executeFrame);
        this.currentState = State.PAUSED;
    }

    private void onEmulatorSteppingCycle() {
        if (this.currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().ifPresent(Emulator::executeCycle);
        this.currentState = State.PAUSED;
    }

    private State onEmulatorPowerCycleCommand(PowerCycleCommand powerCycleCommand) throws Exception {
        this.initializeEmulator(powerCycleCommand.systemDescriptor() instanceof SystemManager systemManager ? systemManager : null);
        boolean powerCycleIntoPaused = powerCycleCommand.powerCycleIntoPaused();
        this.audioEngine.setPaused(powerCycleIntoPaused);
        return powerCycleIntoPaused ? State.PAUSED : State.RUNNING;
    }

    private State onEmulatorResetCommand(ResetEmulatorCommand resetEmulatorCommand) throws Exception {
        if (this.currentSystem == null) {
            return null;
        }
        Optional<SystemDescriptor> currentSystemDescriptor = resetEmulatorCommand.getSystemDescriptor();
        if (currentSystemDescriptor.isPresent() && currentSystemDescriptor.get() instanceof SystemManager systemManger && !systemManger.manages(this.currentSystem)) {
            this.initializeEmulator(systemManger);
        } else {
            this.currentSystem.reset(this.createEmulatorInitializer());
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

    private void initializeEmulator(@Nullable SystemManager systemManager) throws Exception {
        if (this.currentSystem != null) {
            this.currentSystem.close();
        }
        if (systemManager == null) {
            throw new EmulatorException("Must select a system!");
        }
        this.currentSystem = systemManager.createSystem();
        this.currentSystem.powerCycle(this.createEmulatorInitializer());
        this.audioEngine.start();
    }

    private EmulatorInitializer createEmulatorInitializer() {
        return new EmulatorInitializer() {

            @Override
            public Optional<Path> getRomPath() {
                return mainWindow.getFileManager().getSelectedRomPath();
            }

            @Override
            public Optional<byte[]> getRomImage() {
                return this.getRomPath().map(SystemAdapter::readRawRom);
            }

        };
    }

    private Path tryAcquireAndCreateDataDirectory() {
        Path appDataDirectory = null;
        try {
            appDataDirectory = Paths.get(AppDirsFactory.getInstance().getUserDataDir(MavenProperties.ARTIFACT_ID, null, null));
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
        helpManager.setHelpDialogContentsSupplier(HelpDialog::new);
        helpManager.setProjectSourceLink("https://github.com/ArkoSammy12/jemu");
        helpManager.setProjectBugReportLink("https://github.com/ArkoSammy12/jemu/issues");

        List<String> iconPaths = List.of(
                "/icons/jemu_icon_16.png",
                "/icons/jemu_icon_32.png",
                "/icons/jemu_icon_64.png",
                "/icons/jemu_icon_128.png"
        );
        List<Image> icons = new ArrayList<>();
        for (String path : iconPaths) {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                Logger.error("Missing bundled icon resource: {}", path);
                return;
            }
            try (stream) {
                icons.add(ImageIO.read(stream));
            } catch (IOException e) {
                Logger.error(e, "Failed to read icon: {}", path);
                return;
            }
        }
        this.mainWindow.setIcons(icons);
    }

    private void initAudioEngine() throws LineUnavailableException {
        this.audioEngine.soundDevice(this.mainWindow.getConfigurations().getSettings().getAudioSettings().getSoundDevice().orElse(null));
        this.audioEngine.setSampleRate(this.mainWindow.getConfigurations().getSettings().getAudioSettings().getSampleRate());
        this.audioEngine.setMuted(this.mainWindow.getConfigurations().getSettings().getAudioSettings().getMute());
        this.audioEngine.setVolume(this.mainWindow.getConfigurations().getSettings().getAudioSettings().getVolume());
    }

    private void onEmulatorException(Exception e) {
        this.mainWindow.showCoreError(e);
        this.mainWindow.getSystemViewport().setSystemDisplay(null);
        this.mainWindow.getSystemViewport().setSystemKeyListener(null);
        this.mainWindow.submitEmulatorCommand(new StopEmulatorCommand());
        synchronized (this.systemLock) {
            if (this.currentSystem != null) {
                try {
                    this.currentSystem.close();
                } catch (Exception _) {}
                this.currentSystem = null;
            }
        }
    }

    private void shutdown() {
        this.running = false;
        if (this.shutdownStarted) {
            return;
        }
        this.shutdownStarted = true;

        try {
            AudioEngine audioEngine = this.audioEngine;
            if (audioEngine != null) {
                audioEngine.close();
            }

            MainWindow mainWindow = this.mainWindow;
            if (mainWindow != null) {
                mainWindow.close();
            }

            Thread coreThread = this.coreThread;
            if (coreThread != null) {
                coreThread.interrupt();
                tryJoinSafely(coreThread);
            }

            Thread uiEventListenerThread = this.uiEventListenerThread;
            if (uiEventListenerThread != null) {
                uiEventListenerThread.interrupt();
                tryJoinSafely(uiEventListenerThread);
            }

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
        System.exit(1);
    }

    public static void tryJoinSafely(@Nullable Thread thread) {
        tryJoinSafely(thread, 0);
    }

    public static void tryJoinSafely(@Nullable Thread thread, int timeout) {
        if (thread != null && !Thread.currentThread().equals(thread) && thread.isAlive()) {
            try {
                thread.join(timeout);
            } catch (InterruptedException _) {}
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