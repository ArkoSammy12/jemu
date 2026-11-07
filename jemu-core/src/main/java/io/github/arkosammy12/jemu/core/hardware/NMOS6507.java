package io.github.arkosammy12.jemu.core.hardware;

import io.github.arkosammy12.jemu.core.common.Bus;

public class NMOS6507 extends NMOS6502<NMOS6502.SystemBus> {

    public NMOS6507(SystemBus systemBus) {
        super(new SystemBus() {

            @Override
            public boolean getIRQ() {
                return false;
            }

            @Override
            public boolean getNMI() {
                return false;
            }

            @Override
            public boolean getRES() {
                return systemBus.getRES();
            }

            @Override
            public boolean getRDY() {
                return systemBus.getRDY();
            }

            @Override
            public Bus getBus() {
                return systemBus.getBus();
            }

        });
    }

    @Override
    protected int readByte(int address) {
        return super.readByte(address & 0x1FFF);
    }

    @Override
    protected void writeByte(int address, int value) {
        super.writeByte(address & 0x1FFF, value);
    }

}
