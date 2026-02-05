package io.github.arkosammy12.jemu.systems.misc.gameboy;

import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.systems.GameBoyEmulator;
import io.github.arkosammy12.jemu.systems.bus.Bus;
import io.github.arkosammy12.jemu.systems.cpu.SM83;

import static io.github.arkosammy12.jemu.systems.misc.gameboy.GameBoyMMIOBus.*;

// The implementation of this class assumes one cycle call per M-cycle, before the CPU is cycled
public class GameBoyTimerController implements Bus {

    private final GameBoyEmulator emulator;

    private static final int FREQ_0 = 1 << 9;
    private static final int FREQ_1 = 1 << 3;
    private static final int FREQ_2 = 1 << 5;
    private static final int FREQ_3 = 1 << 7;

    private static final int TAC_CLOCK_SELECT_MASK = 0b11;
    private static final int TAC_ENABLE_BIT = 1 << 2;

    private int systemClock = 0xABCC - 1; // DIV (8 upper bits)
    private int timerCounter; // TIMA
    private int timerModulo; // TMA
    private int timerControl; // TAC

    private boolean oldTimerInput = false;

    private int reloadDelay = -1;
    private boolean reload = false;

    public GameBoyTimerController(GameBoyEmulator emulator) {
        this.emulator = emulator;
    }

    public void cycle() {

        this.reload = false;

        this.cycleSystemClock();
        this.cycleSystemClock();
        this.cycleSystemClock();
        this.cycleSystemClock();

    }

    private void cycleSystemClock() {

        if (this.reloadDelay > 0) {
            this.reloadDelay--;

            if (this.reloadDelay <= 0) {
                this.reload = true;
                this.timerCounter = this.timerModulo;
                this.triggerInterrupt();
            }

        }

        boolean frequencyBit = switch (this.timerControl & TAC_CLOCK_SELECT_MASK) {
            case 0 -> (this.systemClock & FREQ_0) != 0;
            case 1 -> (this.systemClock & FREQ_1) != 0;
            case 2 -> (this.systemClock & FREQ_2) != 0;
            case 3 -> (this.systemClock & FREQ_3) != 0;
            default -> throw new IllegalStateException("Lower 2 bits of TAC is not in the range [0, 3]!");
        };

        boolean timerInput = frequencyBit && (this.timerControl & TAC_ENABLE_BIT) != 0;

        if (this.oldTimerInput && !timerInput) {

            int newTimerCounter = this.timerCounter + 1;
            if (newTimerCounter > 0xFF) {
                this.reloadDelay = 4;
            }
            this.timerCounter = newTimerCounter & 0xFF;

        }

        this.oldTimerInput = timerInput;
        this.systemClock = (this.systemClock + 1) & 0xFFFF;
    }

    private void triggerInterrupt() {
        this.emulator.getMMIOController().setIF(this.emulator.getMMIOController().getIF() | SM83.TIMER_MASK);
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case DIV_ADDR -> this.systemClock = 0;
            case TIMA_ADDR -> {
                if (this.reloadDelay > 0) {
                    this.reloadDelay = -1;
                }
                if (!this.reload) {
                    this.timerCounter = value & 0xFF;
                }
            }
            case TMA_ADDR -> {
                this.timerModulo = value & 0xFF;
                if (this.reload) {
                    this.timerCounter = this.timerModulo;
                }
            }
            case TAC_ADDR -> this.timerControl = value & 0xFF;
            default -> throw new EmulatorException("Invalid timer address " + String.format("%04X", address) + "!");
        }
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case DIV_ADDR -> (this.systemClock & 0xFF00) >>> 8;
            case TIMA_ADDR -> this.timerCounter;
            case TMA_ADDR -> this.timerModulo;
            case TAC_ADDR -> this.timerControl;
            default -> throw new EmulatorException("Invalid timer address " + String.format("%04X", address) + "!");
        };
    }

}
