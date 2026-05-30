package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.DefaultSystemVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.MonoAudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.StereoAudioRenderer;
import net.java.games.input.Event;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class AbstractSystemAdapter implements SystemAdapter {

    private final byte[] rom;
    private final Path path;

    private final Emulator emulator;
    private final DefaultSystemVideoDriver videoDriver;
    private final DefaultAudioRendererDriver audioDriver;
    private final AudioRenderer audioRenderer;

    public AbstractSystemAdapter(CoreInitializer initializer) {
        Optional<byte[]> rawRomOptional = initializer.getRawRom();
        Optional<Path> romPathOptional = initializer.getRomPath();
        if (rawRomOptional.isEmpty() || romPathOptional.isEmpty()) {
            throw new EmulatorException("Must select a ROM file before starting emulation!");
        }
        byte[] rom = rawRomOptional.get();
        this.rom = Arrays.copyOf(rom, rom.length);
        this.path = romPathOptional.get();

        this.emulator = this.createEmulator();

        KeyAdapter keyAdapter = new KeyAdapter() {

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

        JoypadAdapter joypadAdapter = new JoypadAdapter(this);

        this.videoDriver = new DefaultSystemVideoDriver(this.emulator.getVideoGenerator(), keyAdapter, joypadAdapter);
        this.audioDriver = this.emulator.getAudioGenerator().isStereo()
                ? new StereoAudioRendererDriver(this.emulator.getAudioGenerator(), new StereoAudioRenderer(this.emulator.getFramerate()))
                : new MonoAudioRendererDriver(this.emulator.getAudioGenerator(), new MonoAudioRenderer(this.emulator.getFramerate()));
        this.audioRenderer = this.audioDriver.getAudioRenderer();
    }

    protected abstract Emulator createEmulator();

    @Nullable
    protected abstract SystemController.Action getActionForKeyCode(int keyCode);

    @Nullable
    protected abstract SystemController.Action getActionForJoypadEvent(Event e);

    @Override
    public byte[] getRom() {
        return Arrays.copyOf(this.rom, this.rom.length);
    }

    @Override
    public Path getRomPath() {
        return this.path;
    }

    @Override
    public Emulator getEmulator() {
        return this.emulator;
    }

    @Override
    public Optional<VideoDriver> getVideoDriver() {
        return Optional.of(this.videoDriver);
    }

    @Override
    public Optional<? extends DefaultAudioRendererDriver> getAudioDriver() {
        return Optional.of(this.audioDriver);
    }

    public DefaultSystemVideoDriver getJPanelVideoDriver() {
        return this.videoDriver;
    }

    public AudioRenderer getAudioRenderer() {
        return this.audioRenderer;
    }

    public void onFrame() {
        this.videoDriver.requestFrame();
        this.audioDriver.onFrame();
    }

    @Override
    public void close() throws IOException {
        if (this.videoDriver != null) {
            this.videoDriver.close();
        }
        if (this.audioDriver != null) {
            this.audioDriver.close();
        }
        if (this.emulator != null) {
            try {
                this.emulator.close();
            } catch (Exception e) {
                Logger.error("Error attempting to release %s emulator resources: {}".formatted(this.getSystemName()), e);
            }
        }
    }

}
