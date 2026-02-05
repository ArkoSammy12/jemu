package io.github.arkosammy12.jemu.systems.misc.gameboy;

import io.github.arkosammy12.jemu.systems.GameBoyEmulator;
import io.github.arkosammy12.jemu.systems.bus.Bus;
import org.tinylog.Logger;

public class GameBoyMMIOController implements Bus {

    public static final int JOYP_ADDR = 0xFF00;
    public static final int SB_ADDR = 0xFF01;
    public static final int SC_ADDR = 0xFF02;

    public static final int DIV_ADDR = 0xFF04;
    public static final int TIMA_ADDR = 0xFF05;
    public static final int TMA_ADDR = 0xFF06;
    public static final int TAC_ADDR = 0xFF07;

    public static final int IF_ADDR = 0xFF0F;

    public static final int NR10_ADDR = 0xFF10;
    public static final int NR11_ADDR = 0xFF11;
    public static final int NR12_ADDR = 0xFF12;
    public static final int NR13_ADDR = 0xFF13;
    public static final int NR14_ADDR = 0xFF14;

    public static final int NR21_ADDR = 0xFF16;
    public static final int NR22_ADDR = 0xFF17;
    public static final int NR23_ADDR = 0xFF18;
    public static final int NR24_ADDR = 0xFF19;
    public static final int NR30_ADDR = 0xFF1A;
    public static final int NR31_ADDR = 0xFF1B;
    public static final int NR32_ADDR = 0xFF1C;
    public static final int NR33_ADDR = 0xFF1D;
    public static final int NR34_ADDR = 0xFF1E;

    public static final int NR41_ADDR = 0xFF20;
    public static final int NR42_ADDR = 0xFF21;
    public static final int NR43_ADDR = 0xFF22;
    public static final int NR44_ADDR = 0xFF23;
    public static final int NR50_ADDR = 0xFF24;
    public static final int NR51_ADDR = 0xFF25;
    public static final int NR52_ADDR = 0xFF26;

    public static final int WAVERAM_START = 0xFF30;
    public static final int WAVERAM_END = 0xFF3F;

    public static final int LCDC_ADDR = 0xFF40;
    public static final int STAT_ADDR = 0xFF41;
    public static final int SCY_ADDR = 0xFF42;
    public static final int SCX_ADDR = 0xFF43;
    public static final int LY_ADDR = 0xFF44;
    public static final int LYC_ADDR = 0xFF45;
    public static final int DMA_ADDR = 0xFF46;
    public static final int BGP_ADDR = 0xFF47;
    public static final int OBP0_ADDR = 0xFF48;
    public static final int OPB1_ADDR = 0xFF49;
    public static final int WY_ADDR = 0xFF4A;
    public static final int WX_ADDR = 0xFF4B;

    public static final int BANK_ADDR = 0xFF50;

    public static final int IE_ADDR = 0xFFFF;

    private final GameBoyEmulator emulator;

    private int joypad;

    private int interruptFlag;
    private int interruptEnable;

    public GameBoyMMIOController(GameBoyEmulator emulator) {
        this.emulator = emulator;
    }

    public int getIE() {
        return this.interruptEnable;
    }

    public void setIF(int value) {
        this.interruptFlag = value;
    }

    public int getIF() {
        return interruptFlag;
    }

    @Override
    public void writeByte(int address, int value) {
        if (address == JOYP_ADDR) {
            this.joypad = value & 0xFF;
        } else if (address == SB_ADDR) {
            System.out.print((char) value);
        } else if (address == SC_ADDR) {

        } else if (address >= DIV_ADDR && address <= TAC_ADDR) {
            this.emulator.getTimerController().writeByte(address, value);
        } else if (address == IF_ADDR) {
            //Logger.info(value & 0xFF);
            this.interruptFlag = value & 0xFF;
        } else if ((address >= NR10_ADDR && address <= NR14_ADDR) || (address >= NR21_ADDR && address <= NR34_ADDR) || (address >= NR41_ADDR && address <= NR52_ADDR) || (address >= WAVERAM_START && address <= WAVERAM_END)) {
            // TODO: APU
        } else if (address >= LCDC_ADDR && address <= WX_ADDR) {
            // TODO: PPU
        } else if (address == BANK_ADDR) {

        } else if (address == IE_ADDR) {
            this.interruptEnable = value & 0xFF;
        }
    }

    @Override
    public int readByte(int address) {
        if (address == JOYP_ADDR) {
            return joypad;
        } else if (address == SB_ADDR) {
            return 0xFF;
        } else if (address == SC_ADDR) {
            return 0xFF;
        } else if (address >= DIV_ADDR && address <= TAC_ADDR) {
            return this.emulator.getTimerController().readByte(address);
        } else if (address == IF_ADDR) {
            return this.interruptFlag;
        } else if ((address >= NR10_ADDR && address <= NR14_ADDR) || (address >= NR21_ADDR && address <= NR34_ADDR) || (address >= NR41_ADDR && address <= NR52_ADDR) || (address >= WAVERAM_START && address <= WAVERAM_END)) {
            // TODO: APU
            return 0xFF;
        } else if (address >= LCDC_ADDR && address <= WX_ADDR) {
            // TODO: PPU
            return 0xFF;
        } else if (address == BANK_ADDR) {
            return 0xFF;
        } else if (address == IE_ADDR) {
            return this.interruptEnable;
        } else {
            return 0xFF;
        }
    }

}
