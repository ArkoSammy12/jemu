package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class Action53Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private static final int[][][] PRG_BANK_LUT = new int[2][16][3];

    static {
        for (int i = 0; i < 16; i++) {
            int bottomTwo = i & 0b11;
            int upperThree = (i >>> 1) & 0b111;

            if (bottomTwo == 0b10) {
                PRG_BANK_LUT[0][i] = new int[]{0b11111111, 0b0000, 1};
            } else {
                PRG_BANK_LUT[0][i] = switch (upperThree) {
                    case 0b000 -> new int[]{0b11111111, 0b0000, 1};
                    case 0b010 -> new int[]{0b11111110, 0b0001, 1};
                    case 0b100 -> new int[]{0b11111100, 0b0011, 1};
                    case 0b110 -> new int[]{0b11111000, 0b0111, 1};
                    default -> switch (i) {
                        case 0b0011 -> new int[]{0b11111111, 0b0001, 0};
                        case 0b0111 -> new int[]{0b11111110, 0b0011, 0};
                        case 0b1011 -> new int[]{0b11111100, 0b0111, 0};
                        case 0b1111 -> new int[]{0b11111000, 0b1111, 0};
                        default -> throw new IllegalStateException("Unexpected value '%d' when initializing Action53 NES mapper PRG bank LUT!".formatted(i));
                    };
                };
            }


            if (bottomTwo == 0b11) {
                PRG_BANK_LUT[1][i] = new int[]{0b11111111, 0b0000, 1};
            } else {
                PRG_BANK_LUT[1][i] = switch (upperThree) {
                    case 0b000 -> new int[]{0b11111111, 0b0000, 1};
                    case 0b010 -> new int[]{0b11111110, 0b0001, 1};
                    case 0b100 -> new int[]{0b11111100, 0b0011, 1};
                    case 0b110 -> new int[]{0b11111000, 0b0111, 1};
                    default -> switch (i) {
                        case 0b0010 -> new int[]{0b11111111, 0b0001, 0};
                        case 0b0110 -> new int[]{0b11111110, 0b0011, 0};
                        case 0b1010 -> new int[]{0b11111100, 0b0111, 0};
                        case 0b1110 -> new int[]{0b11111000, 0b1111, 0};
                        default -> throw new IllegalStateException("Unexpected value '%d' when initializing Action53 NES mapper PRG bank LUT!".formatted(i));
                    };
                };
            }
        }
    }

    private int registerSelect;

    private int chrBankBits = 0b00__0_0000_0000_0000;
    private int nametableMirroringMode = 0b00;
    private int prgModeBits;
    private int prgInnerBankBits;
    private int prgOuterBankBits = 0xFF; // At power on, the last 16 KiB of the ROM is mapped into $C000 - $FFFF

    public Action53Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterROM == null) {
                return (int) this.characterRAM[this.mapChrRomAddress(address) % this.characterRAM.length] & 0xFF;
            } else {
                return (int) this.characterROM[this.mapChrRomAddress(address) % this.characterROM.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES Action53 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterRAM != null) {
                this.characterRAM[this.mapChrRomAddress(address) % this.characterRAM.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES Action53 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programROM[this.mapPrgRomAddress(address) % this.programROM.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, switch (this.nametableMirroringMode) {
            case 0b00 -> NametableArrangement.SINGLE_SCREEN_LOWER_BANK;
            case 0b01 -> NametableArrangement.SINGLE_SCREEN_UPPER_BANK;
            case 0b10 -> NametableArrangement.HORIZONTAL;
            case 0b11 -> NametableArrangement.VERTICAL;
            default -> throw new IllegalStateException("Invalid Action53 NES mapper nametable mirroring mode value '%d'!".formatted(this.nametableMirroringMode));
        });
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x5000 && address <= 0x5FFF) {
            this.registerSelect = (value & 1) | ((value & 0x80) >>> 6);
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            switch (this.registerSelect) {
                case 0b00 -> {
                    this.chrBankBits = (value & 0b11) << 13;
                    this.checkSetMirroringModeBit0(value);
                }
                case 0b01 -> {
                    this.prgInnerBankBits = value & 0xF;
                    this.checkSetMirroringModeBit0(value);
                }
                case 0b10 -> {
                    this.nametableMirroringMode = value & 0b11;
                    this.prgModeBits = (value >>> 2) & 0xF;
                }
                case 0b11 -> this.prgOuterBankBits = value & 0xFF;
            }
        }
    }

    private void checkSetMirroringModeBit0(int value) {
        if ((this.nametableMirroringMode & 0b10) == 0b00) {
            this.nametableMirroringMode = (value & (1 << 4)) != 0 ? 1 : 0;
        }
    }

    private int mapPrgRomAddress(int address) {
        int a14 = address & (1 << 14);
        int[] values = PRG_BANK_LUT[a14 != 0 ? 1 : 0][this.prgModeBits];
        int outerBankBits = this.prgOuterBankBits & values[0];
        int innerBankBits = this.prgInnerBankBits & values[1];
        boolean a14Passthrough = values[2] != 0;
        int finalAddress = (address & 0x3FFF) | (outerBankBits << 15);
        if (a14Passthrough) {
            finalAddress |= (innerBankBits << 15) | a14;
        } else {
            finalAddress |= (innerBankBits << 14);
        }
        return finalAddress;
    }

    private int mapChrRomAddress(int address) {
        return (address & 0x1FFF) | this.chrBankBits;
    }

}
