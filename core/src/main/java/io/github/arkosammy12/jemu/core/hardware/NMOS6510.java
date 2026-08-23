package io.github.arkosammy12.jemu.core.hardware;

import io.github.arkosammy12.jemu.core.util.MOSIOPort;

public class NMOS6510<S extends NMOS6510.SystemBus> extends NMOS6502<S> implements MOSIOPort.PortOwner {

    private int dataDirectionRegister;
    private int outputLatch;

    public NMOS6510(S systemBus) {
        super(systemBus);
    }

    @Override
    public int getDataDirectionRegister() {
        return this.dataDirectionRegister;
    }

    @Override
    public int getOutputLatch() {
        return this.outputLatch;
    }

    // TODO: Different XAA magic (0xEF). CPU IO port decays. For RDY dependent unstable instructions (SHA and friends), highbyte + 1 masking only happens in the last dummy read

    @Override
    protected int readByte(int address) {
        this.readWriteCycle = ReadWriteCycle.READ;
        this.lastAddress = address;
        if (systemBus.getAEC()) {
            return 0x00; // some undefined value :v. The CPU will repeat its read cycle after it comes out of RDY anyways
        } else {
            return switch (address) {
                case 0x0000 -> this.outputLatch;
                case 0x0001 -> systemBus.getIOPort().read();
                default -> systemBus.getBus().readByte(address);
            };
        }
    }

    @Override
    protected void writeByte(int address, int value) {
        this.readWriteCycle = ReadWriteCycle.WRITE;
        this.lastAddress = address;
        if (!systemBus.getAEC()) {
            switch (address) {
                case 0x0000 -> this.dataDirectionRegister = value & 0xFF;
                case 0x0001 -> this.outputLatch = value & 0xFF;
                default -> systemBus.getBus().writeByte(address, value);
            }
        }
    }

    public interface SystemBus extends NMOS6502.SystemBus {

        boolean getAEC();

        MOSIOPort getIOPort();

    }

}
