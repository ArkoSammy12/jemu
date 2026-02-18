package io.github.arkosammy12.jemu.application.adapters;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.application.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.application.drivers.JPanelVideoDriver;
import io.github.arkosammy12.jemu.application.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.application.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.application.util.System;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.backend.common.SystemHost;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import io.github.arkosammy12.jemu.backend.drivers.VideoDriver;
import io.github.arkosammy12.jemu.backend.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.backend.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.backend.gameboy.GameBoyJoypad;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.MonoAudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.StereoAudioRenderer;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Optional;

public class DefaultGameBoyAdapter extends DefaultSystemAdapter implements GameBoyHost {

    private final String romTitle;

    private static final int HEADER_TITLE_START = 0x0134;
    private static final int HEADER_TITLE_END = 0x0143;

    private final System system;
    private final Model model;

    private final GameBoyEmulator emulator;
    private final JPanelVideoDriver videoDriver;
    private final DefaultAudioRendererDriver audioDriver;
    private final AudioRenderer audioRenderer;

    public DefaultGameBoyAdapter(Jemu jemu, CoreInitializer initializer, Model model) {
        super(initializer);
        StringBuilder titleBuilder;
        String title = null;
        try {
            titleBuilder = new StringBuilder();
            int[] rom = SystemHost.byteToIntArray(this.getRom());
            for (int i = HEADER_TITLE_START; i <= HEADER_TITLE_END; i++) {
                int b = rom[i] & 0xFF;
                if (b == 0x00) {
                    break;
                }
                if (b >= 0x20 && b <= 0x7E) {
                    titleBuilder.append((char) b);
                }
            }
            title = titleBuilder.toString();
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.error("Failed to read ROM title from GameBoy cartridge header!", e);
        }
        this.romTitle = title != null ? title : initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = initializer.getSystem().orElse(System.GAME_BOY);
        this.model = model;

        KeyAdapter keyAdapter = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                GameBoyJoypad.Actions action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionPressed(action);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                GameBoyJoypad.Actions action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionReleased(action);
                }
            }

        };

        this.emulator = new GameBoyEmulator(this);
        this.videoDriver = new JPanelVideoDriver(this.emulator.getVideoGenerator(), keyAdapter);

        int framerate = this.emulator.getFramerate();
        boolean isStereo = this.emulator.getAudioGenerator().isStereo();

        this.audioDriver = isStereo
                ? new StereoAudioRendererDriver(jemu, this.emulator.getAudioGenerator(), new StereoAudioRenderer(framerate))
                : new MonoAudioRendererDriver(jemu, this.emulator.getAudioGenerator(), new MonoAudioRenderer(framerate));
        this.audioRenderer = this.audioDriver.getAudioRenderer();
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
    public Model getModel() {
        return this.model;
    }

    @Override
    public String getSystemName() {
        return this.system.getDisplayName();
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<VideoDriver> getVideoDriver() {
        return Optional.of(this.videoDriver);
    }

    @Override
    public Optional<AudioDriver> getAudioDriver() {
        return Optional.of(this.audioDriver);
    }

    @Override
    public JPanelVideoDriver getJPanelVideoDriver() {
        return this.videoDriver;
    }

    @Override
    public AudioRenderer getAudioRenderer() {
        return this.audioRenderer;
    }

    @Nullable
    private GameBoyJoypad.Actions getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_W -> GameBoyJoypad.Actions.UP;
            case KeyEvent.VK_S -> GameBoyJoypad.Actions.DOWN;
            case KeyEvent.VK_A -> GameBoyJoypad.Actions.LEFT;
            case KeyEvent.VK_D -> GameBoyJoypad.Actions.RIGHT;
            case KeyEvent.VK_ENTER -> GameBoyJoypad.Actions.START;
            case KeyEvent.VK_BACK_SPACE -> GameBoyJoypad.Actions.SELECT;
            case KeyEvent.VK_J -> GameBoyJoypad.Actions.A;
            case KeyEvent.VK_K -> GameBoyJoypad.Actions.B;
            default -> null;
        };
    }

    @Override
    public void close() throws IOException {

    }

}
