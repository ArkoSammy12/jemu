package io.github.arkosammy12.jemu.core.util;

public final class StaticIOPort8Bit {

    private final IOBitSource ioBitSource;

    private int dataDirectionRegister;
    private int outputLatch;

    public StaticIOPort8Bit(IOBitSource ioBitSource) {
        this.ioBitSource = ioBitSource;
    }

    public int read() {
        int ret = this.readBit(7) ? 1 << 7 : 0;
        ret |= this.readBit(6) ? 1 << 6 : 0;
        ret |= this.readBit(5) ? 1 << 5 : 0;
        ret |= this.readBit(4) ? 1 << 4 : 0;
        ret |= this.readBit(3) ? 1 << 3 : 0;
        ret |= this.readBit(2) ? 1 << 2 : 0;
        ret |= this.readBit(1) ? 1 << 1 : 0;
        ret |= this.readBit(0) ? 1 : 0;
        return ret;
    }

    public int getDataDirectionRegister() {
        return this.dataDirectionRegister;
    }

    public boolean readBit(int index) {
        index &= 0b111;
        int mask = 1 << index;
        return (this.dataDirectionRegister & mask) != 0 ? (this.outputLatch & mask) != 0 : this.ioBitSource.getBit(index);
    }

    public void writeDataDirectionRegister(int value) {
        this.dataDirectionRegister = value & 0xFF;
    }

    public void writeOutputLatch(int value) {
        this.outputLatch = value & 0xFF;
    }

    public interface IOBitSource {

        boolean getBit(int index);

    }

}
