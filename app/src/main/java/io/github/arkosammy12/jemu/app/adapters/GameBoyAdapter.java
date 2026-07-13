package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.managers.SystemManager;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyJoypad;
import io.github.arkosammy12.jemu.core.gameboycolor.GameBoyColorEmulator;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Optional;

public class GameBoyAdapter extends SystemAdapter implements GameBoyHost {

    private static final int HEADER_TITLE_START = 0x0134;
    private static final int HEADER_TITLE_END = 0x0143;

    private String romTitle;
    private final Model model;

    public GameBoyAdapter(Jemu jemu, System system, Model model, SystemManager systemManager) throws LineUnavailableException {
        this.model = model;
        super(jemu, system, systemManager);
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<Path> getSaveDataDirectory() {
        return this.jemu.getSavesDirectory();
    }

    @Override
    protected Emulator createEmulator() {

        String title = null;
        Optional<byte[]> optionalROM = this.getRom();
        if (optionalROM.isPresent()) {
            try {
                StringBuilder titleBuilder = new StringBuilder();
                int[] rom = SystemHost.byteToIntArray(optionalROM.get());
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
        }

        Optional<Path> romPathOptional = this.getRomPath();
        if (romPathOptional.isPresent()) {
            Path romPath = romPathOptional.get();
            this.romTitle = title == null || title.isBlank() ? romPath.getFileName().toString() : title;
        } else {
            this.romTitle = title;
        }

        return switch (this.model) {
            case CGB -> new GameBoyColorEmulator(this);
            case DMG -> new GameBoyEmulator(this);
        };
    }

    @Override
    @Nullable
    protected GameBoyJoypad.Actions getActionForKeyCode(int keyCode) {
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

}
