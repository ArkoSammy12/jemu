package io.github.arkosammy12.jemu.core.cpu;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

public class CGBSM83<S extends CGBSM83.SystemBus> extends SM83<S> {

    private int exitHaltTimer;

    public CGBSM83(S systemBus) {
        super(systemBus);
    }

    @Override
    protected void execute() {
        if (getIR() == 0x10) { // STOP
            switch (machineCycleIndex) {
                case 0 -> {
                    if (this.systemBus.isButtonHeld()) {
                        if (this.interruptsPending()) {
                            this.systemBus.onStopInstruction(false);
                            machineCycleIndex = TERMINATE_INSTRUCTION;
                        } else {
                            setPC(getPC() + 1);
                            this.mode = Mode.HALTED;
                            this.systemBus.onStopInstruction(false);
                            machineCycleIndex = 1;
                        }
                    } else if (this.systemBus.isSpeedSwitchRequested()) {
                        if (this.interruptsPending()) {
                            if (this.getIME()) {
                                throw new EmulatorException("The SM83 CPU has non-deterministially glitched due to a STOP instruction!");
                            } else {
                                this.systemBus.onStopInstructionWithSpeedSwitch(true);
                                machineCycleIndex = TERMINATE_INSTRUCTION;
                            }
                        } else {
                            setPC(getPC() + 1);
                            this.systemBus.onStopInstructionWithSpeedSwitch(true);
                            this.mode = Mode.HALTED;
                            this.exitHaltTimer = 32768;
                            machineCycleIndex = 3;
                        }
                    } else if (this.interruptsPending()) {
                        this.mode = Mode.STOPPED;
                        this.systemBus.onStopInstruction(true);
                        machineCycleIndex = 2;
                    } else {
                        setPC(getPC() + 1);
                        this.mode = Mode.STOPPED;
                        this.systemBus.onStopInstruction(true);
                        machineCycleIndex = 2;
                    }
                } case 1 -> {
                    if (interruptsPending()) { // HALT mode
                        this.mode = Mode.EXECUTING;
                        machineCycleIndex = TERMINATE_INSTRUCTION;
                    } else {
                        machineCycleIndex = 1;
                    }
                }
                case 2 -> {
                    if (this.systemBus.isButtonHeld()) { // STOP mode
                        this.mode = Mode.EXECUTING;
                        machineCycleIndex = TERMINATE_INSTRUCTION;
                    } else {
                        machineCycleIndex = 2;
                    }
                }
                case 3 -> { // Automatically existing HALT mode
                    if (this.exitHaltTimer > 0) {
                        this.exitHaltTimer--;
                    }
                    if (interruptsPending() || this.exitHaltTimer <= 0) { // HALT mode
                        this.mode = Mode.EXECUTING;
                        machineCycleIndex = TERMINATE_INSTRUCTION;
                    } else {
                        machineCycleIndex = 3;
                    }
                }
            }
        } else {
            super.execute();
        }
    }

    public interface SystemBus extends SM83.SystemBus {

        boolean isSpeedSwitchRequested();

        void onStopInstructionWithSpeedSwitch(boolean resetDiv);

    }

}
