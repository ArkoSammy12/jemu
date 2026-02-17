package io.github.arkosammy12.jemu.backend.cosmacvip;

import io.github.arkosammy12.jemu.backend.common.*;
import io.github.arkosammy12.jemu.backend.exceptions.InvalidInstructionException;
import io.github.arkosammy12.jemu.backend.disassembler.CosmacVipDisassembler;
import io.github.arkosammy12.jemu.backend.disassembler.Disassembler;
import io.github.arkosammy12.jemu.backend.disassembler.AbstractDisassembler;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.backend.cores.CDP1802;
import io.github.arkosammy12.jemu.application.ui.debugger.DebuggerSchema;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

import static io.github.arkosammy12.jemu.backend.cores.CDP1802.isHandled;
import static io.github.arkosammy12.jemu.backend.cosmacvip.IODevice.DmaStatus.IN;
import static io.github.arkosammy12.jemu.backend.cosmacvip.IODevice.DmaStatus.OUT;

public class CosmacVipEmulator implements Emulator, CDP1802.SystemBus {

    public static final int CYCLES_PER_FRAME = 3668;
    //public static final String REGISTERS_ENTRY_KEY = "cosmacvip.processor.registers";

    private final CosmacVipHost host;
    private final CosmacVipHost.Chip8Interpreter chip8Interpreter;
    private final DebuggerSchema debuggerSchema;
    private final AbstractDisassembler<?> disassembler;

    private final CDP1802 cpu;
    private final CosmacVipBus bus;
    private final CDP1861<?> vdp;
    private final AudioGenerator<?> audio;
    private final CosmacVIPKeypad<?> keypad;
    private final List<IODevice> ioDevices;

    private final int frameRate;
    private int currentInstructionsPerFrame;

    public CosmacVipEmulator(CosmacVipHost host) {
        try {
            this.host = host;
            this.chip8Interpreter = host.getChip8Interpreter();
            this.keypad = new CosmacVIPKeypad<>(this);
            this.cpu = new CDP1802(this);
            if (this.chip8Interpreter == CosmacVipHost.Chip8Interpreter.CHIP_8X) {
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
            this.debuggerSchema = this.createDebuggerSchema();
            this.disassembler = new CosmacVipDisassembler<>(this);
            this.disassembler.setProgramCounterSupplier(this::getActualCurrentInstructionAddress);
            //this.cpu.restoreRegisters(this.getEmulatorSettings());
        } catch (Exception e) {
            throw new EmulatorException(e);
        }
    }

    @Override
    public SystemHost getHost() {
        return this.host;
    }

    @Override
    public CDP1802 getCpu() {
        return this.cpu;
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

    @Override
    public DebuggerSchema getDebuggerSchema() {
        return this.debuggerSchema;
    }

    @Override
    public @Nullable Disassembler getDisassembler() {
        return this.disassembler;
    }

    public CosmacVipHost.Chip8Interpreter getChip8Interpreter() {
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
    }

    private void runCycles() {
        for (int i = 0; i < CYCLES_PER_FRAME; i++) {
            this.runCycle();
        }
    }

    private void runCyclesDebug() {
        for (int i = 0; i < CYCLES_PER_FRAME; i++) {
            //CDP1802.State currentState = this.cpu.getCurrentState();
            this.runCycle();
            this.disassembler.disassemble(this.getActualCurrentInstructionAddress());

            // TODO: Handle breakpoints
            /*
            if (currentState == CDP1802.State.S0_FETCH && this.disassembler.checkBreakpoint(this.getActualCurrentInstructionAddress())) {
                //this.jemu.onBreakpoint();
                break;
             */
        }
    }

    private void runCycle() {
        CDP1802.State currentState = this.cpu.getCurrentState();
        this.cycleCpu();
        this.cycleIoDevices();
        this.cpu.nextState();

        CDP1802.State nextState = this.cpu.getCurrentState();
        if (currentState.isS1Execute() && !nextState.isS1Execute()) {
            this.currentInstructionsPerFrame++;
        }
    }

    @Override
    public void executeCycle() {
        this.cycleCpu();
        this.cycleIoDevices();
        this.cpu.nextState();
        //this.display.flush();
        this.disassembler.disassembleRange(this.getActualCurrentInstructionAddress(), 30, true);
    }

    private int getActualCurrentInstructionAddress() {
        int address = this.cpu.getCurrentInstructionAddress();
        return this.bus.isAddressMsbLatched() ? address | 0x8000 : address;
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
            // TODO: Handle backend persistent data
            /*
            if (this.cpu != null) {
                this.cpu.saveRegisters(this.getEmulatorSettings());
            }
             */
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
        debuggerSchema.setMemoryPointerSupplier(() -> this.cpu.getR(this.cpu.getX()));

        Function<Integer, String> byteFormatter = val -> String.format("%02X", val);
        Function<Integer, String> nibbleFormatter = val -> String.format("%01X", val);
        Function<Boolean, String> booleanFormatter = val -> val ? "1" : "0";

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("I")
                .withStateUpdater(this.cpu::getI)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("N")
                .withStateUpdater(this.cpu::getN)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("P")
                .withStateUpdater(this.cpu::getP)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("X")
                .withStateUpdater(this.cpu::getX)
                .withToStringFunction(nibbleFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("D")
                .withStateUpdater(this.cpu::getD)
                .withToStringFunction(byteFormatter);

        debuggerSchema.<Integer>createCpuRegisterEntry()
                .withName("T")
                .withStateUpdater(this.cpu::getT)
                .withToStringFunction(byteFormatter);

        debuggerSchema.<Boolean>createCpuRegisterEntry()
                .withName("DF")
                .withStateUpdater(this.cpu::getDF)
                .withToStringFunction(booleanFormatter);

        debuggerSchema.<Boolean>createCpuRegisterEntry()
                .withName("IE")
                .withStateUpdater(this.cpu::getIE)
                .withToStringFunction(booleanFormatter);

        debuggerSchema.<Boolean>createCpuRegisterEntry()
                .withName("Q")
                .withStateUpdater(this.cpu::getQ)
                .withToStringFunction(booleanFormatter);

        debuggerSchema.setStackSectionName("X Data");

        for (int i = 0; i < 16; i++) {
            int finalI = i;
            debuggerSchema.<Integer>createGeneralPurposeRegisterEntry()
                    .withName(String.format("R%01X", i))
                    .withStateUpdater(() -> this.getCpu().getR(finalI))
                    .withToStringFunction(val -> String.format("%04X", val));

            debuggerSchema.<Integer>createStackEntry()
                    .withName(String.format("%01X", i))
                    .withStateUpdater(() -> this.getBusView().getByte(this.cpu.getR(finalI)))
                    .withToStringFunction(val -> String.format("%02X", val));

        }
        return debuggerSchema;
    }

}
