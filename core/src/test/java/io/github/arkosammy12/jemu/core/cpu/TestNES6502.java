package io.github.arkosammy12.jemu.core.cpu;

import io.github.arkosammy12.jemu.core.ssts.nes6502.NES6502TestCase;
import io.github.arkosammy12.jemu.core.ssts.nes6502.NES6502TestState;

public class TestNES6502 extends NMOS6502 {

    public TestNES6502(SystemBus systemBus) {
        super(systemBus, true);
    }

    public void acceptTestCase(NES6502TestCase testCase) {

        NES6502TestState initialState = testCase.getInitialState();

        this.setPC(initialState.getPC());
        this.setA(initialState.getA());
        this.setSP(initialState.getSP());
        this.setX(initialState.getX());
        this.setY(initialState.getY());
        this.setP(initialState.getP());

    }

}
