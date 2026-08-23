package io.github.arkosammy12.jemu.app.system.cosmacvip;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPEmulator;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;

import javax.sound.sampled.LineUnavailableException;
import java.util.Optional;

public class CosmacVIPAdapter extends SystemAdapter implements CosmacVIPHost {

    private String romTitle;
    private final Chip8Interpreter chip8Interpreter;

    public CosmacVIPAdapter(Jemu jemu, SystemManager systemManager, Chip8Interpreter chip8Interpreter) throws LineUnavailableException {
        this.chip8Interpreter = chip8Interpreter;
        super(jemu, systemManager);
    }

    @Override
    protected Emulator createEmulator() {
        return new CosmacVIPEmulator(this);
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Chip8Interpreter getChip8Interpreter() {
        return this.chip8Interpreter;
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
