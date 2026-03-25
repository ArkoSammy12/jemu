package io.github.arkosammy12.jemu.core.test.cpu;

import io.github.arkosammy12.jemu.core.cpu.SM83;
import io.github.arkosammy12.jemu.core.test.ssts.sm83.SM83TestCase;
import io.github.arkosammy12.jemu.core.test.ssts.sm83.SM83TestState;

public class TestSM83 extends SM83 {

    public TestSM83(SystemBus systemBus) {
        super(systemBus);
    }

    public void acceptTestCase(SM83TestCase testCase) {
        SM83TestState initialState = testCase.getInitialState();

        this.setPC(initialState.getPC());
        this.setSP(initialState.getSP());

        this.setAF((initialState.getA() << 8) | initialState.getF());
        this.setB(initialState.getB());
        this.setC(initialState.getC());
        this.setD(initialState.getD());
        this.setE(initialState.getE());
        this.setH(initialState.getH());
        this.setL(initialState.getL());

        this.setIME(initialState.getIME() != 0);
        //his.setEI(initialState.getIE() != 0);
    }

}
