package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.JPanelVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import io.github.arkosammy12.jemu.core.nes.NESController;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.MonoAudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.StereoAudioRenderer;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Optional;

public class DefaultNESAdapter extends DefaultSystemAdapter {

    private final String romTitle;
    private final System system;

    private final Emulator emulator;
    private final JPanelVideoDriver videoDriver;
    private final DefaultAudioRendererDriver audioDriver;
    private final AudioRenderer audioRenderer;

    public DefaultNESAdapter(CoreInitializer initializer) {
        super(initializer);

        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = System.NES;

        this.emulator = new NESEmulator(this);

        KeyAdapter keyAdapter = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                NESController.Actions action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionPressed(action);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                NESController.Actions action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionReleased(action);
                }
            }

        };


        this.videoDriver = new JPanelVideoDriver(this.emulator.getVideoGenerator(), keyAdapter);

        int framerate = this.emulator.getFramerate();
        boolean isStereo = this.emulator.getAudioGenerator().isStereo();

        this.audioDriver = isStereo
                ? new StereoAudioRendererDriver(this.emulator.getAudioGenerator(), new StereoAudioRenderer(framerate))
                : new MonoAudioRendererDriver(this.emulator.getAudioGenerator(), new MonoAudioRenderer(framerate));
        this.audioRenderer = this.audioDriver.getAudioRenderer();
    }

    @Override
    public JPanelVideoDriver getJPanelVideoDriver() {
        return this.videoDriver;
    }

    @Override
    public AudioRenderer getAudioRenderer() {
        return this.audioRenderer;
    }

    @Override
    public String getSystemName() {
        return this.system.getName();
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<? extends VideoDriver> getVideoDriver() {
        return Optional.ofNullable(this.videoDriver);
    }

    @Override
    public Optional<? extends DefaultAudioRendererDriver> getAudioDriver() {
        return Optional.ofNullable(this.audioDriver);
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    @Override
    public Emulator getEmulator() {
        return this.emulator;
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
                Logger.error("Error closing NES emulator resources: {}", e);
            }
        }
    }


    @Nullable
    private NESController.Actions getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_W -> NESController.Actions.UP;
            case KeyEvent.VK_S -> NESController.Actions.DOWN;
            case KeyEvent.VK_A -> NESController.Actions.LEFT;
            case KeyEvent.VK_D -> NESController.Actions.RIGHT;
            case KeyEvent.VK_ENTER -> NESController.Actions.START;
            case KeyEvent.VK_BACK_SPACE -> NESController.Actions.SELECT;
            case KeyEvent.VK_J -> NESController.Actions.A;
            case KeyEvent.VK_K -> NESController.Actions.B;
            default -> null;
        };
    }

}
