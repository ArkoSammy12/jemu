package io.github.arkosammy12.jemu.backend.disassembler;

import io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVipEmulator;
import io.github.arkosammy12.jemu.backend.common.BusView;

public class CosmacVipDisassembler<E extends CosmacVipEmulator> extends AbstractDisassembler<E> {

    public CosmacVipDisassembler(E emulator) {
        super(emulator);
    }

    @Override
    protected int getLengthForInstructionAt(int address) {
        BusView bus = this.emulator.getBusView();
        int opcode = bus.getByte(address);
        return switch (getI(opcode)) {
            case 0x3 -> switch (getN(opcode)) {
                case 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF -> 2;
                default -> 1;
            };
            case 0x7 -> switch (getN(opcode)) {
                case 0xC, 0xD, 0xF -> 2;
                default -> 1;
            };
            case 0xC -> switch (getN(opcode)) {
                case 0x0, 0x1, 0x2, 0x3, 0x9, 0xA, 0xB -> 3;
                default -> 1;
            };
            case 0xF -> switch (getN(opcode)) {
                case 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xF -> 2;
                default -> 1;
            };
            default -> 1;
        };
    }

    @Override
    protected int getBytecodeForInstructionAt(int address) {
        BusView bus = this.emulator.getBusView();
        int firstByte = bus.getByte(address);
        return switch (this.getLengthForInstructionAt(address)) {
            case 3 -> (firstByte << 16) | (bus.getByte(address + 1) << 8) | bus.getByte(address + 2);
            case 2 -> (firstByte << 8) | bus.getByte(address + 1);
            default -> firstByte;
        };
    }

    @Override
    protected String getTextForInstructionAt(int address) {
        BusView bus = this.emulator.getBusView();
        int opcode = bus.getByte(address);
        return switch (getI(opcode)) {
            case 0x0 -> {
                if (getN(opcode) == 0x0) {
                    yield "IDL";
                } else {
                    yield "LDN R" + getNFormatted(opcode);
                }
            }
            case 0x1 -> "INC R" + getNFormatted(opcode);
            case 0x2 -> "DEC R" + getNFormatted(opcode);
            case 0x3 -> switch (getN(opcode)) {
                case 0x0 -> "BR " + formatByte(bus.getByte(address + 1));
                case 0x1 -> "BQ " + formatByte(bus.getByte(address + 1));
                case 0x2 -> "BZ " + formatByte(bus.getByte(address + 1));
                case 0x3 -> "BDF " + formatByte(bus.getByte(address + 1));
                case 0x4 -> "B1 " + formatByte(bus.getByte(address + 1));
                case 0x5 -> "B2 " + formatByte(bus.getByte(address + 1));
                case 0x6 -> "B3 " + formatByte(bus.getByte(address + 1));
                case 0x7 -> "B4 " + formatByte(bus.getByte(address + 1));
                case 0x8 -> "SKP";
                case 0x9 -> "BNQ " + formatByte(bus.getByte(address + 1));
                case 0xA -> "BNZ " + formatByte(bus.getByte(address + 1));
                case 0xB -> "BNF " + formatByte(bus.getByte(address + 1));
                case 0xC -> "BN1 " + formatByte(bus.getByte(address + 1));
                case 0xD -> "BN2 " + formatByte(bus.getByte(address + 1));
                case 0xE -> "BN3 " + formatByte(bus.getByte(address + 1));
                case 0xF -> "BN4 " + formatByte(bus.getByte(address + 1));
                default -> "invalid";
            };
            case 0x4 -> "LDA R" + getNFormatted(opcode);
            case 0x5 -> "STR R" + getNFormatted(opcode);
            case 0x6 -> {
                int N = getN(opcode);
                if (N == 0x0) {
                    yield "IRX";
                } else if (N >= 0x1 && N <= 0x7) {
                    yield "OUT " + Integer.toHexString(N & 7).toUpperCase();
                } else if (N >= 0x9 && N <= 0xF) {
                    yield "INP " + Integer.toHexString(N & 7).toUpperCase();
                } else {
                    yield "invalid";
                }
            }
            case 0x7 -> switch (getN(opcode)) {
                case 0x0 -> "RET";
                case 0x1 -> "DIS";
                case 0x2 -> "LDXA";
                case 0x3 -> "STXD";
                case 0x4 -> "ADC";
                case 0x5 -> "SDB";
                case 0x6 -> "SHRC";
                case 0x7 -> "SMB";
                case 0x8 -> "SAV";
                case 0x9 -> "MARK";
                case 0xA -> "REQ";
                case 0xB -> "SEQ";
                case 0xC -> "ADCI " + formatByte(bus.getByte(address + 1));
                case 0xD -> "SDBI " + formatByte(bus.getByte(address + 1));
                case 0xE -> "SHLC";
                case 0xF -> "SMBI " + formatByte(bus.getByte(address + 1));
                default -> "invalid";
            };
            case 0x8 -> "GLO R" + getNFormatted(opcode);
            case 0x9 -> "GHI R" + getNFormatted(opcode);
            case 0xA -> "PLO R" + getNFormatted(opcode);
            case 0xB -> "PHI R" + getNFormatted(opcode);
            case 0xC -> switch (getN(opcode)) {
                case 0x0 -> "LBR " + format2Bytes(bus.getByte(address + 1), bus.getByte(address + 2));
                case 0x1 -> "LBQ " + format2Bytes(bus.getByte(address + 1), bus.getByte(address + 2));
                case 0x2 -> "LBZ " + format2Bytes(bus.getByte(address + 1), bus.getByte(address + 2));
                case 0x3 -> "LBDF " + format2Bytes(bus.getByte(address + 1), bus.getByte(address + 2));
                case 0x4 -> "NOP";
                case 0x5 -> "LSNQ";
                case 0x6 -> "LSNZ";
                case 0x7 -> "LSNF";
                case 0x8 -> "LSKP";
                case 0x9 -> "LBNQ " + format2Bytes(bus.getByte(address + 1), bus.getByte(address + 2));
                case 0xA -> "LBNZ " + format2Bytes(bus.getByte(address + 1), bus.getByte(address + 2));
                case 0xB -> "LBNF " + format2Bytes(bus.getByte(address + 1), bus.getByte(address + 2));
                case 0xC -> "LSIE";
                case 0xD -> "LSQ";
                case 0xE -> "LSZ";
                case 0xF -> "LSDF";
                default -> "invalid";
            };
            case 0xD -> "SEP R" + getNFormatted(opcode);
            case 0xE -> "SEX R" + getNFormatted(opcode);
            case 0xF -> switch (getN(opcode)) {
                case 0x0 -> "LDX";
                case 0x1 -> "OR";
                case 0x2 -> "AND";
                case 0x3 -> "XOR";
                case 0x4 -> "ADD";
                case 0x5 -> "SD";
                case 0x6 -> "SHR";
                case 0x7 -> "SM";
                case 0x8 -> "LDI " + formatByte(bus.getByte(address + 1));
                case 0x9 -> "ORI " + formatByte(bus.getByte(address + 1));
                case 0xA -> "ANI " + formatByte(bus.getByte(address + 1));
                case 0xB -> "XRI " + formatByte(bus.getByte(address + 1));
                case 0xC -> "ADI " + formatByte(bus.getByte(address + 1));
                case 0xD -> "SDI " + formatByte(bus.getByte(address + 1));
                case 0xE -> "SHL";
                case 0xF -> "SMI " + formatByte(bus.getByte(address + 1));
                default -> "invalid";
            };
            default -> "invalid";
        };
    }

    private static String format2Bytes(int firstByte, int secondByte) {
        return String.format("0x%04X", (firstByte << 8) | secondByte);
    }

    private static String formatByte(int val) {
        return String.format("0x%02X", val);
    }

    private static String getNFormatted(int opcode) {
        return Integer.toHexString(getN(opcode)).toUpperCase();
    }

    private static int getI(int opcode) {
        return (opcode & 0xF0) >>> 4;
    }

    private static int getN(int opcode) {
        return opcode & 0x0F;
    }

}
