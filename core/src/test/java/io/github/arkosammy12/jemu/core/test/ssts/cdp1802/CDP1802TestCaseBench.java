package io.github.arkosammy12.jemu.core.test.ssts.cdp1802;

import io.github.arkosammy12.jemu.core.test.cpu.TestCDP1802;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.cpu.CDP1802;
import io.github.arkosammy12.jemu.core.cosmacvip.IODevice;
import io.github.arkosammy12.jemu.core.test.util.FlatTestBus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CDP1802TestCaseBench implements CDP1802.SystemBus {

    private final CDP1802TestCase testCase;
    private final TestCDP1802 cpu;
    private final FlatTestBus bus;

    public CDP1802TestCaseBench(CDP1802TestCase testCase) {
        this.testCase = testCase;
        this.cpu = new TestCDP1802(this);
        this.bus = new FlatTestBus(0xFFFF + 1);
        List<List<Integer>> ram = testCase.getInitialState().getRam();
        for (List<Integer> ramElement : ram) {
            this.bus.writeByte(ramElement.get(0), ramElement.get(1));
        }
    }

    @Override
    public IODevice.DmaStatus getDmaStatus() {
        return IODevice.DmaStatus.NONE;
    }

    @Override
    public boolean anyInterrupting() {
        return false;
    }

    @Override
    public int dispatchDmaIn(int address) {
        return 0;
    }

    @Override
    public void dispatchDmaOut(int address, int value) {

    }

    @Override
    public int dispatchInput(int port) {
        return 0;
    }

    @Override
    public void dispatchOutput(int port, int value) {

    }

    @Override
    public Bus getBus() {
        return this.bus;
    }

    public void runTest() {
        List<List<Object>> cycles = this.testCase.getCycles();

        this.cpu.cycle();
        this.cpu.nextState();

        this.cpu.cycle();
        this.cpu.nextState();

        this.cpu.acceptTestCase(this.testCase);

        for (List<Object> cycle : cycles) {
            this.cpu.cycle();
            this.cpu.nextState();
            // TODO: Test bus values
        }

        CDP1802TestState finalState = this.testCase.getFinalState();

        assertEquals(finalState.getP(), this.cpu.getP(), () -> "Test name: %s. Field: P".formatted(testCase.getName()));
        assertEquals(finalState.getX(), this.cpu.getX(), () -> "Test name: %s. Field: X".formatted(testCase.getName()));
        assertEquals(finalState.getN(), this.cpu.getN(), () -> "Test name: %s. Field: N".formatted(testCase.getName()));
        assertEquals(finalState.getI(), this.cpu.getI(), () -> "Test name: %s. Field: I".formatted(testCase.getName()));
        assertEquals(finalState.getT(), this.cpu.getT(), () -> "Test name: %s. Field: T".formatted(testCase.getName()));
        assertEquals(finalState.getD(), this.cpu.getD(), () -> "Test name: %s. Field: D".formatted(testCase.getName()));
        assertEquals(finalState.getDF() != 0, this.cpu.getDF(), () -> "Test name: %s. Field: DF".formatted(testCase.getName()));
        assertEquals(finalState.getIE() != 0, this.cpu.getIE(), () -> "Test name: %s. Field: IE".formatted(testCase.getName()));
        assertEquals(finalState.getQ() != 0, this.cpu.getQ(), () -> "Test name: %s. Field: Q".formatted(testCase.getName()));

        for (int i = 0; i < 16; i++) {
            int finalI = i;
            assertEquals(finalState.getR(i), this.cpu.getR(i), () -> "Test name: %s. Field: R(%d)".formatted(testCase.getName(), finalI));
        }

        List<List<Integer>> finalRam = finalState.getRam();
        for (List<Integer> ramElement : finalRam) {
            int address = ramElement.get(0);
            int value = ramElement.get(1);
            assertEquals(value, this.bus.readByte(address), "Test name: %s. Address: $%04X (%d)".formatted(testCase.getName(), address, address));
        }

    }

}
