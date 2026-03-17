package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.gameboy.*;

public class GameBoyColorEmulator extends GameBoyEmulator {

    private CGBBus<?> bus;
    private CGBPPU<?> ppu;

    private CGBMMMIOBus mmioBus;

    public GameBoyColorEmulator(GameBoyHost host) {
        super(host);
    }

    @Override
    protected DMGBus<?> createBus() {
        this.bus = new CGBBus<>(this);
        return this.bus;
    }

    @Override
    public DMGBus<?> getBus() {
        return this.bus;
    }

    @Override
    protected CGBPPU<?> createPpu() {
        this.ppu = new CGBPPU<>(this);
        return this.ppu;
    }

    @Override
    public CGBPPU<?> getVideoGenerator() {
        return this.ppu;
    }

    protected CGBMMMIOBus createMmioBus() {
        this.mmioBus = new CGBMMMIOBus(this);
        return this.mmioBus;
    }

    @Override
    public CGBMMMIOBus getMMIOBus() {
        return this.mmioBus;
    }

}
