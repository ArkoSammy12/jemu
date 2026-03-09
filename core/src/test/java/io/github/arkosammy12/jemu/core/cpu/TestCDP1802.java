package io.github.arkosammy12.jemu.core.cpu;

import io.github.arkosammy12.jemu.core.ssts.cdp1802.CDP1802TestCase;
import io.github.arkosammy12.jemu.core.ssts.cdp1802.CDP1802TestState;

public class TestCDP1802 extends CDP1802 {

    public TestCDP1802(SystemBus systemBus) {
        super(systemBus);
    }

    public void acceptTestCase(CDP1802TestCase testCase) {
        CDP1802TestState initialState = testCase.getInitialState();

        this.setP(initialState.getP());
        this.setX(initialState.getX());
        this.setN(initialState.getN());
        this.setI(initialState.getI());
        this.setT(initialState.getT());
        this.setD(initialState.getD());
        this.setDF(initialState.getDF() != 0);
        this.setIE(initialState.getIE() != 0);
        this.setQ(initialState.getQ() != 0);

        for (int i = 0; i < 16; i++) {
            this.setR(i, initialState.getR(i));
        }

        for (int i = 0; i < 4; i++) {
            this.setEF(i, true);
        }

    }

}
