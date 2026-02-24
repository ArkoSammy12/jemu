package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.backend.common.Bus;
import io.github.arkosammy12.jemu.backend.cores.SM83;

import static io.github.arkosammy12.jemu.backend.gameboy.GameBoyMMIOBus.*;

public class GameBoyTimerController implements Bus {

    private final GameBoyEmulator emulator;

    private static final int FREQ_0 = 1 << 9;
    private static final int FREQ_1 = 1 << 3;
    private static final int FREQ_2 = 1 << 5;
    private static final int FREQ_3 = 1 << 7;

    private static final int TAC_CLOCK_SELECT_MASK = 0b11;
    private static final int TAC_ENABLE_BIT = 1 << 2;

    private static final int DIV_BIT_4_MASK = 1 << 12;

    private int systemClock; // DIV (8 upper bits)
    private int timerCounter; // TIMA
    private int timerModulo; // TMA
    private int timerControl; // TAC

    private boolean oldTimerInput = false;
    private boolean reloadOccurred = false;

    private int reloadDelay = -1;

    private boolean oldDivBit4 = false;

    public GameBoyTimerController(GameBoyEmulator emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case DIV_ADDR -> (this.systemClock & 0xFF00) >>> 8;
            case TIMA_ADDR -> this.timerCounter;
            case TMA_ADDR -> this.timerModulo;
            case TAC_ADDR -> this.timerControl | 0b11111000;
            default -> throw new EmulatorException("Invalid GameBoy timer address $%04X!".formatted(address));
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case DIV_ADDR -> this.systemClock = 0;
            case TIMA_ADDR -> {
                if (this.reloadDelay >= 0) {
                    this.reloadDelay = -1;
                }
                if (!this.reloadOccurred) {
                    this.timerCounter = value & 0xFF;
                }
            }
            case TMA_ADDR -> {
                this.timerModulo = value & 0xFF;
                if (this.reloadOccurred) {
                    this.timerCounter = this.timerModulo;
                }
            }
            case TAC_ADDR -> this.timerControl = value & 0xFF;
            default -> throw new EmulatorException("Invalid GameBoy timer address $%04X!".formatted(address));
        }
    }

    // It is assumed that this is called once per M-cycle, after the Processor performs the action of the current cycle, but before it fetches (if instruction ended), or polls for interrupts (if any)
    public boolean cycle() {
        this.reloadOccurred = false;

        boolean apuFrameSequencerTick = false;

        apuFrameSequencerTick |= this.cycleSystemClock();
        apuFrameSequencerTick |= this.cycleSystemClock();
        apuFrameSequencerTick |= this.cycleSystemClock();
        apuFrameSequencerTick |= this.cycleSystemClock();

        return apuFrameSequencerTick;
    }

    private boolean cycleSystemClock() {

        this.systemClock = (this.systemClock + 1) & 0xFFFF;
        if (this.reloadDelay > 0) {
            this.reloadDelay--;

            if (this.reloadDelay <= 0) {
                this.timerCounter = this.timerModulo;
                this.triggerInterrupt();
                this.reloadOccurred = true;
            }

        }

        boolean frequencyBit = switch (this.timerControl & TAC_CLOCK_SELECT_MASK) {
            case 0 -> (this.systemClock & FREQ_0) != 0;
            case 1 -> (this.systemClock & FREQ_1) != 0;
            case 2 -> (this.systemClock & FREQ_2) != 0;
            case 3 -> (this.systemClock & FREQ_3) != 0;
            default -> throw new EmulatorException("Lower 2 bits of TAC is not in the range [0, 3]!");
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

        boolean divBit4 = (this.systemClock & DIV_BIT_4_MASK) != 0;
        boolean apuFrameSequencerTick = this.oldDivBit4 && !divBit4;
        this.oldDivBit4 = divBit4;
        return apuFrameSequencerTick;

    }

    private void triggerInterrupt() {
        this.emulator.getMMIOController().setIF(this.emulator.getMMIOController().getIF() | SM83.TIMER_MASK);
    }

    public void onAPUPowerOn() {
        this.oldDivBit4 = (this.systemClock & DIV_BIT_4_MASK) != 0;
    }

}

