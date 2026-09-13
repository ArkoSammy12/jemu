package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.commodore64.crt.CRTFile;
import io.github.arkosammy12.jemu.core.commodore64.tape.Commodore1531;
import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import io.github.arkosammy12.jemu.core.hardware.NMOS6502;
import io.github.arkosammy12.jemu.core.hardware.NMOS6510;
import io.github.arkosammy12.jemu.core.util.BidirectionalPin;
import io.github.arkosammy12.jemu.core.util.MOSIOPort;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Commodore64Emulator implements Emulator, NMOS6510.SystemBus {

    private static final int FRAMES_UNTIL_READY_PROMPT = 111;

    private static final int PAL_CPU_FREQUENCY_HZ = 985248;
    public static final int PAL_CPU_CYCLES_PER_FRAME = MOS6569.SCANLINES_PER_FRAME * MOS6569.CYCLES_PER_SCANLINE;
    private static final double PAL_FRAMERATE = (double) PAL_CPU_FREQUENCY_HZ / PAL_CPU_CYCLES_PER_FRAME;

    private final Commodore64Host systemHost;

    private final NMOS6510<?> cpu;
    private final Commodore64Bus<?> bus;
    private final MOS6569<?> vic2;
    private final MOS6581<?> sid;
    private final MOS6526 cia1;
    private final MOS6526 cia2;
    private final Commodore64Controller<?> systemController;
    private final ExpansionDevice expansionDevice;
    private final Commodore1531 commodore1531;

    private final MOSIOPort cpuIOPort;
    private final MOSIOPort cia1IOPortA;
    private final MOSIOPort cia1IOPortB;
    private final MOSIOPort cia2IOPortA;
    private final MOSIOPort cia2IOPortB;

    private final BidirectionalPin cia1SP;
    private final BidirectionalPin cia1CNT;
    private final BidirectionalPin cia2SP;
    private final BidirectionalPin cia2CNT;

    private boolean prgFilePatchAttempted;
    private int frames;

    public Commodore64Emulator(Commodore64Host systemHost) {
        this.systemHost = systemHost;

        this.commodore1531 = new Commodore1531(this);

        Optional<Path> optionalROMPath = this.systemHost.getRomPath();
        Optional<byte[]> bytes = this.systemHost.getRom();
        if (bytes.isPresent() && optionalROMPath.isEmpty()) {
            throw new ROMInitializationException("ROM path missing! Supported file types are :" + FileType.getFileExtensionsString());
        }

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
        this.sid = new MOS6581<>(this);
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
                return commodore1531.getREAD();
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
        this.systemController = new Commodore64Controller<>(this);

        this.cpuIOPort = new MOSIOPort(this.cpu, () -> 0b100111 | (this.commodore1531.getSENSE() ? 0 : 1 << 4));

        this.cia1IOPortA = new MOSIOPort(this.cia1.getPortOwnerA(), () -> ~this.systemController.getColumnBits((this.getCIA1IOPortB().getDataDirectionRegister() & ~this.getCIA1IOPortB().getOutputLatch())));
        this.cia1IOPortB = new MOSIOPort(this.cia1.getPortOwnerB(), () -> {
            int rowBits = this.systemController.getRowBits((this.getCIA1IOPortA().getDataDirectionRegister() & ~this.getCIA1IOPortA().getOutputLatch()));
            int joystick1Bits = this.systemController.getJoystick1Bits();
            return ~(rowBits | joystick1Bits);
        });
        this.cia2IOPortA = new MOSIOPort(this.cia2.getPortOwnerA(), () -> 0xFF);
        this.cia2IOPortB = new MOSIOPort(this.cia2.getPortOwnerB(), () -> 0xFF);

        ExpansionDevice expansionDevice = (_, _) -> bus.combineWithDataBus(0x00, 0x00);

        if (bytes.isPresent()) {
            Path path = optionalROMPath.get();
            String extension = FilenameUtils.getExtension(path.toString());
            switch (FileType.getFileTypeForExtension(extension)) {
                case PRG -> this.bus.loadPrgFile(bytes.get());
                case CRT -> expansionDevice = Commodore64Cartridge.getCartridge(this, new CRTFile(bytes.get()));
                case null -> throw new ROMInitializationException("The ROM file extension \"%s\" is not supported! Supported file types are: %s".formatted(extension, FileType.getFileExtensionsString()));
            }
        }

        this.expansionDevice = expansionDevice;

        this.systemHost.getTapeImagePath().ifPresent(this.commodore1531::updateTapeImage);

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
    public MOS6581<?> getAudioGenerator() {
        return this.sid;
    }

    @Override
    public Commodore64Controller<?> getSystemController() {
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

    public ExpansionDevice getExpansionPortDevice() {
        return this.expansionDevice;
    }

    public Commodore1531 getDatasette() {
        return this.commodore1531;
    }

    @Override
    public void executeFrame() {
        for (int i = 0; i < PAL_CPU_CYCLES_PER_FRAME; i++) {
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

        this.commodore1531.cycle();

        this.cia1.cycle();
        this.cia2.cycle();
        this.sid.cycle();

        this.expansionDevice.cyclePHI2();
        this.expansionDevice.cycleDot();
        this.expansionDevice.cycleDot();
        this.expansionDevice.cycleDot();
        this.expansionDevice.cycleDot();
        this.expansionDevice.cycleDot();
        this.expansionDevice.cycleDot();
        this.expansionDevice.cycleDot();
        this.expansionDevice.cycleDot();
    }

    public void onVBlank() {
        this.cia1.clockTOD();
        this.cia2.clockTOD();

        if (!this.prgFilePatchAttempted) {
            this.frames++;
            if (this.frames >= FRAMES_UNTIL_READY_PROMPT) {
                this.bus.patchPrgFile();
                this.systemHost.onPrgFilePatched();
                this.prgFilePatchAttempted = true;
            }
        }
    }

    public void updateTapeImage(@Nullable Path tapeImagePath) {
        this.commodore1531.updateTapeImage(tapeImagePath);
    }

    @Override
    public double getFramerate() {
        return PAL_FRAMERATE;
    }

    @Override
    public boolean getAEC() {
        return this.vic2.getAEC() || this.expansionDevice.getDMA();
    }

    @Override
    public MOSIOPort getIOPort() {
        return this.cpuIOPort;
    }

    @Override
    public boolean getIRQ() {
        return this.vic2.getIRQ() || this.cia1.getIRQ() || this.expansionDevice.getIRQ();
    }

    @Override
    public boolean getNMI() {
        return this.cia2.getIRQ() || this.systemController.getRestoreKey() || this.expansionDevice.getNMI();
    }

    @Override
    public boolean getRES() {
    return this.expansionDevice.getRESET();
    }

    @Override
    public boolean getRDY() {
        return this.vic2.getBA() || this.expansionDevice.getDMA();
    }

    public boolean getWRITE() {
        return (this.cpuIOPort.read() & (1 << 3)) != 0;
    }

    public boolean getMOTOR() {
        return (this.cpuIOPort.read() & (1 << 5)) == 0;
    }

    @Override
    public void close() throws Exception {

    }

    public enum FileType {
        PRG("prg"),
        CRT("crt");

        private final String fileExtension;

        FileType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getFileExtension() {
            return this.fileExtension;
        }

        public static List<String> getFileExtensions() {
            return Arrays.stream(Commodore64Emulator.FileType.values()).map(Commodore64Emulator.FileType::getFileExtension).toList();
        }

        public static String getFileExtensionsString() {
            return Arrays.stream(Commodore64Emulator.FileType.values()).map(Commodore64Emulator.FileType::getFileExtension).map(extension -> "." + extension).collect(Collectors.joining(", "));
        }

        @Nullable
        public static FileType getFileTypeForExtension(String extension) {
            for (FileType fileType : FileType.values()) {
                if (fileType.getFileExtension().equalsIgnoreCase(extension)) {
                    return fileType;
                }
            }
            return null;
        }

    }

}
