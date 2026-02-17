package io.github.arkosammy12.jemu.application.config.settings;

import io.github.arkosammy12.jemu.backend.common.SystemHost;
import io.github.arkosammy12.jemu.backend.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.application.config.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.backend.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.application.util.System;
import org.tinylog.Logger;

import java.util.Optional;

public class GameBoyEmulatorSettings extends AbstractEmulatorSettings implements GameBoyHost {

    private final String romTitle;

    private static final int HEADER_TITLE_START = 0x0134;
    private static final int HEADER_TITLE_END = 0x0143;

    private final System system;
    private final Model model;

    public GameBoyEmulatorSettings(CoreInitializer initializer, Model model) {
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
    public System getSystem() {
        return this.system;
    }

    @Override
    public Model getModel() {
        return this.model;
    }

    @Override
    public Emulator getEmulator() {
        return new GameBoyEmulator(this);
    }

}
