package io.github.arkosammy12.jemu.cpu;

import io.github.arkosammy12.jemu.ssts.sm83.SM83TestCase;
import io.github.arkosammy12.jemu.ssts.sm83.SM83TestState;
import io.github.arkosammy12.jemu.systems.SystemBus;
import io.github.arkosammy12.jemu.systems.cpu.SM83;

public class SingleStepSM83 extends SM83 {

    public SingleStepSM83(SystemBus systemBus, SM83TestCase testCase) {
        super(systemBus);

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
        this.setEI(initialState.getIE() != 0);
    }


}
