package io.github.arkosammy12.jemu.backend.common;

public interface Processor {

    int cycle();

    static int setBit(int value, int mask) {
        return value | mask;
    }

    static int clearBit(int value, int mask) {
        return value & ~mask;
    }

    static boolean testBit(int flags, int mask) {
        return (flags & mask) != 0;
    }

    static int getBit(int index, int value) {
        return (value >>> index) & 1;
    }

}
