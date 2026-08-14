package io.github.arkosammy12.jemu.core.hardware;

public class NMOS6510<S extends NMOS6510.SystemBus> extends NMOS6502<S> {

    private int dataDirectionRegister;
    private int outputLatch;

    public NMOS6510(S systemBus) {
        super(systemBus);
    }

    @Override
    protected int readByte(int address) {
        this.readWriteCycle = ReadWriteCycle.READ;
        this.lastAddress = address;
        if (systemBus.getAEC()) {
            return 0x00; // some undefined value :v. The CPU will repeat its read cycle after it comes out of RDY anyways
        } else {
            return switch (address) {
                case 0x0000 -> this.dataDirectionRegister;
                case 0x0001 -> ((this.outputLatch & this.dataDirectionRegister) | (this.systemBus.readIO(this.dataDirectionRegister) & ~this.dataDirectionRegister)) & 0xFF;
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
                case 0x0001 -> {
                    this.outputLatch = value & 0xFF;
                    systemBus.writeIO(this.outputLatch & this.dataDirectionRegister, this.dataDirectionRegister);
                }
                default -> systemBus.getBus().writeByte(address, value);
            }
        }
    }

    public interface SystemBus extends NMOS6502.SystemBus {

        boolean getAEC();

        int readIO(int ddr);

        void writeIO(int value, int ddr);

    }

}
