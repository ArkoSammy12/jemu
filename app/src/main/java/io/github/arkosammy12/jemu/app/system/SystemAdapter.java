package io.github.arkosammy12.jemu.app.system;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.DefaultSystemVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.exceptions.SystemRedirectException;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.Resetable;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.core.SpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.VideoSettingChangedEvent;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class SystemAdapter implements SystemHost, Closeable {

    protected final Jemu jemu;
    protected final SystemManager systemManager;

    protected byte @Nullable [] rom;

    @Nullable
    private Path path;

    @Nullable
    protected volatile Emulator emulator;

    @Nullable
    private volatile DefaultAudioRendererDriver audioDriver;

    @Nullable
    private volatile DefaultSystemVideoDriver videoDriver;

    public SystemAdapter(Jemu jemu, SystemManager systemManager) throws LineUnavailableException {
        this.jemu = jemu;
        this.systemManager = systemManager;
    }

    @Override
    public String getSystemName() {
        return this.systemManager.getName();
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
    public Optional<? extends DefaultSystemVideoDriver> getVideoDriver() {
        return Optional.ofNullable(this.videoDriver);
    }

    @Override
    public Optional<? extends DefaultAudioRendererDriver> getAudioDriver() {
        return Optional.ofNullable(this.audioDriver);
    }

    public void powerCycle(EmulatorInitializer emulatorInitializer) throws LineUnavailableException, SystemRedirectException {
        this.initialize(emulatorInitializer, false);
    }

    public void reset(EmulatorInitializer emulatorInitializer) throws LineUnavailableException, SystemRedirectException {
        this.initialize(emulatorInitializer, true);
    }

    public void onFrame() {
        DefaultSystemVideoDriver videoDriver = this.videoDriver;
        if (videoDriver != null) {
            videoDriver.requestFrame();
        }
        DefaultAudioRendererDriver audioDriver = this.audioDriver;
        if (audioDriver != null) {
            audioDriver.onFrame();
        }
    }

    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) throws LineUnavailableException {
        switch (coreSettingChangedEvent) {
            case SpeedModeSettingChangedEvent speedModeSettingChangedEvent -> {
                this.getAudioDriver().ifPresent(audioDriver -> audioDriver.onSpeedModeSettingChanged(speedModeSettingChangedEvent.getSpeedMode()));
                Emulator emulator = this.emulator;
                if (emulator != null) {
                    this.jemu.getAudioEngine().setFramerate(speedModeSettingChangedEvent.getSpeedMode().scaleFramerate(emulator.getFramerate()));
                }

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

        Emulator emulator = this.emulator;
        if (emulator != null) {
            this.getAudioDriver().ifPresent(DefaultAudioRendererDriver::close);
            this.audioDriver = emulator.getAudioGenerator().isStereo() ? new StereoAudioRendererDriver(jemu, emulator) : new MonoAudioRendererDriver(jemu, emulator);
        }

        this.jemu.getMainWindow().getSystemViewport().setSystemDisplay(() -> {
            this.getVideoDriver().ifPresent(DefaultSystemVideoDriver::close);
            this.videoDriver = null;

            Emulator emu = this.emulator;
            if (emu != null) {
                this.videoDriver = new DefaultSystemVideoDriver(this.jemu, emu.getVideoGenerator());
            }
            return Optional.ofNullable(this.videoDriver);
        });

        this.jemu.getMainWindow().getSystemViewport().setSystemKeyListener(new KeyAdapter() {

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

        });
    }

    @Override
    public void close() {
        DefaultSystemVideoDriver videoDriver = this.videoDriver;
        if (videoDriver != null) {
            videoDriver.close();
        }

        DefaultAudioRendererDriver audioDriver = this.audioDriver;
        if (audioDriver != null) {
            audioDriver.close();
        }
        try {
            Emulator emulator = this.emulator;
            if (emulator != null) {
                emulator.close();
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
