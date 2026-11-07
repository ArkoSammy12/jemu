package io.github.arkosammy12.jemu.core.test.util;

import io.github.arkosammy12.jemu.core.common.Bus;
import it.unimi.dsi.fastutil.ints.Int2ByteArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;

public class FlatTestBus implements Bus {

    private int lastAddress;
    private int lastValue;

    private final Int2ByteMap ram = new Int2ByteArrayMap();

    @Override
    public void writeByte(int address, int value) {
        this.ram.put(address, (byte) value);
        this.lastAddress = address;
        this.lastValue = value;
    }

    @Override
    public int readByte(int address) {
        int ret = (int) this.ram.computeIfAbsent(address, _ -> (byte) 0) & 0xFF;
        this.lastAddress = address;
        this.lastValue = ret;
        return ret;
    }

    public int getLastAddress() {
        return this.lastAddress;
    }

    public int getLastValue() {
        return this.lastValue;
    }

}
