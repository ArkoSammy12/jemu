package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.*;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.util.ByteSizes.KB_1;

public class MMC5Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final byte[] externalRAM = new byte[KB_1];

    private boolean spriteSize;
    private boolean substitutionsEnabled;
    private boolean triggerScanlineCounterReset;

    private int prgBankingMode = 0b11;
    private int chrBankingMode;

    private boolean ramProtect1 = false;
    private boolean ramProtect2 = false;

    private int extendedRAMmode = 0b11;

    private int nametableAt2000;
    private int nametableAt2400;
    private int nametableAt2800;
    private int nametableAt2C00;

    private int fillModeTile;
    private int fillModeNametableBackgroundPaletteIndex;

    private int prgBank5113;
    private int prgBank5114;
    private int prgBank5115;
    private int prgBank5116;
    private int prgBank5117 = 0xFF;

    private int chrSelect5210;
    private int chrSelect5211;
    private int chrSelect5212;
    private int chrSelect5213;
    private int chrSelect5214;
    private int chrSelect5215;
    private int chrSelect5216;
    private int chrSelect5217;
    private int chrSelect5218;
    private int chrSelect5219;
    private int chrSelect521A;
    private int chrSelect521B;
    private int upperChrBankBits = 0b00;

    private int verticalSplitThresholdTileCount = 0b00000;
    private VerticalSplitRegionScreenSide verticalSplitRegionScreenSide = VerticalSplitRegionScreenSide.LEFT;
    private boolean enableVerticalSplitMode = false;

    private int verticalSplitScroll = 0x00;

    private int verticalSplitBank;

    private int scanlineCompare;
    private boolean enableScanlineIrq = false;

    private boolean inFrame;
    private boolean scanlineIrqPending;

    private int multiplicand;
    private int multiplier;
    private int productLowByte;
    private int productHighByte;

    private PrgSelect prgSelect = PrgSelect.ROM;

    private int lastAddress;
    private boolean ppuIsReading;
    private int matchCount;
    private int scanline;
    private int idleCount = 3;

    private boolean enablePCMIrq = false;
    private PCMMode pcmMode = PCMMode.WRITE;

    public MMC5Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
    }

    @Override
    public int readBytePPU(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterROM == null) {

            } else {

            }
            return 0;
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            if (address <= 0x2FFF && address == this.lastAddress) {
                this.matchCount++;
                if (this.matchCount >= 2) {
                    if (!this.inFrame) {
                        this.inFrame = true;
                        this.scanline = 0;
                    } else {
                        this.scanline++;
                        if (this.scanline == this.scanlineCompare) {
                            this.scanlineIrqPending = true;
                        }
                    }
                }
            } else {
                this.matchCount = 0;
            }
            this.lastAddress = address;
            this.ppuIsReading = true;
            return 0;
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES MMC3 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {

        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES MMC3 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public int readByte(int address, int dataBus) {
        return switch (address) {
            case 0x2002 -> {
                if ((dataBus & (1 << 7)) != 0) {
                    this.triggerScanlineCounterReset = true;
                }
                yield -1;
            }
            case 0x5010 -> {
                int ret = (dataBus & 0b01111110) /*| (this.pcmChannel.irqTriggered() ? 1 << 7 : 0) */;
                // TODO: this.pcmChannel.acknowledgeIrq();
                yield ret;
            }
            case 0x5011 -> {
                if (this.pcmMode == PCMMode.READ) {
                    // TODO: Write 8-bit PCM data
                }
                yield dataBus;
            }
            case 0x5204 -> {
                int ret = 1;
                ret |= this.scanlineIrqPending ? 1 << 7 : 0;
                ret |= this.inFrame ? 1 << 6 : 0;
                this.scanlineIrqPending = false;
                yield ret;
            }
            case 0x5205 -> this.productLowByte;
            case 0x5206 -> this.productHighByte;
            default -> {
                if (address >= 0x5C00 && address <= 0x5FFF) {
                    yield -1;
                } else if (address >= 0x6000 && address <= 0xFFFF) {
                    switch (address) {
                        case 0xFFFA, 0xFFFB -> this.inFrame = false;
                    }
                    int index = this.mapPrgAddress(address);
                    byte[] prgChip = switch (this.prgSelect) {
                        case ROM -> this.programROM;
                        case RAM -> this.programRAM;
                    };
                    if (prgChip == null) {
                        yield -1;
                    } else {
                        yield (int) prgChip[index % prgChip.length] & 0xFF;
                    }
                } else {
                    yield -1;
                }
            }
        };
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case 0x2000 -> this.spriteSize = (value & (1 << 5)) != 0;
            case 0x2001 -> {
                boolean originalSubstitutionsEnabled = this.substitutionsEnabled;
                this.substitutionsEnabled = (value & 0b00011000) != 0;
                if (this.substitutionsEnabled) {
                    if (!originalSubstitutionsEnabled) {
                        this.scanline = 0;
                    }
                } else {
                    this.inFrame = false;
                }
            }
            case 0x2005 -> {} // Unknown ($2005 = PPUSCROLL). Reset detection is used
            case 0x2006 -> {} // Unknown ($2006 = PPUADDR, MMC5A only)
            case 0x4014 -> this.triggerScanlineCounterReset = true; // It is believed that writing to this also clears the in-frame flag, acknowledges the IRQ, and the internal scanline counter is reset
            case 0x5000 -> {} // Pulse 1 volume
            case 0x5001 -> {} // Pulse 1 sweep
            case 0x5002 -> {} // Pulse 1 low
            case 0x5003 -> {} // Pulse 1 high
            case 0x5004 -> {} // Pulse 2 volume
            case 0x5005 -> {} // Pulse 2 sweep
            case 0x5006 -> {} // Pulse 2 low
            case 0x5007 -> {} // Pulse 2 high
            case 0x5010 -> {
                this.pcmMode = (value & 1) != 0 ? PCMMode.READ : PCMMode.WRITE;
                this.enablePCMIrq = (value & (1 << 7)) != 0;
            }
            case 0x5011 -> {
                if (this.pcmMode == PCMMode.WRITE) {
                    // TODO: Write 8-bit PCM data
                }
            }
            case 0x5100 -> this.prgBankingMode = value & 0b11;
            case 0x5101 -> this.chrBankingMode = value & 0b11;
            case 0x5102 -> this.ramProtect1 = (value & 0b11) == 0b10;
            case 0x5103 -> this.ramProtect2 = (value & 0b11) == 0b01;
            case 0x5104 -> this.extendedRAMmode = value & 0b11;
            case 0x5105 -> {
                this.nametableAt2000 = value & 0b11;
                this.nametableAt2400 = (value >>> 2) & 0b11;
                this.nametableAt2800 = (value >>> 4) & 0b11;
                this.nametableAt2C00 = (value >>> 6) & 0b11;
            }
            case 0x5106 -> this.fillModeTile = value & 0xFF;
            case 0x5107 -> this.fillModeNametableBackgroundPaletteIndex = value & 0b11;
            case 0x5113 -> this.prgBank5113 = value & 0xFF;
            case 0x5114 -> this.prgBank5114 = value & 0xFF;
            case 0x5115 -> this.prgBank5115 = value & 0xFF;
            case 0x5116 -> this.prgBank5116 = value & 0xFF;
            case 0x5117 -> this.prgBank5117 = value & 0xFF;
            case 0x5120 -> this.chrSelect5210 = this.computeChrSelectRegisterWrite(value);
            case 0x5121 -> this.chrSelect5211 = this.computeChrSelectRegisterWrite(value);
            case 0x5122 -> this.chrSelect5212 = this.computeChrSelectRegisterWrite(value);
            case 0x5123 -> this.chrSelect5213 = this.computeChrSelectRegisterWrite(value);
            case 0x5124 -> this.chrSelect5214 = this.computeChrSelectRegisterWrite(value);
            case 0x5125 -> this.chrSelect5215 = this.computeChrSelectRegisterWrite(value);
            case 0x5126 -> this.chrSelect5216 = this.computeChrSelectRegisterWrite(value);
            case 0x5127 -> this.chrSelect5217 = this.computeChrSelectRegisterWrite(value);
            case 0x5128 -> this.chrSelect5218 = this.computeChrSelectRegisterWrite(value);
            case 0x5129 -> this.chrSelect5219 = this.computeChrSelectRegisterWrite(value);
            case 0x512A -> this.chrSelect521A = this.computeChrSelectRegisterWrite(value);
            case 0x512B -> this.chrSelect521B = this.computeChrSelectRegisterWrite(value);
            case 0x5130 -> this.upperChrBankBits = value & 0b11;
            case 0x5200 -> {
                this.verticalSplitThresholdTileCount = value & 0b11111;
                this.verticalSplitRegionScreenSide = (value & (1 << 6)) != 0 ? VerticalSplitRegionScreenSide.RIGHT : VerticalSplitRegionScreenSide.LEFT;
                this.enableVerticalSplitMode = (value & (1 << 7)) != 0;
            }
            case 0x5201 -> this.verticalSplitScroll = value & 0xFF;
            case 0x5202 -> this.verticalSplitBank = value & 0xFF;
            case 0x5203 -> this.scanlineCompare = value & 0xFF;
            case 0x5204 -> this.enableScanlineIrq = (value & (1 << 7)) != 0;
            case 0x5205 -> {
                this.multiplicand = value & 0xFF;
                this.updateProduct();
            }
            case 0x5206 -> {
                this.multiplier = value & 0xFF;
                this.updateProduct();
            }
            default -> {
                if (address >= 0x5C00 && address <= 0x5FFF) {

                } else if (address >= 0x6000 && address <= 0xFFFF) {
                    int index = this.mapPrgAddress(address);
                    byte[] prgChip = switch (this.prgSelect) {
                        case ROM -> this.programROM;
                        case RAM -> this.prgRAMWritesEnabled() ? this.programRAM : null;
                    };
                    if (prgChip != null) {
                        prgChip[index % prgChip.length] = (byte) value;
                    }
                }
            }
        }
    }

    @Override
    public void onReset() {
        this.prgBankingMode = 0b11;
        this.ramProtect1 = false;
        this.ramProtect2 = false;
        this.extendedRAMmode = 0b11;
        this.prgBank5117 = 0xFF;
        this.upperChrBankBits = 0b00;
        this.verticalSplitThresholdTileCount = 0b00000;
        this.verticalSplitRegionScreenSide =  VerticalSplitRegionScreenSide.LEFT;
        this.enableVerticalSplitMode = false;
        this.verticalSplitScroll = 0x00;
        this.enableScanlineIrq = false;
        this.enablePCMIrq = false;
        this.pcmMode = PCMMode.WRITE;
    }

    @Override
    public void cycle() {
        if (this.ppuIsReading) {
            this.idleCount = 0;
        } else {
            this.idleCount++;
            if (this.idleCount >= 3) {
                this.inFrame = false;
            }
        }
        this.ppuIsReading = false;
    }

    @Override
    public boolean getIRQSignal() {
        return this.enableScanlineIrq && this.scanlineIrqPending;
    }

    private int computeChrSelectRegisterWrite(int data) {
        int value = (this.upperChrBankBits << 8) | (data & 0xFF);
        switch (this.chrBankingMode) {
            // TODO: Figure out the grey boxes
            case 0 -> value = (value << 3) | (data & 1);
            case 1 -> value = (value << 2) | (data & 1);
            case 2 -> value = (value << 1) | (data & 1);
        }
        return value & 0b1111111111;
    }

    private void updateProduct() {
        int product = this.multiplicand * this.multiplier;
        this.productLowByte = product & 0xFF;
        this.productHighByte = (product >>> 8) & 0xFF;
    }

    private boolean prgRAMWritesEnabled() {
        return this.ramProtect1 && this.ramProtect2;
    }

    private int mapPrgAddress(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            this.prgSelect = PrgSelect.RAM;
            return (address & 0x1FFF) | ((this.prgBank5113 & 0b1111) << 13);
        } else {
            return switch (this.prgBankingMode) {
                case 0 -> {
                    this.prgSelect = PrgSelect.ROM;
                    yield (address & 0x7FFF) | ((this.prgBank5117 & 0b01111100) << 13);
                }
                case 1 -> {
                    if (address >= 0x8000 && address <= 0xBFFF) {
                        this.prgSelect = (this.prgBank5115 & (1 << 7)) != 0 ? PrgSelect.ROM : PrgSelect.RAM;
                        yield (address & 0x3FFF) | ((this.prgBank5115 & 0b01111110) << 13);
                    } else {
                        this.prgSelect = PrgSelect.ROM;
                        yield (address & 0x3FFF) | ((this.prgBank5117 & 0b01111110) << 13);
                    }
                }
                case 2 -> {
                    if (address >= 0x8000 && address <= 0xBFFF) {
                        this.prgSelect = (this.prgBank5115 & (1 << 7)) != 0 ? PrgSelect.ROM : PrgSelect.RAM;
                        yield (address & 0x3FFF) | ((this.prgBank5115 & 0b01111110) << 13);
                    } else if (address >= 0xC000 && address <= 0xDFFF) {
                        this.prgSelect = (this.prgBank5116 & (1 << 7)) != 0 ? PrgSelect.ROM : PrgSelect.RAM;
                        yield (address & 0x1FFF) | ((this.prgBank5116 & 0b01111111) << 13);
                    } else {
                        this.prgSelect = PrgSelect.ROM;
                        yield (address & 0x1FFF) | ((this.prgBank5117 & 0b01111111) << 13);
                    }
                }
                case 3 -> {
                    if (address >= 0x8000 && address <= 0x9FFF) {
                        this.prgSelect = (this.prgBank5114 & (1 << 7)) != 0 ? PrgSelect.ROM : PrgSelect.RAM;
                        yield (address & 0x1FFF) | ((this.prgBank5114 & 0b01111111) << 13);
                    } else if (address >= 0xA000 && address <= 0xBFFF) {
                        this.prgSelect = (this.prgBank5115 & (1 << 7)) != 0 ? PrgSelect.ROM : PrgSelect.RAM;
                        yield (address & 0x1FFF) | ((this.prgBank5115 & 0b01111111) << 13);
                    } else if (address >= 0xC000 && address <= 0xDFFF) {
                        this.prgSelect = (this.prgBank5116 & (1 << 7)) != 0 ? PrgSelect.ROM : PrgSelect.RAM;
                        yield (address & 0x1FFF) | ((this.prgBank5116 & 0b01111111) << 13);
                    } else {
                        this.prgSelect = PrgSelect.ROM;
                        yield (address & 0x1FFF) | ((this.prgBank5117 & 0b01111111) << 13);
                    }
                }
                default -> throw new IllegalStateException("Invalid PRG banking mode %d!".formatted(this.prgBankingMode));
            };
        }
    }

    private enum VerticalSplitRegionScreenSide {
        LEFT,
        RIGHT
    }

    private enum PrgSelect {
        ROM,
        RAM,
    }

    private enum PCMMode {
        WRITE,
        READ
    }

}
