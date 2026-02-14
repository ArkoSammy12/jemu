package io.github.arkosammy12.jemu.systems.cosmacvip;

import io.github.arkosammy12.jemu.systems.common.Emulator;
import io.github.arkosammy12.jemu.systems.common.Bus;
import io.github.arkosammy12.jemu.exceptions.InvalidInstructionException;
import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.settings.CosmacVipEmulatorSettings;
import io.github.arkosammy12.jemu.config.settings.EmulatorSettings;
import io.github.arkosammy12.jemu.disassembler.CosmacVipDisassembler;
import io.github.arkosammy12.jemu.disassembler.Disassembler;
import io.github.arkosammy12.jemu.disassembler.AbstractDisassembler;
import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.systems.common.SoundSystem;
import io.github.arkosammy12.jemu.ui.debugger.DebuggerSchema;
import io.github.arkosammy12.jemu.util.System;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyAdapter;
import java.util.List;
import java.util.function.Function;

import static io.github.arkosammy12.jemu.systems.cosmacvip.CDP1802.isHandled;
import static io.github.arkosammy12.jemu.systems.cosmacvip.IODevice.DmaStatus.IN;
import static io.github.arkosammy12.jemu.systems.cosmacvip.IODevice.DmaStatus.OUT;

public class CosmacVipEmulator implements Emulator, CDP1802.SystemBus {

    public static final int CYCLES_PER_FRAME = 3668;
    public static final String REGISTERS_ENTRY_KEY = "cosmacvip.processor.registers";

    private final Jemu jemu;
    private final CosmacVipEmulatorSettings settings;
    private final CosmacVipEmulatorSettings.Chip8Interpreter chip8Interpreter;
    private final DebuggerSchema debuggerSchema;
    private final AbstractDisassembler<?> disassembler;
    private final System system;

    private final CDP1802 processor;
    private final CosmacVipBus bus;
    private final CDP1861<?> display;
    private final SoundSystem soundSystem;
    private final CosmacVIPKeypad keypad;
    private final List<IODevice> ioDevices;

    private final int frameRate;
    private int currentInstructionsPerFrame;

    public CosmacVipEmulator(CosmacVipEmulatorSettings emulatorSettings, CosmacVipEmulatorSettings.Chip8Interpreter chip8Interpreter) {
        try {
            this.jemu = emulatorSettings.getJemu();
            this.settings = emulatorSettings;
            this.chip8Interpreter = chip8Interpreter;
            this.system = emulatorSettings.getSystem();
            this.keypad = new CosmacVIPKeypad(this);
            this.processor = new CDP1802(this);
            if (this.chip8Interpreter == CosmacVipEmulatorSettings.Chip8Interpreter.CHIP_8X) {
                this.bus = new HybridChip8XBus(this);
                this.display = new VP590<>(this);
                VP595 vp595 = new VP595(this);
                this.soundSystem = vp595;
                this.ioDevices = List.of(this.display, this.keypad, vp595);
                this.frameRate = 61;
            } else {
                this.bus = new CosmacVipBus(this);
                this.display = new CDP1861<>(this);
                this.soundSystem = new CosmacVipSoundSystem(this);
                this.ioDevices = List.of(this.display, this.keypad);
                this.frameRate = 60;
            }
            this.debuggerSchema = this.createDebuggerSchema();
            this.disassembler = new CosmacVipDisassembler<>(this);
            this.disassembler.setProgramCounterSupplier(this::getActualCurrentInstructionAddress);
            this.processor.restoreRegisters(this.getEmulatorSettings());
        } catch (Exception e) {
            throw new EmulatorException(e);
        }
    }

    @Override
    public CDP1802 getProcessor() {
        return this.processor;
    }

    @Override
    public CosmacVipBus getBusView() {
        return this.bus;
    }

    @Override
    public Bus getBus() {
        return this.bus;
    }

    @Override
    public CDP1861<?> getDisplay() {
        return this.display;
    }

    @Override
    public SoundSystem getSoundSystem() {
        return this.soundSystem;
    }

    @Override
    public EmulatorSettings getEmulatorSettings() {
        return this.settings;
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    @Override
    public List<KeyAdapter> getKeyAdapters() {
        return List.of(this.keypad);
    }

    @Override
    public DebuggerSchema getDebuggerSchema() {
        return this.debuggerSchema;
    }

    @Override
    public @Nullable Disassembler getDisassembler() {
        return this.disassembler;
    }

    public CosmacVipEmulatorSettings.Chip8Interpreter getChip8Interpreter() {
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

    public IODevice.DmaStatus getDmaStatus() {
        IODevice.DmaStatus highestStatus = IODevice.DmaStatus.NONE;
        for (IODevice ioDevice : this.ioDevices) {
            switch (ioDevice.getDmaStatus()) {
                case IN -> highestStatus = IN;
                case OUT -> {
                    if (highestStatus == IODevice.DmaStatus.NONE) {
                        highestStatus = OUT;
                    }
                }
            }
        }
        return highestStatus;
    }

    public void dispatchDmaOut(int dmaOutAddress, int value) {
        for (IODevice ioDevice : this.ioDevices) {
            if (ioDevice.getDmaStatus() == IODevice.DmaStatus.OUT) {
                ioDevice.doDmaOut(dmaOutAddress, value);
                return;
            }
        }
    }

    public int dispatchDmaIn(int dmaInAddress) {
        for (IODevice ioDevice : this.ioDevices) {
            if (ioDevice.getDmaStatus() == IODevice.DmaStatus.IN) {
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
        if (this.disassembler.isEnabled()) {
            this.runCyclesDebug();
        } else {
            this.runCycles();
        }
        this.display.flush();
        this.soundSystem.pushSamples();
    }

    private void runCycles() {
        for (int i = 0; i < CYCLES_PER_FRAME; i++) {
            this.runCycle();
        }
    }

    private void runCyclesDebug() {
        for (int i = 0; i < CYCLES_PER_FRAME; i++) {
            CDP1802.State currentState = this.processor.getCurrentState();
            this.runCycle();
            this.disassembler.disassemble(this.getActualCurrentInstructionAddress());
            if (currentState == CDP1802.State.S0_FETCH && this.disassembler.checkBreakpoint(this.getActualCurrentInstructionAddress())) {
                this.jemu.onBreakpoint();
                break;
            }
        }
    }

    private void runCycle() {
        CDP1802.State currentState = this.processor.getCurrentState();
        this.cycleCpu();
        this.cycleIoDevices();
        this.processor.nextState();

        CDP1802.State nextState = this.processor.getCurrentState();
        if (currentState.isS1Execute() && !nextState.isS1Execute()) {
            this.currentInstructionsPerFrame++;
        }
    }

    @Override
    public void executeCycle() {
        this.cycleCpu();
        this.cycleIoDevices();
        this.processor.nextState();
        this.display.flush();
        this.disassembler.disassembleRange(this.getActualCurrentInstructionAddress(), 30, true);
    }

    private int getActualCurrentInstructionAddress() {
        int address = this.processor.getCurrentInstructionAddress();
        return this.bus.isAddressMsbLatched() ? address | 0x8000 : address;
    }

    private void cycleCpu() {
        int flags = this.processor.cycle();
        if (!isHandled(flags)) {
            throw new InvalidInstructionException((this.processor.getI() << 4) | this.processor.getN(), this.getSystem());
        }
    }

    private void cycleIoDevices() {
        for (IODevice ioDevice : this.ioDevices) {
            ioDevice.cycle();
        }
    }

    @Override
    public int getCurrentInstructionsPerFrame() {
        int ret = this.currentInstructionsPerFrame;
        this.currentInstructionsPerFrame = 0;
        return ret;
    }

    @Override
    public int getFramerate() {
        return this.frameRate;
    }

    @Override
    public void close() {
        try {
            if (this.processor != null) {
                this.processor.saveRegisters(this.getEmulatorSettings());
            }
            if (this.display != null) {
                this.display.close();
            }
            if (this.disassembler != null) {
                this.disassembler.close();
            }
        } catch (Exception e) {
            throw new EmulatorException("Error releasing current emulator resources: ", e);
        }
    }

    protected DebuggerSchema createDebuggerSchema() {
        DebuggerSchema debuggerSchema = new DebuggerSchema();
        debuggerSchema.setTextSectionName("Cosmac VIP");
        debuggerSchema.setMemoryPointerSupplier(() -> this.processor.getR(this.processor.getX()));

        Function<Integer, String> byteFormatter = val -> String.format("%02X", val);
        Function<Integer, String> nibbleFormatter = val -> String.format("%01X", val);
        Function<Boolean, String> booleanFormatter = val -> val ? "1" : "0";

        //debuggerSchema.createTextEntry()
                //.withName("Cosmac VIP based variant. Does not support custom quirks.");

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("I")
                .withStateUpdater(this.processor::getI)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("N")
                .withStateUpdater(this.processor::getN)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("P")
                .withStateUpdater(this.processor::getP)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("X")
                .withStateUpdater(this.processor::getX)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("D")
                .withStateUpdater(this.processor::getD)
                .withToStringFunction(byteFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("T")
                .withStateUpdater(this.processor::getT)
                .withToStringFunction(byteFormatter);

        debuggerSchema.<Boolean>createCpuRegisterEntry()
                .withName("DF")
                .withStateUpdater(this.processor::getDF)
                .withToStringFunction(booleanFormatter);

        debuggerSchema.<Boolean>createCpuRegisterEntry()
                .withName("IE")
                .withStateUpdater(this.processor::getIE)
                .withToStringFunction(booleanFormatter);

        debuggerSchema.<Boolean>createCpuRegisterEntry()
                .withName("Q")
                .withStateUpdater(this.processor::getQ)
                .withToStringFunction(booleanFormatter);

        debuggerSchema.setStackSectionName("X Data");

        for (int i = 0; i < 16; i++) {
            int finalI = i;
            debuggerSchema.<Integer>createGeneralPurposeRegisterEntry()
                    .withName(String.format("R%01X", i))
                    .withStateUpdater(() -> this.getProcessor().getR(finalI))
                    .withToStringFunction(val -> String.format("%04X", val));

            debuggerSchema.<Integer>createStackEntry()
                    .withName(String.format("%01X", i))
                    .withStateUpdater(() -> this.getBusView().getByte(this.processor.getR(finalI)))
                    .withToStringFunction(val -> String.format("%02X", val));

        }
        return debuggerSchema;
    }

}
