package io.github.arkosammy12.jemu.systems.misc.gameboy;

import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.systems.GameBoyEmulator;
import org.tinylog.Logger;

public class MBC1 extends GameBoyCartridge {

    private final int[][] romBanks;
    private final int[][] ramBanks;

    private boolean extendedRom = false;

    private boolean ramEnable = false;
    private int romBankNumber;
    private int ramBankNumber;

    private boolean bankingModeSelect;

    public MBC1(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);

        this.romBanks = switch (this.romBankAmount) {
            case 0x00 -> new int[1][0x8000];
            case 0x01 -> new int[4][0x4000];
            case 0x02 -> new int[8][0x4000];
            case 0x03 -> new int[16][0x4000];
            case 0x04 -> new int[32][0x4000];
            case 0x05 -> new int[64][0x4000];
            case 0x06 -> new int[128][0x4000];
            case 0x07 -> new int[256][0x4000];
            case 0x08 -> new int[512][0x4000];
            default -> throw new EmulatorException("Unexpected rom bank amount: " + this.ramBankAmount + " for the GameBoy system with MBC1!");
        };

        this.extendedRom = this.romBankAmount >= 0x05;

        if (this.extendedRom) {
            this.ramBanks = new int[1][0x2000];
        } else if (cartridgeType == 0x02 || cartridgeType == 0x03) {
            this.ramBanks = switch (this.ramBankAmount) {
                case 0x00 -> null;
                case 0x01 -> new int[1][0x800];
                case 0x02 -> new int[1][0x2000];
                case 0x03 -> new int[4][0x2000];
                default -> throw new EmulatorException("Unexpected ram bank amount: " + this.ramBankAmount + " for the GameBoy system with MBC1!");
            };
        } else {
            this.ramBanks = null;
        }

        for (int i = 0; i < this.romBanks.length; i++) {
            int[] romBank = this.romBanks[i];
            int startAddress = i * romBank.length;
            System.arraycopy(this.originalRom, startAddress, romBank, 0, romBank.length);
        }

    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x000 && address <= 0x1FFF) {
            this.ramEnable = (value & 0xF) == 0xA;
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            int effectiveBankNumber = value;

            if (this.extendedRom) {
                effectiveBankNumber = (this.ramBankNumber << 5) | effectiveBankNumber;
            }

            if ((effectiveBankNumber & 0b11111) == 0) {
                effectiveBankNumber |= 1;
            }

            this.romBankNumber = effectiveBankNumber % this.romBanks.length;
        } else if (address >= 0x4000 && address <= 0x5FFF) {
            this.ramBankNumber = value & 0b11;
        } else if (address >= 0x6000 && address <= 0x7FFF) {
            this.bankingModeSelect = (value & 1) != 0;
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            if (this.bankingModeSelect) {
                int bankNumber = (this.ramBankNumber << 5) & this.romBanks.length;
                return this.romBanks[bankNumber][address];
            } else {
                return this.rom0[address];
            }
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            if (extendedRom) {
                int bankNumber = (this.ramBankNumber << 5) | (this.romBankNumber & 0b11111);
                return this.romBanks[bankNumber][address - 0x4000];
            } else {
                return this.romBanks[this.romBankNumber & 0b11111][address - 0x4000];
            }
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (!ramEnable) {
                return 0xFF;
            }
            if (this.extendedRom || !this.bankingModeSelect) {
                return this.ramBanks[0][address - 0xA000];
            } else {
                int bankNumber = this.ramBankNumber;
                if (bankNumber >= this.ramBanks.length) {
                    return 0xFF;
                } else {
                    int[] ramBank = this.ramBanks[bankNumber];
                    address -= 0xA000;
                    if (address < ramBank.length) {
                        return ramBank[address];
                    } else {
                        return 0xFF;
                    }
                }
            }
        } else {
            throw new EmulatorException("Invalid address " + String.format("%04X", address) + " for MBC0 cartridge read!");
        }
    }

}




/*
public class MBC1 extends GameBoyCartridge {

    private final int[][] romBanks;
    private final int[][] ramBanks;

    private final boolean extendedRom;

    private boolean ramEnable = false;

    // LOWER 5 bits of ROM bank number
    private int romBankNumber = 1;

    // UPPER 2 bits of ROM bank number (mode 0)
    // OR RAM bank number (mode 1)
    private int ramBankNumber = 0;

    // false = ROM banking mode (default)
    // true  = RAM banking mode
    private boolean bankingModeSelect = false;

    public MBC1(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);

        this.romBanks = switch (this.romBankAmount) {
            case 0x00 -> new int[1][0x8000];
            case 0x01 -> new int[4][0x4000];
            case 0x02 -> new int[8][0x4000];
            case 0x03 -> new int[16][0x4000];
            case 0x04 -> new int[32][0x4000];
            case 0x05 -> new int[64][0x4000];
            case 0x06 -> new int[128][0x4000];
            case 0x07 -> new int[256][0x4000];
            case 0x08 -> new int[512][0x4000];
            default -> throw new EmulatorException(
                    "Unexpected ROM bank amount: " + this.romBankAmount);
        };

        this.extendedRom = this.romBankAmount >= 0x05;

        if (cartridgeType == 0x02 || cartridgeType == 0x03) {
            this.ramBanks = switch (this.ramBankAmount) {
                case 0x00 -> null;
                case 0x01 -> new int[1][0x800];
                case 0x02 -> new int[1][0x2000];
                case 0x03 -> new int[4][0x2000];
                default -> throw new EmulatorException(
                        "Unexpected RAM bank amount: " + this.ramBankAmount);
            };
        } else {
            this.ramBanks = null;
        }

        // Load ROM banks
        for (int i = 0; i < this.romBanks.length; i++) {
            int[] bank = this.romBanks[i];
            int start = i * bank.length;
            System.arraycopy(this.originalRom, start, bank, 0, bank.length);
        }
    }

    @Override
    public void writeByte(int address, int value) {

        if (address <= 0x1FFF) {
            // RAM enable
            ramEnable = (value & 0x0F) == 0x0A;

        } else if (address <= 0x3FFF) {
            // ROM bank lower 5 bits
            romBankNumber = value & 0b11111;
            if (romBankNumber == 0) {
                romBankNumber = 1;
            }

        } else if (address <= 0x5FFF) {
            // Upper ROM bits or RAM bank (mode dependent)
            ramBankNumber = value & 0b11;

        } else if (address <= 0x7FFF) {
            // Banking mode select
            bankingModeSelect = (value & 1) != 0;
        }
    }

    @Override
    public int readByte(int address) {

        // 0000–3FFF
        if (address <= 0x3FFF) {
            if (!bankingModeSelect) {
                // ROM banking mode: always bank 0
                return romBanks[0][address];
            } else {
                // RAM banking mode: upper ROM bits affect bank 0 region
                int bank = (ramBankNumber << 5) % romBanks.length;
                return romBanks[bank][address];
            }
        }

        // 4000–7FFF
        if (address <= 0x7FFF) {
            int bank =
                    ((ramBankNumber << 5) | romBankNumber) % romBanks.length;
            return romBanks[bank][address - 0x4000];
        }

        // A000–BFFF
        if (address >= 0xA000 && address <= 0xBFFF) {
            if (!ramEnable || ramBanks == null) {
                return 0xFF;
            }

            int bank = bankingModeSelect ? ramBankNumber : 0;
            if (bank >= ramBanks.length) {
                return 0xFF;
            }

            int offset = address - 0xA000;
            if (offset >= ramBanks[bank].length) {
                return 0xFF;
            }

            return ramBanks[bank][offset];
        }

        throw new EmulatorException(
                "Invalid MBC1 read address: " + String.format("%04X", address));
    }
}


 */
