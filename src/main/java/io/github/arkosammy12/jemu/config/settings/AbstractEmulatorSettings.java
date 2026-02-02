package io.github.arkosammy12.jemu.config.settings;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.initializers.CommonInitializer;
import io.github.arkosammy12.jemu.exceptions.EmulatorException;

import java.util.Arrays;
import java.util.Optional;

public abstract class AbstractEmulatorSettings implements EmulatorSettings {

    private final int[] rom;
    private final Jemu jemu;

    public AbstractEmulatorSettings(Jemu jemu, CommonInitializer initializer) {
        this.jemu = jemu;

        Optional<byte[]> rawRomOptional = initializer.getRawRom();

        if (rawRomOptional.isEmpty()) {
            throw new EmulatorException("Must select a ROM file before starting emulation!");
        }

        byte[] rawRom = rawRomOptional.get();
        int[] rom = EmulatorSettings.loadRom(rawRom);
        this.rom = Arrays.copyOf(rom, rom.length);

    }

    @Override
    public Jemu getJemu() {
        return this.jemu;
    }

    @Override
    public int[] getRom() {
        return Arrays.copyOf(this.rom, this.rom.length);
    }

}
