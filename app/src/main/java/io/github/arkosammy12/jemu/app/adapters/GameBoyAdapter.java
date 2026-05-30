package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPKeypad;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyJoypad;
import io.github.arkosammy12.jemu.core.gameboycolor.GameBoyColorEmulator;
import net.java.games.input.Event;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Optional;

public class GameBoyAdapter extends AbstractSystemAdapter implements GameBoyHost {

    private String romTitle;

    private static final int HEADER_TITLE_START = 0x0134;
    private static final int HEADER_TITLE_END = 0x0143;

    private final System system;
    private final Model model;

    private Path saveDataDirectory;

    public GameBoyAdapter(CoreInitializer initializer, Model model) {
        this.model = model;
        this.system = initializer.getSystem().orElse(System.GAME_BOY);
        super(initializer);
    }

    @Override
    protected Emulator createEmulator() {

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

        this.romTitle = title != null ? title : this.getRomPath().getFileName().toString();
        this.saveDataDirectory = this.getRomPath().getParent();

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

    @Override
    @Nullable
    protected GameBoyJoypad.Actions getActionForJoypadEvent(Event e) {
        return null;
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    @Override
    public Model getModel() {
        return this.model;
    }

    @Override
    public Optional<Path> getSaveDataDirectory() {
        return Optional.ofNullable(this.saveDataDirectory);
    }

    @Override
    public String getSystemName() {
        return this.system.getDisplayName();
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

}
