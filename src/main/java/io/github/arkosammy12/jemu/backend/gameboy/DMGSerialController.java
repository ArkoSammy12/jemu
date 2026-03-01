package io.github.arkosammy12.jemu.backend.gameboy;

import io.github.arkosammy12.jemu.backend.common.Bus;
import io.github.arkosammy12.jemu.backend.cores.SM83;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import org.tinylog.Logger;

import static io.github.arkosammy12.jemu.backend.gameboy.DMGMMIOBus.SB_ADDR;
import static io.github.arkosammy12.jemu.backend.gameboy.DMGMMIOBus.SC_ADDR;

public class DMGSerialController<E extends GameBoyEmulator> implements Bus {

    private static final int BIT_2_MASK = 1 << 2;
    private static final int BIT_7_MASK = 1 << 7;

    private final E emulator;

    private int internalClock;
    private boolean oldSerialInput;

    private boolean transferring;
    private int transferredBits;

    private int serialData = 0xFF;
    private int serialControl;

    public DMGSerialController(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case SB_ADDR -> this.serialData;
            case SC_ADDR -> 0xFF;
            default -> throw new EmulatorException("Invalid address $%04X for GameBoy serial controller!".formatted(address));
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case SB_ADDR -> this.serialData = value & 0xFF;
            case SC_ADDR -> {
                this.serialControl = value & 0xFF;
                if (this.getSerialEnable()) {
                    this.transferring = true;
                    this.transferredBits = 0;
                }
            }
            default -> throw new EmulatorException("Invalid address $%04X for GameBoy serial controller!".formatted(address));
        }
    }

    public void cycle() {
        this.cycleSerial();
        this.cycleSerial();
        this.cycleSerial();
        this.cycleSerial();
    }

    private void cycleSerial() {
        this.internalClock = (this.internalClock + 1) & 0xFF;

        boolean frequencyBit = ((this.getClockSpeed() ? BIT_2_MASK : BIT_7_MASK) & this.internalClock) != 0;
        boolean serialInput = frequencyBit && this.getClockSelect();

        if (this.oldSerialInput && !serialInput && this.transferring) {
            // Shift 1s into SB as there is no GameBoy supplying serial data
            this.serialData = ((this.serialData << 1) | 1) & 0xFF;
            this.transferredBits++;
            if (this.transferredBits >= 8) {
                this.transferring = false;
                this.transferredBits = 0;
                this.triggerSerialInterrupt();
            }
        }
        this.oldSerialInput = serialInput;
    }


    private boolean getSerialEnable() {
        return (this.serialControl & (1 << 7)) != 0;
    }

    private boolean getClockSpeed() {
        return (this.serialControl & (1 << 1)) != 0;
    }

    private boolean getClockSelect() {
        return (this.serialControl & 1) != 0;
    }

    private void triggerSerialInterrupt() {
        this.emulator.getMMIOController().setIF(this.emulator.getMMIOController().getIF() | SM83.SERIAL_MASK);
    }

}
