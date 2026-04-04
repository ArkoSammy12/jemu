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
    private int processorStatus = M_MASK; // P, 8 bit
    private int stackPointer; // S, 8 bits

    private int instructionRegister; // 8 bits

    protected static final int TERMINATE_INSTRUCTION = -1;

    protected int subCycleIndex = TERMINATE_INSTRUCTION;
    private boolean firstSubCycle = true;
    private int operand;
    private int address;
    private int pointer;
    private int target;
    private int finalVar;
    private int temp;
    private boolean boundaryCrossed;

    private Phase phase = Phase.PHI_1;
    private ReadWriteCycle readWriteCycle = ReadWriteCycle.READ;
    private boolean cpuHalted;
    private int lastAddress;

    private boolean oldNMI;
    protected boolean signalReset;
    private boolean signalNMI;
    private boolean signalIRQ;
    private BRKSource brkSource = BRKSource.SOFTWARE;
    private int brkVector = IRQ_BRK_VECTOR;
    private boolean pushB;

    public NMOS6502(SystemBus systemBus) {
        this.systemBus = systemBus;
    }

    public ReadWriteCycle getReadWriteCycle() {
        return this.readWriteCycle;
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

    protected void setS(int value) {
        this.stackPointer = value & 0xFF;
    }

    public int getS() {
        return this.stackPointer;
    }

    protected void setP(int value) {
        this.processorStatus = value & 0xFF;
        this.processorStatus |= M_MASK;
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

    protected void setFV(boolean value) {
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

    protected void setFD(boolean value) {
        setP(value ? Processor.setBit(getP(), D_MASK) : Processor.clearBit(getP(), D_MASK));
    }

    protected boolean getFD() {
        return Processor.testBit(getP(), D_MASK);
    }

    protected void setFI(boolean value) {
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

    protected void setFC(boolean value) {
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

        boolean halted = this.cpuHalted;

        if (this.firstSubCycle) {
            this.firstSubCycle = false;
            this.onSubCycleEnd();
            return 0;
        }

        if (halted) {
            if (this.phase == Phase.PHI_2) {
                readByte(this.lastAddress);
            }
            this.onSubCycleEnd();
            return 0;
        }

        if (this.subCycleIndex >= 0) {
            this.execute();
        }

        if (this.subCycleIndex < 0) {
            setIR(readByte(getPC()));

            if (!this.cpuHalted) {
                if (this.signalReset) {
                    this.brkSource = BRKSource.RESET;
                } else if (this.signalNMI) {
                    this.brkSource = BRKSource.NMI;
                } else if (this.signalIRQ) {
                    this.brkSource = BRKSource.IRQ;
                } else {
                    this.brkSource = BRKSource.SOFTWARE;
                }

                if (this.brkSource != BRKSource.SOFTWARE) {
                    setIR(0x00);
                }
            }

            subCycleIndex = 0;
        }

        this.onSubCycleEnd();
        return 0;
    }

    private void onSubCycleEnd() {
        if (this.phase == Phase.PHI_2) {
            boolean nmiNow = systemBus.getNMI();
            if (!oldNMI && nmiNow) {
                this.signalNMI = true;
            }
            oldNMI = nmiNow;

            if (systemBus.getRES()) {
                this.signalReset = true;
            }
            if (systemBus.getIRQ() && !getFI()) {
                this.signalIRQ = true;
            }

            this.cpuHalted = systemBus.getRDY() && this.readWriteCycle == ReadWriteCycle.READ;
        }
        this.phase = this.phase.getOpposite();
    }

    private void execute() {
        int IR = getIR();
        switch (IR & 0xF0) {
            case 0x00 -> execute0X(IR & 0xF);
            case 0x10 -> execute1X(IR & 0xF);
            case 0x20 -> execute2X(IR & 0xF);
            case 0x30 -> execute3X(IR & 0xF);
            case 0x40 -> execute4X(IR & 0xF);
            case 0x50 -> execute5X(IR & 0xF);
            case 0x60 -> execute6X(IR & 0xF);
            case 0x70 -> execute7X(IR & 0xF);
            case 0x80 -> execute8X(IR & 0xF);
            case 0x90 -> execute9X(IR & 0xF);
            case 0xA0 -> executeAX(IR & 0xF);
            case 0xB0 -> executeBX(IR & 0xF);
            case 0xC0 -> executeCX(IR & 0xF);
            case 0xD0 -> executeDX(IR & 0xF);
            case 0xE0 -> executeEX(IR & 0xF);
            case 0xF0 -> executeFX(IR & 0xF);
        }
    }

    private void execute0X(int digit) {
        switch (digit) {
            case 0x0 -> { // BRK, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        if (this.brkSource == BRKSource.SOFTWARE) {
                            setPC(getPC() + 1);
                        }
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
                            readByte(getS() | 0x0100);
                        } else {
                            writeByte(getS() | 0x0100, getPCH());
                        }
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        if (this.brkSource == BRKSource.RESET) {
                            // TODO: Populate data bus
                            readByte(((getS() - 1) & 0xFF) | 0x0100);
                        } else {
                            writeByte(((getS() - 1) & 0xFF) | 0x0100, getPCL());
                        }
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int brkVector = IRQ_BRK_VECTOR;
                        this.pushB = true;

                        if (this.signalReset) {
                            brkVector = RESET_VECTOR;
                            this.pushB = false;
                        } else if (this.signalNMI) {
                            brkVector = NMI_VECTOR;
                            this.pushB = false;
                        }

                        if (this.signalReset) {
                            this.signalReset = false;
                        }
                        if (this.signalNMI) {
                            this.signalNMI = false;
                        }
                        if (this.signalIRQ) {
                            this.signalIRQ = false;
                        }

                        setBrkVector(brkVector);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        if (this.brkSource == BRKSource.RESET) {
                            // TODO: Populate data bus
                            readByte(((getS() - 2) & 0xFF) | 0x0100);
                        } else {
                            int P = getP();
                            if (this.pushB) {
                                P |= B_MASK;
                            }
                            writeByte(((getS() - 2) & 0xFF) | 0x0100, P);
                        }
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setS(getS() - 3);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setAddressLow(readByte(getBrkVector()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFI(true);
                        setFB(false);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        setAddressHigh(readByte((getBrkVector() + 1) & 0xFFFF));
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setPC(getAddress());
                        brkSource = BRKSource.SOFTWARE;
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1 -> { // ORA, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // SLO, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // NOP, zero page (read)
                nopZeroPageRead();
            }
            case 0x5 -> { // ORA, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // ASL, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // SLO, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // PHP, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        writeByte(getS() | 0x0100, getP() | B_MASK | M_MASK);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setS(getS() - 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9 -> { // ORA, immediate (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xA -> { // ASL, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xB -> { // ANC, immediate (read)
                ancImmediateRead();
            }
            case 0xC -> { // NOP, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xD -> { // ORA, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // ASL, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // SLO, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
        }
    }

    private void execute1X(int digit) {
        switch (digit) {
            case 0x0 -> { // BPL, relative (jump)
                branchRelative(!getFN());
            }
            case 0x1 -> { // ORA, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // SLO, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // NOP, zero page X (read)
                nopZeroPageXRead();
            }
            case 0x5 -> { // ORA, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // ASL, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // SLO, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // CLC, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // ORA, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xA -> { // NOP, implied
                nopImplied();
            }
            case 0xB -> { // SLO, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xC -> { // NOP, absolute X (read)
                nopAbsoluteXRead();
            }
            case 0xD -> { // ORA, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // ASL, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // SLO, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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

    private void execute2X(int digit) {
        switch (digit) {
            case 0x0 -> { // JSR, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getS() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getS() | 0x0100, getPCH());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(((getS() - 1) & 0xFF) | 0x0100, getPCL());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setPC(getAddress());
                        setS(getS() - 2);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x1 -> { // AND, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // RLA, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // BIT, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x5 -> { // AND, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // ROL, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // RLA, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // PLP, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getS() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setS(getS() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getS() | 0x0100));
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
            case 0x9 -> { // AND, immediate (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xA -> { // ROL, implied (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xB -> { // ANC, immediate (read)
                ancImmediateRead();
            }
            case 0xC -> { // BIT, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 ->  {
                        setOperand(readByte(getAddress()));
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
            case 0xD -> { // AND, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // ROL, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // RLA, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
        }
    }

    private void execute3X(int digit) {
        switch (digit) {
            case 0x0 -> { // BMI, relative (jump)
                branchRelative(getFN());
            }
            case 0x1 -> { // AND, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // RLA, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // NOP, zero page X (read)
                nopZeroPageXRead();
            }
            case 0x5 -> { // AND, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // ROL, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // RLA, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // SEC, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // AND, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xA -> { // NOP, implied
                nopImplied();
            }
            case 0xB -> { // RLA, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xC -> { // NOP, absolute X (read)
                nopAbsoluteXRead();
            }
            case 0xD -> { // AND, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // ROL, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // RLA, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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

    private void execute4X(int digit) {
        switch (digit) {
            case 0x0 -> { // RTI, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getS() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setTemp(readByte(((getS() + 1) & 0xFF) | 0x0100));
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
                        setAddressLow(readByte(((getS() + 2) & 0xFF) | 0x0100));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setS(getS() + 3);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setAddressHigh(readByte(getS() | 0x0100));
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
            case 0x1 -> { // EOR, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // SRE, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // NOP, zero page (read)
                nopZeroPageRead();
            }
            case 0x5 -> { // EOR, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // LSR, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // SRE, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // PHA, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        writeByte(getS() | 0x0100, getA());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setS(getS() - 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0x9 -> { // EOR, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xA -> { // LSR, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xB -> { // ASR, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xC -> { // JMP, absolute
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
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
            case 0xD -> { // EOR, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // LSR, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // SRE, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
        }
    }

    private void execute5X(int digit) {
        switch (digit) {
            case 0x0 -> { // BVC, relative (jump)
                branchRelative(!getFV());
            }
            case 0x1 -> { // EOR, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // SRE, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // NOP, zero page X (read)
                nopZeroPageXRead();
            }
            case 0x5 -> { // EOR, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // LSR, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // SRE, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // CLI, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // EOR, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xA -> { // NOP, implied
                nopImplied();
            }
            case 0xB -> { // SRE, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xC -> { // NOP, absolute X (read)
                nopAbsoluteXRead();
            }
            case 0xD -> { // EOR, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // LSR, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() >>> 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // SRE, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setFC((getOperand() & 1) != 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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

    private void execute6X(int digit) {
        switch (digit) {
            case 0x0 -> { // RTS, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getS() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(((getS() + 1) & 0xFF) | 0x0100));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setS(getS() + 2);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte(getS() | 0x0100));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPC(getAddress());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        readByte(getPC());
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
            case 0x1 -> { // ADC, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // RRA, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // NOP, zero page (read)
                nopZeroPageRead();
            }
            case 0x5 -> { // ADC, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // ROR, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // RRA, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // PLA, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getS() | 0x0100);
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setS(getS() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getS() | 0x0100));
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
            case 0x9 -> { // ADC, immediate (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xA -> { // ROR, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xB -> { // ARR, immediate (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xC -> { // JMP, indirect
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointerLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointerHigh() << 8) | ((getPointerLow() + 1) & 0xFF)));
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
            case 0xD -> { // ADC, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // ROR, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // RRA, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
        }
    }

    private void execute7X(int digit) {
        switch (digit) { // BVS, relative (jump)
            case 0x0 -> {
                branchRelative(getFV());
            }
            case 0x1 -> { // ADC, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // RRA, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // NOP, zero page X (read)
                nopZeroPageXRead();
            }
            case 0x5 -> { // ADC, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // ROR, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // RRA, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // SEI, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // ADC, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xA -> { // NOP, implied
                nopImplied();
            }
            case 0xB -> { // RRA, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xC -> { // NOP, absolute X (read)
                nopAbsoluteXRead();
            }
            case 0xD -> { // ADC, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // ROR, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // RRA, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        setFinal(getAddress() + getX());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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

    private void execute8X(int digit) {
        switch (digit) {
            case 0x0, 0x9, 0x2 -> { // NOP, immediate (read)
                nopImmediate();
            }
            case 0x1 -> { // STA, indirect X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getA());
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
            case 0x3 -> { // SAX, indirect X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getA() & getX());
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
            case 0x4 -> { // STY, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        writeByte(getAddress(), getY());
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
            case 0x5 -> { // STA, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        writeByte(getAddress(), getA());
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
            case 0x6 -> { // STX, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        writeByte(getAddress(), getX());
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
            case 0x7 -> { // SAX, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        writeByte(getAddress(), getA() & getX());
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
            case 0x8 -> { // DEY, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xA -> { // TXA, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xB -> { // ANE, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xC -> { // STY, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getY());
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
            case 0xD -> { // STA, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getA());
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
            case 0xE -> { // STX, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getX());
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
            case 0xF -> { // SAX, absolute (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddressLow(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getA() & getX());
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
        }
    }

    private void execute9X(int digit) {
        switch (digit) {
            case 0x0 -> { // BCC, relative (jump)
                branchRelative(!getFC());
            }
            case 0x1 -> { // STA, indirect Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setAddressHigh(getFinalHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getA());
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // SHA, indirect Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setFinal(getAddress() + getY());
                        setAddressLow(getFinalLow());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
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
                        writeByte(finalAddress, finalVal & 0xFF);
                        subCycleIndex = 11;
                    }
                    case 10 -> { // PAGE BOUNDARY CROSSED BRANCH
                        int high = getAddressHigh() & 0xFFFF;
                        // TODO: If the RDY flag was just set, set High to 0xFFFF
                        int finalVal = (high & getA() & getX()) & 0xFFFF;
                        int finalAddress = ((finalVal << 8) | getAddressLow()) & 0xFFFF;
                        writeByte(finalAddress, finalVal & 0xFF);
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
            case 0x4 -> { // STY, zero page, X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getY());
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
            case 0x5 -> { // STA, zero page (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getA());
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
            case 0x6 -> { // STX, zero page Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getY()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getX());
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
            case 0x7 -> { // SAX, zero page Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getPointer());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getY()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getA() & getX());
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
            case 0x8 -> { // TYA, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 ->  { // STA, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex= 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getA());
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
            case 0xA -> { // TXS, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setS(getX());
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB ->  { // TAS, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setS(getA() & getX());
                        if (getBoundaryCrossed()) {
                            int val = (getAddressHigh() & getA() & getX()) & 0xFF;
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            int val = ((getAddressHigh() + 1) & getA() & getX()) & 0xFF;
                            writeByte(getAddress(), val);
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
            case 0xC -> { // SHY, absolute X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        if (getBoundaryCrossed()) {
                            int val = getAddressHigh() & getY();
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            int val = ((getAddressHigh() + 1) & getY()) & 0xFF;
                            writeByte(getAddress(), val);
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
            case 0xD -> { // STA, absolute X (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getA());
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
            case 0xE -> { // SHX, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        if (getBoundaryCrossed()) {
                            int val = getAddressHigh() & getX();
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            int val = ((getAddressHigh() + 1) & getX()) & 0xFF;
                            writeByte(getAddress(), val);
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
            case 0xF -> { // SHA, absolute Y (write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            writeByte(getAddress(), val);
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
        }
    }

    protected void executeAX(int digit) {
        switch (digit) {
            case 0x0 -> { // LDY, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0x1 -> { // LDA, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // LDX, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0x3 -> { // LAX, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x4 -> { // LDY, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x5 -> { // LDA, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // LDX, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x7 -> { // LAX, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x8 -> { // TAY, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // LDA, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xA -> { // TAX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xB -> { // LXA, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xC -> { // LDY, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xD -> { // LDA, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // LDX, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xF -> { // LAX, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
        }
    }

    private void executeBX(int digit) {
        switch (digit) {
            case 0x0 -> {
                branchRelative(getFC());
            }
            case 0x1 -> { // LDA, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
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
                        setOperand(readByte(getPointer()));
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
                        setOperand(readByte(getPointer()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // LAX, indirect Y
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
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
                        setOperand(readByte(getPointer()));
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
                        setOperand(readByte(getPointer()));
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
            case 0x4 -> { // LDY, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x5 -> { // LDA, zero page X
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // LDX, zero page Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
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
                        setOperand(readByte(getAddress()));
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
            case 0x7 -> { // LAX, zero page Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
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
                        setOperand(readByte(getAddress()));
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
            case 0x8 -> { // CLV, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // LDA, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xA -> { // TSX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getS());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xB -> { // LAS, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int value = (getOperand() & getS());
                        setA(value);
                        setX(value);
                        setS(value);
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            case 0xC -> { // LDY, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xD -> { // LDA, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // LDX, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xF -> { // LAX, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
        }
    }

    private void executeCX(int digit) {
        switch (digit) {
            case 0x0 -> { // CPY, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0x1 -> { // CMP, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // NOP, immediate (read)
                nopImmediate();
            }
            case 0x3 -> { // DCP, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // CPY, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x5 -> { // CMP, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // DEC, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // DCP, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // INY, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // CMP, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xA -> { // DEX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0xB -> { // SBX, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0xC -> { // CPY, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xD -> { // CMP, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // DEC, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // DCP, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
        }
    }

    private void executeDX(int digit) {
        switch (digit) {
            case 0x0 -> { // BNE, relative (jump)
                branchRelative(!getFZ());
            }
            case 0x1 -> { // CMP, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
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
                        setOperand(readByte(getPointer()));
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
                        setOperand(readByte(getPointer()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // DCP, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
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
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getPointer(), getOperand());
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
                        writeByte(getPointer(), getOperand());
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
            case 0x4 -> { // NOP, zero page X (read)
                nopZeroPageXRead();
            }
            case 0x5 -> { // CMP, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // DEC, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // DCP, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // CLD, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // CMP, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xA -> { // NOP, implied
                nopImplied();
            }
            case 0xB -> { // DCP, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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
            case 0xC -> { // NOP, absolute X (read)
                nopAbsoluteXRead();
            }
            case 0xD -> { // CMP, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // DEC, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setOperand(getOperand() - 1);
                        setFN((getOperand() & 0x80) != 0);
                        setFZ(getOperand() == 0);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // DCP, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
                        writeByte(getAddress(), getOperand());
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

    private void executeEX(int digit) {
        switch (digit) {
            case 0x0 -> { // CPX, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
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
            case 0x1 -> { // SBC, indirect X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressLow(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x2 -> { // NOP, immediate (read)
                nopImmediate();
            }
            case 0x3 -> { // ISC, indirect X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        readByte(getOperand());
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPointer((getOperand() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        isc();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x4 -> { // CPX, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x5 -> { // SBC, zero page (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // INC, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        inc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // ISC, zero page (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        isc();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // INX, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9, 0xB -> { // SBC, immediate
                sbcImmediate();
            }
            case 0xA -> { // NOP, implied
                nopImplied();
            }
            case 0xC -> { // CPX, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xD -> { // SBC, absolute (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 ->  {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // INC, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        inc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // ISC, absolute (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setAddress(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddressHigh(readByte(getPC()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        isc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
        }
    }

    private void executeFX(int digit) {
        switch (digit) {
            case 0x0 -> { // BEQ, relative (jump)
                branchRelative(getFZ());
            }
            case 0x1 -> { // SBC, indirect Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
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
                        setOperand(readByte(getPointer()));
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
                        setOperand(readByte(getPointer()));
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
            case 0x2 -> { // JAM, implied
                jam();
            }
            case 0x3 -> { // ISC, indirect Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setAddress(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setAddressHigh(readByte((getPointer() + 1) & 0xFF));
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
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPointerHigh(getTargetHigh());
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getPointer(), getOperand());
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        isc();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getPointer(), getOperand());
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
            case 0x4 -> { // NOP, zero page X (read)
                nopZeroPageXRead();
            }
            case 0x5 -> { // SBC, zero page X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
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
            case 0x6 -> { // INC, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        inc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x7 -> { // ISC, zero page X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setOperand(readByte(getPointer()));
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setAddress((getPointer() + getX()) & 0xFF);
                        subCycleIndex = 5;
                    }
                    case 5 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        isc();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0x8 -> { // SED, implied
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        readByte(getPC());
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
            case 0x9 -> { // SBC, absolute Y (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xA -> { // NOP, implied
                nopImplied();
            }
            case 0xB -> { // ISC, absolute Y (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        isc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0xC -> { // NOP, absolute X (read)
                nopAbsoluteXRead();
            }
            case 0xD -> { // SBC, absolute X (read)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
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
                        setOperand(readByte(getAddress()));
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
            case 0xE -> { // INC, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        inc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
            case 0xF -> { // ISC, absolute X (read/modify/write)
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setPointer(readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        setPointerHigh(readByte(getPC()));
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
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setAddressHigh(getTargetHigh());
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        setOperand(readByte(getAddress()));
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        isc();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
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
                setOperand(readByte(getPC()));
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
                readByte(getPC());
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
                readByte(getPC());
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

    private void jam() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                readByte(getPC());
                subCycleIndex = 2;
            }
            case 2 -> {
                subCycleIndex = 3;
            }
            case 3 -> {
                readByte(0xFFFF);
                subCycleIndex = 4;
            }
            case 4 -> {
                subCycleIndex = 5;
            }
            case 5 -> {
                readByte(0xFFFE);
                subCycleIndex = 6;
            }
            case 6 -> {
                subCycleIndex = 7;
            }
            case 7 -> {
                readByte(0xFFFE);
                subCycleIndex = 8;
            }
            case 8 -> {
                subCycleIndex = 9;
            }
            case 9 -> {
                readByte(0xFFFF);
                subCycleIndex = 8;
            }
        }
    }

    private void nopZeroPageRead() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                setAddress(readByte(getPC()));
                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);
                subCycleIndex = 3;
            }
            case 3 -> {
                setOperand(readByte(getAddress()));
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

    private void ancImmediateRead() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                setOperand(readByte(getPC()));
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

    private void nopZeroPageXRead() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                setPointer(readByte(getPC()));
                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);
                subCycleIndex = 3;
            }
            case 3 -> {
                setOperand(readByte(getPointer()));
                subCycleIndex = 4;
            }
            case 4 -> {
                setAddress((getPointer() + getX()) & 0xFF);
                subCycleIndex = 5;
            }
            case 5 -> {
                setOperand(readByte(getAddress()));
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

    private void nopImplied() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                readByte(getPC());
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

    private void nopAbsoluteXRead() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                setAddressLow(readByte(getPC()));
                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);
                subCycleIndex = 3;
            }
            case 3 -> {
                setAddressHigh(readByte(getPC()));
                subCycleIndex = 4;
            }
            case 4 -> {
                setPC(getPC() + 1);
                setFinal(getAddress() + getX());
                setAddressLow(getFinalLow());
                subCycleIndex = 5;
            }
            case 5 -> {
                setOperand(readByte(getAddress()));
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
                setOperand(readByte(getAddress()));
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

    private void nopImmediate() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                setOperand(readByte(getPC()));
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

    private void sbcImmediate() {
        switch (subCycleIndex) {
            case 0 -> {
                setPC(getPC() + 1);
                subCycleIndex = 1;
            }
            case 1 -> {
                setOperand(readByte(getPC()));
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

    private int readByte(int address) {
        this.readWriteCycle = ReadWriteCycle.READ;
        this.lastAddress = address;
        return systemBus.getBus().readByte(address);
    }

    private void writeByte(int address, int value) {
        this.readWriteCycle = ReadWriteCycle.WRITE;
        this.lastAddress = address;
        systemBus.getBus().writeByte(address, value);
    }

    public interface SystemBus extends io.github.arkosammy12.jemu.core.common.SystemBus {

        boolean getIRQ();

        boolean getNMI();

        boolean getRES();

        boolean getRDY();

    }

    public enum ReadWriteCycle {
        READ,
        WRITE
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
