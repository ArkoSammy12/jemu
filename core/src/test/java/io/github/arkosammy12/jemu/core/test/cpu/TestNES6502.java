package io.github.arkosammy12.jemu.core.test.cpu;

import io.github.arkosammy12.jemu.core.cpu.NES6502;
import io.github.arkosammy12.jemu.core.test.ssts.nes6502.NES6502TestCase;
import io.github.arkosammy12.jemu.core.test.ssts.nes6502.NES6502TestState;

public class TestNES6502 extends NES6502 {

    public TestNES6502(SystemBus systemBus) {
        super(systemBus);
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
