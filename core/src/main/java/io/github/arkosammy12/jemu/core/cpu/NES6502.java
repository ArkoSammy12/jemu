package io.github.arkosammy12.jemu.core.cpu;

public class NES6502 extends NMOS6502 {

    public NES6502(SystemBus systemBus) {
        super(systemBus);

        // Trigger the initial resetting of the CPU
        this.signalReset = true;
    }

    public boolean isHalted() {
        return this.cpuHalted;
    }

    protected void addOrSubCarry(boolean subtract) {
        int a = getA();
        int m = subtract ? getOperand() ^ 0xFF : getOperand();
        int c = getFC() ? 1 : 0;

        int binarySum = a + m + c;
        setFV(((~(a ^ m)) & (a ^ binarySum) & 0x80) != 0);

        int result = binarySum;
        setFC(binarySum > 0xFF);

        result &= 0xFF;
        setA(result);
        setFZ(result == 0);
        setFN((result & 0x80) != 0);
    }

}
