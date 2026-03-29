package io.github.arkosammy12.jemu.core.cpu;

import io.github.arkosammy12.jemu.core.common.Processor;

public class NMOS6502 implements Processor {

    private static final int RESET_VECTOR = 0xFFFC;
    private static final int NMI_VECTOR = 0xFFFA;
    private static final int IRQ_BRK_VECTOR = 0xFFFE;

    private static final int N_MASK = 1 << 7;
    private static final int V_MASK = 1 << 6;
    private static final int M_MASK = 1 << 5;
    private static final int B_MASK = 1 << 4;
    private static final int D_MASK = 1 << 3;
    private static final int I_MASK = 1 << 2;
    private static final int Z_MASK = 1 << 1;
    private static final int C_MASK = 1;

    protected final SystemBus systemBus;

    private int programCounter; // PC, 16 bits
    private int accumulator; // A, 8 bits
    private int X; // 8 bits
    private int Y; // 8 bits
    private int processorStatus; // P, 8 bit
    private int stackPointer; // SP, 8 bits

    private int instructionRegister; // 8 bits

    protected static final int TERMINATE_INSTRUCTION = -1;

    protected int subCycleIndex = TERMINATE_INSTRUCTION;
    private boolean firstCycle = true;
    private int operand;
    private int address;
    private int pointer;
    private int target;
    private int finalVar;
    private int temp;
    private boolean boundaryCrossed;

    private Phase phase = Phase.PHI_1;
    private boolean oldNMI;
    private boolean signalReset;
    private boolean signalNMI;
    private boolean signalIRQ;
    private BRKSource brkSource = BRKSource.SOFTWARE;
    private int brkVector = IRQ_BRK_VECTOR;

    public NMOS6502(SystemBus systemBus) {
        this.systemBus = systemBus;
    }

    private void setBrkVector(int vector) {
        this.brkVector = vector & 0xFFFF;
    }

    private int getBrkVector() {
        return this.brkVector;
    }

    protected void setPC(int value) {
        this.programCounter = value & 0xFFFF;
    }

    private void setPCH(int value) {
        setPC(((value & 0xFF) << 8) | (getPC() & 0xFF));
    }

    private void setPCL(int value) {
        setPC((getPC() & 0xFF00) | (value & 0xFF));
    }

    public int getPC() {
        return this.programCounter;
    }

    private int getPCH() {
        return (this.getPC() >>> 8) & 0xFF;
    }

    private int getPCL() {
        return this.getPC() & 0xFF;
    }

    protected void setA(int value) {
        this.accumulator = value & 0xFF;
    }

    public int getA() {
        return this.accumulator;
    }

    protected void setX(int value) {
        this.X = value & 0xFF;
    }

    public int getX() {
        return this.X;
    }

    protected void setY(int value) {
        this.Y = value & 0xFF;
    }

    public int getY() {
        return this.Y;
    }

    protected void setSP(int value) {
        this.stackPointer = value & 0xFF;
    }

    public int getSP() {
        return this.stackPointer;
    }

    protected void setP(int value) {
        this.processorStatus = value & 0xFF;
    }

    public int getP() {
        return this.processorStatus;
    }

    protected void setFN(boolean value) {
        setP(value ? Processor.setBit(getP(), N_MASK) : Processor.clearBit(getP(), N_MASK));
    }

    private boolean getFN() {
        return Processor.testBit(getP(), N_MASK);
    }

    private void setFV(boolean value) {
        setP(value ? Processor.setBit(getP(), V_MASK) : Processor.clearBit(getP(), V_MASK));
    }

    private boolean getFV() {
        return Processor.testBit(getP(), V_MASK);
    }

    private void setFM(boolean value) {
        setP(value ? Processor.setBit(getP(), M_MASK) : Processor.clearBit(getP(), M_MASK));
    }

    private boolean getFM() {
        return Processor.testBit(getP(), M_MASK);
    }

    private void setFB(boolean value) {
        setP(value ? Processor.setBit(getP(), B_MASK) : Processor.clearBit(getP(), B_MASK));
    }

    private boolean getFB() {
        return Processor.testBit(getP(), B_MASK);
    }

    private void setFD(boolean value) {
        setP(value ? Processor.setBit(getP(), D_MASK) : Processor.clearBit(getP(), D_MASK));
    }

    protected boolean getFD() {
        return Processor.testBit(getP(), D_MASK);
    }

    private void setFI(boolean value) {
        setP(value ? Processor.setBit(getP(), I_MASK) : Processor.clearBit(getP(), I_MASK));
    }

    private boolean getFI() {
        return Processor.testBit(getP(), I_MASK);
    }

    protected void setFZ(boolean value) {
        setP(value ? Processor.setBit(getP(), Z_MASK) : Processor.clearBit(getP(), Z_MASK));
    }

    private boolean getFZ() {
        return Processor.testBit(getP(), Z_MASK);
    }

    private void setFC(boolean value) {
        setP(value ? Processor.setBit(getP(), C_MASK) : Processor.clearBit(getP(), C_MASK));
    }

    private boolean getFC() {
        return Processor.testBit(getP(), C_MASK);
    }

    private void setIR(int value) {
        this.instructionRegister = value & 0xFF;
    }

    protected int getIR() {
        return this.instructionRegister;
    }

    protected void setOperand(int value) {
        this.operand = value & 0xFF;
    }

    protected int getOperand() {
        return this.operand;
    }

    private void setPointer(int value) {
        this.pointer = value & 0xFFFF;
    }

    private void setPointerLow(int value) {
        setPointer((getPointerHigh() << 8) | (value & 0xFF));
    }

    private void setPointerHigh(int value) {
        setPointer(((value & 0xFF) << 8) | getPointerLow());
    }

    private int getPointer() {
        return this.pointer;
    }

    private int getPointerLow() {
        return this.pointer & 0xFF;
    }

    private int getPointerHigh() {
        return (this.pointer >>> 8) & 0xFF;
    }

    private void setTarget(int value) {
        this.target = value & 0xFFFF;
    }

    private void setTargetLow(int value) {
        setTarget((getTargetHigh() << 8) | (value & 0xFF));
    }

    private void setTargetHigh(int value) {
        setTarget(((value & 0xFF) << 8) | (getTargetLow()));
    }

    private int getTargetHigh() {
        return (this.target >>> 8) & 0xFF;
    }

    private int getTargetLow() {
        return this.target & 0xFF;
    }

    private void setAddressLow(int value) {
        setAddress((getAddress() & 0xFF00) | (value & 0xFF));
    }

    private void setAddressHigh(int value) {
        setAddress(((value & 0xFF) << 8) | (this.address & 0xFF));
    }

    private void setAddress(int value) {
        this.address = value & 0xFFFF;
    }

    private int getAddress() {
        return this.address;
    }

    private int getAddressHigh() {
        return (this.address >>> 8) & 0xFF;
    }

    private int getAddressLow() {
        return this.address & 0xFF;
    }

    private void setFinal(int value) {
        this.finalVar = value & 0xFFFF;
    }

    private void setFinalLow(int value) {
        setFinal((getFinalHigh() << 8) | (value & 0xFF));
    }

    private void setFinalHigh(int value) {
        setFinal(((value & 0xFF) << 8) | getFinalLow());
    }

    private int getFinal() {
        return this.finalVar;
    }

    private int getFinalHigh() {
        return (this.finalVar >>> 8) & 0xFF;
    }

    private int getFinalLow() {
        return this.finalVar & 0xFF;
    }

    private void setTemp(int value) {
        this.temp = value & 0xFF;
    }

    private int getTemp() {
        return this.temp;
    }

    private void setBoundaryCrossed(boolean crossed) {
        this.boundaryCrossed = crossed;
    }

    private boolean getBoundaryCrossed() {
        return this.boundaryCrossed;
    }

    @Override
    public int cycle() {
        if (this.firstCycle) {
            this.firstCycle = false;
            this.updateInterruptSignals();
            this.onCycleEnd();
            return 0;
        }

        if (this.subCycleIndex >= 0) {
            this.execute();
        }

        if (this.subCycleIndex < 0) {
            setIR(systemBus.getBus().readByte(getPC()));
            if (this.signalReset || this.signalNMI || this.signalIRQ) {
                setIR(0x00);
                if (this.signalReset) {
                    brkSource = BRKSource.RESET;
                    this.signalReset = false;
                } else if (this.signalNMI) {
                    brkSource = BRKSource.NMI;
                    this.signalNMI = false;
                } else if (this.signalIRQ) {
                    brkSource = BRKSource.IRQ;
                }
            }
            subCycleIndex = 0;
        }

        this.updateInterruptSignals();
        this.onCycleEnd();
        return 0;
    }

    private void updateInterruptSignals() {
        switch (this.phase) {
            case PHI_1 -> {

            }
            case PHI_2 -> {
                if (!oldNMI && this.systemBus.getNMI()) {
                    this.signalNMI = true;
                }
                this.oldNMI = this.systemBus.getNMI();
                this.signalReset = this.systemBus.getRes();
                this.signalIRQ = this.systemBus.getIRQ() && !getFI();
            }
        }
    }

    private void onCycleEnd() {
        this.phase = this.phase.getOpposite();
    }

    protected void execute() {
        switch (getIR()) {
            case 0x00 -> { // BRK, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        if (this.brkSource == BRKSource.SOFTWARE) {
                            setPC(getPC() + 1);
                        }
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        if (this.brkSource == BRKSource.SOFTWARE) {
                            setPC(getPC() + 1);
                        }
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        if (this.brkSource == BRKSource.RESET) {
                            // TODO: Populate data bus
                            systemBus.getBus().readByte(getSP() | 0x0100);
                        } else {
                            systemBus.getBus().writeByte(getSP() | 0x0100, getPCH());
                        }
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        if (this.brkSource == BRKSource.RESET) {
                            // TODO: Populate data bus
                            systemBus.getBus().readByte(((getSP() - 1) & 0xFF) | 0x0100);
                        } else {
                            systemBus.getBus().writeByte(((getSP() - 1) & 0xFF) | 0x0100, getPCL());
                        }
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int brkVector = IRQ_BRK_VECTOR;
                        if (systemBus.getRes()) {
                            brkVector = RESET_VECTOR;
                        } else if (signalNMI) {
                            brkVector = NMI_VECTOR;
                        } else if (signalIRQ) {
                            brkVector = IRQ_BRK_VECTOR;
                        }
                        setBrkVector(brkVector);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        if (this.brkSource == BRKSource.RESET) {
                            // TODO: Populate data bus
                            systemBus.getBus().readByte(((getSP() - 2) & 0xFF) | 0x0100);
                        } else {
                            systemBus.getBus().writeByte(((getSP() - 2) & 0xFF) | 0x0100, getP() | (brkSource == BRKSource.SOFTWARE ? B_MASK : 0));
                        }
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setSP(getSP() - 3);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setAddressLow(systemBus.getBus().readByte(getBrkVector()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFI(true);
                        setFB(false);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        setAddressHigh(systemBus.getBus().readByte((getBrkVector() + 1) & 0xFFFF));
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setPC(getAddress());
                        brkSource = BRKSource.SOFTWARE;
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = 14;
                    }
                }
            }
            case 0x01 -> { // ORA, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x02, 0x12, 0x22, 0x32, 0x42, 0x52, 0x62, 0x72, 0x92, 0xB2, 0xD2, 0xF2 -> { // JAM, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(0xFFFF);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(0xFFFE);
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().readByte(0xFFFE);
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().readByte(0xFFFF);
                        subCycleIndex = 8;
                    }
                }
            }
            case 0x03 -> { // SLO, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x04, 0x44, 0x64 -> { // NOP, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x05 -> { // ORA, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        int result = getA() | getOperand();
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x06 -> { // ASL, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x07 -> { // SLO, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x08 -> { // PHP, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().writeByte(getSP() | 0x0100, getP() | B_MASK | M_MASK);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setSP(getSP() - 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x09 -> { // ORA, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x0A -> { // ASL, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFC((getA() & 0x80) != 0);
                        int result = (getA() << 1) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x0B, 0x2B -> { // ANC, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFC((result & 0x80) != 0);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x0C -> { // NOP, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x0D -> { // ORA, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x0E -> { // ASL, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x0F -> { // SLO, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x10 -> { // BPL, branch relative
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        if (!getFN()) {
                            subCycleIndex = 3;
                        } else {
                            subCycleIndex = 7;
                        }
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        short signedOperand = (short) ((byte) getOperand());
                        int originalPC = getPC();
                        int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                        if (signedOffsetHigh != getPCH()) {
                            subCycleIndex = 5;
                        } else {
                            subCycleIndex = 7;
                        }
                        setPCL(getPCL() + signedOperand);

                        // Save original PC
                        setAddress(originalPC);
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        short signedOperand = (short) ((byte) getOperand());

                        // Original PC value stored in address
                        setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x11 -> { // ORA, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x13 -> { // SLO, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x14, 0x34, 0x54, 0x74, 0xD4, 0xF4 -> { // NOP, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x15 -> { // ORA, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x16 -> { // ASL, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x17 -> { // SLO, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x18 -> { // CLC, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFC(false);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x19 -> { // ORA, absolute Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1A, 0x3A, 0x5A, 0x7A, 0xDA, 0xEA, 0xFA -> { // NOP, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1B -> { // SLO, absolute Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1C, 0x3C, 0x5C, 0x7C, 0xDC, 0xFC -> { // NOP, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1D -> { // ORA, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1E -> { // ASL, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1F -> { // SLO, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC((getOperand() & 0x80) != 0);
                        setOperand(getOperand() << 1);
                        int result = (getA() | getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x20 -> { // JSR, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getSP() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getSP() | 0x0100, getPCH());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(((getSP() - 1) & 0xFF) | 0x0100, getPCL());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setPC(getAddress());
                        setSP(getSP() - 2);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x21 -> { // AND, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x23 -> { // RLA, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFC(originalHighBit);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x24 -> { // BIT, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setFV((getOperand() & (1 << 6)) != 0);
                        setFN((getOperand() & (1 << 7)) != 0);
                        setFZ((getOperand() & getA()) == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x25 -> { // AND, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x26 -> { // ROL, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        setFC(originalHighBit);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x27 -> { // RLA, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        setFC(originalHighBit);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x28 -> { // PLP, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getSP() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setSP(getSP() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getSP() | 0x0100));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setP(getOperand());
                        setFB(false);
                        setFM(true);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x29 -> { // AND, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x2A -> { // ROL, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        boolean originalHighBit = (getA() & 0x80) != 0;
                        int result = ((getA() << 1) | (getFC() ? 1 : 0)) & 0xFF;
                        setA(result);
                        setFC(originalHighBit);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x2C -> { // BIT, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 ->  {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFV((getOperand() & (1 << 6)) != 0);
                        setFN((getOperand() & (1 << 7)) != 0);
                        setFZ((getOperand() & getA()) == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x2D -> { // AND, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x2E -> { // ROL, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        setFC(originalHighBit);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x2F -> { // RLA, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        setFC(originalHighBit);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x30 -> { // BMI, branch relative
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        if (getFN()) {
                            subCycleIndex = 3;
                        } else {
                            subCycleIndex = 7;
                        }
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        short signedOperand = (short) ((byte) getOperand());
                        int originalPC = getPC();
                        int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                        if (signedOffsetHigh != getPCH()) {
                            subCycleIndex = 5;
                        } else {
                            subCycleIndex = 7;
                        }
                        setPCL(getPCL() + signedOperand);

                        // Save original PC
                        setAddress(originalPC);
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        short signedOperand = (short) ((byte) getOperand());

                        // Original PC value stored in address
                        setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x31 -> { // AND, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x33 ->  { // RLA, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        setFC(originalHighBit);
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x35 -> { // AND, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x36 -> { // ROL, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        setFC(originalHighBit);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x37 -> { // RLA, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        setFC(originalHighBit);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x38 -> { // SEC, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 ->  {
                        setFC(true);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x39 -> { // AND, absolute Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x3B -> { // RLA, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        setFC(originalHighBit);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x3D -> { // AND, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x3E -> { // ROL, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        setFC(originalHighBit);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x3F -> { // RLA, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        boolean originalHighBit = (getOperand() & 0x80) != 0;
                        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
                        int result = (getA() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        setFC(originalHighBit);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x40 -> { // RTI, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getSP() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setTemp(systemBus.getBus().readByte(((getSP() + 1) & 0xFF) | 0x0100));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        boolean originalB = getFB();
                        boolean originalM = getFM();
                        setP(getTemp());
                        setFB(originalB);
                        setFM(originalM);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressLow(systemBus.getBus().readByte(((getSP() + 2) & 0xFF) | 0x0100));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setSP(getSP() + 3);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setAddressHigh(systemBus.getBus().readByte(getSP() | 0x0100));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setPC(getAddress());
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x41 -> { // EOR, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x43 -> { // SRE, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setOperand(getOperand() >>> 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x45 -> { // EOR, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x46 -> { // LSR, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x47 -> { // SRE, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setOperand(getOperand() >>> 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x48 -> { // PHA
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().writeByte(getSP() | 0x0100, getA());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setSP(getSP() - 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x49 -> { // EOR, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x4A -> { // LSR, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFC((getA() & 1) != 0);
                        int result = (getA() >>> 1) & 0xFF;
                        setA(result);
                        setFN(false);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x4B -> { // ASR, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        setA(getA() & getOperand());
                        setFC((getA() & 1) != 0);
                        int result = (getA() >>> 1) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x4C -> { // JMP, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getAddress());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x4D -> { // EOR, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x4E -> { // LSR, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x4F -> { // SRE, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() >>> 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x50 -> { // BVC, branch relative
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        if (!getFV()) {
                            subCycleIndex = 3;
                        } else {
                            subCycleIndex = 7;
                        }
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        short signedOperand = (short) ((byte) getOperand());
                        int originalPC = getPC();
                        int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                        if (signedOffsetHigh != getPCH()) {
                            subCycleIndex = 5;
                        } else {
                            subCycleIndex = 7;
                        }
                        setPCL(getPCL() + signedOperand);

                        // Save original PC
                        setAddress(originalPC);
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        short signedOperand = (short) ((byte) getOperand());

                        // Original PC value stored in address
                        setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x51 -> { // EOR, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x53 -> { // SRE, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setOperand(getOperand() >>> 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x55 -> { // EOR, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x56 -> { // LSR, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x57 -> { // SRE, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() >>> 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x58 -> { // CLI, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 ->  {
                        setFI(false);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x59 -> { // EOR, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x5B -> {
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() >>> 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x5D -> { // EOR, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x5E -> { // LSR, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x5F -> { // SRE, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() >>> 1);
                        int result = (getA() ^ getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x60 -> { // RTS, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getSP() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(((getSP() + 1) & 0xFF) | 0x0100));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setSP(getSP() + 2);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte(getSP() | 0x0100));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPC(getAddress());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x61 -> { // ADC, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        adc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x63 -> { // RRA, indirect X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        adc();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x65 -> { // ADC, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        adc();
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x66 -> { // ROR, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x67 -> { // RRA, zero page
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        adc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x68 -> { // PLA, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getSP() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setSP(getSP() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getSP() | 0x0100));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 ->  {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x69 -> { // ADC, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        adc();
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x6A -> { // ROR, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getA() & 1) != 0);
                        int result = ((getA() >>> 1) | temp) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x6B -> { // ARR, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        setA(getA() & getOperand());
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getA() & 0x80) != 0);
                        setA((getA() >>> 1) | temp);
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        setFV(((getFC() ? 1 : 0) ^ ((getA() >>> 5) & 1)) != 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x6C -> { // JMP, indirect
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointerLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointerHigh() << 8) | ((getPointerLow() + 1) & 0xFF)));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPC(getAddress());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x6D -> { // ADC, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        adc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x6E -> { // ROR
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x6F -> { // RRA, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        adc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x70 -> { // BVS, branch relative
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        if (getFV()) {
                            subCycleIndex = 3;
                        } else {
                            subCycleIndex = 7;
                        }
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        short signedOperand = (short) ((byte) getOperand());
                        int originalPC = getPC();
                        int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                        if (signedOffsetHigh != getPCH()) {
                            subCycleIndex = 5;
                        } else {
                            subCycleIndex = 7;
                        }
                        setPCL(getPCL() + signedOperand);

                        // Save original PC
                        setAddress(originalPC);
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        short signedOperand = (short) ((byte) getOperand());

                        // Original PC value stored in address
                        setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x71 -> { // ADC, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        adc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x73 -> { // RRA, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        adc();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x75 -> { // ADC, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        adc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x76 -> { // ROR, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x77 ->  { // RRA, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        adc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x78 -> { // SEI, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFI(true);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x79 -> { // ADC, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        adc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x7B -> { // RRA, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        adc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x7D -> { // ADC, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        adc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x7E -> { // ROR, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x7F -> { // RRA, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getOperand() & 1) != 0);
                        setOperand((getOperand() >>> 1) | temp);
                        adc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            default -> execute2();
        }

    }

    private void execute2() {
        switch (getIR()) {
            case 0x80, 0x82, 0x89, 0xC2, 0xE2 -> { // NOP, immediate (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x81 -> { // STA, indirect X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getA());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x83 -> { // SAX, indirect X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getA() & getX());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x84 -> { // STY, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().writeByte(getAddress(), getY());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x85 -> { // STA, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().writeByte(getAddress(), getA());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x86 -> { // STX, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().writeByte(getAddress(), getX());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x87 -> { // SAX, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().writeByte(getAddress(), getA() & getX());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x88 -> { // DEY, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setY(getY() - 1);
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x8A -> { // TXA, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setA(getX());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x8B -> { // ANE, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        // L. Spiro's NES instructions says that the constant 0xEE passes all known tests,
                        // so this is what we will go to
                        int result = ((getA() | 0xEE) & getX() & getOperand()) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x8C -> { // STY, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getY());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x8D -> { // STA, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getA());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x8E -> { // STX, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getX());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x8F -> { // SAX, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getA() & getX());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x90 -> { // BCC, branch relative
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        if (!getFC()) {
                            subCycleIndex = 3;
                        } else {
                            subCycleIndex = 7;
                        }
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        short signedOperand = (short) ((byte) getOperand());
                        int originalPC = getPC();
                        int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                        if (signedOffsetHigh != getPCH()) {
                            subCycleIndex = 5;
                        } else {
                            subCycleIndex = 7;
                        }
                        setPCL(getPCL() + signedOperand);

                        // Save original PC
                        setAddress(originalPC);
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        short signedOperand = (short) ((byte) getOperand());

                        // Original PC value stored in address
                        setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x91 -> { // STA, indirect Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getA());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x93 -> { // SHA, indirect Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        if (getAddressHigh() == getFinalHigh()) {
                            subCycleIndex = 9;
                        } else {
                            subCycleIndex = 10;
                        }
                        setAddressHigh(getFinalHigh());
                    }
                    case 9 -> { // PAGE BOUNDARY NOT CROSSED BRANCH
                        int high = getAddressHigh() & 0xFFFF;
                        high = (high + 1) & 0xFFFF;
                        // TODO: If the RDY flag was just set, set High to 0xFFFF
                        int finalVal = (high & getA() & getX()) & 0xFFFF;
                        int finalAddress = address;
                        systemBus.getBus().writeByte(finalAddress, finalVal & 0xFF);
                        subCycleIndex = 11;
                    }
                    case 10 -> { // PAGE BOUNDARY CROSSED BRANCH
                        int high = getAddressHigh() & 0xFFFF;
                        // TODO: If the RDY flag was just set, set High to 0xFFFF
                        int finalVal = (high & getA() & getX()) & 0xFFFF;
                        int finalAddress = ((finalVal << 8) | getAddressLow()) & 0xFFFF;
                        systemBus.getBus().writeByte(finalAddress, finalVal & 0xFF);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x94 -> { // zero page, X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getY());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x95 -> { // STA, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getA());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x96 -> { // STX, zero page Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getY()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getX());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x97 -> { // SAX, zero page Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getY()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getA() & getX());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x98 -> { // TYA, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setA(getY());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x99 -> { // STA, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex= 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getA());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9A -> { // TXS, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setSP(getX());
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9B ->  { // TAS, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setSP(getA() & getX());
                        if (getBoundaryCrossed()) {
                            int val = (getAddressHigh() & getA() & getX()) & 0xFF;
                            systemBus.getBus().writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            int val = ((getAddressHigh() + 1) & getA() & getX()) & 0xFF;
                            systemBus.getBus().writeByte(getAddress(), val);
                        }
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9C -> { // SHY, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        if (getBoundaryCrossed()) {
                            int val = getAddressHigh() & getY();
                            systemBus.getBus().writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            int val = ((getAddressHigh() + 1) & getY()) & 0xFF;
                            systemBus.getBus().writeByte(getAddress(), val);
                        }
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9D -> { // STA, absolute X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getA());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9E -> { // SHX, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        if (getBoundaryCrossed()) {
                            int val = getAddressHigh() & getX();
                            systemBus.getBus().writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            int val = ((getAddressHigh() + 1) & getX()) & 0xFF;
                            systemBus.getBus().writeByte(getAddress(), val);
                        }
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9F -> { // SHA, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        int high = getAddressHigh();
                        if (!getBoundaryCrossed()) {
                            high++;
                        }
                        // TODO: IF RDY WENT LOW 4 CYCLES AGO, HIGH = $FFFF
                        int val = (high & getA() & getX()) & 0xFF;
                        if (getBoundaryCrossed()) {
                            systemBus.getBus().writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            systemBus.getBus().writeByte(getAddress(), val);
                        }
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA0 -> { // LDY, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        setY(getOperand());
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA1 -> { // LDA, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA2 -> { // LDX, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        setX(getOperand());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA3 -> { // LAX, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setA(getOperand());
                        setX(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA4 -> { // LDY, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setY(getOperand());
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA5 -> { // LDA, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA6 -> { // LDX, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setX(getOperand());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA7 -> { // LAX, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setA(getOperand());
                        setX(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA8 -> { // TAY, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setY(getA());
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xA9 -> { // LDA, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xAA -> { // TAX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getA());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xAB -> { // LXA, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int value = ((getA() | 0xFF) & getOperand()) & 0xFF;
                        setA(value);
                        setX(value);
                        setFN((value & 0x80) != 0);
                        setFZ(value == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xAC -> { // LDY, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setY(getOperand());
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xAD -> { // LDA, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xAE -> { // LDX, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setX(getOperand());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xAF -> { // LAX, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setA(getOperand());
                        setX(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB0 -> { // BCS, relative
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        if (getFC()) {
                            subCycleIndex = 3;
                        } else {
                            subCycleIndex = 7;
                        }
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        short signedOperand = (short) ((byte) getOperand());
                        int originalPC = getPC();
                        int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                        if (signedOffsetHigh != getPCH()) {
                            subCycleIndex = 5;
                        } else {
                            subCycleIndex = 7;
                        }
                        setPCL(getPCL() + signedOperand);

                        // Save original PC
                        setAddress(originalPC);
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        short signedOperand = (short) ((byte) getOperand());

                        // Original PC value stored in address
                        setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB1 -> { // LDA, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setTarget(getAddress() + getY());
                        setPointerLow(getTargetLow());
                        setPointerHigh(getAddressHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB3 -> { // LAX, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setTarget(getAddress() + getY());
                        setPointerLow(getTargetLow());
                        setPointerHigh(getAddressHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setA(getOperand());
                        setX(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB4 -> { // LDY, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setY(getOperand());
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB5 -> { // LDA, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB6 -> { // LDX, zero page Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setX(getOperand());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB7 -> { // LAX, zero page Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setA(getOperand());
                        setX(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB8 -> { // CLV, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFV(false);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB9 -> { // LDA, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xBA -> { // TSX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getSP());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xBB -> { // LAS, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int value = (getOperand() & getSP());
                        setA(value);
                        setX(value);
                        setSP(value);
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xBC -> { // LDY, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setY(getOperand());
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xBD -> { // LDA, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setA(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xBE -> { // LDX, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setX(getOperand());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xBF -> { // LAX, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setA(getOperand());
                        setX(getOperand());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC0 -> { // CPY, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        setFC(getY() >= getOperand());
                        setFN(((getY() - getOperand()) & 0x80) != 0);
                        setFZ(getY() == getOperand());
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC1 -> { // CMP, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC3 -> { // DCP, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setOperand(getOperand() - 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC4 -> { // CPY, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setFC(getY() >= getOperand());
                        setFN(((getY() - getOperand()) & 0x80) != 0);
                        setFZ(getY() == getOperand());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC5 -> { // CMP, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC6 -> { // DEC, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC7 -> { // DCP, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setOperand(getOperand() - 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC8 -> { // INY, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setY(getY() + 1);
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC9 -> { // CMP, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xCA -> { // DEX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getX() - 1);
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xCB -> { // SBX, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int anx = getA() & getX();
                        setFC(anx >= getOperand());
                        setX(anx - getOperand());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xCC -> { // CPY, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC(getY() >= getOperand());
                        setFN(((getY() - getOperand()) & 0x80) != 0);
                        setFZ(getY() == getOperand());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xCD -> { // CMP, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xCE -> { // DEC, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xCF -> { // DCP, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() - 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD0 -> { // BNE, relative (jump)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        if (!getFZ()) {
                            subCycleIndex = 3;
                        } else {
                            subCycleIndex = 7;
                        }
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        short signedOperand = (short) ((byte) getOperand());
                        int originalPC = getPC();
                        int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                        if (signedOffsetHigh != getPCH()) {
                            subCycleIndex = 5;
                        } else {
                            subCycleIndex = 7;
                        }
                        setPCL(getPCL() + signedOperand);

                        // Save original PC
                        setAddress(originalPC);
                    }
                    case 5 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        short signedOperand = (short) ((byte) getOperand());

                        // Original PC value stored in address
                        setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD1 -> { // CMP, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setTarget(getAddress() + getY());
                        setPointerLow(getTargetLow());
                        setPointerHigh(getAddressHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD3 -> { // DCP, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setTarget(getAddress() + getY());
                        setPointerLow(getTargetLow());
                        setPointerHigh(getAddressHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getPointer(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setOperand(getOperand() - 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getPointer(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD5 -> { // CMP, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD6 -> { // DEC, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD7 -> { // DCP, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() - 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD8 -> { // CLD, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFD(false);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xD9 -> { // CMP, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xDB -> { // DCP, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() - 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xDD -> { // CMP, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xDE -> { // DEC, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xDF -> { // DCP, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() - 1);
                        setFC(getA() >= getOperand());
                        setFN(((getA() - getOperand()) & 0x80) != 0);
                        setFZ(getA() == getOperand());
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE0 -> { // CPX, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        cpx();
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE1 -> { // SBC, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        sbc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE3 -> { // ISC, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        systemBus.getBus().readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        isc();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE4 -> { // CPX, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        cpx();
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE5 -> { // SBC, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        sbc();
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE6 -> { // INC, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        inc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE7 -> { // ISC, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        isc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE8 -> { // INX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getX() + 1);
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xE9, 0xEB -> { // SBC, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        sbc();
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xEC -> { // CPX, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        cpx();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xED -> { // SBC, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        sbc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xEE -> { // INC, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        inc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xEF -> { // ISC, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        isc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xF0 -> { // BEQ, relative (jump)
                branchRelative(getFZ());
            }
            case 0xF1 -> { // SBC, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setTarget(getAddress() + getY());
                        setPointerLow(getTargetLow());
                        setPointerHigh(getAddressHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 10;
                        } else {
                            subCycleIndex = 8;
                        }
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        sbc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xF3 -> { // ISC, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(systemBus.getBus().readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setTarget(getAddress() + getY());
                        setPointerLow(getTargetLow());
                        setPointerHigh(getAddressHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getPointer(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        isc();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        systemBus.getBus().writeByte(getPointer(), getOperand());
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        subCycleIndex = 15;
                    }
                    case 15 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xF5 -> { // SBC, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        sbc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xF6 -> { // INC, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        inc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xF7 -> { // ISC, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(systemBus.getBus().readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        isc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xF8 -> { // SED, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        systemBus.getBus().readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFD(true);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xF9 -> { // SBC, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        sbc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xFB -> { // ISC, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getY());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        isc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xFD -> { // SBC, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        if (!getBoundaryCrossed()) {
                            subCycleIndex = 8;
                        } else {
                            subCycleIndex = 6;
                        }
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        sbc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xFE -> { // INC, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        inc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xFF -> { // ISC, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setTarget(getPointer() + getX());
                        setAddressLow(getTargetLow());
                        setAddressHigh(getPointerHigh());
                        setBoundaryCrossed(getPointerHigh() != getTargetHigh());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(systemBus.getBus().readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        isc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        systemBus.getBus().writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
        }
    }

    private void branchRelative(boolean condition) {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                setOperand(systemBus.getBus().readByte(getPC()));
                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);
                if (condition) {
                    subCycleIndex = 3;
                } else {
                    subCycleIndex = 7;
                }
            }
            case 3 -> {
                systemBus.getBus().readByte(getPC());
                subCycleIndex = 4;
            }
            case 4 -> {
                short signedOperand = (short) ((byte) getOperand());
                int originalPC = getPC();
                int signedOffsetHigh = ((originalPC + signedOperand) >>> 8) & 0xFF;
                if (signedOffsetHigh != getPCH()) {
                    subCycleIndex = 5;
                } else {
                    subCycleIndex = 7;
                }
                setPCL(getPCL() + signedOperand);

                // Save original PC
                setAddress(originalPC);
            }
            case 5 -> {
                systemBus.getBus().readByte(getPC());
                subCycleIndex = 6;
            }
            case 6 -> {
                short signedOperand = (short) ((byte) getOperand());

                // Original PC value stored in address
                setPCH(((getAddress() + signedOperand) >>> 8) & 0xFF);
                subCycleIndex = 7;
            }
            case 7 -> {
                subCycleIndex = TERMINATE_INSTRUCTION;
            }
        }
    }

    private void cpx() {
        setFC(getX() >= getOperand());
        setFN(((getX() - getOperand()) & 0x80) != 0);
        setFZ(getX() == getOperand());
    }

    private void isc() {
        setOperand(getOperand() + 1);
        sbc();
    }

    private void inc() {
        setOperand(getOperand() + 1);
        setFN((getOperand() & 0x80) != 0);
        setFZ(getOperand() == 0);
    }

    private void adc() {
        addOrSubCarry(false);
    }

    private void sbc() {
        addOrSubCarry(true);
    }

    private void addOrSubCarry(boolean subtract) {
        int a = getA();
        int m = subtract ? getOperand() ^ 0xFF : getOperand();
        int c = getFC() ? 1 : 0;

        int binarySum = a + m + c;
        setFV(((~(a ^ m)) & (a ^ binarySum) & 0x80) != 0);

        int result;

        if (getFD()) {
            int lo = (a & 0x0F) + (m & 0x0F) + c;
            int hi = (a & 0xF0) + (m & 0xF0);
            if (lo > 9) {
                lo += 6;
            }
            if (lo > 0x0F) {
                hi += 0x10;
            }
            if ((hi & 0x1F0) > 0x90) {
                hi += 0x60;
            }
            result = (lo & 0x0F) | (hi & 0xF0);
            setFC(hi > 0xF0);
        } else {
            result = binarySum;
            setFC(binarySum > 0xFF);
        }

        result &= 0xFF;
        setA(result);
        setFZ(result == 0);
        setFN((result & 0x80) != 0);
    }

    public interface SystemBus extends io.github.arkosammy12.jemu.core.common.SystemBus {

        boolean getIRQ();

        boolean getNMI();

        boolean getRes();

        boolean getRdy();

    }

    private enum Phase {
        PHI_1,
        PHI_2;

        private Phase getOpposite() {
            return switch (this) {
                case PHI_1 -> PHI_2;
                case PHI_2 -> PHI_1;
            };
        }

    }

    private enum BRKSource {
        SOFTWARE,
        IRQ,
        NMI,
        RESET
    }

}
