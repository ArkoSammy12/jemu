package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

import static io.github.arkosammy12.jemu.core.nes.NESMMIOBus.*;

public class NESPPU<E extends NESEmulator> extends VideoGenerator<E> implements Bus {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 240;

    public NESPPU(E emulator) {
        super(emulator);
    }

    @Override
    public int getImageWidth() {
        return WIDTH;
    }

    @Override
    public int getImageHeight() {
        return HEIGHT;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case PPUCTRL_ADDR -> 0xFF;
            case PPUMASK_ADDR -> 0xFF;
            case PPUSTATUS_ADDR -> 0xFF;
            case OAMADDR_ADDR -> 0xFF;
            case OAMDATA_ADDR -> 0xFF;
            case PPUSCROLL_ADDR -> 0xFF;
            case PPUADDR_ADDR -> 0xFF;
            case PPUDATA_ADDR -> 0xFF;
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case PPUCTRL_ADDR -> {}
            case PPUMASK_ADDR -> {}
            case PPUSTATUS_ADDR -> {}
            case OAMADDR_ADDR -> {}
            case OAMDATA_ADDR -> {}
            case PPUSCROLL_ADDR -> {}
            case PPUADDR_ADDR -> {}
            case PPUDATA_ADDR -> {}
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
    }

    public void subCycle() {

    }

}
