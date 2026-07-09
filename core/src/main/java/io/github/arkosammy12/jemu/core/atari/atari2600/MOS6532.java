package io.github.arkosammy12.jemu.core.atari.atari2600;

import io.github.arkosammy12.jemu.core.common.Bus;

public class MOS6532<E extends MOS6532.SystemBus> implements Bus {

    private static final int ENABLE_TIMER_IRQ_ADDRESS_MASK = 1 << 3;

    private final E systemBus;

    private final byte[] ram = new byte[128];

    private int dataDirectionRegisterA;
    private int dataDirectionRegisterB;

    private int timer = 0;
    private int timerDivisor = 1;
    private int timerDivisorReload = 1;
    private boolean timerUnderflowed = false;
    private boolean enableTimerIrq = false;

    // TODO: Implement PA7 edge transition interrupt
    private boolean enablePA7Interrupt = false;
    private PA7EdgeDetect pa7EdgeDetect = PA7EdgeDetect.NEGATIVE;

    public MOS6532(E systemBus) {
        this.systemBus = systemBus;
    }

    @Override
    public int readByte(int address) {
        if ((address & 0x200) != 0) {
            return switch (address & 0b111) {
                case 0 -> (this.systemBus.readSWCHA(this.dataDirectionRegisterA) & ~this.dataDirectionRegisterA) & 0xFF;
                case 1 -> this.dataDirectionRegisterA;
                case 2 -> (this.systemBus.readSWCHB(this.dataDirectionRegisterB) & ~this.dataDirectionRegisterB) & 0xFF;
                case 3 -> this.dataDirectionRegisterB;
                default -> {
                    if ((address & (1 << 2)) != 0) {
                        if ((address & 1) != 0) {
                            // bit 7 = timer underflow, bit 6 = PA7 flag (stubbed)
                            // reading INSTAT clears the PA7 flag but NOT the timer flag
                            yield (this.timerUnderflowed ? 0x80 : 0x00);
                        } else {
                            this.timerUnderflowed = false;
                            this.enableTimerIrq = (address & ENABLE_TIMER_IRQ_ADDRESS_MASK) != 0;
                            yield this.timer & 0xFF;
                        }
                    } else {
                        yield this.systemBus.combineWithDataBus(0x00, 0x00);
                    }
                }
            };
        } else {
            return (int) this.ram[address & 0x7F] & 0xFF;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if ((address & 0x200) != 0) {
            switch (address & 0b111) {
                case 0 -> this.systemBus.writeSWCHA(value & this.dataDirectionRegisterA, this.dataDirectionRegisterA);
                case 1 -> this.dataDirectionRegisterA = value & 0xFF;
                case 2 -> this.systemBus.writeSWCHB(value & this.dataDirectionRegisterB, this.dataDirectionRegisterB);
                case 3 -> this.dataDirectionRegisterB = value & 0xFF;
                default -> {
                    if ((address & (1 << 4)) != 0) {
                        switch (address & 0b111) {
                            case 4 -> this.writeTimer(address, value, 1);
                            case 5 -> this.writeTimer(address, value, 8);
                            case 6 -> this.writeTimer(address, value, 64);
                            case 7 -> this.writeTimer(address, value, 1024);
                        }
                    } else if ((address & (1 << 2)) != 0) {
                        this.enablePA7Interrupt = (address & (1 << 1)) != 0;
                        this.pa7EdgeDetect = (address & 1) != 0 ? PA7EdgeDetect.POSITIVE : PA7EdgeDetect.NEGATIVE;
                    }
                }
            }
        } else {
            this.ram[address & 0x7F] = (byte) value;
        }
    }

    public boolean getIRQSignal() {
        return this.timerUnderflowed && this.enableTimerIrq;
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

    private void writeTimer(int address, int value, int divisor) {
        this.timer = value & 0xFF;
        this.timerDivisorReload = divisor;
        this.timerDivisor = divisor;
        this.timerUnderflowed = false;
        this.enableTimerIrq = (address & ENABLE_TIMER_IRQ_ADDRESS_MASK) != 0;
    }

    public interface SystemBus {

        int readSWCHA(int ddrA);

        int readSWCHB(int ddrB);

        void writeSWCHA(int value, int ddrA);

        void writeSWCHB(int value, int ddrB);

        int combineWithDataBus(int value, int validBitsMask);

    }

    private enum PA7EdgeDetect {
        POSITIVE,
        NEGATIVE
    }

}
