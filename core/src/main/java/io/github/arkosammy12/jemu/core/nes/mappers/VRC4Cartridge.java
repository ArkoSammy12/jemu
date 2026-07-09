package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.util.ToIntBiIntFunction;

import static io.github.arkosammy12.jemu.core.nes.ines.INESFile.KB_2;

public class VRC4Cartridge<E extends NESEmulator> extends VRC2Cartridge<E> {

    private boolean wramControl;
    private boolean swapMode;
    private final VRCIRQEngine vrcirqEngine = new VRCIRQEngine();

    public VRC4Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
    }

    @Override
    protected int getA0Bit() {
        return switch (this.iNESFile.getMapperNumber()) {
            case 21 -> switch (this.iNESFile.getSubmapperNumber()) {
                case 1 -> 1;
                case 2 -> 6;
                default -> throw new ROMInitializationException("Invalid submapper number %d for VRC4!".formatted(this.iNESFile.getSubmapperNumber()));
            };
            case 23 -> switch (this.iNESFile.getSubmapperNumber()) {
                case 1 -> 0;
                case 2 -> 2;
                default -> throw new ROMInitializationException("Invalid submapper number %d for VRC4!".formatted(this.iNESFile.getSubmapperNumber()));
            };
            case 25 -> switch (this.iNESFile.getSubmapperNumber()) {
                case 1 -> 1;
                case 2 -> 3;
                default -> throw new ROMInitializationException("Invalid submapper number %d for VRC4!".formatted(this.iNESFile.getSubmapperNumber()));
            };
            default -> throw new ROMInitializationException("Invalid mapper number %d for VRC4!".formatted(this.iNESFile.getMapperNumber()));
        };
    }

    @Override
    protected int getA1Bit() {
        return switch (this.iNESFile.getMapperNumber()) {
            case 21 -> switch (this.iNESFile.getSubmapperNumber()) {
                case 1 -> 2;
                case 2 -> 7;
                default -> throw new EmulatorException("Invalid submapper number %d for VRC4!".formatted(this.iNESFile.getSubmapperNumber()));
            };
            case 23 -> switch (this.iNESFile.getSubmapperNumber()) {
                case 1 -> 1;
                case 2 -> 3;
                default -> throw new EmulatorException("Invalid submapper number %d for VRC4!".formatted(this.iNESFile.getSubmapperNumber()));
            };
            case 25 -> switch (this.iNESFile.getSubmapperNumber()) {
                case 1 -> 0;
                case 2 -> 2;
                default -> throw new EmulatorException("Invalid submapper number %d for VRC4!".formatted(this.iNESFile.getSubmapperNumber()));
            };
            default -> throw new EmulatorException("Invalid mapper number %d for VRC4!".formatted(this.iNESFile.getMapperNumber()));
        };
    }

    @Override
    protected ToIntBiIntFunction getSetChrSelectLowFunction() {
        return (chrSelect, value) -> (chrSelect & 0b111110000) | (value & 0b1111);
    }

    @Override
    protected ToIntBiIntFunction getSetChrSelectHighFunction() {
        return (chrSelect, value) -> ((value & 0b11111) << 4) | (chrSelect & 0b000001111);
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null && this.wramControl) {
                if (this.programRAM.length <= KB_2) {
                    if (address <= 0x6FFF) {
                        return (int) this.programRAM[(address & 0x7FF) % this.programRAM.length] & 0xFF;
                    } else {
                        return -1;
                    }
                } else {
                    return (int) this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] & 0xFF;
                }
            } else {
                return -1;
            }
        } else {
            return super.readByte(address);
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null && this.wramControl) {
                if (this.programRAM.length <= KB_2) {
                    if (address <= 0x6FFF) {
                        this.programRAM[(address & 0x7FF) % this.programRAM.length] = (byte) value;
                    }
                } else {
                    this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] = (byte) value;
                }
            }
        } else if (address >= 0x9000 && address <= 0x9FFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.nametableArrangement = switch (value & 0b11) {
                    case 0 -> NametableArrangement.HORIZONTAL;
                    case 1 -> NametableArrangement.VERTICAL;
                    case 2 -> NametableArrangement.SINGLE_SCREEN_LOWER_BANK;
                    case 3 -> NametableArrangement.SINGLE_SCREEN_UPPER_BANK;
                    default -> throw new EmulatorException("Invalid mirroring value %d for VRC4 $9002 write!".formatted(value & 0b11));
                };
                case 2 -> {
                    this.wramControl = (value & 1) != 0;
                    this.swapMode = (value & 0b10) != 0;
                }
            }
        } else if (address >= 0xF000 && address <= 0xFFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.vrcirqEngine.writeIRQLatchLow(value);
                case 1 -> this.vrcirqEngine.writeIRQLatchHigh(value);
                case 2 -> this.vrcirqEngine.writeIRQControl(value);
                case 3 -> this.vrcirqEngine.writeIRQAcknowledge();
            }
        } else {
            super.writeByte(address, value);
        }
    }

    protected int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (address <= 0x1FFF) {
            if (this.swapMode) {
                return (address & 0x1FFF) | (0b11110 << 13);
            } else {
                return (address & 0x1FFF) | (this.prgSelect0 << 13);
            }
        } else if (address <= 0x3FFF) {
            return (address & 0x1FFF) | (this.prgSelect1 << 13);
        } else if (address <= 0x5FFF) {
            if (this.swapMode) {
                return (address & 0x1FFF) | (this.prgSelect0 << 13);
            } else {
                return (address & 0x1FFF) | (0b11110 << 13);
            }
        } else {
            return (address & 0x1FFF) | (0b11111 << 13);
        }
    }

    @Override
    public void cycle() {
        this.vrcirqEngine.cycle();
    }

    @Override
    public boolean getIRQSignal() {
        return this.vrcirqEngine.getIRQSignal();
    }

    public static class VRCIRQEngine {

        private int irqLatch;
        private boolean irqEnableAfterAcknowledgement;
        private boolean irqEnable;
        private boolean irqMode;

        private boolean irqSignal;

        private int prescaler = 341;
        private int irqCounter;

        public void writeIRQLatch(int value) {
            this.irqLatch = value & 0xFF;
        }

        public void writeIRQLatchLow(int value) {
            this.irqLatch = (this.irqLatch & 0xF0) | (value & 0xF);
        }

        public void writeIRQLatchHigh(int value) {
            this.irqLatch = ((value & 0xF) << 4) | (this.irqLatch & 0xF);
        }

        public void writeIRQControl(int value) {
            this.irqEnableAfterAcknowledgement = (value & 1) != 0;
            this.irqEnable = (value & (1 << 1)) != 0;
            this.irqMode = (value & (1 << 2)) != 0;

            this.irqSignal = false;
            this.prescaler = 341;
            if (this.irqEnable) {
                this.irqCounter = this.irqLatch;
            }
        }

        public void writeIRQAcknowledge() {
            this.irqSignal = false;
            this.irqEnable = this.irqEnableAfterAcknowledgement;
        }

        public void cycle() {
            if (this.irqMode) {
                if (this.irqEnable) {
                    this.clockIRQCounter();
                }
            } else {
                if (this.irqEnable) {
                    this.prescaler -= 3;
                    if (this.prescaler <= 0) {
                        this.prescaler += 341;
                        this.clockIRQCounter();
                    }
                }
            }
        }

        private void clockIRQCounter() {
            if (this.irqCounter >= 0xFF) {
                this.irqCounter = this.irqLatch;
                this.irqSignal = true;
            } else {
                this.irqCounter = (this.irqCounter + 1) & 0xFF;
            }
        }

        public boolean getIRQSignal() {
            return this.irqSignal;
        }

    }

}
