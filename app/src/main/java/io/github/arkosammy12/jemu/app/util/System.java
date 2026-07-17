package io.github.arkosammy12.jemu.app.util;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.*;
import io.github.arkosammy12.jemu.app.managers.*;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.util.Collection;

public enum System implements SystemDescriptor {
    COSMAC_VIP(new CosmacVIPManager(CosmacVIPHost.Chip8Interpreter.NONE)),
    VIP_CHIP_8(new CosmacVIPManager(CosmacVIPHost.Chip8Interpreter.CHIP_8)),
    VIP_CHIP_8X(new CosmacVIPManager(CosmacVIPHost.Chip8Interpreter.CHIP_8X)),
    RCA_STUDIO_II(new RCAStudioIIManager()),
    GAME_BOY(new GameBoyManager(GameBoyHost.Model.DMG)),
    GAME_BOY_COLOR(new GameBoyManager(GameBoyHost.Model.CGB)),
    NES(new NESManager()),
    ATARI_2600(new Atari2600Manager());

    private final SystemManager systemManager;

    System(SystemManager systemManager) {
        this.systemManager = systemManager;
    }

    public static SystemAdapter getSystemAdapter(Jemu jemu, @Nullable System system) throws Exception {
        if (system != null) {
            return system.systemManager.createSystem(jemu, system);
        }
        throw new EmulatorException("Must select a system!");
    }

    public static System getSystemForIdentifier(String identifier) {
        for (System system : System.values()) {
            if (system.systemManager.getId().equals(identifier)) {
                return system;
            }
        }
        throw new IllegalArgumentException("Unknown system identifier \"" + identifier + "\"!");
    }

    @Override
    public String getName() {
        return this.systemManager.getName();
    }

    @Override
    public String getId() {
        return this.systemManager.getId();
    }

    @Override
    public Collection<String> getFileExtensions() {
        return this.systemManager.getFileExtensions();
    }

    public static class Converter implements CommandLine.ITypeConverter<System> {

        @Override
        public System convert(String value) {
            return getSystemForIdentifier(value);
        }

    }

}
