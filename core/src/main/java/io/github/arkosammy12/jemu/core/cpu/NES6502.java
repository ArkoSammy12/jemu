package io.github.arkosammy12.jemu.core.cpu;

public class NES6502 extends NMOS6502 {

    public NES6502(SystemBus systemBus) {
        super(systemBus);
    }

    public boolean isHalted() {
        return this.isHalted;
    }

    @Override
    protected void adc() {
        int carry = getFC() ? 1 : 0;
        int val = getA() + getOperand() + carry;
        setFV((((val ^ getA()) & (val ^ getOperand())) & 0x80) != 0);
        setA(val);
        setFC(val > 0xFF);
        setFN((getA() & 0x80) != 0);
        setFZ(getA() == 0);
    }

    @Override
    protected void sbc() {
        int carry = getFC() ? 0 : 1;
        int val = getA() - getOperand() - carry;
        setFV(((getA() & 0x80) != (getOperand() & 0x80)) && ((getA() & 0x80) != (val & 0x80)));
        setA(val);
        setFC(val >= 0);
        setFN((getA() & 0x80) != 0);
        setFZ(getA() == 0);
    }

    @Override
    protected void arr() {
        setA(getA() & getOperand());
        int temp = getFC() ? 0x80 : 0x00;
        setFC((getA() & 0x80) != 0);
        setA((getA() >>> 1) | temp);
        setFN((getA() & 0x80) != 0);
        setFZ(getA() == 0);
        setFV(((getFC() ? 1 : 0) ^ ((getA() >>> 5) & 1)) != 0);
    }

}
