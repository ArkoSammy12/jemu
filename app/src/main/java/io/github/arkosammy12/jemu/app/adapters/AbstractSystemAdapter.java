package io.github.arkosammy12.jemu.app.adapters;

import de.gurkenlabs.input4j.InputComponent;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.DefaultSystemVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.JoypadDriver;
import io.github.arkosammy12.jemu.app.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.Resetable;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class AbstractSystemAdapter implements SystemAdapter {

    protected final Jemu jemu;

    private byte[] rom;
    private Path path;

    private Emulator emulator;
    private DefaultAudioRendererDriver audioDriver;
    private JoypadDriver joypadDriver;
    private KeyListener keyListener;

    @Nullable
    private DefaultSystemVideoDriver videoDriver;

    public AbstractSystemAdapter(Jemu jemu, EmulatorInitializer initializer) {
        this.jemu = jemu;
        this.initialize(jemu, initializer, false);
    }

    protected abstract Emulator createEmulator();

    @Nullable
    protected abstract SystemController.Action getActionForKeyCode(int keyCode);

    @Nullable
    public abstract SystemController.Action getActionForJoypadEvent(InputComponent.ID id);

    @Override
    public Optional<byte[]> getRom() {
        return Optional.ofNullable(this.rom).map(rom -> Arrays.copyOf(rom, rom.length));
    }

    @Override
    public Optional<Path> getRomPath() {
        return Optional.ofNullable(this.path);
    }

    @Override
    public Emulator getEmulator() {
        return this.emulator;
    }

    @Override
    public Optional<VideoDriver> getVideoDriver() {
        return Optional.ofNullable(this.videoDriver);
    }

    @Override
    public Optional<? extends DefaultAudioRendererDriver> getAudioDriver() {
        return Optional.of(this.audioDriver);
    }

    public KeyListener getSystemKeyListener() {
        return this.keyListener;
    }

    public Component createAWTComponentVideoDriver() {
        this.videoDriver = new DefaultSystemVideoDriver(this.emulator.getVideoGenerator());
        return this.videoDriver;
    }

    public void onFrame() {
        if (this.videoDriver != null) {
            this.videoDriver.requestFrame();
        }
        this.joypadDriver.poll();
    }

    public void reset(EmulatorInitializer emulatorInitializer) {
        this.initialize(this.jemu, emulatorInitializer, true);
    }

    protected void initialize(Jemu jemu, EmulatorInitializer initializer, boolean tryReset) {
        Optional<byte[]> rawRomOptional = initializer.getRawRom();
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

        this.joypadDriver = new JoypadDriver(this);
        Optional.ofNullable(this.emulator).map(Emulator::getAudioGenerator).ifPresent(audioGenerator -> {
            this.audioDriver = audioGenerator.isStereo() ? new StereoAudioRendererDriver(jemu, this.emulator.getAudioGenerator()) : new MonoAudioRendererDriver(jemu, this.emulator.getAudioGenerator());
        });

        this.keyListener = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                SystemController.Action action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionPressed(action);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                SystemController.Action action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionReleased(action);
                }
            }

        };
    }

    @Override
    public void close() {
        if (this.videoDriver != null) {
            this.videoDriver.close();
        }
        try {
            this.joypadDriver.close();
            if (this.emulator != null) {
                this.emulator.close();
            }
        } catch (Exception e) {
            Logger.error("Error attempting to release %s emulator resources: {}".formatted(this.getSystemName()), e);
        }
    }

}
