package io.github.arkosammy12.jemu.core.cosmacvip;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.exceptions.InvalidInstructionException;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.cpu.CDP1802;

import java.util.List;

import static io.github.arkosammy12.jemu.core.cpu.CDP1802.DmaStatus.IN;
import static io.github.arkosammy12.jemu.core.cpu.CDP1802.DmaStatus.OUT;
import static io.github.arkosammy12.jemu.core.cpu.CDP1802.isHandled;

public class CosmacVipEmulator implements Emulator, CDP1802.SystemBus {

    public static final int CYCLES_PER_FRAME = 3668;

    private final CosmacVIPHost host;
    private final CosmacVIPHost.Chip8Interpreter chip8Interpreter;

    private final CDP1802 cpu;
    private final CosmacVipBus bus;
    private final CDP1861<?> vdp;
    private final AudioGenerator<?> audio;
    private final CosmacVIPKeypad<?> keypad;
    private final List<IODevice> ioDevices;

    private final int frameRate;

    public CosmacVipEmulator(CosmacVIPHost host) {
        try {
            this.host = host;
            this.chip8Interpreter = host.getChip8Interpreter();
            this.keypad = new CosmacVIPKeypad<>(this);
            this.cpu = new CDP1802(this);
            if (this.chip8Interpreter == CosmacVIPHost.Chip8Interpreter.CHIP_8X) {
                this.bus = new HybridChip8XBus(this);
                this.vdp = new VP590<>(this);
                VP595<?> vp595 = new VP595<>(this);
                this.audio = vp595;
                this.ioDevices = List.of(this.vdp, this.keypad, vp595);
                this.frameRate = 61;
            } else {
                this.bus = new CosmacVipBus(this);
                this.vdp = new CDP1861<>(this);
                this.audio = new CosmacVipAudioGenerator<>(this);
                this.ioDevices = List.of(this.vdp, this.keypad);
                this.frameRate = 60;
            }
        } catch (Exception e) {
            throw new EmulatorException(e);
        }
    }

    @Override
    public SystemHost getHost() {
        return this.host;
    }

    public CDP1802 getCpu() {
        return this.cpu;
    }

    public CosmacVipBus getBusView() {
        return this.bus;
    }

    @Override
    public Bus getBus() {
        return this.bus;
    }

    @Override
    public CDP1861<?> getVideoGenerator() {
        return this.vdp;
    }

    @Override
    public AudioGenerator<?> getAudioGenerator() {
        return this.audio;
    }

    @Override
    public SystemController<?> getSystemController() {
        return this.keypad;
    }

    public CosmacVIPHost.Chip8Interpreter getChip8Interpreter() {
        return this.chip8Interpreter;
    }

    public int dispatchInput(int ioPort) {
        for (IODevice ioDevice : this.ioDevices) {
            if (ioDevice.isInputPort(ioPort)) {
                return ioDevice.onInput(ioPort);
            }
        }
        return 0xFF;
    }

    public void dispatchOutput(int ioPort, int value) {
        if ((ioPort & 4) != 0) {
            this.bus.unlatchAddressMsb();
        }
        for (IODevice ioDevice : this.ioDevices) {
            if (ioDevice.isOutputPort(ioPort)) {
                ioDevice.onOutput(ioPort, value);
                return;
            }
        }
    }

    public CDP1802.DmaStatus getDmaStatus() {
        CDP1802.DmaStatus highestStatus = CDP1802.DmaStatus.NONE;
        for (IODevice ioDevice : this.ioDevices) {
            switch (ioDevice.getDmaStatus()) {
                case IN -> highestStatus = IN;
                case OUT -> {
                    if (highestStatus == CDP1802.DmaStatus.NONE) {
                        highestStatus = OUT;
                    }
                }
            }
        }
        return highestStatus;
    }

    public void dispatchDmaOut(int dmaOutAddress, int value) {
        for (IODevice ioDevice : this.ioDevices) {
            if (ioDevice.getDmaStatus() == CDP1802.DmaStatus.OUT) {
                ioDevice.doDmaOut(dmaOutAddress, value);
                return;
            }
        }
    }

    public int dispatchDmaIn(int dmaInAddress) {
        for (IODevice ioDevice : this.ioDevices) {
            if (ioDevice.getDmaStatus() == CDP1802.DmaStatus.IN) {
                return ioDevice.doDmaIn(dmaInAddress);
            }
        }
        return 0xFF;
    }

    public boolean anyInterrupting() {
        for (IODevice ioDevice : this.ioDevices) {
            if (ioDevice.isInterrupting()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void executeFrame() {
        //if (this.disassembler.isEnabled()) {
            //this.runCyclesDebug();
        //} else {
            this.runCycles();
        //}
    }

    private void runCycles() {
        for (int i = 0; i < CYCLES_PER_FRAME; i++) {
            this.runCycle();
        }
    }

    private void runCycle() {
        this.cpu.getCurrentState();
        this.cycleCpu();
        this.cycleIoDevices();
        this.cpu.nextState();

        this.cpu.getCurrentState();
    }

    @Override
    public void executeCycle() {
        this.cycleCpu();
        this.cycleIoDevices();
        this.cpu.nextState();
    }

    private void cycleCpu() {
        int flags = this.cpu.cycle();
        if (!isHandled(flags)) {
            throw new InvalidInstructionException((this.cpu.getI() << 4) | this.cpu.getN(), "Cosmac VIP");
        }
    }

    private void cycleIoDevices() {
        for (IODevice ioDevice : this.ioDevices) {
            ioDevice.cycle();
        }
    }

    @Override
    public int getFramerate() {
        return this.frameRate;
    }

    @Override
    public void close() {

    }

}
