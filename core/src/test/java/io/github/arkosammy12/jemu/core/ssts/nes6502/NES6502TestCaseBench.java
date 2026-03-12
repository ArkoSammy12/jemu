package io.github.arkosammy12.jemu.core.ssts.nes6502;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;
import io.github.arkosammy12.jemu.core.cpu.TestNES6502;
import io.github.arkosammy12.jemu.core.util.FlatTestBus;
import org.tinylog.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NES6502TestCaseBench implements NMOS6502.SystemBus {

    private final NES6502TestCase testCase;
    private final TestNES6502 cpu;
    private final FlatTestBus bus;

    public NES6502TestCaseBench(NES6502TestCase testCase) {
        this.testCase = testCase;
        this.cpu = new TestNES6502(this);
        this.cpu.acceptTestCase(testCase);
        this.bus = new FlatTestBus(0xFFFF + 1);
        List<List<Integer>> ram = testCase.getInitialState().getRam();
        for (List<Integer> ramElement : ram) {
            this.bus.writeByte(ramElement.get(0), ramElement.get(1));
        }
    }

    public void runTest() {
        List<List<Object>> cycles = this.testCase.getCycles();
        Logger.info("Running test case: " + this.testCase.getName());

        this.cpu.cycle();
        this.cpu.cycle();

        for (List<Object> cycle : cycles) {
            this.cpu.cycle();
            this.cpu.cycle();
            // TODO: Test bus values
        }

        NES6502TestState finalState = this.testCase.getFinalState();

        assertEquals(finalState.getPC(), this.cpu.getPC());
        assertEquals(finalState.getSP(), this.cpu.getSP());
        assertEquals(finalState.getA(), this.cpu.getA());
        assertEquals(finalState.getX(), this.cpu.getX());
        assertEquals(finalState.getY(), this.cpu.getY());
        assertEquals(finalState.getP(), this.cpu.getP());

        List<List<Integer>> finalRam = finalState.getRam();
        for (List<Integer> ramElement : finalRam) {
            int address = ramElement.get(0);
            int value = ramElement.get(1);
            assertEquals(value, this.bus.readByte(address), "Address: $%04X (%d)".formatted(address, address));
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
    public boolean getRes() {
        return false;
    }

    @Override
    public boolean getRdy() {
        return false;
    }

    @Override
    public Bus getBus() {
        return this.bus;
    }
}
