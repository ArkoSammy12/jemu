package io.github.arkosammy12.jemu.core.hardware;

import io.github.arkosammy12.jemu.core.util.MOSIOPort;

public class NMOS6510<S extends NMOS6510.SystemBus> extends NMOS6502<S> implements MOSIOPort.PortOwner {

    private int dataDirectionRegister;
    private int outputLatch;
    private int unusedBitsLastOutput;

    public NMOS6510(S systemBus) {
        super(systemBus);
    }

    @Override
    protected int getANEMagic() {
        return 0xEF;
    }

    @Override
    public int getDataDirectionRegister() {
        return this.dataDirectionRegister;
    }

    @Override
    public int getOutputLatch() {
        return this.outputLatch;
    }

    // TODO: CPU IO port decays. For RDY dependent unstable instructions (SHA and friends), highbyte + 1 masking only happens in the last dummy read

    @Override
    protected void execute8X(int digit) {
        if (digit == 0xB) {
            switch (subCycleIndex) {
                case 0 -> {
                    setRDYFlag(systemBus.getRDY());
                    super.execute8X(digit);
                }
                case 2 -> {
                    setRDYFlag(systemBus.getRDY());

                    setPC(getPC() + 1);
                    int result = ((getA() | (getRDYFlag() ? 0xEE : 0xEF)) & getX() & getOperand()) & 0xFF;
                    setA(result);
                    setFN((result & 0x80) != 0);
                    setFZ(result == 0);

                    onFetchCyclePHI1();
                    subCycleIndex = 3;
                }
                default -> super.execute8X(digit);
            }
        } else {
            super.execute8X(digit);
        }
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
                case 0x0001 -> {
                    int ioPort = systemBus.getIOPort().read() & 0x3F;
                    yield (ioPort & 0x37)
                            | (this.systemBus.isDatasetteConnected() ? ioPort & (1 << 3) : ((this.dataDirectionRegister & (1 << 3)) != 0 ? this.outputLatch & (1 << 3) : this.unusedBitsLastOutput & (1 << 3)))
                            | ((this.dataDirectionRegister & (1 << 6)) != 0 ? this.outputLatch & (1 << 6) : this.unusedBitsLastOutput & (1 << 6))
                            | ((this.dataDirectionRegister & (1 << 7)) != 0 ? this.outputLatch & (1 << 7) : this.unusedBitsLastOutput & (1 << 7));
                }
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
                case 0x0000 -> {
                    this.dataDirectionRegister = value & 0xFF;
                    this.unusedBitsLastOutput =
                                      (this.systemBus.isDatasetteConnected() || (this.dataDirectionRegister & (1 << 3)) != 0 ? this.outputLatch & (1 << 3) : this.unusedBitsLastOutput & (1 << 3))
                                    | ((this.dataDirectionRegister & (1 << 6)) != 0 ? this.outputLatch & (1 << 6) : this.unusedBitsLastOutput & (1 << 6))
                                    | ((this.dataDirectionRegister & (1 << 7)) != 0 ? this.outputLatch & (1 << 7) : this.unusedBitsLastOutput & (1 << 7));
                }
                case 0x0001 -> {
                    this.outputLatch = value & 0xFF;
                    this.unusedBitsLastOutput =
                                      (this.systemBus.isDatasetteConnected() || (this.dataDirectionRegister & (1 << 3)) != 0 ? value & (1 << 3) : this.unusedBitsLastOutput & (1 << 3))
                                    | ((this.dataDirectionRegister & (1 << 6)) != 0 ? value & (1 << 6) : this.unusedBitsLastOutput & (1 << 6))
                                    | ((this.dataDirectionRegister & (1 << 7)) != 0 ? value & (1 << 7) : this.unusedBitsLastOutput & (1 << 7));
                }
                default -> systemBus.getBus().writeByte(address, value);
            }
        }
    }

    public interface SystemBus extends NMOS6502.SystemBus {

        boolean getAEC();

        MOSIOPort getIOPort();

        boolean isDatasetteConnected();

    }

}
