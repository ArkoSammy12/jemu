package io.github.arkosammy12.jemu.core.test.gameboy;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.hardware.SM83;
import io.github.arkosammy12.jemu.core.gameboy.DMGSerialController;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.core.gameboycolor.GameBoyColorEmulator;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Drives a {@link GameBoyEmulator} headlessly, with result detection for the Mooneye
 * and Blargg test ROM conventions.
 */
public final class GameBoyTestHarness implements AutoCloseable {

    private final GameBoyEmulator emulator;

    public GameBoyTestHarness(Path romPath, GameBoyHost.Model model) throws IOException {
        HeadlessGameBoyHost host = new HeadlessGameBoyHost(romPath);
        this.emulator = switch (model) {
            case DMG -> new GameBoyEmulator(host);
            case CGB -> new GameBoyColorEmulator(host);
        };
    }

    public enum Result {
        PASSED,
        FAILED,
        TIMED_OUT,
    }

    public record BlarggResult(Result status, String output) { }

    /**
     * Runs until the CPU registers contain the Mooneye result signature, polling once
     * per frame: B, C, D, E, H, L hold 3, 5, 8, 13, 21, 34 on pass or $42 on failure.
     */
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
        return cpu.getB() == b && cpu.getC() == c && cpu.getD() == d
                && cpu.getE() == e && cpu.getH() == h && cpu.getL() == l;
    }

    /**
     * Runs until a Blargg test ROM reports a result, polling once per frame. Watches
     * both reporting conventions: text printed over the serial port, and the cart RAM
     * protocol ($A000 holds $80 while running and the final status once done, with
     * signature $DE $B0 $61 at $A001-$A003 and zero-terminated text from $A004).
     */
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

    // Captures outgoing serial bytes. SB is sampled while idle and emitted on the rising
    // edge of SC bit 7, as an SC write racing a serial clock edge can shift SB before the
    // transfer is observable.
    private static final class SerialWatcher {

        private final StringBuilder output = new StringBuilder();
        private boolean transferInProgress;
        private int idleSerialData;

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

}
