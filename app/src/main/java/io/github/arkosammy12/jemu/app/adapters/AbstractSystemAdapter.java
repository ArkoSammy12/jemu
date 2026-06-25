package io.github.arkosammy12.jemu.app.adapters;

import de.gurkenlabs.input4j.InputComponent;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.DefaultSystemVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.Resetable;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
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
    //private JoypadDriver joypadDriver;
    private KeyListener keyListener;

    @Nullable
    private DefaultSystemVideoDriver videoDriver;

    public AbstractSystemAdapter(Jemu jemu, EmulatorInitializer initializer) throws LineUnavailableException {
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

    public void onFrame() {
        if (this.videoDriver != null) {
            this.videoDriver.requestFrame();
        }
        if (this.audioDriver != null) {
            this.audioDriver.onFrame();
        }
        //this.joypadDriver.poll();
    }

    public void reset(EmulatorInitializer emulatorInitializer) throws LineUnavailableException {
        this.initialize(this.jemu, emulatorInitializer, true);
    }

    protected void initialize(Jemu jemu, EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
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

        //this.joypadDriver = new JoypadDriver(this);
        if (this.emulator != null) {
            this.audioDriver = this.emulator.getAudioGenerator().isStereo() ? new StereoAudioRendererDriver(jemu, this.emulator) : new MonoAudioRendererDriver(jemu, this.emulator);
        }

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

        jemu.getMainWindow().getSystemViewport().setSystemDisplay(() -> {
            this.videoDriver = new DefaultSystemVideoDriver(this.emulator.getVideoGenerator());
            return this.videoDriver;
        });
        jemu.getMainWindow().getSystemViewport().setSystemKeyListener(this.keyListener);
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
            //this.joypadDriver.close();
            if (this.emulator != null) {
                this.emulator.close();
            }
        } catch (Exception e) {
            Logger.error("Error attempting to release %s emulator resources: {}".formatted(this.getSystemName()), e);
        }
    }

}
