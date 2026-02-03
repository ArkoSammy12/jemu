package io.github.arkosammy12.jemu.ssts.cdp1802;

import io.github.arkosammy12.jemu.cpu.TestCDP1802;
import io.github.arkosammy12.jemu.ssts.sm83.SM83TestCase;
import io.github.arkosammy12.jemu.systems.bus.ReadWriteBus;
import io.github.arkosammy12.jemu.systems.cpu.CDP1802;
import io.github.arkosammy12.jemu.systems.misc.cosmacvip.IODevice;
import io.github.arkosammy12.jemu.util.FlatTestBus;
import org.tinylog.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CDP1802TestCaseBench implements CDP1802.SystemBus {

    private final CDP1802TestCase testCase;
    private final TestCDP1802 cpu;
    private final FlatTestBus bus;

    public CDP1802TestCaseBench(CDP1802TestCase testCase) {
        this.testCase = testCase;
        this.cpu = new TestCDP1802(this);
        this.bus = new FlatTestBus(0xFFFF + 1, 0xFFFF);
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
    public ReadWriteBus getBus() {
        return this.bus;
    }

    public void runTest() {
        List<List<Object>> cycles = this.testCase.getCycles();
        Logger.info("Running test case: " + this.testCase.getName());
        this.cpu.cycle();
        this.cpu.nextState();

        this.cpu.cycle();
        this.cpu.nextState();

        this.cpu.acceptTestCase(this.testCase);

        if (this.testCase.getName().startsWith("C0 0F BE")) {
            int a = 1;
        }

        for (List<Object> cycle : cycles) {
            this.cpu.cycle();
            this.cpu.nextState();
            // TODO: Test bus values
        }

        CDP1802TestState finalState = this.testCase.getFinalState();

        assertEquals(finalState.getP(), this.cpu.getP());
        assertEquals(finalState.getX(), this.cpu.getX());
        assertEquals(finalState.getN(), this.cpu.getN());
        assertEquals(finalState.getI(), this.cpu.getI());
        assertEquals(finalState.getT(), this.cpu.getT());
        assertEquals(finalState.getD(), this.cpu.getD());
        assertEquals(finalState.getDF() != 0, this.cpu.getDF());
        assertEquals(finalState.getIE() != 0, this.cpu.getIE());
        assertEquals(finalState.getQ() != 0, this.cpu.getQ());

        for (int i = 0; i < 16; i++) {
            int finalI = i;
            assertEquals(finalState.getR(i), this.cpu.getR(i), () -> "R(" + finalI + ")");
        }

        List<List<Integer>> finalRam = finalState.getRam();
        for (List<Integer> ramElement : finalRam) {
            int address = ramElement.get(0);
            int value = ramElement.get(1);
            assertEquals(value, this.bus.readByte(address));
        }

    }

}
