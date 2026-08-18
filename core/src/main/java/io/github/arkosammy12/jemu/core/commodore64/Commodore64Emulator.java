package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.hardware.NMOS6502;
import io.github.arkosammy12.jemu.core.hardware.NMOS6510;
import io.github.arkosammy12.jemu.core.util.BidirectionalPin;
import io.github.arkosammy12.jemu.core.util.MOSIOPort;

public class Commodore64Emulator implements Emulator, NMOS6510.SystemBus {

    private static final int CPU_CLOCK_DIVISOR = 8;

    private static final int PAL_PHI_IN_HZ = 7_881_990;
    private static final int PAL_FRAMERATE = 50;

    private final Commodore64Host systemHost;

    private final NMOS6510<?> cpu;
    private final Commodore64Bus<?> bus;
    private final MOS6569<?> vic2;
    private final MOS6581 sid;
    private final MOS6526 cia1;
    private final MOS6526 cia2;
    private final Commodore64Controller systemController;

    private final MOSIOPort cpuIOPort;
    private final MOSIOPort cia1IOPortA;
    private final MOSIOPort cia1IOPortB;
    private final MOSIOPort cia2IOPortA;
    private final MOSIOPort cia2IOPortB;

    private final BidirectionalPin cia1SP;
    private final BidirectionalPin cia1CNT;
    private final BidirectionalPin cia2SP;
    private final BidirectionalPin cia2CNT;

    private final int framerate;
    private final int iterationsPerFrame;

    public Commodore64Emulator(Commodore64Host systemHost) {
        this.systemHost = systemHost;

        this.framerate = PAL_FRAMERATE;
        this.iterationsPerFrame = PAL_PHI_IN_HZ / CPU_CLOCK_DIVISOR / this.framerate;

        this.cia1SP = new BidirectionalPin(() -> false);
        this.cia1CNT = new BidirectionalPin(new BidirectionalPin.SystemBus() {

            @Override
            public boolean getBit() {
                return false;
            }

            @Override
            public void clockInput() {
                cia1.clockCNT();
            }

        });

        this.cia2SP = new BidirectionalPin(() -> false);
        this.cia2CNT = new BidirectionalPin(new BidirectionalPin.SystemBus() {

            @Override
            public boolean getBit() {
                return false;
            }

            @Override
            public void clockInput() {
                cia2.clockCNT();
            }

        });

        this.bus = new Commodore64Bus<>(this);
        this.cpu = new NMOS6510<>(this);
        this.vic2 = new MOS6569<>(this);
        this.sid = new MOS6581();
        this.cia1 = new MOS6526(new MOS6526.SystemBus() {

            @Override
            public MOSIOPort getIOPortA() {
                return cia1IOPortA;
            }

            @Override
            public MOSIOPort getIOPortB() {
                return cia1IOPortB;
            }

            @Override
            public boolean getFLAG() {
                return false;
            }

            @Override
            public BidirectionalPin getSP() {
                return cia2SP;
            }

            @Override
            public BidirectionalPin getCNT() {
                return cia1CNT;
            }

        });
        this.cia2 = new MOS6526(new MOS6526.SystemBus() {

            @Override
            public MOSIOPort getIOPortA() {
                return cia2IOPortA;
            }

            @Override
            public MOSIOPort getIOPortB() {
                return cia2IOPortB;
            }

            @Override
            public boolean getFLAG() {
                return false;
            }

            @Override
            public BidirectionalPin getSP() {
                return cia2SP;
            }

            @Override
            public BidirectionalPin getCNT() {
                return cia2CNT;
            }


        });
        this.systemController = new Commodore64Controller();

        this.cpuIOPort = new MOSIOPort(this.cpu, () -> 0b111);

        this.cia1IOPortA = new MOSIOPort(this.cia1.getPortOwnerA(), () -> ~this.systemController.getColumnBits((this.getCIA1IOPortB().getDataDirectionRegister() & ~this.getCIA1IOPortB().getOutputLatch())));
        this.cia1IOPortB = new MOSIOPort(this.cia1.getPortOwnerB(), () -> ~this.systemController.getRowBits((this.getCIA1IOPortA().getDataDirectionRegister() & ~this.getCIA1IOPortA().getOutputLatch())));
        this.cia2IOPortA = new MOSIOPort(this.cia2.getPortOwnerA(), () -> 0xFF);
        this.cia2IOPortB = new MOSIOPort(this.cia2.getPortOwnerB(), () -> 0xFF);
    }

    @Override
    public Commodore64Host getHost() {
        return this.systemHost;
    }

    @Override
    public Commodore64Bus<?> getBus() {
        return this.bus;
    }

    @Override
    public MOS6569<?> getVideoGenerator() {
        return this.vic2;
    }

    @Override
    public MOS6581 getAudioGenerator() {
        return this.sid;
    }

    @Override
    public SystemController getSystemController() {
        return this.systemController;
    }

    public MOS6526 getCIA1() {
        return this.cia1;
    }

    public MOS6526 getCIA2() {
        return this.cia2;
    }

    public MOSIOPort getCPUIOPort() {
        return this.cpuIOPort;
    }

    public MOSIOPort getCIA1IOPortA() {
        return this.cia1IOPortA;
    }

    public MOSIOPort getCIA1IOPortB() {
        return this.cia1IOPortB;
    }

    public MOSIOPort getCIA2IOPortA() {
        return this.cia2IOPortA;
    }

    public MOSIOPort getCia2IOPortB() {
        return this.cia2IOPortB;
    }

    @Override
    public void executeFrame() {
        for (int i = 0; i < this.iterationsPerFrame; i++) {
            this.runCycle();
        }
    }

    @Override
    public void executeCycle() {
        this.runCycle();
    }

    private void runCycle() {
        this.cpu.cycle();
        this.vic2.cycleHalf(NMOS6502.Phase.PHI_1);

        this.cpu.cycle();
        this.vic2.cycleHalf(NMOS6502.Phase.PHI_2);

        this.cia1.cycle();
        this.cia2.cycle();
        this.sid.cycle();
    }

    @Override
    public int getFramerate() {
        return this.framerate;
    }

    @Override
    public boolean getAEC() {
        return this.vic2.getAEC();
    }

    @Override
    public MOSIOPort getIOPort() {
        return this.cpuIOPort;
    }

    @Override
    public boolean getIRQ() {
        return this.vic2.getIRQ() || this.cia1.getIRQ();
    }

    @Override
    public boolean getNMI() {
        return this.cia2.getIRQ() || this.systemController.getRestoreKey();
    }

    @Override
    public boolean getRES() {
        return false;
    }

    @Override
    public boolean getRDY() {
        return this.vic2.getBA();
    }

    @Override
    public void close() throws Exception {

    }

}
