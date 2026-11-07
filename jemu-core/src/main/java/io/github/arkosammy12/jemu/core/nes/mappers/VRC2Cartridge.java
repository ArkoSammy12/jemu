package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.nes.ines.NES20File;
import io.github.arkosammy12.jemu.core.util.ToIntBiIntFunction;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class VRC2Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final int a0Bit;
    private final int a1Bit;

    private final ToIntBiIntFunction setChrSelectLowFunction;
    private final ToIntBiIntFunction setChrSelectHighFunction;

    protected int prgSelect0;
    protected int prgSelect1;
    private int chrSelect0;
    private int chrSelect1;
    private int chrSelect2;
    private int chrSelect3;
    private int chrSelect4;
    private int chrSelect5;
    private int chrSelect6;
    private int chrSelect7;

    protected NametableArrangement nametableArrangement = NametableArrangement.HORIZONTAL;
    private int latch;

    public VRC2Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
        this.a0Bit = this.getA0Bit();
        this.a1Bit = this.getA1Bit();
        this.setChrSelectLowFunction = this.getSetChrSelectLowFunction();
        this.setChrSelectHighFunction = this.getSetChrSelectHighFunction();
    }

    protected int getA0Bit() {
        return switch (this.iNESFile.getMapperNumber()) {
            case 23 -> 0;
            case 22, 25 -> 1;
            default -> throw new EmulatorException("Invalid mapper number %d for VRC2!".formatted(this.iNESFile.getMapperNumber()));
        };
    }

    protected int getA1Bit() {
        return switch (this.iNESFile.getMapperNumber()) {
            case 23 -> 1;
            case 22, 25 -> 0;
            default -> throw new EmulatorException("Invalid mapper number %d for VRC2!".formatted(this.iNESFile.getMapperNumber()));
        };
    }

    protected ToIntBiIntFunction getSetChrSelectLowFunction() {
        if (this.iNESFile.getMapperNumber() == 22) {
            return (chrSelect, value) -> (chrSelect & 0b011111000) | ((value >>> 1) & 0b111);
        } else {
            return (chrSelect, value) -> (chrSelect & 0b111110000) | (value & 0b1111);
        }
    }

    protected ToIntBiIntFunction getSetChrSelectHighFunction() {
        // VRC2 only has 4 high bits of CHR select
        if (this.iNESFile.getMapperNumber() == 22) {
            return (chrSelect, value) -> ((value & 0b01111) << 3) | (chrSelect & 0b000000111);
        } else {
            return (chrSelect, value) -> ((value & 0b01111) << 4) | (chrSelect & 0b000001111);
        }
    }

    @Override
    public int readBytePPU(int address) {
        this.observePPUAddress(address);
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterROM == null) {
                return (int) this.characterRAM[this.mapChrAddress(address) % this.characterRAM.length] & 0xFF;
            } else {
                return (int) this.characterROM[this.mapChrAddress(address) % this.characterROM.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES VRC2 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        this.observePPUAddress(address);
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterRAM != null) {
                this.characterRAM[this.mapChrAddress(address) % this.characterRAM.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES VRC2 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, this.nametableArrangement);
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null) {
                return (int) this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] & 0xFF;
            } else if (address <= 0x6FFF) {
                return (this.emulator.getCpuBus().getExternalDataBus() & 0xFE) | this.latch;
            } else {
                return -1;
            }
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programROM[this.mapPrgRomAddress(address) % this.programROM.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null) {
                this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] = (byte) value;
            } else if (address <= 0x6FFF) {
                this.latch = value & 1;
            }
        } else if (address >= 0x8000 && address <= 0x8FFF) {
            if (this.getRegisterSlot(address) <= 3) {
                this.prgSelect0 = value & 0x1F;
            }
        } else if (address >= 0x9000 && address <= 0x9FFF) {
            if (this.getRegisterSlot(address) <= 3) {
                // VRC2 ignores bit 1
                this.nametableArrangement = (value & 1) != 0 ? NametableArrangement.VERTICAL : NametableArrangement.HORIZONTAL;
            }
        } else if (address >= 0xA000 && address <= 0xAFFF) {
            if (this.getRegisterSlot(address) <= 3) {
                this.prgSelect1 = value & 0x1F;
            }
        } else if (address >= 0xB000 && address <= 0xEFFF) {
            switch (address & 0xF000) {
                case 0xB000 -> {
                    switch (this.getRegisterSlot(address)) {
                        case 0 -> this.chrSelect0 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect0, value);
                        case 1 -> this.chrSelect0 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect0, value);
                        case 2 -> this.chrSelect1 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect1, value);
                        case 3 -> this.chrSelect1 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect1, value);
                    }
                }
                case 0xC000 -> {
                    switch (this.getRegisterSlot(address)) {
                        case 0 -> this.chrSelect2 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect2, value);
                        case 1 -> this.chrSelect2 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect2, value);
                        case 2 -> this.chrSelect3 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect3, value);
                        case 3 -> this.chrSelect3 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect3, value);
                    }
                }
                case 0xD000 -> {
                    switch (this.getRegisterSlot(address)) {
                        case 0 -> this.chrSelect4 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect4, value);
                        case 1 -> this.chrSelect4 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect4, value);
                        case 2 -> this.chrSelect5 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect5, value);
                        case 3 -> this.chrSelect5 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect5, value);
                    }
                }
                case 0xE000 -> {
                    switch (this.getRegisterSlot(address)) {
                        case 0 -> this.chrSelect6 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect6, value);
                        case 1 -> this.chrSelect6 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect6, value);
                        case 2 -> this.chrSelect7 = this.setChrSelectLowFunction.applyAsInt(this.chrSelect7, value);
                        case 3 -> this.chrSelect7 = this.setChrSelectHighFunction.applyAsInt(this.chrSelect7, value);
                    }
                }
            }
        }
    }

    protected int getRegisterSlot(int address) {
        return ((address >> this.a0Bit) & 1) | (((address >> this.a1Bit) & 1) << 1);
    }

    private int mapChrAddress(int address) {
        address &= 0x1FFF;
        if (address <= 0x03FF) {
            return (address & 0x3FF) | (this.chrSelect0 << 10);
        } else if (address <= 0x07FF) {
            return (address & 0x3FF) | (this.chrSelect1 << 10);
        } else if (address <= 0x0BFF) {
            return (address & 0x3FF) | (this.chrSelect2 << 10);
        } else if (address <= 0x0FFF) {
            return (address & 0x3FF) | (this.chrSelect3 << 10);
        } else if (address <= 0x13FF) {
            return (address & 0x3FF) | (this.chrSelect4 << 10);
        } else if (address <= 0x17FF) {
            return (address & 0x3FF) | (this.chrSelect5 << 10);
        } else if (address <= 0x1BFF) {
            return (address & 0x3FF) | (this.chrSelect6 << 10);
        } else {
            return (address & 0x3FF) | (this.chrSelect7 << 10);
        }
    }

    protected int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (address <= 0x1FFF) {
            return (address & 0x1FFF) | (this.prgSelect0 << 13);
        } else if (address <= 0x3FFF) {
            return (address & 0x1FFF) | (this.prgSelect1 << 13);
        } else if (address <= 0x5FFF) {
            return (address & 0x1FFF) | (0b11110 << 13);
        } else {
            return (address & 0x1FFF) | (0b11111 << 13);
        }
    }

    protected int mapPrgRamAddress(int address) {
        return address & 0x1FFF;
    }

    @Override
    protected Optional<byte[]> getNonVolatilePrgRam() {
        return Optional.ofNullable(this.programRAM);
    }

    @Override
    protected Optional<byte[]> getNonVolatileChrRam() {
        return Optional.ofNullable(this.characterRAM);
    }

}
