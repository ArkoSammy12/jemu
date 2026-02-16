package io.github.arkosammy12.jemu.config.settings;

import io.github.arkosammy12.jemu.config.initializers.CommonInitializer;
import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.common.Emulator;
import io.github.arkosammy12.jemu.systems.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.util.DisplayAngle;
import io.github.arkosammy12.jemu.util.System;
import org.tinylog.Logger;

import java.util.Optional;

public class GameBoyEmulatorSettings extends AbstractEmulatorSettings {

    private final String romTitle;

    private static final int HEADER_TITLE_START = 0x0134;
    private static final int HEADER_TITLE_END = 0x0143;

    private final DisplayAngle displayAngle;
    private final System system;
    private final Model model;

    public GameBoyEmulatorSettings(Jemu jemu, CommonInitializer initializer, Model model) {
        super(jemu, initializer);
        this.displayAngle = initializer.getDisplayAngle().orElse(DisplayAngle.DEG_0);

        StringBuilder titleBuilder;
        String title = null;
        try {
            titleBuilder = new StringBuilder();
            int[] rom = this.getRom();
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
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public DisplayAngle getDisplayAngle() {
        return this.displayAngle;
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    public Model getModel() {
        return this.model;
    }

    @Override
    public Emulator getEmulator() {
        return new GameBoyEmulator(this);
    }

    public enum Model {
        DMG,
        CGB,
    }

}
