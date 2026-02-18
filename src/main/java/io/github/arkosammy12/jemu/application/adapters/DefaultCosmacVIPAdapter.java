package io.github.arkosammy12.jemu.application.adapters;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.application.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.application.drivers.JPanelVideoDriver;
import io.github.arkosammy12.jemu.application.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.application.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.application.util.System;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVIPKeypad;
import io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVipEmulator;
import io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import io.github.arkosammy12.jemu.backend.drivers.VideoDriver;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.MonoAudioRenderer;
import io.github.arkosammy12.jemu.frontend.audio.StereoAudioRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Optional;

import static io.github.arkosammy12.jemu.application.util.System.COSMAC_VIP;

public class DefaultCosmacVIPAdapter extends DefaultSystemAdapter implements CosmacVIPHost {

    private final String romTitle;
    private final System system;
    private final Chip8Interpreter chip8Interpreter;

    private final CosmacVipEmulator emulator;
    private final JPanelVideoDriver videoDriver;
    private final DefaultAudioRendererDriver audioDriver;
    private final AudioRenderer audioRenderer;

    public DefaultCosmacVIPAdapter(Jemu jemu, CoreInitializer initializer, Chip8Interpreter chip8Interpreter) {
        super(initializer);

        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = initializer.getSystem().orElse(COSMAC_VIP);
        this.chip8Interpreter = chip8Interpreter;

        this.emulator = new CosmacVipEmulator(this);

        KeyAdapter keyAdapter = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                CosmacVIPKeypad.Actions action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionPressed(action);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                CosmacVIPKeypad.Actions action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionReleased(action);
                }
            }

        };

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
    public Chip8Interpreter getChip8Interpreter() {
        return this.chip8Interpreter;
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
    private CosmacVIPKeypad.Actions getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_X -> CosmacVIPKeypad.Actions.KEY_0;
            case KeyEvent.VK_1 -> CosmacVIPKeypad.Actions.KEY_1;
            case KeyEvent.VK_2 -> CosmacVIPKeypad.Actions.KEY_2;
            case KeyEvent.VK_3 -> CosmacVIPKeypad.Actions.KEY_3;
            case KeyEvent.VK_Q -> CosmacVIPKeypad.Actions.KEY_4;
            case KeyEvent.VK_W -> CosmacVIPKeypad.Actions.KEY_5;
            case KeyEvent.VK_E -> CosmacVIPKeypad.Actions.KEY_6;
            case KeyEvent.VK_A -> CosmacVIPKeypad.Actions.KEY_7;
            case KeyEvent.VK_S -> CosmacVIPKeypad.Actions.KEY_8;
            case KeyEvent.VK_D -> CosmacVIPKeypad.Actions.KEY_9;
            case KeyEvent.VK_Z -> CosmacVIPKeypad.Actions.KEY_A;
            case KeyEvent.VK_C -> CosmacVIPKeypad.Actions.KEY_B;
            case KeyEvent.VK_4 -> CosmacVIPKeypad.Actions.KEY_C;
            case KeyEvent.VK_R -> CosmacVIPKeypad.Actions.KEY_D;
            case KeyEvent.VK_F -> CosmacVIPKeypad.Actions.KEY_E;
            case KeyEvent.VK_V -> CosmacVIPKeypad.Actions.KEY_F;
            default -> null;
        };
    }

    @Override
    public void close() throws IOException {
        if (this.videoDriver != null) {
            this.videoDriver.close();
        }
        if (this.audioDriver != null) {
            this.audioDriver.close();
        }
    }

}
