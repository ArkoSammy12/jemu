package io.github.arkosammy12.jemu.ssts.sm83;

import io.github.arkosammy12.jemu.cpu.SingleStepSM83;
import io.github.arkosammy12.jemu.systems.bus.ReadWriteBus;
import io.github.arkosammy12.jemu.systems.SystemBus;
import org.tinylog.Logger;
import java.util.List;

import static io.github.arkosammy12.jemu.systems.cpu.SM83.PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SM83TestCaseBench implements SystemBus {

    private final SM83TestCase testCase;
    private final SingleStepSM83 cpu;
    private final SM83TestBus bus;

    public SM83TestCaseBench(SM83TestCase testCase) {
        this.testCase = testCase;
        this.cpu = new SingleStepSM83(this, testCase);
        this.bus = new SM83TestBus(testCase);
    }

    @Override
    public ReadWriteBus getBus() {
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

    private static class SM83TestBus implements ReadWriteBus {

        private final int[] ram = new int[0xFFFF + 1];

        private SM83TestBus(SM83TestCase testCase) {
            for (List<Integer> ramElement : testCase.getInitialState().getRam()) {
                this.ram[ramElement.get(0)] = ramElement.get(1);
            }
        }


        @Override
        public void writeByte(int address, int value) {
            this.ram[address] = value & 0xFF;
        }

        @Override
        public int readByte(int address) {
            return this.ram[address];
        }

        @Override
        public int getMemorySize() {
            return this.ram.length;
        }

        @Override
        public int getMemoryBoundsMask() {
            return 0xFFFF;
        }

        @Override
        public int getByte(int address) {
            return this.readByte(address);
        }
    }

}
