package io.github.arkosammy12.jemu.backend.cosmacvip;

import io.github.arkosammy12.jemu.backend.common.VideoGenerator;

public class CDP1861<E extends CosmacVipEmulator> extends VideoGenerator<E> implements IODevice {

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

    protected final int[][] displayBuffer;
    protected long cycles;
    protected int scanlineIndex;

    private DmaStatus dmaStatus = DmaStatus.NONE;
    private boolean interrupting = false;
    private boolean enabled = false;
    private boolean displayEnableLatch;

    public CDP1861(E emulator) {
        super(emulator);
        this.displayBuffer = new int[this.getImageWidth()][this.getImageHeight()];
    }

    @Override
    public int getImageWidth() {
        return 256;
    }

    @Override
    public int getImageHeight() {
        return 128;
    }

    @Override
    public DmaStatus getDmaStatus() {
        return this.dmaStatus;
    }

    @Override
    public boolean isInterrupting() {
        return this.interrupting;
    }

    @Override
    public void cycle() {
        if (this.cycles % CosmacVipEmulator.CYCLES_PER_FRAME == 0) {
            this.enabled = this.displayEnableLatch;
        }
        if (this.enabled) {
            this.emulator.getCpu().setEF(0, (this.scanlineIndex >= FIRST_EFX_BEGIN && this.scanlineIndex < FIRST_EFX_END) || (this.scanlineIndex >= SECOND_EFX_BEGIN && this.scanlineIndex < SECOND_EFX_END));
            this.interrupting = this.scanlineIndex >= INTERRUPT_BEGIN && this.scanlineIndex < INTERRUPT_END;
            if (this.scanlineIndex >= DISPLAY_AREA_BEGIN && this.scanlineIndex < DISPLAY_AREA_END) {
                long scanLineCycles = this.cycles % MACHINE_CYCLES_PER_SCANLINE;
                if (scanLineCycles >= (DMAO_BEGIN - 1) && scanLineCycles < (DMAO_END - 1)) {
                    this.dmaStatus = DmaStatus.OUT;
                } else {
                    this.dmaStatus = DmaStatus.NONE;
                }
            }
        } else {
            this.interrupting = false;
            this.dmaStatus = DmaStatus.NONE;
            this.emulator.getCpu().setEF(0, false);
        }
        if (this.cycles != 0 && (this.cycles % MACHINE_CYCLES_PER_SCANLINE == 0)) {
            this.scanlineIndex = (this.scanlineIndex + 1) % SCANLINES_PER_FRAME;
            if (this.scanlineIndex == 0) {
                this.emulator.getHost().getVideoDriver().ifPresent(driver ->  driver.outputFrame(this.displayBuffer));
            }
        }
        this.cycles++;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public void doDmaOut(int dmaOutAddress, int value) {
        if (!this.emulator.getCpu().getCurrentState().isS2Dma()) {
            return;
        }
        int row = this.scanlineIndex - DISPLAY_AREA_BEGIN;
        if (row < 0 || row >= this.getImageWidth()) {
            return;
        }
        int dmaIndex = (int) ((this.cycles % MACHINE_CYCLES_PER_SCANLINE) - DMAO_BEGIN);
        int colStart = dmaIndex * 8;
        for (int i = 0, mask = 0x80; i < 8; i++, mask >>>= 1) {
            int col = colStart + i;
            if (col < 0 || col >= 64) {
                break;
            }
            for (int j = 0; j < 4; j++) {
                this.displayBuffer[(col * 4) + j][row] = (value & mask) != 0 ? 0xFFFFFFFF : 0xFF000000;
            }
        }
    }

    @Override
    public boolean isOutputPort(int port) {
        return port == 1;
    }

    @Override
    public void onOutput(int port, int value) {
        this.displayEnableLatch = false;
    }

    @Override
    public boolean isInputPort(int port) {
        return port == 1;
    }

    @Override
    public int onInput(int port) {
        this.displayEnableLatch = true;
        return 0xFF;
    }

}
