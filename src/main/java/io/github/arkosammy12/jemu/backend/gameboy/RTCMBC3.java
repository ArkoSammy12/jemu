package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;

public class RTCMBC3 extends MBC3 {

    private static final int CLOCK_FREQUENCY = 32768;

    private static final int HALT_MASK = 1 << 6;
    private static final int DAY_CARRY = 1 << 7;

    private static final int RTC_S_ADDR = 0x08;
    private static final int RTC_M_ADDR = 0x09;
    private static final int RTC_H_ADDR = 0x0A;
    private static final int RTC_DL_ADDR = 0x0B;
    private static final int RTC_DH_ADDR = 0x0C;

    private int seconds;
    private int minutes;
    private int hours;
    private int daysLower;
    private int daysUpperAndControl = 0b00111110;

    private int internalSeconds; // 0 - 59
    private int internalMinutes; // 0 - 59
    private int internalHours; // 0 - 23
    private int internalDays; // 0 - 511

    private int latchControl = -1;
    private int subSecondCounter;
    private int cycles;

    public RTCMBC3(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);
    }

    @Override
    public int readByte(int address) {
        if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramBankNumber >= 0x08 && this.ramBankNumber <= 0x0C && this.ramEnable == 0x0A) {
                return switch (this.ramBankNumber) {
                    case RTC_S_ADDR -> this.seconds;
                    case RTC_M_ADDR -> this.minutes;
                    case RTC_H_ADDR -> this.hours;
                    case RTC_DL_ADDR -> this.daysLower;
                    case RTC_DH_ADDR -> this.daysUpperAndControl;
                    default -> throw new EmulatorException("Invalid RTC register address \"%04X\" for the GameBoy MBC3 cartridge type!");
                };
            }
        }
        return super.readByte(address);
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramBankNumber >= 0x08 && this.ramBankNumber <= 0x0C && this.ramEnable == 0x0A) {
                switch (this.ramBankNumber) {
                    case RTC_S_ADDR -> {
                        this.internalSeconds = value & 0x3F;
                        this.subSecondCounter = 0;
                    }
                    case RTC_M_ADDR -> this.internalMinutes = value & 0x3F;
                    case RTC_H_ADDR -> this.internalHours = value & 0x1F;
                    case RTC_DL_ADDR -> this.internalDays = (this.internalDays & 0b100000000) | (value & 0xFF);
                    case RTC_DH_ADDR -> {
                        this.internalDays = ((value & 1) << 8) | (this.internalDays & 0xFF);
                        this.daysUpperAndControl = value & 0xC1;
                    }
                    default -> throw new EmulatorException("Invalid RTC register address \"$%04X\" for the GameBoy MBC3 cartridge type!");
                }
            }
        } else if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.latchControl == 0x00 && (value & 0xFF) == 0x01) {
                this.seconds = this.internalSeconds & 0xFF;
                this.minutes = this.internalMinutes & 0xFF;
                this.hours = this.internalHours & 0xFF;
                this.daysLower = this.internalDays & 0xFF;
                this.daysUpperAndControl = (this.daysUpperAndControl & 0b11111110) | ((this.internalDays & 0b100000000) >>> 8);
                this.daysUpperAndControl &= 0xC1;
            }
            this.latchControl = value & 0xFF;
        } else {
            super.writeByte(address, value);
        }
    }

    @Override
    public void cycle() {
        this.cycles++;

        if (this.cycles >= 32) {
            this.cycles = 0;

            if ((this.daysUpperAndControl & HALT_MASK) == 0) {
                this.subSecondCounter++;

                if (this.subSecondCounter >= CLOCK_FREQUENCY) {
                    this.subSecondCounter = 0;

                    this.internalSeconds++;
                    if (this.internalSeconds == 60) {
                        this.internalSeconds = 0;

                        this.internalMinutes++;
                        if (this.internalMinutes == 60) {
                            this.internalMinutes = 0;

                            this.internalHours++;
                            if (this.internalHours == 24) {
                                this.internalHours = 0;

                                this.internalDays++;
                                if (this.internalDays >= 512) {
                                    this.internalDays = 0;
                                    this.daysUpperAndControl |= DAY_CARRY;
                                }
                            } else if (this.internalHours == 32) {
                                this.internalHours = 0;
                            }
                        } else if (this.internalMinutes == 64) {
                            this.internalMinutes = 0;
                        }
                    } else if (this.internalSeconds == 64) {
                        this.internalSeconds = 0;
                    }
                }
            }
        }
    }

}
