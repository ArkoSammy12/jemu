package io.github.arkosammy12.jemu.cpu;

import com.sun.jna.platform.win32.COM.tlb.imp.TlbPropertyGet;
import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.systems.SystemBus;

public class SM83 implements Processor {

    private static final int Z_MASK = 1 << 7;
    private static final int N_MASK = 1 << 6;
    private static final int H_MASK = 1 << 5;
    private static final int C_MASK = 1 << 4;

    private final SystemBus systemBus;

    private int programCounter = 0x0100; // PC, 16 bits
    private int stackPointer = 0xFFFE; // SP, 16 bits
    private int instructionRegister; // IR, 8 bits
    private int interruptEnable; // IE, 8 Bits

    private int AF = 0x01B0; // 16 bits
    private int BC = 0x0013; // 16 bits
    private int DE = 0x00D8; // 16 bits
    private int HL = 0x014D; // 16 bits

    private int WZ; // 16 bits

    public SM83(SystemBus systemBus) {
        this.systemBus = systemBus;
    }

    private void setPC(int value) {
        this.programCounter = value & 0xFFFF;
    }

    public int getPC() {
        return this.programCounter;
    }

    private void setSP(int value) {
        this.stackPointer = value & 0xFFFF;
    }

    public int getSP() {
        return this.stackPointer;
    }

    private void setIR(int value) {
        this.instructionRegister = value & 0xFF;
    }

    public int getIR() {
        return this.instructionRegister;
    }

    private void setIE(int value) {
        this.interruptEnable = value & 0xFF;
    }

    public int getIE() {
        return this.interruptEnable;
    }

    private void setAF(int value) {
        this.AF = value & 0xFFFF;
        // Make sure the lower 4 bits are always 0
        this.AF &= ~0b00001111;
    }

    public int getAF() {
        return this.AF;
    }

    private void setBC(int value) {
        this.BC = value & 0xFFFF;
    }

    private int getBC() {
        return this.BC;
    }

    private void setDE(int value) {
        this.DE = value & 0xFFFF;
    }

    public int getDE() {
        return this.DE;
    }

    private void setHL(int value) {
        this.HL = value & 0xFFFF;
    }

    public int getHL() {
        return this.HL;
    }

    private void setA(int value) {
        setAF((value & 0xFF) << 8 | (this.getAF() & 0xFF));
    }

    public int getA() {
        return (this.AF & 0xFF00) >>> 8;
    }

    private void setFZ(boolean value) {
        setAF(value ? Processor.setBit(getAF(), Z_MASK) : Processor.clearBit(getAF(), Z_MASK));
    }

    public boolean getFZ() {
        return Processor.testBit(getAF(), Z_MASK);
    }

    private void setFN(boolean value) {
        setAF(value ? Processor.setBit(getAF(), N_MASK) : Processor.clearBit(getAF(), N_MASK));
    }

    public boolean getFN() {
        return Processor.testBit(getAF(), N_MASK);
    }

    private void setFH(boolean value) {
        setAF(value ? Processor.setBit(getAF(), H_MASK) : Processor.clearBit(getAF(), H_MASK));
    }

    public boolean getFH() {
        return Processor.testBit(getAF(), H_MASK);
    }

    private void setFC(boolean value) {
        setAF(value ? Processor.setBit(getAF(), C_MASK) : Processor.clearBit(getAF(), C_MASK));
    }

    public boolean getFC() {
        return Processor.testBit(getAF(), C_MASK);
    }

    private void setB(int value) {
        setBC((value & 0xFF) << 8 | getC());
    }

    public int getB() {
        return (this.BC & 0xFF00) >>> 8;
    }

    private void setC(int value) {
        setBC(this.getB() << 8 | (value & 0xFF));
    }

    public int getC() {
        return this.BC & 0xFF;
    }

    private void setD(int value) {
        setDE((value & 0xFF) << 8 | getE());
    }

    public int getD() {
        return (this.DE & 0xFF00) >>> 8;
    }

    private void setE(int value) {
        setDE(getD() << 8 | (value & 0xFF));
    }

    public int getE() {
        return this.DE & 0xFF;
    }

    private void setH(int value) {
        setHL((value & 0xFF) << 8 | getL());
    }

    public int getH() {
        return (this.HL & 0xFF00) >>> 8;
    }

    private void setL(int value) {
        setHL(getH() << 8 | (value & 0xFF));
    }

    public int getL() {
        return this.HL & 0xFF;
    }

    private void setWZ(int value) {
        this.WZ = value & 0xFFFF;
    }

    public int getWZ() {
        return this.WZ;
    }

    private void setW(int value) {
        setWZ((value & 0xFF) << 8 | getZ());
    }

    public int getW() {
        return (this.WZ & 0xFF00) >>> 8;
    }

    private void setZ(int value) {
        setWZ(getW() << 8 | (value & 0xFF));
    }

    public int getZ() {
        return this.WZ & 0xFF;
    }

    private static final int PREFIX = 0xCB;
    private static final int TERMINATE_INSTRUCTION = -1;

    private int machineCycleIndex = -1;
    private boolean opcodeIsPrefixed = false;

    @Override
    public int cycle() {
        if (this.machineCycleIndex >= 0) {
            if (this.opcodeIsPrefixed) {
                this.executePrefixed();
            } else {
                this.execute();
            }
            if (this.machineCycleIndex < 0) {
                this.opcodeIsPrefixed = false;
            }
        }
        if (this.machineCycleIndex < 0) {
            this.fetch();
            if (getIR() == PREFIX) {
                this.opcodeIsPrefixed = true;
            } else {
                this.machineCycleIndex = 0;
            }
        }
        return 0;
    }

    private void fetch() {
        setIR(this.systemBus.getBus().readByte(getPC()));
        setPC(getPC() + 1);
    }

    private void execute() {
        int x = getX(getIR());
        int y = getY(getIR());
        int z = getZ(getIR());
        int p = getP(getIR());
        int q = getQ(getIR());

        /*
            d = displacement byte (8-bit signed integer)
            n = 8-bit immediate operand (unsigned integer)
            nn = 16-bit immediate operand (unsigned integer)
         */

        switch (x) {
            case 0 -> {
                switch (z) {
                    case 0 -> {
                        switch (y) {
                            case 0 -> {} // NOP
                            case 1 -> { // LD (nn), SP
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setW(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        systemBus.getBus().writeByte(getWZ(), getSP() & 0xFF);
                                        setWZ(getWZ() + 1);
                                        machineCycleIndex = 3;
                                    }
                                    case 3 -> {
                                        systemBus.getBus().writeByte(getWZ(), (getSP() & 0xFF00) >>> 8);
                                        machineCycleIndex = 4;
                                    }
                                    case 4 -> {
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 2 -> {} // STOP
                            case 3 -> {} // JR d
                            case 4, 5, 6, 7 -> {} // JR cc[y-4], d
                        }
                    }
                    case 1 -> {
                        switch (q) {
                            case 0 -> { // LR rp[p], nn
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setW(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        setRP(p, getWZ());
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 1 -> { // ADD HL, rp[p]
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        int left = getL();
                                        int right = getRP(p) & 0xFF;
                                        int result = left + right;
                                        setL(result);
                                        setFN(false);
                                        setFH((left & 0xF) + (right & 0xF) > 0xF);
                                        setFC(result > 0xFF);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        int left = getH();
                                        int right = (getRP(p) & 0xFF00) >>> 8;
                                        int result = left + right + (getFC() ? 1 : 0);
                                        setH(result);
                                        setFN(false);
                                        setFH((left & 0xF) + (right & 0xF) + (getFC() ? 1 : 0) > 0xF);
                                        setFC(result > 0xFF);
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                        }
                    }
                    case 2 -> {
                        switch (q) {
                            case 0 -> {
                                switch (p) {
                                    case 0 -> { // LD (BC), A
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                systemBus.getBus().writeByte(getBC(), getA());
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                    case 1 -> { // LD (DE), A
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                systemBus.getBus().writeByte(getDE(), getA());
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                    case 2 -> { // LD (HL+), A
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                systemBus.getBus().writeByte(getHL(), getA());
                                                setHL(getHL() + 1);
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                    case 3 -> { // LD (HL-), A
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                systemBus.getBus().writeByte(getHL(), getA());
                                                setHL(getHL() - 1);
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                }
                            }
                            case 1 -> {
                                switch (p) {
                                    case 0 -> { // LD A, (BC)
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                setZ(systemBus.getBus().readByte(getBC()));
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                setA(getZ());
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                    case 1 -> { // LD A, (DE)
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                setZ(systemBus.getBus().readByte(getDE()));
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                setA(getZ());
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                    case 2 -> { // LD A, (HL+)
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                setZ(systemBus.getBus().readByte(getHL()));
                                                setHL(getHL() + 1);
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                setA(getZ());
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                    case 3 -> { // LD A, (HL-)
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                setZ(systemBus.getBus().readByte(getHL()));
                                                setHL(getHL() - 1);
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                setA(getZ());
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case 3 -> {
                        switch (q) {
                            case 0 -> { // INC rp[p]
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setRP(p, getRP(p) + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 1 -> { // DEC rp[p]
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setRP(p, getRP(p) - 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                        }
                    }
                    case 4 -> {
                        if (y == 6) { // INC (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    int result = getZ() + 1;
                                    this.systemBus.getBus().writeByte(getHL(), result);
                                    setFZ((result & 0xFF) == 0);
                                    setFN(false);
                                    setFH((getZ() & 0xF) + 1 > 0xF);
                                    machineCycleIndex = 2;
                                }
                                case 2 -> {
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // INC r[y]
                            int ry = getR(y);
                            int result = ry + 1;
                            setR(y, result);
                            setFZ((result & 0xFF) == 0);
                            setFN(false);
                            setFH((ry & 0xF) + 1 > 0xF);
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 5 -> {
                        if (y == 6) { // DEC (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    int result = getZ() - 1;
                                    this.systemBus.getBus().writeByte(getHL(), result);
                                    setFZ((result & 0xFF) == 0);
                                    setFN(true);
                                    setFH((getZ() & 0xF) < 1);
                                    machineCycleIndex = 2;
                                }
                                case 2 -> {
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // DEC r[y]
                            int ry = getR(y);
                            int result = ry - 1;
                            setR(y, result);
                            setFZ((result & 0xFF) == 0);
                            setFN(true);
                            setFH((ry & 0xF) < 1);
                        }
                    }
                    case 6 -> {
                        if (y == 6) { // LD (HL), n
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(systemBus.getBus().readByte(getPC()));
                                    setPC(getPC() + 1);
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    systemBus.getBus().writeByte(getHL(), getZ());
                                    machineCycleIndex = 2;
                                }
                                case 2 -> {
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // LD r[y], n
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(systemBus.getBus().readByte(getPC()));
                                    setPC(getPC() + 1);
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setR(y, getZ());
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        }
                    }
                    case 7 -> {
                        switch (y) {
                            case 0 -> {} // RLCA
                            case 1 -> {} // RRCA
                            case 2 -> {} // RLA
                            case 3 -> {} // RRA
                            case 4 -> { // DAA
                                int correction = 0;
                                if (getFH() || (!getFN() && (getA() & 0x0F) > 0x09)) {
                                    correction |= 0x06;
                                }
                                if (getFC() || (!getFN() && (getA() & 0xFF) > 0x99)) {
                                    correction |= 0x60;
                                    setFC(true);
                                }
                                boolean carry = false;
                                int right = correction;
                                if (getFN()) {
                                    carry = true;
                                    right = (~right) & 0xFF;
                                }
                                int result = (getA() + right + (carry ? 1 : 0));
                                setA(result);
                                setFH(false);
                                setFZ((result & 0xFF) == 0);
                                machineCycleIndex = TERMINATE_INSTRUCTION;
                            }
                            case 5 -> { // CPL
                                setA(~getA());
                                setFN(true);
                                setFH(true);
                                machineCycleIndex = TERMINATE_INSTRUCTION;
                            }
                            case 6 -> { // SCF
                                setFN(false);
                                setFH(false);
                                setFC(true);
                                machineCycleIndex = TERMINATE_INSTRUCTION;
                            }
                            case 7 -> { // CCF
                                setFN(false);
                                setFH(false);
                                setFC(!getFC());
                                machineCycleIndex = TERMINATE_INSTRUCTION;
                            }
                        }
                    }
                }
            }
            case 1 -> {
                if (z == 6 && y == 6) { // HALT

                } else if (z == 6) { // LD r, (HL)
                    switch (machineCycleIndex) {
                        case 0 -> {
                            setZ(systemBus.getBus().readByte(getHL()));
                            machineCycleIndex = 1;
                        } case 1 -> {
                            setR(y, getZ());
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                } else if (y == 6) { // LD (HL), r
                    switch (machineCycleIndex) {
                        case 0 -> {
                            systemBus.getBus().writeByte(getHL(), getR(z));
                            machineCycleIndex = 1;
                        }
                        case 1 -> {
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                } else { // LD r[y], r[z]
                    setR(y, getR(z));
                    machineCycleIndex = TERMINATE_INSTRUCTION;
                }
            }
            case 2 -> { // alu[y] r[z]
                switch (y) {
                    case 0 -> {
                        if (z == 6) { // ADD (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setA(add8(getA(), getZ()));
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // ADD A, r[z]
                            setA(add8(getA(), getR(z)));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 1 -> {
                        if (z == 6) { // ADC (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setA(adc8(getA(), getZ()));
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // ADC A, r[z]
                            setA(adc8(getA(), getR(z)));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 2 -> {
                        if (z == 6) { // SUB (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setA(sub8(getA(), getZ()));
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // SUB r[z]
                            setA(sub8(getA(), getR(z)));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 3 -> {
                        if (z == 6) { // SBC (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setA(sbc8(getA(), getZ()));
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // SBC A, r[z]
                            setA(sbc8(getA(), getR(z)));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 4 -> {
                        if (z == 6) { // AND (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setA(and8(getA(), getZ()));
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // AND r[z]
                            setA(and8(getA(), getR(z)));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 5 -> {
                        if (z == 6) { // XOR (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setA(xor8(getA(), getZ()));
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // XOR r[z]
                            setA(xor8(getA(), getR(z)));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 6 -> {
                        if (z == 6) { // OR (HL)
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    setA(or8(getA(), getZ()));
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // OR r[z]
                            setA(or8(getA(), getR(z)));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                    case 7 -> {
                        if (z == 6) {
                            switch (machineCycleIndex) {
                                case 0 -> {
                                    setZ(this.systemBus.getBus().readByte(getHL()));
                                    machineCycleIndex = 1;
                                }
                                case 1 -> {
                                    sub8(getA(), getZ());
                                    machineCycleIndex = TERMINATE_INSTRUCTION;
                                }
                            }
                        } else { // CP r[z]
                            sub8(getA(), getR(z));
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        }
                    }
                }
            }
            case 3 -> {
                switch (z) {
                    case 0 -> {
                        switch (y) {
                            case 0, 1, 2, 3 -> {} // RET cc[y]
                            case 4 -> { // LD (0xFF00 + n), A
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        systemBus.getBus().writeByte(0xFF00 | getZ(), getA());
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 5 -> { // ADD SP, d
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        int left = (getSP() & 0xFF);
                                        int right = getZ();
                                        int result = left + right;
                                        setZ(result);
                                        setFZ(false);
                                        setFN(false);
                                        setFH((left & 0xF) + (right & 0xF) > 0xF);
                                        setFC(result > 0xFF);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        // TODO: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
                                    }

                                }
                            }
                            case 6 -> { // LD A, (0xFF00 + n)
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setZ(systemBus.getBus().readByte(0xFF00 | getZ()));
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        setA(getZ());
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 7 -> { // LD HL, SP + d
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        int spLow = (getSP() & 0xFF);
                                        int result = spLow + getZ();
                                        setL(result);
                                        setFZ(false);
                                        setFN(false);
                                        setFH((spLow & 0xF) + (getZ() & 0xF) > 0xF);
                                        setFC(result > 0xFF);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        int adj = Processor.getBit(7, getZ()) != 0 ? 0xFF : 0x00;
                                        int result = ((getSP() & 0xFF00) >>> 8) + adj + (getFC() ? 1 : 0);
                                        setH(result);
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                        }
                    }
                    case 1 -> {
                        switch (q) {
                            case 0 -> { // POP rp2[p]
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getSP()));
                                        setSP(getSP() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setW(systemBus.getBus().readByte(getSP()));
                                        setSP(getSP() + 1);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        setRP2(p, getWZ());
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 1 -> {
                                switch (p) {
                                    case 0 -> {} // RET
                                    case 1 -> {} // RETI
                                    case 2 -> {} // JP HL
                                    case 3 -> { // LD SP, HL
                                        switch (machineCycleIndex) {
                                            case 0 -> {
                                                setSP(getHL());
                                                machineCycleIndex = 1;
                                            }
                                            case 1 -> {
                                                machineCycleIndex = TERMINATE_INSTRUCTION;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case 2 -> {
                        switch (y) {
                            case 0, 1, 2, 3 -> {} // JP cc[y], nn
                            case 4 -> { // LD (0xFF00 + C), A
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        systemBus.getBus().writeByte(0xFF00 | getC(), getA());
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 5 -> { // LD (nn), A
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setW(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        systemBus.getBus().writeByte(getWZ(), getA());
                                        machineCycleIndex = 3;
                                    }
                                    case 3 -> {
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 6 -> { // LD A, (0xFF00 + C)
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(0xFF00 | getC()));
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setA(getZ());
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 7 -> { // LD A, (nn)
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setW(systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        setZ(systemBus.getBus().readByte(getWZ()));
                                        machineCycleIndex = 3;
                                    }
                                    case 3 -> {
                                        setA(getZ());
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                        }
                    }
                    case 3 -> {
                        switch (y) {
                            case 0 -> {} // JP nn
                            case 6 -> {} // DI
                            case 7 -> {} // EI
                        }
                    }
                    case 4 -> {
                        switch (y) {
                            case 0, 1, 2, 3 -> {} // CALL cc[y], nn
                        }
                    }
                    case 5 -> {
                        switch (q) {
                            case 0 -> { // PUSH rp2[p]
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setSP(getSP() - 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        systemBus.getBus().writeByte(getSP(), (getRP2(p) & 0xFF00) >>> 8);
                                        setSP(getSP() - 1);
                                        machineCycleIndex = 2;
                                    }
                                    case 2 -> {
                                        systemBus.getBus().writeByte(getSP(), getRP2(p) & 0xFF);
                                        machineCycleIndex = 3;
                                    }
                                    case 3 -> {
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 1 -> {
                                if (p == 0) { // CALL nn

                                }
                            }
                        }
                    }
                    case 6 -> { // alu[y] n
                        switch (y) {
                            case 0 -> { // ADD A, n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        add8(getA(), getZ());
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 1 -> { // ADC A, n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setA(adc8(getA(), getZ()));
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 2 -> { // SUB A, n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setA(sub8(getA(), getZ()));
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 3 -> { // SBC A, n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setA(sbc8(getA(), getZ()));
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 4 -> { // AND n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setA(and8(getA(), getZ()));
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 5 -> { // XOR n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setA(xor8(getA(), getZ()));
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 6 -> { // OR n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        setA(or8(getA(), getZ()));
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                            case 7 -> { // // CP n
                                switch (machineCycleIndex) {
                                    case 0 -> {
                                        setZ(this.systemBus.getBus().readByte(getPC()));
                                        setPC(getPC() + 1);
                                        machineCycleIndex = 1;
                                    }
                                    case 1 -> {
                                        sub8(getA(), getZ());
                                        machineCycleIndex = TERMINATE_INSTRUCTION;
                                    }
                                }
                            }
                        }
                    }
                    case 7 -> {} // RST y*8
                }
            }
        }

    }

    private void executePrefixed() {
        int x = getX(getIR());
        int y = getY(getIR());
        switch (x) {
            case 0 -> {
                switch (y) { // rot[y] r[z]
                    case 0 -> {} // RLC r[z]
                    case 1 -> {} // RRC r[z]
                    case 2 -> {} // RL r[z]
                    case 3 -> {} // RR r[z]
                    case 4 -> {} // SLA r[z]
                    case 5 -> {} // SRA r[z]
                    case 6 -> {} // SWAP r[z]
                    case 7 -> {} // SRL
                }
            }
            case 1 -> {} // BIT y, r[z]
            case 2 -> {} // RES y, r[z]
            case 3 -> {} // SET y, r[z]
        }
    }

    private boolean getCondition(int index) {
        return switch (index) {
            case 0 -> !getFZ();
            case 1 -> getFZ();
            case 2 -> !getFC();
            case 3 -> getFC();
            default -> throw new EmulatorException("Illegal condition index " + index + " for SM83 core!");
        };
    }

    private void setR(int index, int value) {
        switch (index) {
            case 0 -> setB(value);
            case 1 -> setC(value);
            case 2 -> setD(value);
            case 3 -> setE(value);
            case 4 -> setH(value);
            case 5 -> setL(value);
            case 6 -> throw new EmulatorException("Index 6 for \"r\" must be handled separately!");
            case 7 -> setA(value);
            default -> throw new EmulatorException("Illegal index " + index + " for \"r\" table!");
        }
    }

    private int getR(int index) {
        return switch (index) {
            case 0 -> getB();
            case 1 -> getC();
            case 2 -> getD();
            case 3 -> getE();
            case 4 -> getH();
            case 5 -> getL();
            case 6 -> throw new EmulatorException("Index 6 for \"r\" must be handled separately!");
            case 7 -> getA();
            default -> throw new EmulatorException("Illegal index " + index + " for \"r\" table!");
        };
    }

    private void setRP(int index, int value) {
        switch (index) {
            case 0 -> setBC(value);
            case 1 -> setDE(value);
            case 2 -> setHL(value);
            case 3 -> setSP(value);
            default -> throw new EmulatorException("Illegal index " + index + " for \"rp\" table!");
        }
    }

    private int getRP(int index) {
        return switch (index) {
            case 0 -> getBC();
            case 1 -> getDE();
            case 2 -> getHL();
            case 3 -> getSP();
            default -> throw new EmulatorException("Illegal index " + index + " for \"rp\" table!");
        };
    }

    private void setRP2(int index, int value) {
        switch (index) {
            case 0 -> setBC(value);
            case 1 -> setDE(value);
            case 2 -> setHL(value);
            case 3 -> setAF(value);
            default -> throw new EmulatorException("Illegal index " + index + " for \"rp2\" table!");
        }
    }

    private int getRP2(int index) {
        return switch (index) {
            case 0 -> getBC();
            case 1 -> getDE();
            case 2 -> getHL();
            case 3 -> getAF();
            default -> throw new EmulatorException("Illegal index " + index + " for \"rp2\" table!");
        };
    }

    private int add8(int left, int right) {
        int result = left + right;
        setFZ((result & 0xFF) == 0);
        setFN(false);
        setFH((left & 0xF) + (right & 0xF) > 0xF);
        setFC(result > 0xFF);
        return result;
    }

    private int adc8(int left, int right) {
        int result = left + right + (getFC() ? 1 : 0);
        setFZ((result & 0xFF) == 0);
        setFN(false);
        setFH((left & 0xF) + (right & 0xF) + (getFC() ? 1 : 0) > 0xF);
        setFC(result > 0xFF);
        return result;
    }

    private int sub8(int left, int right) {
        int result = left - right;
        setFZ((result & 0xFF) == 0);
        setFN(true);
        setFH((left & 0xF) < (right & 0xF));
        setFC(left < right);
        return result;
    }


    private int sbc8(int left, int right) {
        int result = left - right - (getFC() ? 1 : 0);
        setFZ((result & 0xFF) == 0);
        setFN(true);
        setFH((left & 0xF) < (((right & 0xF) + ((getFC() ? 1 : 0) & 0xF)) & 0xFF));
        setFC(left < ((right + (getFC() ? 1 : 0)) & 0xFF));
        return result;
    }

    private int and8(int left, int right) {
        int result = left & right;
        setFZ((result & 0xFF) == 0);
        setFN(false);
        setFH(true);
        setFC(false);
        return result;
    }

    private int or8(int left, int right) {
        int result = left | right;
        setFZ((result & 0xFF) == 0);
        setFN(false);
        setFH(false);
        setFC(false);
        return result;
    }

    private int xor8(int left, int right) {
        int result = left ^ right;
        setFZ((result & 0xFF) == 0);
        setFN(false);
        setFH(false);
        setFC(false);
        return result;
    }

    private static int getX(int opcode) {
        return (opcode & 0b11000000) >>> 6;
    }

    private static int getY(int opcode) {
        return (opcode & 0b00111000) >>> 3;
    }

    private static int getZ(int opcode) {
        return opcode & 0b00000111;
    }

    private static int getP(int opcode) {
        return (opcode & 0b00110000) >>> 4;
    }

    private static int getQ(int opcode) {
        return (opcode & 0b00001000) >>> 3;
    }


}
