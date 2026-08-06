package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.hardware.SM83;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

public class DMGSerialController<E extends GameBoyEmulator> implements Bus {

    public static final int SB_ADDR = 0xFF01;
    public static final int SC_ADDR = 0xFF02;

    // Shift the masks once to the left to account for extra clocking caused by falling edge bit 2/7
    private static final int FREQUENCY_BIT_2_MASK = 1 << 2;
    private static final int FREQUENCY_BIT_7_MASK = 1 << 7;

    protected final E emulator;

    private int serialData = 0xFF;
    protected int serialControl;

    private boolean oldSerialInput;
    private boolean dLatch;
    private int transferredBits;

    public DMGSerialController(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case SB_ADDR -> this.serialData;
            case SC_ADDR -> this.serialControl | 0b01111110;
            default -> throw new EmulatorException("Invalid address $%04X for GameBoy serial controller!".formatted(address));
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case SB_ADDR -> this.serialData = value & 0xFF;
            case SC_ADDR -> {
                this.serialControl = value & 0xFF;
                this.transferredBits = 0;
                this.dLatch = false;
            }
            default -> throw new EmulatorException("Invalid address $%04X for GameBoy serial controller!".formatted(address));
        }
    }

    public void cycle() {
        boolean frequencyBit = ((this.getClockSpeed() ? FREQUENCY_BIT_2_MASK : FREQUENCY_BIT_7_MASK) & this.emulator.getTimerController().getSystemClock()) != 0;
        boolean serialInput = frequencyBit && this.getClockSelect();

        if (this.oldSerialInput && !serialInput) {
            if (this.dLatch && this.getTransferEnable()) {
                // Shift 1s into SB as there is no GameBoy supplying serial data
                this.serialData = ((this.serialData << 1) | 1) & 0xFF;
                this.transferredBits++;
                if (this.transferredBits >= 8) {
                    this.serialControl &= ~(1 << 7);
                    this.transferredBits = 0;
                    this.triggerSerialInterrupt();
                }
            }
            this.dLatch = !this.dLatch;
        }

        this.oldSerialInput = serialInput;
    }


    private boolean getTransferEnable() {
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
