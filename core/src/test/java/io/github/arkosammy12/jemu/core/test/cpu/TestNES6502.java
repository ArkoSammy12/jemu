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
        this.setS(initialState.getSP());
        this.setX(initialState.getX());
        this.setY(initialState.getY());
        this.setP(initialState.getP());

    }

    @Override
    protected void executeAX(int digit) {
        switch (digit) {
            case 0xB -> { // LXA, immediate
                switch (subCycleIndex) {
                    case 0 -> {
                        setPC(getPC() + 1);
                        subCycleIndex = 1;
                    }
                    case 1 -> {
                        setOperand(systemBus.getBus().readByte(getPC()));
                        subCycleIndex = 2;
                    }
                    case 2 -> {
                        setPC(getPC() + 1);
                        int value = ((getA() | 0xEE) & getOperand()) & 0xFF;
                        setA(value);
                        setX(value);
                        setFN((value & 0x80) != 0);
                        setFZ(value == 0);
                        subCycleIndex = 3;
                    }
                    case 3 -> {
                        subCycleIndex = TERMINATE_INSTRUCTION;
                    }
                }
            }
            default -> super.executeAX(digit);
        }
    }

}
