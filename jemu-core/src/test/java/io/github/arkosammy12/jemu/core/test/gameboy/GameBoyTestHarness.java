package io.github.arkosammy12.jemu.core.test.gameboy;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.hardware.SM83;
import io.github.arkosammy12.jemu.core.gameboy.DMGSerialController;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.core.gameboycolor.GameBoyColorEmulator;

import java.io.IOException;
import java.nio.file.Path;

public final class GameBoyTestHarness implements AutoCloseable {

    private final GameBoyEmulator emulator;

    public GameBoyTestHarness(Path romPath, GameBoyHost.Model model) throws IOException {
        HeadlessGameBoyHost host = new HeadlessGameBoyHost(romPath);
        this.emulator = switch (model) {
            case DMG -> new GameBoyEmulator(host);
            case CGB -> new GameBoyColorEmulator(host);
        };
    }

    // Run until registers contain the pass signature of 3, 5, 8, 13, 21, 34 for B, C, D, E, H, and L.
    // Fail if all contain $42
    public Result runMooneye(int timeoutFrames) {
        for (int frame = 0; frame < timeoutFrames; frame++) {
            for (int i = 0; i < GameBoyEmulator.M_CYCLES_PER_FRAME; i++) {
                this.emulator.executeCycle();
            }
            if (this.registersHold(3, 5, 8, 13, 21, 34)) {
                return Result.PASSED;
            }
            if (this.registersHold(0x42, 0x42, 0x42, 0x42, 0x42, 0x42)) {
                return Result.FAILED;
            }
        }
        return Result.TIMED_OUT;
    }

    private boolean registersHold(int b, int c, int d, int e, int h, int l) {
        SM83<?> cpu = this.emulator.getCpu();
        return cpu.getB() == b && cpu.getC() == c && cpu.getD() == d && cpu.getE() == e && cpu.getH() == h && cpu.getL() == l;
    }

    // Run until serial outputs "Passed" or "Failed" or there is $DE $B0 $61 in A001-A003.
    public BlarggResult runBlargg(int timeoutFrames) {
        SerialWatcher serialWatcher = new SerialWatcher();
        for (int frame = 0; frame < timeoutFrames; frame++) {
            for (int i = 0; i < GameBoyEmulator.M_CYCLES_PER_FRAME; i++) {
                this.emulator.executeCycle();
                serialWatcher.tick(this.emulator);
            }

            String serialOutput = serialWatcher.output.toString();
            if (serialOutput.contains("Passed")) {
                return new BlarggResult(Result.PASSED, serialOutput);
            }
            if (serialOutput.contains("Failed")) {
                return new BlarggResult(Result.FAILED, serialOutput);
            }

            int cartRamStatus = this.readBlarggCartRamStatus();
            if (cartRamStatus >= 0 && cartRamStatus != 0x80) {
                Result result = cartRamStatus == 0 ? Result.PASSED : Result.FAILED;
                return new BlarggResult(result, this.readBlarggCartRamText());
            }
        }
        return new BlarggResult(Result.TIMED_OUT, serialWatcher.output.toString());
    }

    private int readBlarggCartRamStatus() {
        Bus bus = this.emulator.getBus();
        if (bus.readByte(0xA001) != 0xDE || bus.readByte(0xA002) != 0xB0 || bus.readByte(0xA003) != 0x61) {
            return -1;
        }
        return bus.readByte(0xA000);
    }

    private String readBlarggCartRamText() {
        Bus bus = this.emulator.getBus();
        StringBuilder text = new StringBuilder();
        for (int address = 0xA004; address < 0xA204; address++) {
            int value = bus.readByte(address);
            if (value == 0) {
                break;
            }
            text.append((char) value);
        }
        return text.toString();
    }

    private static final class SerialWatcher {

        private final StringBuilder output = new StringBuilder();
        private boolean transferInProgress;
        private int idleSerialData;

        // Sample SB when no transfer is occurring, and emit on rising edge of SC bit 7,
        // as that is what triggers a new transfer.
        private void tick(GameBoyEmulator emulator) {
            Bus bus = emulator.getBus();
            boolean transferring = (bus.readByte(DMGSerialController.SC_ADDR) & 0b10000000) != 0;
            if (transferring && !this.transferInProgress) {
                this.output.append((char) this.idleSerialData);
            } else if (!transferring) {
                this.idleSerialData = bus.readByte(DMGSerialController.SB_ADDR);
            }
            this.transferInProgress = transferring;
        }

    }

    @Override
    public void close() {
        try {
            this.emulator.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public enum Result {
        PASSED,
        FAILED,
        TIMED_OUT,
    }

    public record BlarggResult(Result status, String output) {}

}
