package io.github.arkosammy12.jemu.core.cpu;

public class NES6502 extends NMOS6502 {

    public NES6502(SystemBus systemBus) {
        super(systemBus);
    }

    public boolean isHalted() {
        return this.isHalted;
    }

    @Override
    protected void addOrSubCarry(int operand) {
        int a = getA();
        int c = getFC() ? 1 : 0;

        int binarySum = a + operand + c;
        setFV(((~(a ^ operand)) & (a ^ binarySum) & 0x80) != 0);

        int result = binarySum;
        setFC(binarySum > 0xFF);

        result &= 0xFF;
        setA(result);
        setFZ(result == 0);
        setFN((result & 0x80) != 0);
    }

}
