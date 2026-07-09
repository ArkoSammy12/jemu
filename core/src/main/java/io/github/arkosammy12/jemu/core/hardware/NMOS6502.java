package io.github.arkosammy12.jemu.core.hardware;

import io.github.arkosammy12.jemu.core.common.Processor;

public abstract class NMOS6502 implements Processor {

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

    protected static final int TERMINATE_INSTRUCTION = -1;

    protected final SystemBus systemBus;

    private int programCounter; // PC, 16 bits
    private int accumulator; // A, 8 bits
    private int X; // 8 bits
    private int Y; // 8 bits
    private int processorStatus = M_MASK; // P, 8 bit
    private int stackPointer; // S, 8 bits

    // Initialize to a negative value to force the first cycle to fetch only
    private int instructionRegister = -1; // 8 bits

    protected int subCycleIndex;
    private int operand;
    private int address;
    private int pointer;
    private int target;
    private int finalVar;
    private int temp;
    private boolean boundaryCrossed;
    private boolean shxSkipHigh;

    private Phase phase = Phase.PHI_1;
    private ReadWriteCycle readWriteCycle = ReadWriteCycle.READ;
    protected boolean isHalted;
    private int lastAddress;
    private boolean sync;

    private boolean oldNMI;
    private boolean nmiEdgeLatch;
    private boolean disablePCWrites;
    protected BRKSource brkSource = null;
    private int brkVector = IRQ_BRK_VECTOR;
    private boolean pushB;
    private boolean syncOnThisPHI1;

    public NMOS6502(SystemBus systemBus) {
        this.systemBus = systemBus;

        // Trigger the initial resetting of the CPU
        this.brkSource = BRKSource.RESET;
    }

    public Phase getHalfCyclePhase() {
        return this.phase;
    }

    public ReadWriteCycle getReadWriteCycle() {
        return this.readWriteCycle;
    }

    public int getLastAddress() {
        return this.lastAddress;
    }

    public boolean getSYNC() {
        return this.sync;
    }

    private void setBRKVector(int vector) {
        this.brkVector = vector & 0xFFFF;
    }

    private int getBRKVector() {
        return this.brkVector;
    }

    protected void setPC(int value) {
        if (!this.disablePCWrites) {
            this.programCounter = value & 0xFFFF;
        }
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
        setP(value ? getP() | N_MASK : getP() & ~N_MASK);
    }

    private boolean getFN() {
        return (getP() & N_MASK) != 0;
    }

    protected void setFV(boolean value) {
        setP(value ? getP() | V_MASK : getP() & ~V_MASK);
    }

    private boolean getFV() {
        return (getP() & V_MASK) != 0;
    }

    private void setFM(boolean value) {
        setP(value ? getP() | M_MASK : getP() & ~M_MASK);
    }

    private boolean getFM() {
        return (getP() & M_MASK) != 0;
    }

    private void setFB(boolean value) {
        setP(value ? getP() | B_MASK : getP() & ~B_MASK);
    }

    private boolean getFB() {
        return (getP() & B_MASK) != 0;
    }

    protected void setFD(boolean value) {
        setP(value ? getP() | D_MASK : getP() & ~D_MASK);
    }

    protected boolean getFD() {
        return (getP() & D_MASK) != 0;
    }

    protected void setFI(boolean value) {
        setP(value ? getP() | I_MASK : getP() & ~I_MASK);
    }

    private boolean getFI() {
        return (getP() & I_MASK) != 0;
    }

    protected void setFZ(boolean value) {
        setP(value ? getP() | Z_MASK : getP() & ~Z_MASK);
    }

    private boolean getFZ() {
        return (getP() & Z_MASK) != 0;
    }

    protected void setFC(boolean value) {
        setP(value ? getP() | C_MASK : getP() & ~C_MASK);
    }

    protected boolean getFC() {
        return (getP() & C_MASK) != 0;
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

    private void setSHXSkipHigh(boolean value) {
        this.shxSkipHigh = value;
    }

    private boolean getSHXSkipHigh() {
        return this.shxSkipHigh;
    }

    @Override
    public int cycle() {
        int originalSubCycleIndex = this.subCycleIndex;
        int originalInstructionRegister = this.getIR();
        boolean originalDisablePCWrites = this.disablePCWrites;
        BRKSource originalBrkSource = this.brkSource;
        int originalBrkVector = this.brkVector;
        boolean originalPushB = this.pushB;

        if (this.isHalted) {
            // Repeat the last read cycle on each PHI2
             if (this.phase == Phase.PHI_2) {
                 if (this.subCycleIndex >= 0) {
                     this.execute();
                 }
                 if (this.subCycleIndex < 0) {
                     setIR(readByte(getPC()));
                     subCycleIndex = 0;
                 }
             }
            this.onSubCycleEnd(originalSubCycleIndex, originalInstructionRegister, originalDisablePCWrites, originalBrkSource, originalBrkVector, originalPushB);
            return 0;
        }

        if (this.subCycleIndex >= 0) {
            this.execute();
        }

        if (this.subCycleIndex < 0) {
            setIR(readByte(getPC()));

            if (this.brkSource != null) {
                setIR(0x00);
            } else if (getIR() == 0x00) {
                this.brkSource = BRKSource.SOFTWARE;
            }

            if (getIR() == 0x00) {
                this.pushB = true;
            }

            this.disablePCWrites = switch (this.brkSource) {
                case IRQ, NMI, RESET -> true;
                case null, default -> false;
            };

            subCycleIndex = 0;
        }

        this.onSubCycleEnd(originalSubCycleIndex, originalInstructionRegister, originalDisablePCWrites, originalBrkSource, originalBrkVector, originalPushB);
        return 0;
    }

    private void onSubCycleEnd(int originalSubCycleIndex, int originalInstructionRegister, boolean originalDisablePCWrites, BRKSource originalBrkSource, int originalBrkVector, boolean originalPushB) {
        switch (this.phase) {
            case PHI_1 -> {
                if (this.syncOnThisPHI1) {
                    this.syncOnThisPHI1 = false;
                    this.sync = true;
                } else if (!this.isHalted) {
                    this.sync = false;
                }
            }
            case PHI_2 -> {
                boolean currentNMI = systemBus.getNMI();
                if (!this.oldNMI && currentNMI) {
                    this.nmiEdgeLatch = true;
                }
                this.oldNMI = currentNMI;
                this.isHalted = systemBus.getRDY() && this.readWriteCycle == ReadWriteCycle.READ;
                if (this.isHalted) {
                    setIR(originalInstructionRegister);
                    this.subCycleIndex = originalSubCycleIndex;
                    this.disablePCWrites = originalDisablePCWrites;
                    this.brkSource = originalBrkSource;
                    setBRKVector(originalBrkVector);
                    this.pushB = originalPushB;
                }

                if (systemBus.getRES()) {
                    setIR(0x00);
                    subCycleIndex = 0;
                    brkSource = BRKSource.RESET;
                    pushB = true;
                    disablePCWrites = true;
                }
            }
        }
        this.phase = this.phase.getOpposite();
    }

    private void onFetchCyclePHI1() {
        this.syncOnThisPHI1 = true;
    }

    protected void pollInterrupts() {
        if (systemBus.getRDY() && this.readWriteCycle == ReadWriteCycle.READ) {
            return;
        }
        if (this.nmiEdgeLatch) {
            this.brkSource = BRKSource.NMI;
        } else if (systemBus.getIRQ() && !getFI()) {
            this.brkSource = BRKSource.IRQ;
        }
    }

    private void execute() {
        int IR = getIR();
        if (IR < 0) {
            this.subCycleIndex = this.phase == Phase.PHI_2 ? TERMINATE_INSTRUCTION : 0;
            return;
        }
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
                        if (this.brkSource == BRKSource.RESET) {
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
                            readByte(((getS() - 1) & 0xFF) | 0x0100);
                        } else {
                            writeByte(((getS() - 1) & 0xFF) | 0x0100, getPCL());
                        }
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        int brkVector = IRQ_BRK_VECTOR;
                        if (this.brkSource == BRKSource.RESET) {
                            brkVector = RESET_VECTOR;
                        } else if (this.nmiEdgeLatch) {
                            brkVector = NMI_VECTOR;
                            this.nmiEdgeLatch = false;
                        }

                        if (this.brkSource != BRKSource.SOFTWARE) {
                            this.pushB = false;
                        }

                        setBRKVector(brkVector);
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        if (this.brkSource == BRKSource.RESET) {
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
                        setAddressLow(readByte(getBRKVector()));
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setFI(true);
                        setFB(false);
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        this.disablePCWrites = false;
                        setAddressHigh(readByte((getBRKVector() + 1) & 0xFFFF));
                        // Does not poll interrupts
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        setPC(getAddress());
                        brkSource = null;

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        ora();

                        onFetchCyclePHI1();
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
                        slo();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        ora();

                        onFetchCyclePHI1();
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
                        asl();

                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        slo();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setS(getS() - 1);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        ora();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFC((getA() & 0x80) != 0);
                        int result = (getA() << 1) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        ora();

                        onFetchCyclePHI1();
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
                        asl();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        slo();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        ora();

                        onFetchCyclePHI1();
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
                        slo();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        ora();

                        onFetchCyclePHI1();
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
                        asl();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        slo();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFC(false);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        ora();

                        onFetchCyclePHI1();
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
                        slo();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        ora();

                        onFetchCyclePHI1();
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
                        asl();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        slo();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setPC(getAddress());
                        setS(getS() - 2);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        and();

                        onFetchCyclePHI1();
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
                        rla();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        bit();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        and();

                        onFetchCyclePHI1();
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
                        rol();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        rla();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        setP(getOperand());
                        setFB(false);
                        setFM(true);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        and();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        boolean originalHighBit = (getA() & 0x80) != 0;
                        int result = ((getA() << 1) | (getFC() ? 1 : 0)) & 0xFF;
                        setA(result);
                        setFC(originalHighBit);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        bit();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        and();

                        onFetchCyclePHI1();
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
                        rol();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        rla();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        and();

                        onFetchCyclePHI1();
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
                        rla();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        and();

                        onFetchCyclePHI1();
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
                        rol();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        rla();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 ->  {
                        setFC(true);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        and();

                        onFetchCyclePHI1();
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
                        rla();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        and();

                        onFetchCyclePHI1();
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
                        rol();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        rla();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setPC(getAddress());

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        eor();

                        onFetchCyclePHI1();
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
                        sre();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        eor();

                        onFetchCyclePHI1();
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
                        lsr();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        sre();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setS(getS() - 1);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        eor();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFC((getA() & 1) != 0);
                        int result = (getA() >>> 1) & 0xFF;
                        setA(result);
                        setFN(false);
                        setFZ(result == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
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

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        setPC(getAddress());

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        eor();

                        onFetchCyclePHI1();
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
                        lsr();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        sre();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        eor();

                        onFetchCyclePHI1();
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
                        sre();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        eor();

                        onFetchCyclePHI1();
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
                        lsr();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        sre();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 ->  {
                        setFI(false);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        eor();

                        onFetchCyclePHI1();
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
                        sre();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        eor();

                        onFetchCyclePHI1();
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
                        lsr();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        sre();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        setPC(getPC() + 1);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        adc();

                        onFetchCyclePHI1();
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
                        rra();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        adc();

                        onFetchCyclePHI1();
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
                        ror();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        rra();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        lda();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        adc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        int temp = getFC() ? 0x80 : 0x00;
                        setFC((getA() & 1) != 0);
                        int result = ((getA() >>> 1) | temp) & 0xFF;
                        setA(result);
                        setFN((result & 0x80) != 0);
                        setFZ(result == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        arr();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        setPC(getAddress());

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        adc();

                        onFetchCyclePHI1();
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
                        ror();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        rra();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        adc();

                        onFetchCyclePHI1();
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
                        rra();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        adc();

                        onFetchCyclePHI1();
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
                        ror();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        rra();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFI(true);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        adc();

                        onFetchCyclePHI1();
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
                        rra();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        adc();

                        onFetchCyclePHI1();
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
                        ror();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        rra();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setY(getY() - 1);
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setA(getX());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
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

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        setSHXSkipHigh(systemBus.getRDY());
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
                        if (getSHXSkipHigh()){
                            high = 0xFF;
                        }
                        int finalVal = (high & getA() & getX()) & 0xFFFF;
                        int finalAddress = address;
                        writeByte(finalAddress, finalVal & 0xFF);
                        pollInterrupts();
                        subCycleIndex = 11;
                    }
                    case 10 -> { // PAGE BOUNDARY CROSSED BRANCH
                        int high = getAddressHigh() & 0xFFFF;
                        if (getSHXSkipHigh()){
                            high = 0xFF;
                        }
                        int finalVal = (high & getA() & getX()) & 0xFFFF;
                        int finalAddress = ((finalVal << 8) | getAddressLow()) & 0xFFFF;
                        writeByte(finalAddress, finalVal & 0xFF);
                        pollInterrupts();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setA(getY());
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setS(getX());

                        onFetchCyclePHI1();
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
                        setSHXSkipHigh(systemBus.getRDY());
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
                        int high = getAddressHigh();
                        if (getBoundaryCrossed()) {
                            if (getSHXSkipHigh()) {
                                high = 0xFF;
                            }
                            int val = (high & getA() & getX()) & 0xFF;
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            high = (high + 1) & 0xFF;
                            if (getSHXSkipHigh()) {
                                high = 0xFF;
                            }
                            int val = (high & getA() & getX()) & 0xFF;
                            writeByte(getAddress(), val);
                        }
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        setSHXSkipHigh(systemBus.getRDY());
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
                        if (getBoundaryCrossed()) {
                            if (getSHXSkipHigh()) {
                                high = 0xFF;
                            }
                            int val = high & getY();
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            high = (high + 1) & 0xFF;
                            if (getSHXSkipHigh()) {
                                high = 0xFF;
                            }
                            int val = (high & getY()) & 0xFF;
                            writeByte(getAddress(), val);
                        }
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        setSHXSkipHigh(systemBus.getRDY());
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
                        if (getBoundaryCrossed()) {
                            if (getSHXSkipHigh()) {
                                high = 0xFF;
                            }
                            int val = high & getX();
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            high = (high + 1) & 0xFF;
                            if (getSHXSkipHigh()) {
                                high = 0xFF;
                            }
                            int val = (high & getX()) & 0xFF;
                            writeByte(getAddress(), val);
                        }
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        setSHXSkipHigh(systemBus.getRDY());
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
                        if (getSHXSkipHigh()){
                            high = 0xFF;
                        }
                        int val = (high & getA() & getX()) & 0xFF;
                        if (getBoundaryCrossed()) {
                            writeByte(getAddressLow() | (val << 8), val);
                        } else {
                            writeByte(getAddress(), val);
                        }
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        ldy();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        lda();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        ldx();
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        lax();
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        ldy();
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        lda();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        ldx();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        lax();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setY(getA());
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        lda();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getA());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int value = ((getA() | 0xFF) & getOperand()) & 0xFF;
                        setA(value);
                        setX(value);
                        setFN((value & 0x80) != 0);
                        setFZ(value == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        ldy();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        lda();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        ldx();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        lax();

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        lda();

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        lax();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        ldy();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        lda();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        ldx();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        lax();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFV(false);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        lda();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getS());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        int value = (getOperand() & getS());
                        setA(value);
                        setX(value);
                        setS(value);
                        setFN((getA() & 0x80) != 0);
                        setFZ(getA() == 0);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        ldy();

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        lda();

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        ldx();

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        lax();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        cpy();
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        cmp();

                        onFetchCyclePHI1();
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
                        dcp();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        cpy();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        cmp();

                        onFetchCyclePHI1();
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
                        dec();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        dcp();
                        subCycleIndex = 7;
                    }
                    case 7 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setY(getY() + 1);
                        setFN((getY() & 0x80) != 0);
                        setFZ(getY() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        cmp();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getX() - 1);
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int anx = getA() & getX();
                        setFC(anx >= getOperand());
                        setX(anx - getOperand());
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        cpy();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        cmp();

                        onFetchCyclePHI1();
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
                        dec();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        dcp();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        cmp();

                        onFetchCyclePHI1();
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
                        dcp();
                        subCycleIndex = 13;
                    }
                    case 13 -> {
                        writeByte(getPointer(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        cmp();

                        onFetchCyclePHI1();
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
                        dec();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        dcp();
                        subCycleIndex = 9;
                    }
                    case 9 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFD(false);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        cmp();

                        onFetchCyclePHI1();
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
                        dcp();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        cmp();

                        onFetchCyclePHI1();
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
                        dec();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        dcp();
                        subCycleIndex = 11;
                    }
                    case 11 -> {
                        writeByte(getAddress(), getOperand());
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        cpx();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        sbc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        cpx();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 4;
                    }
                    case 4 -> {
                        sbc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setX(getX() + 1);
                        setFN((getX() & 0x80) != 0);
                        setFZ(getX() == 0);

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        cpx();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        sbc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        sbc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 14;
                    }
                    case 14 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 6;
                    }
                    case 6 -> {
                        sbc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 10;
                    }
                    case 10 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setFD(true);

                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        sbc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                            pollInterrupts();
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
                        pollInterrupts();
                        subCycleIndex = 8;
                    }
                    case 8 -> {
                        sbc();

                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                        pollInterrupts();
                        subCycleIndex = 12;
                    }
                    case 12 -> {
                        onFetchCyclePHI1();
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
                pollInterrupts();
                if (condition) {
                    int base = (getPC() + 1) & 0xFFFF;
                    setAddress((base + (int) (byte) getOperand()) & 0xFFFF);
                    setBoundaryCrossed(getAddressHigh() != ((base >>> 8) & 0xFF));
                }

                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);
                if (condition) {
                    subCycleIndex = 3;
                } else {
                    onFetchCyclePHI1();
                    subCycleIndex = 7;
                }
            }
            case 3 -> {
                readByte(getPC());
                subCycleIndex = 4;
            }
            case 4 -> {
                setPCL(getAddressLow());
                if (!getBoundaryCrossed()) {
                    onFetchCyclePHI1();
                    subCycleIndex = 7;
                } else {
                    subCycleIndex = 5;
                }
            }
            case 5 -> {
                readByte(getPC());
                pollInterrupts();
                subCycleIndex = 6;
            }
            case 6 -> {
                setPCH(getAddressHigh());
                onFetchCyclePHI1();
                subCycleIndex = 7;
            }
            case 7 -> {
                subCycleIndex = TERMINATE_INSTRUCTION;
            }
        }
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
                // Does not poll interrupts
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
                pollInterrupts();
                subCycleIndex = 4;
            }
            case 4 -> {
                onFetchCyclePHI1();
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
                pollInterrupts();
                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);
                int result = (getA() & getOperand()) & 0xFF;
                setA(result);
                setFC((result & 0x80) != 0);
                setFN((result & 0x80) != 0);
                setFZ(result == 0);

                onFetchCyclePHI1();
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
                pollInterrupts();
                subCycleIndex = 6;
            }
            case 6 -> {
                onFetchCyclePHI1();
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
                pollInterrupts();
                subCycleIndex = 2;
            }
            case 2 -> {
                onFetchCyclePHI1();
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
                    pollInterrupts();
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
                pollInterrupts();
                subCycleIndex = 8;
            }
            case 8 -> {
                onFetchCyclePHI1();
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
                pollInterrupts();
                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);

                onFetchCyclePHI1();
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
                pollInterrupts();
                subCycleIndex = 2;
            }
            case 2 -> {
                setPC(getPC() + 1);
                sbc();

                onFetchCyclePHI1();
                subCycleIndex = 3;
            }
            case 3 -> {
                subCycleIndex = TERMINATE_INSTRUCTION;
            }
        }
    }

    private void slo() {
        setFC((getOperand() & 0x80) != 0);
        setOperand(getOperand() << 1);
        ora();
    }

    private void ora() {
        int result = (getA() | getOperand()) & 0xFF;
        setA(result);
        setFN((result & 0x80) != 0);
        setFZ(result == 0);
    }

    private void asl() {
        setFC((getOperand() & 0x80) != 0);
        setOperand(getOperand() << 1);
        setFN((getOperand() & 0x80) != 0);
        setFZ(getOperand() == 0);
    }

    private void and() {
        int result = (getA() & getOperand()) & 0xFF;
        setA(result);
        setFN((result & 0x80) != 0);
        setFZ(result == 0);
    }

    private void rla() {
        boolean originalHighBit = (getOperand() & 0x80) != 0;
        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
        int result = (getA() & getOperand()) & 0xFF;
        setA(result);
        setFC(originalHighBit);
        setFN((result & 0x80) != 0);
        setFZ(result == 0);
    }

    private void bit() {
        setFV((getOperand() & (1 << 6)) != 0);
        setFN((getOperand() & (1 << 7)) != 0);
        setFZ((getOperand() & getA()) == 0);
    }

    private void rol() {
        boolean originalHighBit = (getOperand() & 0x80) != 0;
        setOperand((getOperand() << 1) | (getFC() ? 1 : 0));
        setFC(originalHighBit);
        setFN((getOperand() & 0x80) != 0);
        setFZ(getOperand() == 0);
    }

    private void eor() {
        int result = (getA() ^ getOperand()) & 0xFF;
        setA(result);
        setFN((result & 0x80) != 0);
        setFZ(result == 0);
    }

    private void sre() {
        setOperand(getOperand() >>> 1);
        eor();
    }

    private void lsr() {
        setOperand(getOperand() >>> 1);
        setFN((getOperand() & 0x80) != 0);
        setFZ(getOperand() == 0);
    }

    private void rra() {
        int temp = getFC() ? 0x80 : 0x00;
        setFC((getOperand() & 1) != 0);
        setOperand((getOperand() >>> 1) | temp);
        adc();
    }

    private void ror() {
        int temp = getFC() ? 0x80 : 0x00;
        setFC((getOperand() & 1) != 0);
        setOperand((getOperand() >>> 1) | temp);
        setFN((getOperand() & 0x80) != 0);
        setFZ(getOperand() == 0);
    }

    private void lda() {
        setA(getOperand());
        setFN((getA() & 0x80) != 0);
        setFZ(getA() == 0);
    }

    private void ldx() {
        setX(getOperand());
        setFN((getX() & 0x80) != 0);
        setFZ(getX() == 0);
    }

    private void lax() {
        setA(getOperand());
        setX(getOperand());
        setFN((getA() & 0x80) != 0);
        setFZ(getA() == 0);
    }

    private void ldy() {
        setY(getOperand());
        setFN((getY() & 0x80) != 0);
        setFZ(getY() == 0);
    }

    private void cpx() {
        setFC(getX() >= getOperand());
        setFN(((getX() - getOperand()) & 0x80) != 0);
        setFZ(getX() == getOperand());
    }

    private void cpy() {
        setFC(getY() >= getOperand());
        setFN(((getY() - getOperand()) & 0x80) != 0);
        setFZ(getY() == getOperand());
    }

    private void cmp() {
        setFC(getA() >= getOperand());
        setFN(((getA() - getOperand()) & 0x80) != 0);
        setFZ(getA() == getOperand());
    }

    private void dcp() {
        setOperand(getOperand() - 1);
        cmp();
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

    private void dec() {
        setOperand(getOperand() - 1);
        setFN((getOperand() & 0x80) != 0);
        setFZ(getOperand() == 0);
    }

    protected void adc() {
        int carry = getFC() ? 1 : 0;
        if (getFD()) {
            int al = (getA() & 0xF) + (getOperand() & 0xF) + carry;
            if (al > 0x9) {
                al += 0x6;
            }
            al &= 0xFF;
            int ah = (((getA() >>> 4) & 0xF) + ((getOperand() >>> 4) & 0xF) + ((al > 0xF) ? 1 : 0)) & 0xFF;
            setFN((ah & 0x8) != 0);
            setFV((~(getA() ^ getOperand()) & (getA() ^ (ah << 4)) & 0x80) != 0);
            if (ah > 0x9) {
                ah = (ah + 0x6) & 0xFF;
            }
            setFC(ah > 0xF);
            setFZ(((getA() + getOperand() + carry) & 0xFF) == 0);
            setA(((ah << 4) & 0xF0) | (al & 0xF));
        } else {
            int val = getA() + getOperand() + carry;
            setFV((((val ^ getA()) & (val ^ getOperand())) & 0x80) != 0);
            setA(val);
            setFC(val > 0xFF);
            setFN((getA() & 0x80) != 0);
            setFZ(getA() == 0);
        }
    }

    protected void sbc() {
        int carry = getFC() ? 0 : 1;
        if (getFD()) {
            int al = ((getA() & 0xF) - (getOperand() & 0xF) - carry) & 0xFF;
            if (al > 0xF) {
                al = (al - 0x6) & 0xFF;
            }
            int ah = (((getA() >>> 4) & 0xF) - ((getOperand() >>> 4) & 0xF) - (((al & 0x80) != 0) ? 1 : 0)) & 0xFF;
            setFN((ah & 0x8) != 0);
            setFV(((getA() ^ getOperand()) & (getA() ^ (ah << 4)) & 0x80) != 0);
            if (ah > 0xF) {
                ah = (ah - 0x6) & 0xFF;
            }
            int sub = getA() - getOperand() - carry;
            setFC(sub >= 0);
            setFZ((sub & 0xFF) == 0);
            setA(((ah << 4) & 0xF0) | (al & 0xF));
        } else {
            int val = getA() - getOperand() - carry;
            setFV(((getA() & 0x80) != (getOperand() & 0x80)) && ((getA() & 0x80) != (val & 0x80)));
            setA(val);
            setFC(val >= 0);
            setFN((getA() & 0x80) != 0);
            setFZ(getA() == 0);
        }
    }

    protected void arr() {
        setA(getA() & getOperand());
        boolean carry = getFC();
        int A = (getA() >>> 1) & 0xFF;
        A |= carry ? 0x80 : 0x00;
        setFZ(A == 0);
        setFN((A & 0x80) != 0);
        if (getFD()) {
            setFV(((A ^  getA()) & 0x40) != 0);
            if ((getA() & 0x0F) >= 0x05) {
                A = ((A + 0x6) & 0xF) | (A & 0xF0);
            }
            if ((getA() & 0xF0) >= 0x50) {
                A = (A + 0x60) & 0xFF;
                setFC(true);
            } else {
                setFC(false);
            }
        } else {
            setFC((A & 0x40) != 0);
            setFV((A & 0x40) != 0);
            if ((A & 0x20) != 0) {
                setFV(!getFV());
            }
        }
        setA(A);
    }

    protected int readByte(int address) {
        this.readWriteCycle = ReadWriteCycle.READ;
        this.lastAddress = address;
        return systemBus.getBus().readByte(address);
    }

    protected void writeByte(int address, int value) {
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

    public enum Phase {
        PHI_1,
        PHI_2;

        private Phase getOpposite() {
            return switch (this) {
                case PHI_1 -> PHI_2;
                case PHI_2 -> PHI_1;
            };
        }

    }

    protected enum BRKSource {
        SOFTWARE,
        IRQ,
        NMI,
        RESET
    }

}
