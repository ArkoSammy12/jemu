package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.util.BidirectionalPin;
import io.github.arkosammy12.jemu.core.util.MOSIOPort;

public class MOS6526 implements Bus {

    private static final int PRA = 0x0;
    private static final int PRB = 0x1;
    private static final int DDRA = 0x2;
    private static final int DDRB = 0x3;
    private static final int TA_LO = 0x4;
    private static final int TA_HI = 0x5;
    private static final int TB_LO = 0x6;
    private static final int TB_HI = 0x7;
    private static final int TOD_10THS = 0x8;
    private static final int TOD_SEC = 0x9;
    private static final int TOD_MIN = 0xA;
    private static final int TOD_HR = 0xB;
    private static final int SDR = 0xC;
    private static final int ICR = 0xD;
    private static final int CRA = 0xE;
    private static final int CRB = 0xF;

    private final SystemBus systemBus;

    private boolean pcOutput;
    private boolean pcSetThisCycle;

    private boolean previousFLAG;

    private final TimerA timerA = new TimerA();
    private final TimerB timerB = new TimerB();

    private SerialPortMode serialPortMode = SerialPortMode.INPUT;
    private TimeOfDayInput timeOfDayInput = TimeOfDayInput.HZ_60;
    private AlarmMode alarmMode = AlarmMode.SET_TOD_CLOCK;

    private boolean interruptRequest;
    private boolean irqFLAG;
    private boolean irqSerialPort;
    private boolean irqAlarm;

    private boolean irqFLAGEnable;
    private boolean irqSerialPortEnable;
    private boolean irqAlarmEnable;

    private int todDivisorCounter;
    private boolean todClockRunning = true;
    private final AbstractTODCounter tenthsCounter = new TODCounter(0x09, 0b1111);
    private final AbstractTODCounter secondsCounter = new TODCounter(0x59, 0b1111111);
    private final AbstractTODCounter minutesCounter = new TODCounter(0x59, 0b1111111);
    private final AbstractTODCounter hoursCounter = new HoursTODCounter();

    private AmPmFlag amPmFlag = AmPmFlag.AM;
    private AmPmFlag alarmAmPmFlag = AmPmFlag.AM;

    private final MOSIOPort.DefaultPortOwner portOwnerA = new MOSIOPort.DefaultPortOwner();
    private final PortOwnerB portOwnerB = new PortOwnerB();

    public MOS6526(SystemBus systemBus) {
        this.systemBus = systemBus;
    }

    @Override
    public int readByte(int address) {
        address &= 0xF;
        return switch (address) {
            case PRA -> this.systemBus.getIOPortA().read();
            case PRB -> {
                this.pcOutput = true;
                this.pcSetThisCycle = true;
                yield this.systemBus.getIOPortB().read();
            }
            case DDRA -> this.portOwnerA.getDataDirectionRegister();
            case DDRB -> this.portOwnerB.getDataDirectionRegisterRaw();
            case TA_LO -> this.timerA.getTimerLow();
            case TA_HI -> this.timerA.getTimerHigh();
            case TB_LO -> this.timerB.getTimerLow();
            case TB_HI -> this.timerB.getTimerHigh();
            case TOD_10THS -> {
                this.tenthsCounter.setLatched(false);
                this.secondsCounter.setLatched(false);
                this.minutesCounter.setLatched(false);
                this.hoursCounter.setLatched(false);
                yield this.tenthsCounter.read();
            }
            case TOD_SEC -> this.secondsCounter.read();
            case TOD_MIN -> this.minutesCounter.read();
            case TOD_HR -> {
                this.tenthsCounter.setLatched(true);
                this.secondsCounter.setLatched(true);
                this.minutesCounter.setLatched(true);
                this.hoursCounter.setLatched(true);
                yield this.hoursCounter.read() | (this.amPmFlag == AmPmFlag.PM ? 1 << 7 : 0);
            }
            case SDR -> 0; // TODO: Implement serial
            case ICR -> {
                int ret = this.interruptRequest ? 1 << 7 : 0;
                ret |= this.irqFLAG ? 1 << 4 : 0;
                ret |= this.irqSerialPort ? 1 << 3 : 0;
                ret |= this.irqAlarm ? 1 << 2 : 0;
                ret |= this.timerB.getInterruptFlag() ? 1 << 1 : 0;
                ret |= this.timerA.getInterruptFlag() ? 1 : 0;

                this.interruptRequest = false;
                this.irqFLAG = false;
                this.irqSerialPort = false;
                this.irqAlarm = false;
                this.timerB.clearInterruptFlag();
                this.timerA.clearInterruptFlag();

                yield ret;
            }
            case CRA -> {
                int ret = this.timerA.getControlBits();
                ret |= this.serialPortMode == SerialPortMode.OUTPUT ? 1 << 6 : 0;
                ret |= this.timeOfDayInput == TimeOfDayInput.HZ_50 ? 1 << 7 : 0;
                yield ret;
            }
            case CRB -> this.timerB.getControlBits() | (this.alarmMode == AlarmMode.SET_ALARM ? 1 << 7 : 0);
            default -> 0;
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0xF;
        switch (address) {
            case PRA -> this.portOwnerA.setOutputLatch(value);
            case PRB -> {
                this.portOwnerB.setOutputLatch(value);
                this.pcOutput = true;
                this.pcSetThisCycle = true;
            }
            case DDRA -> this.portOwnerA.setDataDirectionRegister(value);
            case DDRB -> this.portOwnerB.setDataDirectionRegister(value);
            case TA_LO -> this.timerA.writeTimerLatchLow(value);
            case TA_HI -> this.timerA.writeTimerLatchHigh(value);
            case TB_LO -> this.timerB.writeTimerLatchLow(value);
            case TB_HI -> this.timerB.writeTimerLatchHigh(value);
            case TOD_10THS -> {
                this.todClockRunning = true;
                this.tenthsCounter.write(value);
            }
            case TOD_SEC -> this.secondsCounter.write(value);
            case TOD_MIN -> this.minutesCounter.write(value);
            case TOD_HR -> {
                this.todClockRunning = false;
                this.hoursCounter.write(value);
                AmPmFlag amPmFlag = (value & (1 << 7)) != 0 ? AmPmFlag.PM : AmPmFlag.AM;
                switch (this.alarmMode) {
                    case SET_ALARM -> this.alarmAmPmFlag = amPmFlag;
                    case SET_TOD_CLOCK -> this.amPmFlag = amPmFlag;
                }
            }
            case SDR -> {} // TODO: Implement serial
            case ICR -> {
                boolean interruptEnable = (value & (1 << 7)) != 0;
                if ((value & (1 << 4)) != 0) {
                    this.irqFLAGEnable = interruptEnable;
                    if (this.irqFLAGEnable && this.irqFLAG) {
                        this.interruptRequest = true;
                    }
                }
                if ((value & (1 << 3)) != 0) {
                    this.irqSerialPortEnable = interruptEnable;
                    if (this.irqSerialPortEnable && this.irqSerialPort) {
                        this.interruptRequest = true;
                    }
                }
                if ((value & (1 << 2)) != 0) {
                    this.irqAlarmEnable = interruptEnable;
                    if (this.irqAlarmEnable && this.irqAlarm) {
                        this.interruptRequest = true;
                    }
                }
                if ((value & (1 << 1)) != 0) {
                    this.timerB.setInterruptEnable(interruptEnable);
                    if (this.timerB.getInterruptFlag()) {
                        this.interruptRequest = true;
                    }
                }
                if ((value & 1) != 0) {
                    this.timerA.setInterruptEnable(interruptEnable);
                    if (this.timerA.getInterruptFlag()) {
                        this.interruptRequest = true;
                    }
                }
            }
            case CRA -> {
                this.writeTimer(this.timerA, value);
                this.timerA.setInMode((value & (1 << 5)) != 0 ? TimerA.InMode.CNT_RISING_EDGES : TimerA.InMode.PHI2_PULSES);
                this.serialPortMode = (value & (1 << 6)) != 0 ? SerialPortMode.OUTPUT : SerialPortMode.INPUT;
                this.timeOfDayInput = (value & (1 << 7)) != 0 ? TimeOfDayInput.HZ_50 : TimeOfDayInput.HZ_60;
            }
            case CRB -> {
                this.writeTimer(this.timerB, value);
                this.timerB.setInMode(switch ((value >>> 5) & 0b11) {
                    case 0b00 -> TimerB.InMode.PHI2_PULSES;
                    case 0b01 -> TimerB.InMode.CNT_RISING_EDGES;
                    case 0b10 -> TimerB.InMode.TIMER_A_UNDERFLOWS;
                    default -> TimerB.InMode.TIMER_A_UNDERFLOWS_CNT_HIGH;
                });
                this.alarmMode = (value & (1 << 7)) != 0 ? AlarmMode.SET_ALARM : AlarmMode.SET_TOD_CLOCK;
            }
        }
    }

    private void writeTimer(Timer timer, int value) {
        if ((value & 1) != 0) {
            timer.start();
        } else {
            timer.stop();
        }
        timer.setPortBEnabled((value & (1 << 1)) != 0);
        timer.setOutMode((value & (1 << 2)) != 0 ? Timer.OutMode.TOGGLE : Timer.OutMode.PULSE);
        timer.setRunMode((value & (1 << 3)) != 0 ? Timer.RunMode.ONE_SHOT : Timer.RunMode.CONTINUOUS);
        if ((value & (1 << 4)) != 0) {
            timer.forceLoad();
        }
    }

    public boolean getIRQ() {
        return this.interruptRequest;
    }

    public boolean getPC() {
        return this.pcOutput;
    }

    public MOSIOPort.DefaultPortOwner getPortOwnerA() {
        return this.portOwnerA;
    }

    public MOSIOPort.DefaultPortOwner getPortOwnerB() {
        return this.portOwnerB;
    }

    public void cycle() {
        if (this.pcSetThisCycle) {
            this.pcSetThisCycle = false;
        } else if (this.pcOutput) {
            this.pcOutput = false;
        }

        boolean flag = this.systemBus.getFLAG();
        if (!this.previousFLAG && flag) {
            this.irqFLAG = true;
            if (this.irqFLAGEnable) {
                this.interruptRequest = true;
            }
        }
        this.previousFLAG = flag;

        this.timerA.onCycle();
        this.timerB.onCycle();
    }

    public void clockTOD() {
        this.todDivisorCounter++;
        if (this.todDivisorCounter >= this.timeOfDayInput.todClockDivisor) {
            this.todDivisorCounter = 0;
            if (this.todClockRunning) {
                if (this.tenthsCounter.increment() && this.secondsCounter.increment() && this.minutesCounter.increment() && this.hoursCounter.increment()) {
                    this.amPmFlag = this.amPmFlag.getOpposite();
                }
                this.checkTODAlarm();
            }
        }
    }

    private void checkTODAlarm() {
        if (this.tenthsCounter.isAlarmValue() && this.secondsCounter.isAlarmValue() && this.minutesCounter.isAlarmValue() && this.hoursCounter.isAlarmValue() && this.amPmFlag == this.alarmAmPmFlag) {
            this.irqAlarm = true;
            if (this.irqAlarmEnable) {
                this.interruptRequest = true;
            }
        }
    }

    public void clockCNT() {
        this.timerA.onCNTClock();
        this.timerB.onCNTClock();
    }

    public interface SystemBus {

        MOSIOPort getIOPortA();

        MOSIOPort getIOPortB();

        boolean getFLAG();

        BidirectionalPin getSP();

        BidirectionalPin getCNT();
    }

    private enum SerialPortMode {
        OUTPUT,
        INPUT
    }

    private enum TimeOfDayInput {
        HZ_50(5),
        HZ_60(6);

        private final int todClockDivisor;

        TimeOfDayInput(int todClockDivisor) {
            this.todClockDivisor = todClockDivisor;
        }

    }

    private enum AlarmMode {
        SET_ALARM,
        SET_TOD_CLOCK
    }

    private enum AmPmFlag {
        AM,
        PM;

        private AmPmFlag getOpposite() {
            return switch (this) {
                case AM -> PM;
                case PM -> AM;
            };
        }

    }

    private class PortOwnerB extends MOSIOPort.DefaultPortOwner {

        protected int getDataDirectionRegisterRaw() {
            return this.dataDirectionRegister;
        }

        @Override
        public int getDataDirectionRegister() {
            int ret = super.getDataDirectionRegister();
            if (timerA.portBOutputEnabled()) {
                ret |= 1 << 6;
            }
            if (timerB.portBOutputEnabled()) {
                ret |= 1 << 7;
            }
            return ret;
        }

        @Override
        public int getOutputLatch() {
            int ret = super.getOutputLatch();
            if (timerA.portBOutputEnabled()) {
                ret = (ret & ~(1 << 6)) | (timerA.getPortBOutput() ? 1 << 6 : 0);
            }
            if (timerB.portBOutputEnabled()) {
                ret = (ret & ~(1 << 7)) | (timerB.getPortBOutput() ? 1 << 7 : 0);
            }
            return ret;
        }

    }

    private abstract class AbstractTODCounter {

        private final int writeMask;

        private boolean latch;
        protected int counter;
        private int counterLatch;
        private int alarmValue;

        private AbstractTODCounter(int writeMask) {
            this.writeMask = writeMask;
        }

        private int read() {
            return this.latch ? this.counterLatch : this.counter;
        }

        private void write(int value) {
            switch (alarmMode) {
                case SET_ALARM -> this.alarmValue = value & this.writeMask;
                case SET_TOD_CLOCK -> {
                    this.counter = value & this.writeMask;
                    checkTODAlarm();
                }
            }
        }

        private void setLatched(boolean latch) {
            this.latch = latch;
            if (this.latch) {
                this.counterLatch = this.counter;
            }
        }

        abstract protected boolean increment();

        private boolean isAlarmValue() {
            return this.counter == this.alarmValue;
        }

    }

    private class TODCounter extends AbstractTODCounter {

        private final int maxLowerDigitMask;
        private final int maxUpperDigitMask;

        private final int lowerDigitWriteMask;
        private final int upperDigitWriteMask;

        private TODCounter(int maximumValue, int writeMask) {
            super(writeMask);
            this.maxLowerDigitMask = maximumValue & 0x0F;
            this.maxUpperDigitMask = maximumValue & 0xF0;
            this.lowerDigitWriteMask = writeMask & 0x0F;
            this.upperDigitWriteMask = writeMask & 0xF0;
        }

        @Override
        protected boolean increment() {
            boolean carry = false;

            if ((this.counter & 0x0F) == this.maxLowerDigitMask) {
                this.counter &= 0xF0;

                if ((this.counter & 0xF0) == this.maxUpperDigitMask) {
                    this.counter = 0x00;
                    carry = true;
                } else {
                    this.counter = (((this.counter & 0xF0) + 0x10) & this.upperDigitWriteMask) | (this.counter & this.lowerDigitWriteMask);
                }

            } else {
                this.counter = (this.counter & this.upperDigitWriteMask) | (((this.counter & 0xF) + 1) & this.lowerDigitWriteMask);
            }

            return carry;
        }

    }

    private class HoursTODCounter extends AbstractTODCounter {

        private HoursTODCounter() {
            super(0b11111);
            this.counter = 0x01;
        }

        @Override
        protected boolean increment() {

            boolean carry = false;

            int counterLowerDigit = this.counter & 0xF;
            if ((this.counter & 0b10000) != 0) {
                if (counterLowerDigit == 0x02) {
                    this.counter = 0x01;
                } else {
                    if (counterLowerDigit == 0x01) {
                        carry = true;
                    }
                    this.counter = (this.counter & 0b10000) | (((this.counter & 0xF) + 1) & 0xF);
                }
            } else {
                if (counterLowerDigit == 0x09) {
                    this.counter = 0x10;
                } else {
                    this.counter = (this.counter & 0b10000) | (((this.counter & 0xF) + 1) & 0xF);
                }
            }

            return carry;
        }

    }

    private abstract class Timer {

        private boolean running;
        private boolean outputOnPortB;
        private OutMode outMode = OutMode.PULSE;
        private RunMode runMode = RunMode.CONTINUOUS;

        private int timerLatch;
        private int timerCounter;

        private boolean interruptFlag;
        private boolean interruptEnable;

        private boolean previousCNT;

        private boolean toggleOutput; // TODO: Set low by !RES input
        private boolean pulseOutput;
        private boolean pulseOutputSetOnThisCycle;

        protected void writeTimerLatchLow(int value) {
            this.timerLatch = (this.timerLatch & 0xFF00) | (value & 0x00FF);
        }

        protected void writeTimerLatchHigh(int value) {
            this.timerLatch = ((value & 0xFF) << 8) | (this.timerLatch & 0x00FF);
            if (!this.running) {
                this.timerCounter = this.timerLatch;
            }
        }

        protected int getTimerLow() {
            return this.timerCounter & 0xFF;
        }

        protected int getTimerHigh() {
            return (this.timerCounter >>> 8) & 0xFF;
        }

        private void start() {
            if (!this.running) {
                this.toggleOutput = true;
            }
            this.running = true;
        }

        private void stop() {
            this.running = false;
        }

        private void setPortBEnabled(boolean value) {
            this.outputOnPortB = value;
        }

        protected boolean portBOutputEnabled() {
            return this.outputOnPortB;
        }

        protected boolean getPortBOutput() {
            return switch (this.outMode) {
                case PULSE -> this.pulseOutput;
                case TOGGLE -> this.toggleOutput;
            };
        }

        private void setOutMode(OutMode outMode) {
            this.outMode = outMode;
        }

        private void setRunMode(RunMode runMode) {
            this.runMode = runMode;
        }

        private void forceLoad() {
            this.timerCounter = this.timerLatch;
        }

        protected boolean getInterruptFlag() {
            return this.interruptFlag;
        }

        protected void clearInterruptFlag() {
            this.interruptFlag = false;
        }

        protected void setInterruptEnable(boolean value) {
            this.interruptEnable = value;
        }

        protected int getControlBits() {
            int ret = this.running ? 1 : 0;
            ret |= this.outputOnPortB ? 1 << 1 : 0;
            ret |= this.outMode == OutMode.TOGGLE ? 1 << 2 : 0;
            ret |= this.runMode == RunMode.ONE_SHOT ? 1 << 3 : 0;
            return ret;
        }

        protected void onCycle() {
            if (this.pulseOutputSetOnThisCycle) {
                this.pulseOutputSetOnThisCycle = false;
            } else if (this.pulseOutput) {
                this.pulseOutput = false;
            }
        }

        abstract protected void onCNTClock();

        protected void clockTimer() {
            if (this.running) {
                this.timerCounter--;
                if (this.timerCounter < 0) {
                    this.onTimerUnderflow();
                }
            }
        }

        protected void onTimerUnderflow() {
            this.timerCounter = this.timerLatch;
            this.toggleOutput = !this.toggleOutput;
            this.pulseOutput = true;
            this.pulseOutputSetOnThisCycle = true;
            this.interruptFlag = true;
            if (this.interruptEnable) {
                interruptRequest = true;
            }
            this.running = switch (this.runMode) {
                case CONTINUOUS -> true;
                case ONE_SHOT -> false;
            };
        }

        protected boolean isCNTRisingEdge() {
            boolean currentCNT = systemBus.getCNT().read();
            boolean risingEdge = !this.previousCNT && currentCNT;
            this.previousCNT = currentCNT;
            return risingEdge;
        }

        private enum OutMode {
            TOGGLE,
            PULSE
        }

        private enum RunMode {
            ONE_SHOT,
            CONTINUOUS
        }

    }

    private class TimerA extends Timer {

        private InMode inMode = InMode.PHI2_PULSES;

        private void setInMode(InMode inMode) {
            this.inMode = inMode;
        }

        @Override
        protected int getControlBits() {
            return super.getControlBits() | (this.inMode == InMode.CNT_RISING_EDGES ? 1 << 5 : 0);
        }

        @Override
        protected void onCycle() {
            super.onCycle();
            boolean isCNTRising = this.isCNTRisingEdge();
            switch (this.inMode) {
                case PHI2_PULSES -> this.clockTimer();
                case CNT_RISING_EDGES -> {
                    if (isCNTRising) {
                        this.clockTimer();
                    }
                }
            }
        }

        @Override
        protected void onTimerUnderflow() {
            super.onTimerUnderflow();
            timerB.onTimerAUnderflow();
        }

        @Override
        protected void onCNTClock() {
            if (this.inMode == InMode.CNT_RISING_EDGES) {
                this.clockTimer();
            }
        }

        private enum InMode {
            CNT_RISING_EDGES,
            PHI2_PULSES
        }

    }

    private class TimerB extends Timer {

        private InMode inMode = InMode.PHI2_PULSES;

        private void setInMode(InMode inMode) {
            this.inMode = inMode;
        }

        @Override
        protected int getControlBits() {
            return super.getControlBits() | ((switch (this.inMode) {
                case PHI2_PULSES -> 0b00;
                case CNT_RISING_EDGES -> 0b01;
                case TIMER_A_UNDERFLOWS -> 0b10;
                case TIMER_A_UNDERFLOWS_CNT_HIGH -> 0b11;
            }) << 5);
        }

        @Override
        protected void onCycle() {
            super.onCycle();
            boolean isCNTRising = this.isCNTRisingEdge();
            switch (this.inMode) {
                case PHI2_PULSES -> this.clockTimer();
                case CNT_RISING_EDGES -> {
                    if (isCNTRising) {
                        this.clockTimer();
                    }
                }
            }
        }

        @Override
        protected void onCNTClock() {
            if (this.inMode == InMode.CNT_RISING_EDGES) {
                this.clockTimer();
            }
        }

        private void onTimerAUnderflow() {
            switch (this.inMode) {
                case TIMER_A_UNDERFLOWS -> this.clockTimer();
                case TIMER_A_UNDERFLOWS_CNT_HIGH -> {
                    if (systemBus.getCNT().read()) {
                        this.clockTimer();
                    }
                }
            }
        }

        private enum InMode {
            PHI2_PULSES,
            CNT_RISING_EDGES,
            TIMER_A_UNDERFLOWS,
            TIMER_A_UNDERFLOWS_CNT_HIGH
        }

    }

}
