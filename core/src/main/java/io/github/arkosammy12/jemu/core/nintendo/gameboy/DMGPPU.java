package io.github.arkosammy12.jemu.core.nintendo.gameboy;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.cpu.SM83;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.util.ShiftRegister;

import java.util.Arrays;

import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGBus.*;

public class DMGPPU<E extends GameBoyEmulator> implements VideoGenerator, Bus {

    protected static final int WIDTH = 160;
    private static final int HEIGHT = 144;

    public static final int LCDC_ADDR = 0xFF40;
    public static final int STAT_ADDR = 0xFF41;
    public static final int SCY_ADDR = 0xFF42;
    public static final int SCX_ADDR = 0xFF43;
    public static final int LY_ADDR = 0xFF44;
    public static final int LYC_ADDR = 0xFF45;
    public static final int BGP_ADDR = 0xFF47;
    public static final int OBP0_ADDR = 0xFF48;
    public static final int OBP1_ADDR = 0xFF49;
    public static final int WY_ADDR = 0xFF4A;
    public static final int WX_ADDR = 0xFF4B;

    private static final int CYCLES_PER_SCANLINE = 456;
    private static final int SCANLINES_PER_FRAME = 154;

    private static final int[] DMG_PALETTE = {
            0x9BBC0F,
            0x8BAC0F,
            0x306230,
            0x0F380F
    };

    protected final E emulator;

    protected final byte[] vram = new byte[0x2000];
    private final byte[] oam = new byte[0x00A0];

    private int lcdControl;
    private boolean lcdPPUEnable;
    private int windowTileMap = 0x9800;
    private boolean windowEnable;
    private boolean backgroundAndWindowTiles;
    private int backgroundTileMap = 0x9800;
    private boolean objectSize;
    private boolean objectEnable;
    private boolean backgroundAndWindowEnable;

    private int ppuStatus;
    private boolean lycInterruptSelect;
    private boolean mode2InterruptSelect;
    private boolean mode1InterruptSelect;
    private boolean mode0InterruptSelect;
    private boolean lyEqualsLYCFlag;
    private int ppuMode;

    protected int scrollY;
    protected int scrollX;
    private int lcdY;
    private int lcdYCompare;
    protected int backgroundPalette;
    protected int objectPalette0;
    protected int objectPalette1;
    private int windowY;
    private int windowX;

    // TODO: Implement the PPU behavior when the CPU is in STOP mode for the DMG and CGB
    protected final int[] lcd;

    protected Mode currentMode = Mode.HBLANK_0;
    private int dotNumber;
    private int dotCycleIndex;
    protected int scanlineNumber;
    private int statModeForInterrupt;

    protected boolean enablePixelWrites;
    private int enablePixelWritesDelay;
    private boolean lcdOnLine;
    private int pendingVisibleMode = -1;

    private boolean oldStatInterruptLine;
    private boolean oldMode1Leg;
    private boolean oldMode0Leg;
    private boolean oldMode2Leg;

    private boolean windowPixelRendered;

    protected int pixelX;
    protected int discardedPixels;
    protected int windowLine;
    private boolean windowYCondition;
    private boolean windowXCondition;

    protected final long[] spriteBuffer = new long[10];
    private int scannedEntries = 0;

    protected final ShiftRegister backgroundFifo = new ShiftRegister(8, 32);
    protected int bgFifoStep = 0;
    protected boolean bgFifoFirstFetch = true;
    protected int bgFifoFetcherX;
    protected int bgFifoCurrentTileNumber;
    protected int bgFifoTileDataEffectiveAddress;
    protected int bgFifoTileDataLow;
    protected int bgFifoTileDataHigh;

    protected final ShiftRegister spriteFifo = new ShiftRegister(8, 32);
    protected int spriteFifoCurrentEntryIndex;
    protected int spriteFifoStep = 0;
    protected int spriteFifoCurrentTileNumber;
    protected int spriteFifoTileDataEffectiveAddress;
    protected int spriteFifoTileDataLow;
    protected int spriteFifoTileDataHigh;

    private boolean armOAMBugRead;
    private boolean armOAMBugWrite;

    public DMGPPU(E emulator) {
        this.emulator = emulator;
        this.lcd = new int[this.getImageWidth() * this.getImageHeight()];
        Arrays.fill(this.lcd, this.getLCDOffColor());
        Arrays.fill(this.spriteBuffer, -1);
    }

    @Override
    public int getImageWidth() {
        return WIDTH;
    }

    @Override
    public int getImageHeight() {
        return HEIGHT;
    }

    protected int getLCDOffColor() {
        return 0x9BBC0F;
    }

    @Override
    public int readByte(int address) {
        if (address >= OAM_START && address <= OAM_END) {
            int ppuMode = this.getPPUMode();
            if (Mode.HBLANK_0.matchesValue(ppuMode) || Mode.VBLANK_1.matchesValue(ppuMode) || !this.getLCDPPUEnable()) {
                return (int) this.oam[address - OAM_START] & 0xFF;
            } else {
                return 0xFF;
            }

        } else if (address >= VRAM_START && address <= VRAM_END) {
            if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                return (int) this.vram[address - VRAM_START] & 0xFF;
            } else {
                return 0xFF;
            }
        } else {
            return switch (address) {
                case LCDC_ADDR -> this.lcdControl;
                case STAT_ADDR -> this.ppuStatus | 0b10000000;
                case SCY_ADDR -> this.scrollY;
                case SCX_ADDR -> this.scrollX;
                case LY_ADDR -> this.lcdY;
                case LYC_ADDR -> this.lcdYCompare;
                case BGP_ADDR -> this.backgroundPalette;
                case OBP0_ADDR -> this.objectPalette0;
                case OBP1_ADDR -> this.objectPalette1;
                case WY_ADDR -> this.windowY;
                case WX_ADDR -> this.windowX;
                default -> throw new EmulatorException("Invalid address $%04X for GameBoy PPU!".formatted(address));
            };
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= OAM_START && address <= OAM_END) {
            int ppuMode = this.getPPUMode();
            if (Mode.HBLANK_0.matchesValue(ppuMode) || Mode.VBLANK_1.matchesValue(ppuMode) || !this.getLCDPPUEnable()) {
              this.oam[address - OAM_START] = (byte) value;
            }
        } else if (address >= VRAM_START && address <= VRAM_END) {
            if (!Mode.DRAWING_3.matchesValue(this.getPPUMode()) || !this.getLCDPPUEnable()) {
                this.vram[address - VRAM_START] = (byte) value;
            }
        } else {
            switch (address) {
                case LCDC_ADDR -> {
                    boolean oldLcdEnable = this.getLCDPPUEnable();

                    this.lcdControl = value & 0xFF;
                    this.lcdPPUEnable = (value & 0b10000000) != 0;
                    this.windowTileMap = (value & 0b01000000) != 0? 0x9C00 : 0x9800;
                    this.windowEnable = (value & 0b00100000) != 0;
                    this.backgroundAndWindowTiles = (value & 0b00010000) != 0;
                    this.backgroundTileMap = (value & 0b00001000) != 0 ? 0x9C00 : 0x9800;
                    this.objectSize = (value & 0b00000100) != 0;
                    this.objectEnable = (value & 0b00000010) != 0;
                    this.backgroundAndWindowEnable = (value & 0b00000001) != 0;

                    boolean newLcdEnable = this.getLCDPPUEnable();
                    if (oldLcdEnable != newLcdEnable) {
                        this.scanlineNumber = 0;
                        this.lcdY = 0;
                        this.dotNumber = 0;
                        this.currentMode = Mode.HBLANK_0;
                        this.setPPUMode(Mode.HBLANK_0.getValue());
                        this.setSTATModeForInterrupt(Mode.HBLANK_0.getValue());
                    }
                    if (!oldLcdEnable && newLcdEnable) {
                        this.onLCDOn();
                    } else if (oldLcdEnable && !newLcdEnable) {
                        this.onLCDOff();
                    }
                }
                case STAT_ADDR -> {
                    this.checkSTATWriteBug();
                    this.ppuStatus = (value & 0b11111000) | (this.ppuStatus & 0b111);
                    this.lycInterruptSelect = (value & 0b01000000) != 0;
                    this.mode2InterruptSelect = (value & 0b00100000) != 0;
                    this.mode1InterruptSelect = (value & 0b00010000) != 0;
                    this.mode0InterruptSelect = (value & 0b00001000) != 0;

                }
                case SCY_ADDR -> this.scrollY = value & 0xFF;
                case SCX_ADDR -> this.scrollX = value & 0xFF;
                case LY_ADDR -> {}
                case LYC_ADDR -> this.lcdYCompare = value & 0xFF;
                case BGP_ADDR -> this.backgroundPalette = value & 0xFF;
                case OBP0_ADDR -> this.objectPalette0 = value & 0xFF;
                case OBP1_ADDR -> this.objectPalette1 = value & 0xFF;
                case WY_ADDR -> this.windowY = value & 0xFF;
                case WX_ADDR -> this.windowX = value & 0xFF;
                default -> throw new EmulatorException("Invalid address $%04X for GameBoy PPU!".formatted(address));
            }
        }
    }

    protected void checkSTATWriteBug() {
        // The write transient drives all four enables high while the bus settles
        boolean vblank = this.scanlineNumber >= 144;
        this.settleSTATLegs(this.getLYEqualsLYCFlag(), vblank, Mode.HBLANK_0.matchesValue(this.statModeForInterrupt), this.isLineEndPulse() && !vblank);
    }

    public void checkArmOAMBugRead(int address) {
        if (address >= OAM_START && address <= UNUSED_END && this.getLCDPPUEnable()) {
            this.armOAMBugRead = true;
        }
    }

    public void checkArmOAMBugWrite(int address) {
        if (address >= OAM_START && address <= UNUSED_END && this.getLCDPPUEnable()) {
            this.armOAMBugWrite = true;
        }
    }

    private void onLCDOn() {
        this.enablePixelWritesDelay = 2;
        this.dotNumber = 2;
        this.lcdOnLine = true;
    }

    private void onLCDOff() {
        this.enablePixelWrites = false;
        this.enablePixelWritesDelay = -1;
        this.lcdOnLine = false;
        this.pendingVisibleMode = -1;
        this.armOAMBugRead = false;
        this.armOAMBugWrite = false;
        Arrays.fill(this.lcd, this.getLCDOffColor());
        this.emulator.getHost().getVideoDriver().ifPresent(driver -> driver.outputFrame(this.lcd));
    }

    void cycleDot(int tCycle) {
        if (!this.getLCDPPUEnable()) {
            return;
        }
        // STAT mode bits latched by a CPU read in the same M-cycle as a mode transition
        // show the pre-transition value; publish at the next M-cycle boundary
        if (tCycle == 0 && this.pendingVisibleMode >= 0) {
            this.setPPUMode(this.pendingVisibleMode);
            this.pendingVisibleMode = -1;
        }
        this.nextState();

        switch (this.currentMode) {
            case HBLANK_0 -> this.onHBlank();
            case VBLANK_1 -> this.onVBlank();
            case OAM_SCAN_2 -> this.onOAMScan();
            case DRAWING_3 -> this.onDrawing();
        }

        this.checkOAMBugTrigger(tCycle);

        this.dotNumber++;
        // The first scanline after LCD-on is 454 dots, its Mode 0 ending early
        int lineLength = this.lcdOnLine ? CYCLES_PER_SCANLINE - 2 : CYCLES_PER_SCANLINE;
        if (this.dotNumber >= lineLength) {
            this.lcdOnLine = false;
            this.dotNumber = 0;
            this.dotCycleIndex = 0;

            this.scanlineNumber = (this.scanlineNumber + 1) % SCANLINES_PER_FRAME;
            if (this.scanlineNumber == 144) {
                this.triggerVBlankInterrupt();
            }
            // line 153 already reset LY to 0
            if (this.scanlineNumber != 0) {
                this.lcdY = this.scanlineNumber;
            }

            if (this.windowPixelRendered) {
                this.windowPixelRendered = false;
                this.windowLine = (this.windowLine + 1) % SCANLINES_PER_FRAME;
            }
        }

        // The LY=LYC comparator output is registered once per M-cycle
        if ((this.dotNumber & 3) == 0) {
            this.setLYEqualsLYCFlag(this.lcdY == this.lcdYCompare);
        }
        // LY resets early in line 153; the comparator capture above wins the race
        if (this.scanlineNumber == 153 && this.dotNumber == 4) {
            this.lcdY = 0;
        }

        this.updateSTATLine();
    }

    private void updateSTATLine() {
        boolean vblank = this.scanlineNumber >= 144;
        boolean lycLeg = this.getLYCInterruptSelect() && this.getLYEqualsLYCFlag();
        boolean mode1Leg = this.getMode1InterruptSelect() && vblank;
        boolean mode0Leg = this.getMode0InterruptSelect() && Mode.HBLANK_0.matchesValue(this.statModeForInterrupt);
        boolean mode2Leg = this.getMode2InterruptSelect() && this.isLineEndPulse() && !vblank;
        // One settle stage per gate-depth rank: LYC, then mode 1, then modes 0/2. A
        // through-zero between stages fires; a covered swap doesn't
        this.settleSTATLegs(lycLeg, this.oldMode1Leg, this.oldMode0Leg, this.oldMode2Leg);
        this.settleSTATLegs(lycLeg, mode1Leg, this.oldMode0Leg, this.oldMode2Leg);
        this.settleSTATLegs(lycLeg, mode1Leg, mode0Leg, mode2Leg);
        this.oldMode1Leg = mode1Leg;
        this.oldMode0Leg = mode0Leg;
        this.oldMode2Leg = mode2Leg;
    }

    // The OAM-scan STAT condition is the line-end pulse itself, straddling the line boundary
    private boolean isLineEndPulse() {
        int pulseStart = (this.lcdOnLine ? CYCLES_PER_SCANLINE - 2 : CYCLES_PER_SCANLINE) - 1;
        return this.dotNumber >= pulseStart || this.dotNumber <= 1;
    }

    private void settleSTATLegs(boolean lycLeg, boolean mode1Leg, boolean mode0Leg, boolean mode2Leg) {
        boolean statInterruptLine = lycLeg || mode1Leg || mode0Leg || mode2Leg;
        if (!this.oldStatInterruptLine && statInterruptLine) {
            this.triggerSTATInterrupt();
        }
        this.oldStatInterruptLine = statInterruptLine;
    }

    private void nextState() {
        Mode oldMode = this.currentMode;
        if (this.dotNumber == 0) {
            if (this.scanlineNumber >= 144) {
                this.currentMode = Mode.VBLANK_1;
            } else if (!this.lcdOnLine) {
                // OAM scan is skipped on the first scanline after LCD-on
                this.currentMode = Mode.OAM_SCAN_2;
            }
        } else if (this.dotNumber == 80) {
            if (this.scanlineNumber >= 144) {
                this.currentMode = Mode.VBLANK_1;
            } else {
                this.currentMode = Mode.DRAWING_3;
            }
        } else if (this.pixelX >= 168) {
            this.currentMode = Mode.HBLANK_0;
        }
        if (oldMode != this.currentMode) {
            this.dotCycleIndex = 0;
            // The mode-bit bus drivers settle in about a dot and a half: a transition
            // early in the M-cycle is visible to a read latching at its end, a later
            // one only from the next M-cycle
            if (this.emulator.mCycleDot <= 1) {
                this.setPPUMode(this.currentMode.getValue());
            } else {
                this.pendingVisibleMode = this.currentMode.getValue();
            }
        }
    }

    private void onVBlank() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                this.dotCycleIndex = 1;
            }
            case 1 -> {
                if (this.scanlineNumber == 144) {
                    this.setSTATModeForInterrupt(Mode.VBLANK_1.getValue());
                    this.windowYCondition = false;
                    this.windowLine = 0;

                    if (this.enablePixelWritesDelay > 0) {
                        this.enablePixelWritesDelay--;
                        if (this.enablePixelWritesDelay == 0) {
                            this.enablePixelWrites = true;
                        }
                    }

                    this.emulator.getHost().getVideoDriver().ifPresent(driver -> driver.outputFrame(this.lcd));
                }
                this.dotCycleIndex = 2;
            }
            case 2 -> {
                this.dotCycleIndex = 3;
            }
            case 3 -> {
                this.dotCycleIndex = 4;
            }
            case 4 -> {}
        }
    }

    private void onHBlank() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                this.onHBlankStart();
                this.dotCycleIndex = 1;
            }
            case 1 -> {
                this.setSTATModeForInterrupt(Mode.HBLANK_0.getValue());
                this.dotCycleIndex = 2;
            }
            case 2 -> {
                this.dotCycleIndex = 3;
            }
            case 3 -> {
                this.dotCycleIndex = 4;
            }
            case 4 -> {}
        }
    }

    protected void onHBlankStart() {
        this.scannedEntries = 0;

        this.pixelX = 0;
        this.discardedPixels = 0;
        this.windowXCondition = false;

        this.backgroundFifo.clear();
        this.bgFifoStep = 0;
        this.bgFifoFirstFetch = true;
        this.bgFifoFetcherX = 0;
        this.bgFifoCurrentTileNumber = 0;
        this.bgFifoTileDataEffectiveAddress = 0;
        this.bgFifoTileDataLow = 0;
        this.bgFifoTileDataHigh = 0;

        Arrays.fill(this.spriteBuffer, -1);

        this.spriteFifo.clear();

        this.spriteFifoStep = 0;
        this.spriteFifoCurrentEntryIndex = -1;
        this.spriteFifoCurrentTileNumber = 0;
        this.spriteFifoTileDataEffectiveAddress = 0;
        this.spriteFifoTileDataLow = 0;
        this.spriteFifoTileDataHigh = 0;
    }

    private void onOAMScan() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                if (this.scanlineNumber == this.windowY) {
                    this.windowYCondition = true;
                }
                this.dotCycleIndex = 1;
            }
            case 1 -> {
                this.setSTATModeForInterrupt(Mode.OAM_SCAN_2.getValue());
                this.tickOAMScan();
                this.dotCycleIndex = 2;
            }
            case 2 -> {
                this.dotCycleIndex = 3;
            }
            case 3, 5 -> {
                this.tickOAMScan();
                this.dotCycleIndex = 4;
            }
            case 4 -> {
                this.dotCycleIndex = 5;
            }
        }
    }

    // TODO: The OAM bus has a 16-bit address bus. During OAM scan, the PPU reads only the X and Y bytes to determine whether they are in range.
    // During sprite fetching, the PPU fetches the attribute and tile index bytes at once.
    // Store only the X and Y bytes in the sprite buffer alongside the OAM index from which to calculate the attribute and tile index bytes fetch addresses.
    private void tickOAMScan() {
        int spriteY = this.getOAMByte(0xFE00 + (this.scannedEntries * 4));
        int spriteX = this.getOAMByte(0xFE00 + (this.scannedEntries * 4) + 1);
        int tileIndex = this.getOAMByte(0xFE00 + (this.scannedEntries * 4) + 2);
        int spriteAttributes = this.getOAMByte(0xFE00 + (this.scannedEntries * 4) + 3);
        for (int i = 0; i < 10; i++) {
            if (this.spriteBuffer[i] < 0) {
                if ((this.scanlineNumber + 16 >= spriteY) && (this.scanlineNumber + 16 < spriteY + (this.getObjectSize() ? 16 : 8))) {
                    this.spriteBuffer[i] = createSpriteBufferEntry(spriteY, spriteX, tileIndex, spriteAttributes);
                }
                break;
            }
        }
        this.scannedEntries++;
    }

    protected void checkOAMBugTrigger(int tCycle) {
        if (tCycle == 2) {
            if (this.armOAMBugRead) {
                this.doOAMBugRead();
            } else if (this.armOAMBugWrite) {
                this.doOAMBugWrite();
            }
            this.armOAMBugRead = false;
            this.armOAMBugWrite = false;
        }
    }

    protected void doOAMBugRead() {
        // The scanner reaches its first entry 4 dots into Mode 2, two entries behind the scan counter
        if (this.scannedEntries < 2) {
            return;
        }
        int cur = ((Math.max(this.scannedEntries - 2, 0) / 2) + 1) * 8;
        if (cur < 8 || cur >= 160 || !Mode.OAM_SCAN_2.matchesValue(this.getPPUMode())) {
            return;
        }
        int prev = cur - 8;
        int rowGroup = cur & 0x18;
        if (rowGroup == 0x08 || rowGroup == 0x18) {
            // Simple
            this.oam[prev] = (byte) ((this.oam[prev] | (this.oam[cur] & this.oam[prev + 4])));
            this.oam[prev + 1] = (byte) ((this.oam[prev + 1] | (this.oam[cur + 1] & this.oam[prev + 5])));
            for (int i = 0; i <= 7; i++) {
                this.oam[cur + i] = this.oam[prev + i];
            }
        } else if (rowGroup == 0x10 && cur < 0x98) {
            // Secondary
            int prev2 = cur - 16;
            this.oam[prev] = (byte) (((this.oam[prev] & (this.oam[prev2] | this.oam[cur] | this.oam[prev + 4])) | (this.oam[prev2] & this.oam[cur] & this.oam[prev + 4])));
            this.oam[prev + 1] = (byte) (((this.oam[prev + 1] & (this.oam[prev2 + 1] | this.oam[cur + 1] | this.oam[prev + 5])) | (this.oam[prev2 + 1] & this.oam[cur + 1] & this.oam[prev + 5])));
            for (int i = 0; i <= 7; i++) {
                this.oam[prev2 + i] = this.oam[prev + i];
                this.oam[cur + i] = this.oam[prev + i];
            }
        } else if (rowGroup == 0x00 && cur < 0x98) {
            // Tertiary / Quaternary
            int prev2 = cur - 16;
            int prev4 = cur - 32;
            if (cur == 0x20) {
                this.oam[prev] = (byte) (((this.oam[prev] & (this.oam[cur] | this.oam[prev + 4] | this.oam[prev2] | this.oam[prev4])) | (this.oam[cur] & this.oam[prev + 4] & this.oam[prev2] & this.oam[prev4])));
                this.oam[prev + 1] = (byte) (((this.oam[prev + 1] & (this.oam[cur + 1] | this.oam[prev + 5] | this.oam[prev2 + 1] | this.oam[prev4 + 1])) | (this.oam[cur + 1] & this.oam[prev + 5] & this.oam[prev2 + 1] & this.oam[prev4 + 1])));
            } else if (cur == 0x40) {
                // Quaternary
                this.oam[prev] = (byte) (((this.oam[prev] & (this.oam[cur] | this.oam[prev + 4] | this.oam[prev2] | this.oam[prev4] | (~this.oam[prev + 2] & this.oam[prev2 + 2]))) | (this.oam[prev + 4] & this.oam[prev2] & this.oam[prev4])));
                this.oam[prev + 1] = (byte) (((this.oam[prev + 1] & (this.oam[cur + 1] | this.oam[prev + 5] | this.oam[prev2 + 1] | this.oam[prev4 + 1] | (~this.oam[prev + 3] & this.oam[prev2 + 3]))) | (this.oam[prev + 5] & this.oam[prev2 + 1] & this.oam[prev4 + 1])));
            } else if (cur == 0x60) {
                this.oam[prev] = (byte) (((this.oam[prev] & (this.oam[cur] | this.oam[prev + 4] | this.oam[prev2] | this.oam[prev4])) | (this.oam[prev + 4] & this.oam[prev2] & this.oam[prev4])));
                this.oam[prev + 1] = (byte) (((this.oam[prev + 1] & (this.oam[cur + 1] | this.oam[prev + 5] | this.oam[prev2 + 1] | this.oam[prev4 + 1])) | (this.oam[prev + 5] & this.oam[prev2 + 1] & this.oam[prev4 + 1])));
            } else if (cur == 0x80) {
                this.oam[prev] = (byte) ((this.oam[prev] | (this.oam[cur] & this.oam[prev + 4] & this.oam[prev2] & this.oam[prev4])) & 0xFF);
                this.oam[prev + 1] = (byte) ((this.oam[prev + 1] | (this.oam[cur + 1] & this.oam[prev + 5] & this.oam[prev2 + 1] & this.oam[prev4 + 1])));
            }

            for (int i = 0; i <= 7; i++) {
                this.oam[prev2 + i] = this.oam[prev + i];
                this.oam[cur + i] = this.oam[prev + i];
            }

            if (cur == 0x80) {
                for (int i = 0; i <= 7; i++) {
                    this.oam[i] = this.oam[cur + i];
                }
            }
        }
    }

    protected void doOAMBugWrite() {
        if (this.scannedEntries < 2) {
            return;
        }
        int cur = ((Math.max(this.scannedEntries - 2, 0) / 2) + 1) * 8;
        if (cur < 8 || cur >= 160 || !Mode.OAM_SCAN_2.matchesValue(this.getPPUMode())) {
            return;
        }
        int prev = cur - 8;
        this.oam[cur] = bitwiseMajority(this.oam[cur], this.oam[prev], this.oam[prev + 4]);
        this.oam[cur + 1] = bitwiseMajority(this.oam[cur + 1], this.oam[prev + 1], this.oam[prev + 5]);
        for (int i = 2; i <= 7; i++) {
            this.oam[cur + i] = this.oam[prev + i];
        }
    }

    private static byte bitwiseMajority(byte x, byte y, byte z) {
        return (byte) (((int) x & (int) y) | ((int) x & (int) z) | ((int) y & (int) z));
    }

    private void onDrawing() {
        switch (this.dotCycleIndex) {
            case 0 -> {
                this.tickDraw();
                this.dotCycleIndex = 1;
            }
            case 1 -> {
                this.setSTATModeForInterrupt(Mode.DRAWING_3.getValue());
                this.tickDraw();
                this.dotCycleIndex = 2;
            }
            case 2 -> {
                this.tickDraw();
                this.dotCycleIndex = 3;
            }
            case 3 -> {
                this.tickDraw();
                this.dotCycleIndex = 4;
            }
            case 4 -> {
                this.tickDraw();
            }
        }
    }

    private void tickDraw() {
        boolean originalWindowCondition = this.isRenderingWindow();
        if (this.pixelX == this.windowX + 1 && this.getWindowEnable()) {
            this.windowXCondition = true;
        }

        if (!originalWindowCondition && this.isRenderingWindow()) {
            this.bgFifoStep = 0;
            this.bgFifoFetcherX = 0;
            this.windowPixelRendered = true;
            this.backgroundFifo.clear();
        }

        int currentSpriteEntryIndex = this.getSpriteEntryIndexMatchingX(this.pixelX);
        boolean fetchingSprite = this.isFetchingSprites(currentSpriteEntryIndex);

        if (!fetchingSprite) {
            this.tickPixelShifter();
        }

        if (!fetchingSprite || this.backgroundFifo.isEmpty() || (this.bgFifoStep <= 4)) {
            this.tickBackgroundFifo();
        } else {
            if (this.spriteFifoCurrentEntryIndex < 0) {
                this.spriteFifoCurrentEntryIndex = currentSpriteEntryIndex;
            }
            this.tickSpriteFifo();
        }

    }

    protected void tickBackgroundFifo() {
        switch (this.bgFifoStep) {
            case 0 -> {
                this.bgFifoStep = 1;
            }
            case 1 -> {
                if (this.isRenderingWindow()) {
                    int tileX = this.bgFifoFetcherX & 0x1F;
                    int tileY = this.windowLine >>> 3;
                    int tileMapIndex = tileX + (tileY * 32);
                    tileMapIndex &= 0x3FF;
                    int address = this.getWindowTileMap() + tileMapIndex;
                    this.bgFifoCurrentTileNumber = this.getVRAMByte(address);
                } else {
                    int tileX = ((this.pixelX + this.scrollX) >> 3) & 0x1F;
                    int tileY = ((this.scanlineNumber + this.scrollY) & 0xFF) >>> 3;
                    int tileMapIndex = tileX + (tileY * 32);
                    tileMapIndex &= 0x3FF;
                    int address = this.getBackgroundTileMap() + tileMapIndex;
                    this.bgFifoCurrentTileNumber = this.getVRAMByte(address);
                }
                this.bgFifoStep = 2;
            }
            case 2 -> {
                this.bgFifoStep = 3;
            }
            case 3 -> {
                if (this.bgFifoFirstFetch) {
                    this.bgFifoFirstFetch = false;
                    for (int i = 7; i >= 0; i--) {
                        this.backgroundFifo.set(0, 0);
                    }
                    this.backgroundFifo.setFull();
                    this.bgFifoStep = 0;
                } else {
                    int effectiveAddress;
                    if (this.getBackgroundAndWindowTiles()) {
                        effectiveAddress = 0x8000 + (this.bgFifoCurrentTileNumber * 16);
                    } else {
                        byte signedTileNumber =  (byte) this.bgFifoCurrentTileNumber;
                        effectiveAddress = 0x9000 + ((int) signedTileNumber * 16);
                    }

                    if (this.isRenderingWindow()) {
                        effectiveAddress = (effectiveAddress + (2 * (this.windowLine % 8))) & 0xFFFF;
                    } else {
                        effectiveAddress = (effectiveAddress + (2 * ((this.scanlineNumber + this.scrollY) % 8))) & 0xFFFF;
                    }

                    this.bgFifoTileDataEffectiveAddress = effectiveAddress;
                    this.bgFifoTileDataLow = this.getVRAMByte(effectiveAddress);

                    this.bgFifoStep = 4;
                }
            }
            case 4 -> {
                this.bgFifoStep = 5;
            }
            case 5 -> {
                this.bgFifoTileDataHigh = this.getVRAMByte((this.bgFifoTileDataEffectiveAddress + 1) & 0xFFFF);
                if (this.backgroundFifo.isEmpty()) {
                    this.pushBgPixels();
                    this.bgFifoStep = 0;
                } else {
                    this.bgFifoStep = 6;
                }
            }
            case 6 -> {
                if (this.backgroundFifo.isEmpty()) {
                    this.pushBgPixels();
                    this.bgFifoStep = 0;
                }
            }
        }
    }

    protected void pushBgPixels() {
        for (int i = 7; i >= 0; i--) {
            int low = (this.bgFifoTileDataLow >>> i) & 1;
            int high = (this.bgFifoTileDataHigh >>> i) & 1;
            this.backgroundFifo.set(7 - i, (high << 1) | low);
        }
        this.backgroundFifo.setFull();
        this.bgFifoFetcherX++;
    }

    protected boolean isFetchingSprites(int currentSpriteEntryIndex) {
        return currentSpriteEntryIndex >= 0 && this.getObjectEnable();
    }

    @SuppressWarnings("DuplicatedCode")
    protected void tickSpriteFifo() {
        switch (this.spriteFifoStep) {
            case 0 -> {
                this.spriteFifoStep = 1;
            }
            case 1 -> {
                this.spriteFifoCurrentTileNumber = getTileIndexFromSpriteEntry(this.spriteBuffer[this.spriteFifoCurrentEntryIndex]);
                this.spriteFifoStep = 2;
            }
            case 2 -> {
                this.spriteFifoStep = 3;
            }
            case 3 -> {
                boolean objSize = this.getObjectSize();
                long spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
                int spriteAttributes = getSpriteAttributesFromEntry(spriteEntry);
                boolean yFlip = getYFlipFromObjAttributes(spriteAttributes);
                int spriteY = getSpriteYFromSpriteEntry(spriteEntry);
                int tileIndex = this.spriteFifoCurrentTileNumber;

                int width = objSize ? 15 : 7;
                if (objSize) {
                    tileIndex &= ~1;
                }

                int row = ((this.scanlineNumber + 16) - spriteY) % (width + 1);
                if (row < 0) {
                    row += (width + 1);
                }

                int offset = yFlip ? (width - row) * 2 : row * 2;
                this.spriteFifoTileDataEffectiveAddress = (0x8000 + tileIndex * 16 + offset) & 0xFFFF;

                this.spriteFifoTileDataLow = this.getVRAMByte(this.spriteFifoTileDataEffectiveAddress);
                this.spriteFifoStep = 4;
            }
            case 4 -> {
                this.spriteFifoStep = 5;
            }
            case 5 -> {
                this.spriteFifoTileDataHigh = this.getVRAMByte((this.spriteFifoTileDataEffectiveAddress + 1) & 0xFFFF);

                long spriteEntry = this.spriteBuffer[this.spriteFifoCurrentEntryIndex];
                int spriteX = getSpriteXFromSpriteEntry(spriteEntry);
                int spriteAttributes = getSpriteAttributesFromEntry(spriteEntry);
                boolean xFlip = getXFlipFromObjAttributes(spriteAttributes);
                boolean priority = getPriorityFromObjAttributes(spriteAttributes);
                boolean palette = getDmgPaletteFromObjAttributes(spriteAttributes);

                for (int i = 0; i < 8; i++) {
                    if (spriteX + i < 8) {
                        continue;
                    }
                    int bit = xFlip ? 1 << i : 1 << (7 - i);
                    int low = (this.spriteFifoTileDataLow & bit) != 0 ? 1 : 0;
                    int high = (this.spriteFifoTileDataHigh & bit) != 0 ? 1 : 0;
                    int colorNumber = (low | (high << 1));
                    if (colorNumber == 0) {
                        continue;
                    }

                    int currentQueuedPixel = this.spriteFifo.get(i);
                    if (getDmgColorNumberFromObjPixelEntry(currentQueuedPixel) == 0) {
                        this.spriteFifo.set(i, createDmgObjPixelEntry(colorNumber, priority, palette));
                    }
                }

                this.spriteBuffer[this.spriteFifoCurrentEntryIndex] = -1;
                this.spriteFifoCurrentEntryIndex = -1;
                this.spriteFifoStep = 0;
            }
        }
    }

    protected void tickPixelShifter() {
        if (this.backgroundFifo.isEmpty()) {
            return;
        }

        int bgPixel = this.backgroundFifo.shiftHead(0);
        if (!this.getBackgroundAndWindowEnable()) {
            bgPixel = 0;
        }
        int bgPaletteIndex = (this.backgroundPalette >> (bgPixel * 2)) & 0b11;
        int finalPixel = DMG_PALETTE[bgPaletteIndex];

        int bgDiscardTarget = this.scrollX % 8;
        if (!this.isRenderingWindow() && this.discardedPixels < bgDiscardTarget) {
            this.discardedPixels++;
            finalPixel = -1;
        }

        int objPixel = this.spriteFifo.shiftHead(0);
        int objColorNumber = getDmgColorNumberFromObjPixelEntry(objPixel);
        if (!this.getObjectEnable()) {
            objColorNumber = 0;
        }
        if (objColorNumber != 0 && !(getDmgPriorityForObjPixelEntry(objPixel) && bgPixel != 0)) {
            int objPaletteIndex = ((getDmgPaletteForObjPixelEntry(objPixel) ? this.objectPalette1 : this.objectPalette0) >>> (objColorNumber * 2)) & 0b11;
            finalPixel = DMG_PALETTE[objPaletteIndex];
        }

        // TODO: Emulate color shown in the LCD during CPU STOP mode depending on which mode the STOP mode lands on. Same for CGB
        if (finalPixel >= 0) {
            if (this.pixelX >= 8 && this.enablePixelWrites) {
                this.lcd[(this.scanlineNumber * WIDTH) + (this.pixelX - 8)] = finalPixel;
            }
            this.pixelX++;
        }
    }

    protected boolean getLCDPPUEnable() {
        return this.lcdPPUEnable;
    }

    protected int getWindowTileMap() {
        return this.windowTileMap;
    }

    private boolean getWindowEnable() {
        return this.windowEnable;
    }

    protected boolean getBackgroundAndWindowTiles() {
        return this.backgroundAndWindowTiles;
    }

    protected int getBackgroundTileMap() {
        return this.backgroundTileMap;
    }

    protected boolean getObjectSize() {
        return this.objectSize;
    }

    protected boolean getObjectEnable() {
        return this.objectEnable;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean getBackgroundAndWindowEnable() {
        return this.backgroundAndWindowEnable;
    }

    private boolean getLYCInterruptSelect() {
        return this.lycInterruptSelect;
    }

    private boolean getMode2InterruptSelect() {
        return this.mode2InterruptSelect;
    }

    private boolean getMode1InterruptSelect() {
        return this.mode1InterruptSelect;
    }

    private boolean getMode0InterruptSelect() {
        return this.mode0InterruptSelect;
    }

    public boolean getLYEqualsLYCFlag() {
        return this.lyEqualsLYCFlag;
    }

    protected int getPPUMode() {
        return this.ppuMode;
    }

    private void setLYEqualsLYCFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 0b100;
            this.lyEqualsLYCFlag = true;
        } else {
            this.ppuStatus &= ~0b100;
            this.lyEqualsLYCFlag = false;
        }
    }

    private void setPPUMode(int mode) {
        this.ppuStatus = (this.ppuStatus & 0b11111100) | (mode & 0b11);
        this.ppuMode = mode & 0b11;
    }

    private void setSTATModeForInterrupt(int mode) {
        if (!(mode >= 0 && mode <= 3)) {
            throw new EmulatorException(new IllegalArgumentException("Invalid GameBoy PPU STAT mode for interrupt value %d!".formatted(mode)));
        }
        this.statModeForInterrupt = mode;
    }

    public void writeOAMDMA(int address, int value) {
        if (address >= OAM_START && address <= OAM_END) {
            this.oam[address - OAM_START] = (byte) value;
        } else {
            throw new EmulatorException("Invalid GameBoy OAM address \"%04X\"!".formatted(address));
        }
    }

    private int getOAMByte(int address) {
        if (address >= OAM_START && address <= OAM_END) {
            return (int) this.oam[address - OAM_START] & 0xFF;
        } else {
            throw new EmulatorException("Invalid GameBoy OAM address \"%04X\"!".formatted(address));
        }
    }

    protected int getVRAMByte(int address) {
        if (address >= VRAM_START && address <= VRAM_END) {
            return (int) this.vram[address - VRAM_START] & 0xFF;
        } else {
            throw new EmulatorException("Invalid GameBoy VRAM address \"%04X\"!".formatted(address));
        }
    }

    private int getSpriteEntryIndexMatchingX(int x) {
        for (int i = 0; i < 10; i++) {
             long spriteEntry = this.spriteBuffer[i];
            if (spriteEntry >= 0 && getSpriteXFromSpriteEntry(spriteEntry) == x) {
                return i;
            }
        }
        return -1;
    }

    protected boolean isRenderingWindow() {
        return this.windowXCondition && this.windowYCondition;
    }

    private void triggerVBlankInterrupt() {
        DMGBus<?> bus = this.emulator.getBus();
        bus.setIF(bus.getIF() | SM83.VBLANK_MASK);
    }

    private void triggerSTATInterrupt() {
        DMGBus<?> bus = this.emulator.getBus();
        bus.setIF(bus.getIF() | SM83.LCD_MASK);
    }

    private static long createSpriteBufferEntry(int spriteY, int spriteX, int tileIndex, int spriteAttributes) {
        return ((long) (spriteAttributes & 0xFF) << 24) | (long) ((tileIndex & 0xFF) << 16) | (long) ((spriteX & 0xFF) << 8) | (long) (spriteY & 0xFF);
    }

    protected static int getSpriteAttributesFromEntry(long entry) {
        return (int) (entry >>> 24) & 0xFF;
    }

    protected static int getTileIndexFromSpriteEntry(long entry) {
        return (int) (entry >>> 16) & 0xFF;
    }

    protected static int getSpriteXFromSpriteEntry(long entry) {
        return (int) (entry >>> 8) & 0xFF;
    }

    protected static int getSpriteYFromSpriteEntry(long entry) {
        return (int) (entry) & 0xFF;
    }

    protected static boolean getPriorityFromObjAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b10000000) != 0;
    }

    protected static boolean getYFlipFromObjAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b01000000) != 0;
    }

    protected static boolean getXFlipFromObjAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b00100000) != 0;
    }

    private static boolean getDmgPaletteFromObjAttributes(int spriteAttributes) {
        return (spriteAttributes & 0b00010000) != 0;
    }

    private static int createDmgObjPixelEntry(int colorNumber, boolean priority, boolean palette) {
        return ((palette ? 1 : 0) << 16) | ((priority ? 1 : 0) << 8) | colorNumber;
    }

    protected static int getDmgColorNumberFromObjPixelEntry(int pixel) {
        return pixel & 0b11;
    }

    protected static boolean getDmgPriorityForObjPixelEntry(int pixel) {
        return ((pixel >>> 8) & 1) != 0;
    }

    protected static boolean getDmgPaletteForObjPixelEntry(int pixel) {
        return ((pixel >>> 16) & 1) != 0;
    }

    public enum Mode {
        HBLANK_0(0),
        VBLANK_1(1),
        OAM_SCAN_2(2),
        DRAWING_3(3);

        private final int value;

        Mode(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public boolean matchesValue(int value) {
            return value == this.value;
        }

    }

}