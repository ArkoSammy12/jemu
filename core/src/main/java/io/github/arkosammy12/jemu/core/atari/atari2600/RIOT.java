package io.github.arkosammy12.jemu.core.atari.atari2600;

import io.github.arkosammy12.jemu.core.common.Bus;

public class RIOT<E extends Atari2600Emulator> implements Bus {

    private static final int SWCHA = 0x00;
    private static final int SWACNT = 0x01;
    private static final int SWCHB = 0x02;
    private static final int SWBCNT = 0x03;
    private static final int INTIM = 0x04;
    private static final int INSTAT = 0x05;

    private static final int TIM1T = 0x14;
    private static final int TIM8T = 0x15;
    private static final int TIM64T = 0x16;
    private static final int T1024T = 0x17;

    private final E emulator;

    private final byte[] ram = new byte[128];

    private int timer = 0;
    private int timerDivisor = 1;
    private int timerDivisorReload = 1;
    private boolean timerUnderflowed = false;

    public RIOT(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        if ((address & 0x200) != 0) {
            int reg = address & 0x1F;
            return switch (reg) {
                case SWCHA -> 0;
                case SWACNT -> 0;
                case SWCHB -> 0;
                case SWBCNT -> 0;
                case INTIM -> {
                    // reading INTIM clears the underflow flag
                    this.timerUnderflowed = false;
                    yield this.timer & 0xFF;
                }
                case INSTAT -> {
                    // bit 7 = timer underflow, bit 6 = PA7 flag (stubbed)
                    // reading INSTAT clears the PA7 flag but NOT the timer flag
                    yield (this.timerUnderflowed ? 0x80 : 0x00);
                }
                default -> this.emulator.getBus().combineWithDataBus(0, 0);
            };
        } else {
            return (int) this.ram[address & 0x7F] & 0xFF;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if ((address & 0x200) != 0) {
            int reg = address & 0x1F;
            switch (reg) {
                case SWCHA -> {}
                case SWACNT -> {}
                case SWCHB -> {}
                case SWBCNT -> {}
                case TIM1T -> {
                    this.timer = value & 0xFF;
                    this.timerDivisorReload = 1;
                    this.timerDivisor = 1;
                    this.timerUnderflowed = false;
                }
                case TIM8T -> {
                    this.timer = value & 0xFF;
                    this.timerDivisorReload = 8;
                    this.timerDivisor = 8;
                    this.timerUnderflowed = false;
                }
                case TIM64T -> {
                    this.timer = value & 0xFF;
                    this.timerDivisorReload = 64;
                    this.timerDivisor = 64;
                    this.timerUnderflowed = false;
                }
                case T1024T -> {
                    this.timer = value & 0xFF;
                    this.timerDivisorReload = 1024;
                    this.timerDivisor = 1024;
                    this.timerUnderflowed = false;
                }
            }
        } else {
            this.ram[address & 0x7F] = (byte) value;
        }
    }

    public void cycle() {
        this.timerDivisor--;
        if (this.timerDivisor <= 0) {
            this.timerDivisor = this.timerDivisorReload;
            this.timer--;
            if (this.timer < 0) {
                this.timer = 0xFF;
                this.timerUnderflowed = true;
                this.timerDivisorReload = 1;
                this.timerDivisor = 1;
            }
        }
    }

}
