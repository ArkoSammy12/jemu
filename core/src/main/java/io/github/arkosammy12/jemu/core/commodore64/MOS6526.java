package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.util.ActionSignalDispatcher;
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

    private final ActionSignalDispatcher actionSignalDispatcher = new ActionSignalDispatcher();
    private final int triggerSerialInterruptSignal;

    private final TimerA timerA = new TimerA();
    private final TimerB timerB = new TimerB();

    private boolean pcOutput;
    private boolean pcSetThisCycle;

    private boolean previousFLAG;
    private boolean previousCNT;

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
    private boolean todClockRunning = false;
    private final AbstractTODCounter tenthsCounter = new TODCounter(0x09, 0b1111);
    private final AbstractTODCounter secondsCounter = new TODCounter(0x59, 0b1111111);
    private final AbstractTODCounter minutesCounter = new TODCounter(0x59, 0b1111111);
    private final AbstractTODCounter hoursCounter = new HoursTODCounter();

    private AmPmFlag amPmFlag = AmPmFlag.AM;
    private AmPmFlag alarmAmPmFlag = AmPmFlag.AM;

    private final MOSIOPort.DefaultPortOwner mosIOPortOwnerA = new MOSIOPort.DefaultPortOwner();
    private final MOSIOPortOwnerB mosIOPortOwnerB = new MOSIOPortOwnerB();
    private final BidirectionalPin.DefaultPortOwner cntPortOwner = new BidirectionalPin.DefaultPortOwner();
    private final BidirectionalPin.DefaultPortOwner spPortOwner = new BidirectionalPin.DefaultPortOwner();

    private int serialData;
    private int serialShiftRegister;
    private int serialShiftRegisterCounter;

    // Used during output mode
    private boolean serialShiftStarted; // Track if we have successfully shifted out the first bit
    private boolean serialShiftRegisterFull; // Track if the shift register is full. Set on loads, cleared at the end of a transfer
    private boolean serialShiftRegisterReloadPending; // Track if we wrote to SDR on output mode while the shift register was full, queueing up an immediate reloading

    public MOS6526(SystemBus systemBus) {
        this.systemBus = systemBus;
        this.triggerSerialInterruptSignal = this.actionSignalDispatcher.addSignal(2, _ -> {
            this.irqSerialPort = true;
            if (this.irqSerialPortEnable) {
                this.interruptRequest = true;
            }
        });
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
            case DDRA -> this.mosIOPortOwnerA.getDataDirectionRegister();
            case DDRB -> this.mosIOPortOwnerB.getDataDirectionRegisterRaw();
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
            case SDR -> this.serialData;
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
            case PRA -> this.mosIOPortOwnerA.setOutputLatch(value);
            case PRB -> {
                this.mosIOPortOwnerB.setOutputLatch(value);
                this.pcOutput = true;
                this.pcSetThisCycle = true;
            }
            case DDRA -> this.mosIOPortOwnerA.setDataDirectionRegister(value);
            case DDRB -> this.mosIOPortOwnerB.setDataDirectionRegister(value);
            case TA_LO -> this.timerA.writeTimerLatchLow(value);
            case TA_HI -> this.timerA.writeTimerLatchHigh(value);
            case TB_LO -> this.timerB.writeTimerLatchLow(value);
            case TB_HI -> this.timerB.writeTimerLatchHigh(value);
            case TOD_10THS -> {
                if (this.alarmMode == AlarmMode.SET_TOD_CLOCK) {
                    this.todClockRunning = true;
                }
                this.tenthsCounter.write(value);
            }
            case TOD_SEC -> this.secondsCounter.write(value);
            case TOD_MIN -> this.minutesCounter.write(value);
            case TOD_HR -> {
                if (this.alarmMode == AlarmMode.SET_TOD_CLOCK) {
                    this.todClockRunning = false;
                }
                this.hoursCounter.write(value);
                AmPmFlag amPmFlag = (value & (1 << 7)) != 0 ? AmPmFlag.PM : AmPmFlag.AM;
                switch (this.alarmMode) {
                    case SET_ALARM -> this.alarmAmPmFlag = amPmFlag;
                    case SET_TOD_CLOCK -> this.amPmFlag = amPmFlag;
                }
            }
            case SDR -> {
                this.serialData = value & 0xFF;
                if (this.serialPortMode == SerialPortMode.OUTPUT) {
                    // If the serial shift register is currently full, then mark the write as a pending reload.
                    // Otherwise, load the shift register immediately.
                    if (this.serialShiftRegisterFull) {
                        this.serialShiftRegisterReloadPending = true;
                    } else {
                        this.loadShiftRegister();
                    }
                }
            }
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
                    if (this.timerB.isInterruptEnabled() && this.timerB.getInterruptFlag()) {
                        this.interruptRequest = true;
                    }
                }
                if ((value & 1) != 0) {
                    this.timerA.setInterruptEnable(interruptEnable);
                    if (this.timerA.isInterruptEnabled() && this.timerA.getInterruptFlag()) {
                        this.interruptRequest = true;
                    }
                }
            }
            case CRA -> {
                SerialPortMode oldSerialPortMode = this.serialPortMode;

                this.timerA.writeControl(value);
                this.serialPortMode = (value & (1 << 6)) != 0 ? SerialPortMode.OUTPUT : SerialPortMode.INPUT;
                this.timeOfDayInput = (value & (1 << 7)) != 0 ? TimeOfDayInput.HZ_50 : TimeOfDayInput.HZ_60;

                if (oldSerialPortMode != this.serialPortMode) {
                    switch (this.serialPortMode) {
                        case OUTPUT -> {
                            this.cntPortOwner.setDirection(true);
                            this.spPortOwner.setDirection(true);

                            // Reset the CNT clock phase
                            this.cntPortOwner.setOutput(true);

                            // Wait until a transfer is requested
                            this.serialShiftRegisterCounter = 8;

                            // Clear flags
                            this.serialShiftStarted = false;
                            this.serialShiftRegisterFull = false;
                            this.serialShiftRegisterReloadPending = false;
                        }
                        case INPUT -> {
                            this.cntPortOwner.setDirection(false);
                            this.spPortOwner.setDirection(false);
                            this.serialShiftRegisterCounter = 0; // Just let it continue counting shift-ins
                        }
                    }
                }
            }
            case CRB -> {
                this.timerB.writeControl(value);
                this.alarmMode = (value & (1 << 7)) != 0 ? AlarmMode.SET_ALARM : AlarmMode.SET_TOD_CLOCK;
            }
        }
    }

    public boolean getIRQ() {
        return this.interruptRequest;
    }

    public boolean getPC() {
        return this.pcOutput;
    }

    public MOSIOPort.DefaultPortOwner getMOSIOPortOwnerA() {
        return this.mosIOPortOwnerA;
    }

    public MOSIOPort.DefaultPortOwner getMOSIOPortOwnerB() {
        return this.mosIOPortOwnerB;
    }

    public BidirectionalPin.PortOwner getCNTPortOwner() {
        return this.cntPortOwner;
    }

    public BidirectionalPin.PortOwner getSPPortOwner() {
        return this.spPortOwner;
    }

    public void cycle() {
        this.actionSignalDispatcher.tick();

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

        boolean currentCNT = systemBus.getCNT().read();
        boolean cntRisingEdge = !this.previousCNT && currentCNT;
        this.previousCNT = currentCNT;

        this.timerA.cycle(cntRisingEdge);
        this.timerB.cycle(cntRisingEdge);

        if (this.serialShiftRegisterCounter < 8) {
            switch (this.serialPortMode) {
                case INPUT -> {
                    if (cntRisingEdge) {
                        this.serialShiftRegister = ((this.serialShiftRegister << 1) | (this.systemBus.getSP().read() ? 1 : 0)) & 0xFF;
                        if (this.serialShiftRegisterCounter == 7) {
                            this.serialData = this.serialShiftRegister;
                            this.triggerSerialInterrupt();
                        }
                        this.serialShiftRegisterCounter++;
                        if (this.serialShiftRegisterCounter >= 8) {
                            this.serialShiftRegisterCounter = 0;
                        }
                    }
                }
                case OUTPUT -> {
                    if (this.timerA.underflowed()) {
                        if (currentCNT) {
                            this.spPortOwner.setOutput((this.serialShiftRegister & 0x80) != 0);
                            this.serialShiftRegister = (this.serialShiftRegister << 1) & 0xFF;
                            switch (this.serialShiftRegisterCounter) {
                                // Only set this to true here to make sure that we start the transfer on the correct phase
                                case 0 -> this.serialShiftStarted = true;
                                case 7 -> {
                                    this.serialShiftRegisterFull = false;
                                    this.triggerSerialInterrupt();
                                }
                            }
                        } else {
                            // Only advance the shift counter if we have already shifted one bit.
                            // Otherwise, we don't do anything and force the serial to properly begin on the next cycle
                            if (this.serialShiftStarted) {
                                this.serialShiftRegisterCounter++;
                                if (this.serialShiftRegisterCounter >= 8) {
                                    if (this.serialShiftRegisterReloadPending) {
                                        this.loadShiftRegister();
                                    }
                                }
                            }
                        }
                        this.cntPortOwner.setOutput(!currentCNT);
                    }
                }
            }
        }
    }

    private void loadShiftRegister() {
        this.serialShiftRegisterCounter = 0;
        this.serialShiftRegister = this.serialData;

        // We are back to not having sent the first bit
        this.serialShiftStarted = false;

        // We have just reloaded the shift register so it's back to being full
        this.serialShiftRegisterFull = true;

        // We have just reloaded, so there's no reload pending anymore
        this.serialShiftRegisterReloadPending = false;
    }

    private void triggerSerialInterrupt() {
        this.actionSignalDispatcher.trigger(this.triggerSerialInterruptSignal, 0);
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

    private class MOSIOPortOwnerB extends MOSIOPort.DefaultPortOwner {

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

        protected static final int COUNT_0 = 1;
        protected static final int COUNT_1 = 1 << 1;
        protected static final int COUNT_2 = 1 << 2;
        protected static final int COUNT_3 = 1 << 3;
        protected static final int LOAD_0 = 1 << 4;
        protected static final int LOAD_1 = 1 << 5;
        protected static final int PULSE_LOW_0 = 1 << 6;
        protected static final int PULSE_LOW_1 = 1 << 7;
        protected static final int INTERRUPT_0 = 1 << 8;
        protected static final int INTERRUPT_1 = 1 << 9;
        protected static final int ONE_SHOT_0 = 1 << 10;
        protected static final int DELAY_MASK = ~((1 << 11) | COUNT_0 | LOAD_0 | PULSE_LOW_0 | INTERRUPT_0 | ONE_SHOT_0);

        protected boolean running;
        private boolean outputOnPortB;
        private OutMode outMode = OutMode.PULSE;
        private RunMode runMode = RunMode.CONTINUOUS;

        private int timerLatch;
        private int timerCounter;

        private boolean interruptFlag;
        private boolean interruptEnable;

        private boolean toggleOutput; // TODO: Set low by !RES input
        private boolean pulseOutput;

        protected int delay;
        private int feed;

        protected Timer() {
        }

        protected void writeTimerLatchLow(int value) {
            this.timerLatch = (this.timerLatch & 0xFF00) | (value & 0x00FF);
        }

        protected void writeTimerLatchHigh(int value) {
            this.timerLatch = ((value & 0xFF) << 8) | (this.timerLatch & 0x00FF);
            if (!this.running) {
                this.delay |= LOAD_0;
            }
        }

        protected int getTimerLow() {
            return this.timerCounter & 0xFF;
        }

        protected int getTimerHigh() {
            return (this.timerCounter >>> 8) & 0xFF;
        }

        protected void writeControl(int value) {
            boolean start = (value & 1) != 0;
            this.outputOnPortB = (value & (1 << 1)) != 0;
            this.outMode = (value & (1 << 2)) != 0 ? Timer.OutMode.TOGGLE : Timer.OutMode.PULSE;
            this.runMode = (value & (1 << 3)) != 0 ? Timer.RunMode.ONE_SHOT : Timer.RunMode.CONTINUOUS;

            switch (this.runMode) {
                case ONE_SHOT -> this.feed |= ONE_SHOT_0;
                case CONTINUOUS -> this.feed &= ~ONE_SHOT_0;
            }

            if ((value & (1 << 4)) != 0) {
                this.delay |= LOAD_0;
            }

            if (start && !this.running) {
                this.toggleOutput = true;
            }

            this.running = start;
        }

        protected void checkStartWithCountPhi2(boolean startAndCountPhi2) {
            if (startAndCountPhi2) {
                this.delay |= COUNT_1 | COUNT_0;
                this.feed |= COUNT_0;
            } else {
                this.delay &= ~(COUNT_1 | COUNT_0);
                this.feed &= ~COUNT_0;
            }
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

        protected boolean getInterruptFlag() {
            return this.interruptFlag;
        }

        protected void clearInterruptFlag() {
            this.interruptFlag = false;
        }

        protected boolean isInterruptEnabled() {
            return this.interruptEnable;
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

        protected abstract boolean isCountingCNTEdges();

        protected void cycle(boolean cntRising) {
            if (this.isCountingCNTEdges() && cntRising) {
                this.delay |= COUNT_0;
            }

            if ((this.delay & COUNT_3) != 0) {
                this.timerCounter = (this.timerCounter - 1) & 0xFFFF;
            }

            if (this.timerCounter <= 0 && (this.delay & COUNT_2) != 0) {
                 this.onTimerUnderflow();
            }

            if ((this.delay & LOAD_1) != 0) {
                this.timerCounter = this.timerLatch;
                this.delay &= ~COUNT_2;
            }

            if ((this.delay & PULSE_LOW_1) != 0) {
                this.pulseOutput = false;
            }

            if ((this.delay & INTERRUPT_1) != 0) {
                interruptRequest = true;
            }

            this.delay = (this.delay << 1) & DELAY_MASK | this.feed;
        }

        protected void onTimerUnderflow() {
            this.interruptFlag = true;
            if (this.interruptEnable) {
                this.delay |= INTERRUPT_0;
            }

            this.toggleOutput = !this.toggleOutput;

            if (this.outputOnPortB) {
                switch (this.outMode) {
                    case PULSE -> {
                        this.pulseOutput = true;
                        this.delay |= PULSE_LOW_0;
                        this.delay &= ~(PULSE_LOW_1);
                    }
                    case TOGGLE -> this.pulseOutput = this.toggleOutput;
                }
            }

            if (((this.delay | this.feed) & ONE_SHOT_0) != 0) {
                this.running = false;
                this.delay &= ~(COUNT_2 | COUNT_1 | COUNT_0);
                this.feed &= ~COUNT_0;
            }

            this.delay |= LOAD_1;
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

        private boolean underflowed;
        private InMode inMode = InMode.PHI2_PULSES;

        @Override
        protected void writeControl(int value) {
            super.writeControl(value);
            this.inMode = (value & (1 << 5)) != 0 ? TimerA.InMode.CNT_RISING_EDGES : TimerA.InMode.PHI2_PULSES;
            this.checkStartWithCountPhi2(this.running && this.inMode == InMode.PHI2_PULSES);
        }

        @Override
        protected int getControlBits() {
            return super.getControlBits() | (this.inMode == InMode.CNT_RISING_EDGES ? 1 << 5 : 0);
        }

        @Override
        protected boolean isCountingCNTEdges() {
            return this.inMode == InMode.CNT_RISING_EDGES;
        }

        @Override
        protected void cycle(boolean cntRising) {
            if (this.underflowed) {
                this.underflowed = false;
            }
            super.cycle(cntRising);
        }

        @Override
        protected void onTimerUnderflow() {
            super.onTimerUnderflow();
            timerB.onTimerAUnderflow();
            this.underflowed = true;
        }

        protected boolean underflowed() {
            return this.underflowed;
        }

        private enum InMode {
            CNT_RISING_EDGES,
            PHI2_PULSES
        }

    }

    private class TimerB extends Timer {

        private InMode inMode = InMode.PHI2_PULSES;

        @Override
        protected void writeControl(int value) {
            super.writeControl(value);
            this.inMode = switch ((value >>> 5) & 0b11) {
                case 0b00 -> TimerB.InMode.PHI2_PULSES;
                case 0b01 -> TimerB.InMode.CNT_RISING_EDGES;
                case 0b10 -> TimerB.InMode.TIMER_A_UNDERFLOWS;
                default -> TimerB.InMode.TIMER_A_UNDERFLOWS_CNT_HIGH;
            };
            this.checkStartWithCountPhi2(this.running && this.inMode == InMode.PHI2_PULSES);
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
        protected boolean isCountingCNTEdges() {
            return this.inMode == InMode.CNT_RISING_EDGES;
        }

        private void onTimerAUnderflow() {
            if (this.running) {
                switch (this.inMode) {
                    case TIMER_A_UNDERFLOWS -> this.delay |= COUNT_1;
                    case TIMER_A_UNDERFLOWS_CNT_HIGH -> {
                        if (systemBus.getCNT().read()) {
                            this.delay |= COUNT_1;
                        }
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
