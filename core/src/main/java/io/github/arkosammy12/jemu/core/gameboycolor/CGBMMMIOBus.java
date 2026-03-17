package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.gameboy.DMGMMIOBus;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;

public class CGBMMMIOBus extends DMGMMIOBus {

    public static final int KEY_0 = 0xFF4C;
    public static final int KEY_1 = 0xFF4D;

    public static final int VBK = 0xFF4F;

    public static final int HDMA_1 = 0xFF51;
    public static final int HDMA_2 = 0xFF52;
    public static final int HDMA_3 = 0xFF53;
    public static final int HDMA_4 = 0xFF54;
    public static final int HDMA_5 = 0xFF55;

    public static final int RP = 0xFF56;

    public static final int BGPI = 0xFF68;
    public static final int BGPD = 0xFF69;
    public static final int OBPI = 0xFF6A;
    public static final int OBPD = 0xFF6B;
    public static final int OPRI = 0xFF6C;

    public static final int WBK = 0xFF70;

    // Undocumented
    public static final int UNK_1 = 0xFF72;
    public static final int UNK_2 = 0xFF73;
    public static final int UNK_3 = 0xFF74;
    public static final int UNK_4 = 0xFF75;

    private boolean dmgCompatibilityMode;
    private int workRamBank = 1;

    public CGBMMMIOBus(GameBoyEmulator emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address) {
        if (address == KEY_0) {
            return this.dmgCompatibilityMode ? 0xFF : 0xFB;
        } else if (address == WBK) {
            return this.workRamBank | 0b11111000;
        } else if (address >= BGPI && address <= OPRI || address == VBK) {
            return this.emulator.getVideoGenerator().readByte(address);
        } else {
            return super.readByte(address);
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address == KEY_0) {
            if (this.emulator.getBus().isBootRomEnabled()) {
                this.dmgCompatibilityMode = (value & 0b100) != 0;
            }
        } else if (address == WBK) {
            this.workRamBank = value & 0b111;
            if (this.workRamBank == 0) {
                this.workRamBank = 1;
            }
        } else if (address >= BGPI && address <= OPRI || address == VBK) {
            //if (address != OPRI || this.emulator.getBus().isBootRomEnabled()) {
            // TODO: Properly investigate when exactly this applies
                this.emulator.getVideoGenerator().writeByte(address, value);
            //}
        } else {
            super.writeByte(address, value);
        }
    }

    public boolean isDmgCompatibilityMode() {
        return this.dmgCompatibilityMode;
    }

    public int getWorkRamBank() {
        return this.workRamBank;
    }

}
