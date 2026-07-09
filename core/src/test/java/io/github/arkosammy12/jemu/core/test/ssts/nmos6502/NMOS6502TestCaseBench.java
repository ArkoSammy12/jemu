package io.github.arkosammy12.jemu.core.test.ssts.nmos6502;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;
import io.github.arkosammy12.jemu.core.test.cpu.TestNES6502;
import io.github.arkosammy12.jemu.core.test.cpu.TestNMOS6502;
import io.github.arkosammy12.jemu.core.test.util.FlatTestBus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NMOS6502TestCaseBench implements NMOS6502.SystemBus {

    private final NMOS6502TestCase testCase;
    private final TestNMOS6502 cpu;
    private final FlatTestBus bus;

    public NMOS6502TestCaseBench(NMOS6502TestCase testCase) {
        this.testCase = testCase;
        this.cpu = new TestNMOS6502(this);
        this.cpu.acceptTestCase(testCase);
        this.bus = new FlatTestBus();
        List<List<Integer>> ram = testCase.getInitialState().getRam();
        for (List<Integer> ramElement : ram) {
            this.bus.writeByte(ramElement.get(0), ramElement.get(1));
        }
    }

    public void runTest() {
        List<List<Object>> cycles = this.testCase.getCycles();

        for (int i = 0; i < cycles.size(); i++) {
            List<Object> cycle = cycles.get(i);
            this.cpu.cycle();
            this.cpu.cycle();

            NMOS6502.ReadWriteCycle readWriteCycle = cycle.get(2).equals("read") ? NMOS6502.ReadWriteCycle.READ : NMOS6502.ReadWriteCycle.WRITE;
            assertEquals(((Number) cycle.get(0)).intValue(), this.bus.getLastAddress(), "Test name: %s. Field: Bus access address. Cycle: %s".formatted(testCase.getName(), i));
            assertEquals(((Number) cycle.get(1)).intValue(), this.bus.getLastValue(), "Test name: %s. Field: Bus access value. Cycle: %s".formatted(testCase.getName(), i));
            assertEquals(readWriteCycle, this.cpu.getReadWriteCycle(), "Test name: %s. Field: Bus access type. Cycle %s".formatted(testCase.getName(), i));

        }

        this.cpu.cycle();
        this.cpu.cycle();

        NMOS6502TestState finalState = this.testCase.getFinalState();

        assertEquals(finalState.getPC(), this.cpu.getPC(), () -> "Test name: %s. Field: PC".formatted(testCase.getName()));
        assertEquals(finalState.getSP(), this.cpu.getS(), () -> "Test name: %s. Field: SP".formatted(testCase.getName()));
        assertEquals(finalState.getA(), this.cpu.getA(), () -> "Test name: %s. Field: A".formatted(testCase.getName()));
        assertEquals(finalState.getX(), this.cpu.getX(), () -> "Test name: %s. Field: X".formatted(testCase.getName()));
        assertEquals(finalState.getY(), this.cpu.getY(), () -> "Test name: %s. Field: Y".formatted(testCase.getName()));
        assertEquals(finalState.getP(), this.cpu.getP(), () -> "Test name: %s. Field: P".formatted(testCase.getName()));

        List<List<Integer>> finalRam = finalState.getRam();
        for (List<Integer> ramElement : finalRam) {
            int address = ramElement.get(0);
            int value = ramElement.get(1);
            assertEquals(value, this.bus.readByte(address), "Test name: %s. Address: $%04X (%d)".formatted(testCase.getName(), address, address));
        }

    }

    @Override
    public boolean getIRQ() {
        return false;
    }

    @Override
    public boolean getNMI() {
        return false;
    }

    @Override
    public boolean getRES() {
        return false;
    }

    @Override
    public boolean getRDY() {
        return false;
    }

    @Override
    public Bus getBus() {
        return this.bus;
    }
}
