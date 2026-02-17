package io.github.arkosammy12.jemu.ssts.sm83;

import io.github.arkosammy12.jemu.cpu.TestSM83;
import io.github.arkosammy12.jemu.backend.common.Bus;
import io.github.arkosammy12.jemu.backend.cores.SM83;
import io.github.arkosammy12.jemu.util.FlatTestBus;
import org.tinylog.Logger;
import java.util.List;

import static io.github.arkosammy12.jemu.backend.cores.SM83.PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SM83TestCaseBench implements SM83.SystemBus {

    private final SM83TestCase testCase;
    private final TestSM83 cpu;
    private final FlatTestBus bus;

    public SM83TestCaseBench(SM83TestCase testCase) {
        this.testCase = testCase;
        this.cpu = new TestSM83(this);
        this.cpu.acceptTestCase(testCase);
        this.bus = new FlatTestBus(0xFFFF + 1);
        List<List<Integer>> ram = testCase.getInitialState().getRam();
        for (List<Integer> ramElement : ram) {
            this.bus.writeByte(ramElement.get(0), ramElement.get(1));
        }
    }

    @Override
    public Bus getBus() {
        return this.bus;
    }

    public void runTest() {
        List<List<Object>> cycles = this.testCase.getCycles();
        Logger.info("Running test case: " + this.testCase.getName());

        // Skip the tests for the HALT and STOP instructions for now
        // TODO: Implement proper instruction handling and re-add these tests
        if (this.testCase.getName().startsWith("10") || this.testCase.getName().startsWith("76")) {
            return;
        }
        boolean prefixed = false;
        this.cpu.cycle();
        if (this.cpu.getIR() == PREFIX) {
            this.cpu.cycle();
            prefixed = true;
        }
        for (List<Object> cycle : cycles) {
            this.cpu.cycle();
            // TODO: Test bus values
        }
        SM83TestState finalState = this.testCase.getFinalState();

        assertEquals(finalState.getPC(), (this.cpu.getPC() - (prefixed ? 2 : 1)) & 0xFFFF);
        assertEquals(finalState.getSP(), this.cpu.getSP());

        assertEquals(finalState.getA(), this.cpu.getA());
        assertEquals(finalState.getF(), this.cpu.getAF() & 0xFF);
        assertEquals(finalState.getB(), this.cpu.getB());
        assertEquals(finalState.getC(), this.cpu.getC());
        assertEquals(finalState.getD(), this.cpu.getD());
        assertEquals(finalState.getE(), this.cpu.getE());
        assertEquals(finalState.getH(), this.cpu.getH());
        assertEquals(finalState.getL(), this.cpu.getL());

        // Test repo says to ignore the IME and IE registers for now

        /*
        assertEquals(finalState.getIME() != 0, this.cpu.getIME());
        assertEquals(finalState.IE() != 0, TODO);
         */

        List<List<Integer>> finalRam = finalState.getRam();
        for (List<Integer> ramElement : finalRam) {
            int address = ramElement.get(0);
            int value = ramElement.get(1);
            assertEquals(value, this.bus.readByte(address));
        }

    }

    // TODO: VERIFY CORRECT RETURN VALUES FOR THESE REGISTERS
    @Override
    public int getIE() {
        return 0;
    }

    @Override
    public int getIF() {
        return 0;
    }

    @Override
    public void setIF(int value) {

    }

}
