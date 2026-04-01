package io.github.arkosammy12.jemu.core.cpu;

public class NES6502 extends NMOS6502 {

    public NES6502(SystemBus systemBus) {
        super(systemBus);

        // Trigger the initial resetting of the CPU
        this.signalReset = true;
    }

    @Override
    protected boolean getFD() {
        return false;
    }

}
