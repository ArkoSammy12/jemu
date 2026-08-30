package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.commodore64.crt.CRTFile;
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

    private static final int CPU_CLOCK_DIVISOR = 8;

    private static final int PAL_PHI_IN_HZ = 7_862_400;
    private static final int PAL_FRAMERATE = 50;

    private final Commodore64Host systemHost;

    private final NMOS6510<?> cpu;
    private final Commodore64Bus<?> bus;
    private final MOS6569<?> vic2;
    private final MOS6581<?> sid;
    private final MOS6526 cia1;
    private final MOS6526 cia2;
    private final Commodore64Controller systemController;
    private final ExpansionPortDevice expansionPortDevice;

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

    private boolean prgFilePatchAttempted;
    private int frames;

    public Commodore64Emulator(Commodore64Host systemHost) {
        this.systemHost = systemHost;

        Optional<Path> optionalROMPath = this.systemHost.getRomPath();
        Optional<byte[]> bytes = this.systemHost.getRom();
        if (bytes.isPresent() && optionalROMPath.isEmpty()) {
            throw new ROMInitializationException("ROM path missing! Supported file types are :" + FileType.getFileExtensionsString());
        }

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
        this.sid = new MOS6581<>(this, this.iterationsPerFrame);
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

        this.cpuIOPort = new MOSIOPort(this.cpu, () -> 0b10111);

        this.cia1IOPortA = new MOSIOPort(this.cia1.getPortOwnerA(), () -> ~this.systemController.getColumnBits((this.getCIA1IOPortB().getDataDirectionRegister() & ~this.getCIA1IOPortB().getOutputLatch())));
        this.cia1IOPortB = new MOSIOPort(this.cia1.getPortOwnerB(), () -> {
            int rowBits = this.systemController.getRowBits((this.getCIA1IOPortA().getDataDirectionRegister() & ~this.getCIA1IOPortA().getOutputLatch()));
            int joystick1Bits = this.systemController.getJoystick1Bits();
            return ~(rowBits | joystick1Bits);
        });
        this.cia2IOPortA = new MOSIOPort(this.cia2.getPortOwnerA(), () -> 0xFF);
        this.cia2IOPortB = new MOSIOPort(this.cia2.getPortOwnerB(), () -> 0xFF);

        ExpansionPortDevice expansionPortDevice = (_, _) -> bus.combineWithDataBus(0x00, 0x00);

        if (bytes.isPresent()) {
            Path path = optionalROMPath.get();
            String extension = FilenameUtils.getExtension(path.toString());
            switch (FileType.getFileTypeForExtension(extension)) {
                case PRG -> this.bus.loadPrgFile(bytes.get());
                case CRT -> expansionPortDevice = Commodore64Cartridge.getCartridge(this, new CRTFile(bytes.get()));
                case null -> throw new ROMInitializationException("The ROM file extension \"%s\" is not supported! Supported file types are: %s".formatted(extension, FileType.getFileExtensionsString()));
            }
        }

        this.expansionPortDevice = expansionPortDevice;

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

    public ExpansionPortDevice getExpansionPortDevice() {
        return this.expansionPortDevice;
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

        this.expansionPortDevice.cyclePHI2();
        this.expansionPortDevice.cycleDot();
        this.expansionPortDevice.cycleDot();
        this.expansionPortDevice.cycleDot();
        this.expansionPortDevice.cycleDot();
        this.expansionPortDevice.cycleDot();
        this.expansionPortDevice.cycleDot();
        this.expansionPortDevice.cycleDot();
        this.expansionPortDevice.cycleDot();
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

    @Override
    public int getFramerate() {
        return this.framerate;
    }

    @Override
    public boolean getAEC() {
        return this.vic2.getAEC() || this.expansionPortDevice.getDMA();
    }

    @Override
    public MOSIOPort getIOPort() {
        return this.cpuIOPort;
    }

    @Override
    public boolean getIRQ() {
        return this.vic2.getIRQ() || this.cia1.getIRQ() || this.expansionPortDevice.getIRQ();
    }

    @Override
    public boolean getNMI() {
        return this.cia2.getIRQ() || this.systemController.getRestoreKey() || this.expansionPortDevice.getNMI();
    }

    @Override
    public boolean getRES() {
    return this.expansionPortDevice.getRESET();
    }

    @Override
    public boolean getRDY() {
        return this.vic2.getBA() || this.expansionPortDevice.getDMA();
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
