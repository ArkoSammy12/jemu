package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.DefaultSystemVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.managers.SystemManager;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.Resetable;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.core.SpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.core.VideoSettingChangedEvent;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class SystemAdapter implements SystemHost, Closeable {

    protected final Jemu jemu;
    private final System system;
    protected final SystemManager systemManager;

    protected byte[] rom;
    private Path path;

    private Emulator emulator;
    private DefaultAudioRendererDriver audioDriver;

    @Nullable
    private DefaultSystemVideoDriver videoDriver;

    public SystemAdapter(Jemu jemu, System system, SystemManager systemManager) throws LineUnavailableException {
        this.jemu = jemu;
        this.system = system;
        this.systemManager = systemManager;
    }

    public System getSystem() {
        return this.system;
    }

    @Override
    public String getSystemName() {
        return this.system.getName();
    }

    public Optional<Emulator> getEmulator() {
        return Optional.ofNullable(this.emulator);
    }

    @Override
    public Optional<byte[]> getRom() {
        return Optional.ofNullable(this.rom).map(rom -> Arrays.copyOf(rom, rom.length));
    }

    @Override
    public Optional<Path> getRomPath() {
        return Optional.ofNullable(this.path);
    }

    @Override
    public Optional<VideoDriver> getVideoDriver() {
        return Optional.ofNullable(this.videoDriver);
    }

    @Override
    public Optional<? extends DefaultAudioRendererDriver> getAudioDriver() {
        return Optional.of(this.audioDriver);
    }


    public void onFrame() {
        if (this.videoDriver != null) {
            this.videoDriver.requestFrame();
        }
        if (this.audioDriver != null) {
            this.audioDriver.onFrame();
        }
    }

    public void onCoreSettingEvent(CoreSettingChangeEvent coreSettingChangeEvent) throws LineUnavailableException {
        switch (coreSettingChangeEvent) {
            case SpeedModeSettingChangedEvent speedModeSettingChangedEvent -> {
                this.audioDriver.clearAudioBuffer();
                this.jemu.getAudioEngine().setFramerate(speedModeSettingChangedEvent.getSpeedMode().scaleFramerate(emulator.getFramerate()));
            }
            case VideoSettingChangedEvent videoSettingChangedEvent -> {
                DefaultSystemVideoDriver videoDriver = this.videoDriver;
                if (videoDriver != null) {
                    videoDriver.onVideoSettingChangedEvent(videoSettingChangedEvent);
                }
            }
            case null, default -> {}
        }
    }

    public void powerCycle(EmulatorInitializer emulatorInitializer) throws LineUnavailableException {
        this.initialize(emulatorInitializer, false);
    }

    public void reset(EmulatorInitializer emulatorInitializer) throws LineUnavailableException {
        this.initialize(emulatorInitializer, true);
    }

    protected abstract Emulator createEmulator();

    @Nullable
    protected abstract SystemController.Action getActionForKeyCode(int keyCode);

    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        Optional<byte[]> rawRomOptional = initializer.getRomImage();
        if (rawRomOptional.isPresent()) {
            byte[] rom = rawRomOptional.get();
            this.rom = Arrays.copyOf(rom, rom.length);
        } else {
            this.rom = null;
        }
        this.path = initializer.getRomPath().orElse(null);

        if (tryReset && this.emulator instanceof Resetable resetableEmulator) {
            resetableEmulator.reset();
        } else {
            this.emulator = this.createEmulator();
        }

        if (this.emulator != null) {
            if (this.audioDriver != null) {
                this.audioDriver.close();
            }
            this.audioDriver = this.emulator.getAudioGenerator().isStereo() ? new StereoAudioRendererDriver(this.jemu, this.emulator) : new MonoAudioRendererDriver(jemu, this.emulator);
        }

        KeyListener keyListener = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                SystemController.Action action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().map(Emulator::getSystemController).ifPresent(systemController -> systemController.onActionPressed(action));
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                SystemController.Action action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().map(Emulator::getSystemController).ifPresent(systemController -> systemController.onActionReleased(action));
                }
            }

        };

        this.jemu.getMainWindow().getSystemViewport().setSystemDisplay(() -> {
            if (this.videoDriver != null) {
                this.videoDriver.close();
            }
            this.videoDriver = new DefaultSystemVideoDriver(this.jemu, this.emulator.getVideoGenerator());
            return this.videoDriver;
        });
        this.jemu.getMainWindow().getSystemViewport().setSystemKeyListener(keyListener);
    }

    @Override
    public void close() {
        if (this.videoDriver != null) {
            this.videoDriver.close();
        }
        if (this.audioDriver != null) {
            this.audioDriver.close();
        }
        try {
            if (this.emulator != null) {
                this.emulator.close();
            }
        } catch (Exception e) {
            Logger.error("Error attempting to release %s emulator resources: {}".formatted(this.getSystemName()), e);
        }
    }

    public static byte[] readRawRom(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new EmulatorException("Failed to read ROM file from path: " + path, e);
        }
    }

}
