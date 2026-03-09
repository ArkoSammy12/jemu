package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.JPanelVideoDriver;
import io.github.arkosammy12.jemu.app.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;

import java.util.Arrays;
import java.util.Optional;

public abstract class DefaultSystemAdapter implements SystemAdapter {

    private final byte[] rom;

    public DefaultSystemAdapter(CoreInitializer coreInitializer) {
        Optional<byte[]> rawRomOptional = coreInitializer.getRawRom();
        if (rawRomOptional.isEmpty()) {
            throw new EmulatorException("Must select a ROM file before starting emulation!");
        }
        byte[] rom = rawRomOptional.get();
        this.rom = Arrays.copyOf(rom, rom.length);
    }

    @Override
    public byte[] getRom() {
        return Arrays.copyOf(this.rom, this.rom.length);
    }

    public abstract JPanelVideoDriver getJPanelVideoDriver();

    public abstract AudioRenderer getAudioRenderer();

    @Override
    public abstract Optional<? extends DefaultAudioRendererDriver> getAudioDriver();

    public void onFrame() {
        this.getJPanelVideoDriver().requestFrame();
        this.getAudioDriver().ifPresent(DefaultAudioRendererDriver::onFrame);
    }

}
