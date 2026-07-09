package io.github.arkosammy12.jemu.core.hardware;

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
                                throw new EmulatorException("The SM83 CPU has glitched non-deterministially due to a STOP instruction!");
                            } else {
                                this.systemBus.onStopInstructionWithSpeedSwitch(true);
                                machineCycleIndex = TERMINATE_INSTRUCTION;
                            }
                        } else {
                            setPC(getPC() + 1);
                            this.systemBus.onStopInstructionWithSpeedSwitch(true);
                            this.mode = Mode.HALTED;
                            // TODO: Apparently this isn't what happens.
                            // When a speed switch is performed, the GameBoy Color will wait until the system clock has overflowed to 0,
                            // then perform the speed switch. The current solution is the one stated by Liji's STOP instruction flowchart,
                            // which is a good enough approximation for now.
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
                case 3 -> { // Automatically exiting HALT mode
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
