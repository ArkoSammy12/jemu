package io.github.arkosammy12.jemu.core.nintendo.gameboy;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.cpu.SM83;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

public class DMGSerialController<E extends GameBoyEmulator> implements Bus {

    public static final int SB_ADDR = 0xFF01;
    public static final int SC_ADDR = 0xFF02;

    // Shift the masks once to the left to account for extra clocking caused by falling edge bit 2/7
    private static final int FREQUENCY_BIT_2_MASK = 1 << (2 + 1);
    private static final int FREQUENCY_BIT_7_MASK = 1 << (7 + 1);

    protected final E emulator;

    private boolean oldSerialInput;

    protected boolean transferring;
    private int transferredBits;

    private int serialData = 0xFF;
    protected int serialControl;

    public DMGSerialController(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case SB_ADDR -> this.serialData;
            case SC_ADDR -> (this.serialControl & 0x7F) | (this.transferring ? 1 << 7 : 0) | 0b01111110;
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
        boolean frequencyBit = ((this.getClockSpeed() ? FREQUENCY_BIT_2_MASK : FREQUENCY_BIT_7_MASK) & this.emulator.getTimerController().getSystemClock()) != 0;
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

    protected boolean getClockSpeed() {
        return false;
    }

    private boolean getClockSelect() {
        return (this.serialControl & 1) != 0;
    }

    private void triggerSerialInterrupt() {
        DMGBus<?> bus = this.emulator.getBus();
        bus.setIF(bus.getIF() | SM83.SERIAL_MASK);
    }

}
