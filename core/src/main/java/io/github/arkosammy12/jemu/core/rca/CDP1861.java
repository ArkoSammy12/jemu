package io.github.arkosammy12.jemu.core.rca;

import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.cpu.CDP1802;

public class CDP1861<E extends CDP1802System> implements VideoGenerator {

    public static final int CPU_CYCLES_PER_FRAME = 3668;

    protected static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 128;

    private static final int SCANLINES_PER_FRAME = 262;
    protected static final int MACHINE_CYCLES_PER_SCANLINE = 14;

    private static final int INTERRUPT_BEGIN = 78;
    private static final int INTERRUPT_END = 80;

    private static final int FIRST_EFX_BEGIN = 76;
    private static final int FIRST_EFX_END = 80;

    protected static final int DISPLAY_AREA_BEGIN = 80;
    private static final int DISPLAY_AREA_END = 208;

    private static final int SECOND_EFX_BEGIN = 204;
    private static final int SECOND_EFX_END = 208;

    protected static final int DMAO_BEGIN = 4;
    private static final int DMAO_END = 12;

    private final E emulator;

    protected final int[] displayBuffer;
    protected long cycles;
    protected int scanlineIndex;

    private boolean interrupting;
    private boolean efx;
    private boolean dmaOut;
    private boolean enabled;
    private boolean displayEnableLatch;

    public CDP1861(E emulator) {
        this.emulator = emulator;
        this.displayBuffer = new int[this.getImageWidth() * this.getImageHeight()];
    }

    @Override
    public int getImageWidth() {
        return IMAGE_WIDTH;
    }

    @Override
    public int getImageHeight() {
        return IMAGE_HEIGHT;
    }

    @Override
    public double getPixelAspectRatio() {
        return 4.0;
    }

    public boolean getInterruptSignal() {
        return this.interrupting;
    }

    public boolean getDMAOUTSignal() {
        return this.dmaOut;
    }

    public boolean getEFX() {
        return this.efx;
    }

    public void setDisplayEnable(boolean value) {
        this.displayEnableLatch = value;
    }

    public void cycle() {
        if (this.cycles % CPU_CYCLES_PER_FRAME == 0) {
            this.enabled = this.displayEnableLatch;
        }
        if (this.enabled) {
            this.efx = (this.scanlineIndex >= FIRST_EFX_BEGIN && this.scanlineIndex < FIRST_EFX_END) || (this.scanlineIndex >= SECOND_EFX_BEGIN && this.scanlineIndex < SECOND_EFX_END);
            this.interrupting = this.scanlineIndex >= INTERRUPT_BEGIN && this.scanlineIndex < INTERRUPT_END;
            if (this.scanlineIndex >= DISPLAY_AREA_BEGIN && this.scanlineIndex < DISPLAY_AREA_END) {
                long scanLineCycles = this.cycles % MACHINE_CYCLES_PER_SCANLINE;
                this.dmaOut = scanLineCycles >= (DMAO_BEGIN - 1) && scanLineCycles < (DMAO_END - 1);
            }
        } else {
            this.interrupting = false;
            this.efx = false;
            this.dmaOut = false;
        }
        if (this.cycles != 0 && (this.cycles % MACHINE_CYCLES_PER_SCANLINE == 0)) {
            this.scanlineIndex = (this.scanlineIndex + 1) % SCANLINES_PER_FRAME;
            if (this.scanlineIndex == 0) {
                this.emulator.getHost().getVideoDriver().ifPresent(driver ->  driver.outputFrame(this.displayBuffer));
            }
        }
        this.cycles++;
    }

    @SuppressWarnings("DuplicatedCode")
    public void onDMAOUT(int dmaOutAddress, int value) {
        CDP1802 cpu = this.emulator.getCpu();
        if (!(cpu.getSC1() && !cpu.getSC0())) {
            return;
        }
        int row = this.scanlineIndex - DISPLAY_AREA_BEGIN;
        if (row < 0 || row >= this.getImageHeight()) {
            return;
        }
        int dmaIndex = (int) ((this.cycles % MACHINE_CYCLES_PER_SCANLINE) - DMAO_BEGIN);
        int colStart = dmaIndex * 8;
        for (int i = 0, mask = 0x80; i < 8; i++, mask >>>= 1) {
            int col = colStart + i;
            if (col < 0 || col >= 64) {
                break;
            }
            this.displayBuffer[(row * IMAGE_WIDTH) + col] = this.getPixelRGB(dmaOutAddress, (value & mask) != 0);
        }
    }

    protected int getPixelRGB(int dmaOutAddress, boolean bit) {
        return bit ? 0xFFFFFF : 0x000000;
    }

}
