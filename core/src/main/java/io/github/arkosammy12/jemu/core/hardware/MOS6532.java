package io.github.arkosammy12.jemu.core.hardware;

import io.github.arkosammy12.jemu.core.common.Bus;

public class MOS6532<E extends MOS6532.SystemBus> implements Bus {

    private static final int ENABLE_TIMER_IRQ_ADDRESS_MASK = 1 << 3;

    private final E systemBus;

    private final byte[] ram = new byte[128];

    private int outputLatchA;
    private int outputLatchB;

    private int dataDirectionRegisterA;
    private int dataDirectionRegisterB;

    private int timer = 0;
    private int timerDivisor = 1;
    private int timerDivisorCounter = 1;
    private boolean timerUnderflowedThisCycle = false;
    private boolean timerUnderflowed = false;
    private boolean enableTimerIrq = false;

    private boolean pa7InterruptFlag;
    private boolean enablePA7Interrupt = false;
    private PA7EdgeDetect pa7EdgeDetectMode = PA7EdgeDetect.NEGATIVE;

    private boolean oldOutputLatchAPA7;
    private boolean oldSWCHAAPA7;

    public MOS6532(E systemBus) {
        this.systemBus = systemBus;
        this.oldSWCHAAPA7 = (this.systemBus.readSWCHA(this.dataDirectionRegisterA) & (1 << 7)) != 0;
    }

    @Override
    public int readByte(int address) {
        if ((address & 0x200) != 0) {
            return switch (address & 0b111) {
                case 0 -> ((this.outputLatchA & this.dataDirectionRegisterA) | (this.systemBus.readSWCHA(this.dataDirectionRegisterA) & ~this.dataDirectionRegisterA)) & 0xFF;
                case 1 -> this.dataDirectionRegisterA;
                case 2 -> ((this.outputLatchB & this.dataDirectionRegisterB) | (this.systemBus.readSWCHB(this.dataDirectionRegisterB) & ~this.dataDirectionRegisterB)) & 0xFF;
                case 3 -> this.dataDirectionRegisterB;
                default -> {
                    if ((address & (1 << 2)) != 0) {
                        if ((address & 1) != 0) {
                            int ret = (this.timerUnderflowed ? (1 << 7) : 0) | (this.pa7InterruptFlag ? (1 << 6) : 0);
                            this.pa7InterruptFlag = false;
                            yield ret;
                        } else {
                            if (!this.timerUnderflowedThisCycle) {
                                this.timerUnderflowed = false;
                            }
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
                case 0 -> {
                    this.outputLatchA = value & 0xFF;
                    this.systemBus.writeSWCHA(this.outputLatchA & this.dataDirectionRegisterA, this.dataDirectionRegisterA);
                }
                case 1 -> this.dataDirectionRegisterA = value & 0xFF;
                case 2 -> {
                    this.outputLatchB = value & 0xFF;
                    this.systemBus.writeSWCHB(this.outputLatchB & this.dataDirectionRegisterB, this.dataDirectionRegisterB);
                }
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
                        this.pa7EdgeDetectMode = (address & 1) != 0 ? PA7EdgeDetect.POSITIVE : PA7EdgeDetect.NEGATIVE;

                        // According to the MOS 6532 RIOT datasheet, the flag may also be set by changing the polarity. So we take the current value of
                        // PA7 and use it as the new edge transition value, and depending on the selected edge detection mode, we may set the flag.
                        boolean currentPA7Level = (this.dataDirectionRegisterA & (1 << 7)) != 0 ? this.oldOutputLatchAPA7 : this.oldSWCHAAPA7;
                        if ((this.pa7EdgeDetectMode == PA7EdgeDetect.POSITIVE && currentPA7Level) || (this.pa7EdgeDetectMode == PA7EdgeDetect.NEGATIVE && !currentPA7Level)) {
                            this.pa7InterruptFlag = true;
                        }
                    }
                }
            }
        } else {
            this.ram[address & 0x7F] = (byte) value;
        }
    }

    public boolean getIRQSignal() {
        return (this.timerUnderflowed && this.enableTimerIrq) || (this.pa7InterruptFlag && this.enablePA7Interrupt);
    }

    public void cycle() {
        boolean currentOutputLatchAPA7 = (this.outputLatchA & (1 << 7)) != 0;
        boolean currentSWCHAAPA7 = (this.systemBus.readSWCHA(this.dataDirectionRegisterA) & (1 << 7)) != 0;

        boolean oldPA7Level;
        boolean currentPA7Level;
        if ((this.dataDirectionRegisterA & (1 << 7)) != 0) {
            oldPA7Level = this.oldOutputLatchAPA7;
            currentPA7Level = currentOutputLatchAPA7;
        } else {
            oldPA7Level = this.oldSWCHAAPA7;
            currentPA7Level = currentSWCHAAPA7;
        }

        if (this.pa7EdgeDetectMode == PA7EdgeDetect.POSITIVE && !oldPA7Level && currentPA7Level) {
            this.pa7InterruptFlag = true;
        } else if (this.pa7EdgeDetectMode == PA7EdgeDetect.NEGATIVE && oldPA7Level && !currentPA7Level) {
            this.pa7InterruptFlag = true;
        }

        this.oldOutputLatchAPA7 = currentOutputLatchAPA7;
        this.oldSWCHAAPA7 = currentSWCHAAPA7;

        this.timerUnderflowedThisCycle = false;

        this.timerDivisorCounter--;
        boolean timerDivisorCounterReached0 = this.timerDivisorCounter <= 0;
        if (timerDivisorCounterReached0 || this.timerUnderflowed) {
            this.timer--;
            if (this.timer < 0) {
                this.timer = 0xFF;
                if (!this.timerUnderflowed) {
                    this.timerUnderflowedThisCycle = true;
                }
                this.timerUnderflowed = true;
            }
        }

        if (timerDivisorCounterReached0) {
            this.timerDivisorCounter = this.timerDivisor;
        }

    }

    private void writeTimer(int address, int value, int divisor) {
        this.timer = value & 0xFF;
        this.timerDivisor = divisor;
        this.timerDivisorCounter = 1;
        if (!this.timerUnderflowedThisCycle) {
            this.timerUnderflowed = false;
        }
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
