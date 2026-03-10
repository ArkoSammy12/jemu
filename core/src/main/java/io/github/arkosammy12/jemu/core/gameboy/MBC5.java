package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Optional;

public class MBC5 extends GameBoyCartridge {

    private final int[][] romBanks;

    private final int @Nullable [][] ramBanks;

    private final int romBankMask;
    private final int ramBankMask;
    private final boolean hasBattery;

    private int ramGate;
    private int romBankLower = 1;
    private int romBankUpper;
    private int ramBankNumber;

    public MBC5(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);

        this.romBanks = switch (this.romSizeHeader) {
            case 0x00 -> new int[2][0x4000];
            case 0x01 -> new int[4][0x4000];
            case 0x02 -> new int[8][0x4000];
            case 0x03 -> new int[16][0x4000];
            case 0x04 -> new int[32][0x4000];
            case 0x05 -> new int[64][0x4000];
            case 0x06 -> new int[128][0x4000];
            case 0x07 -> new int[256][0x4000];
            case 0x08 -> new int[512][0x4000];
            default -> throw new EmulatorException("Incompatible ROM size header $%02X for MBC5 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };

        if (cartridgeType == 0x1A || cartridgeType == 0x1B || cartridgeType == 0x1D || cartridgeType == 0x1E) {
            this.ramBanks = switch (this.ramSizeHeader) {
                case 0x00 -> null;
                case 0x01 -> new int[1][0x800];
                case 0x02 -> new int[1][0x2000];
                case 0x03 -> new int[4][0x2000];
                case 0x04 -> new int[16][0x2000];
                case 0x05 -> new int[8][0x2000];
                default -> throw new EmulatorException("Incompatible RAM size header $%02X for MBC5 GameBoy cartridge type!".formatted(this.ramSizeHeader));
            };
        } else {
            this.ramBanks = null;
        }

        this.hasBattery = cartridgeType == 0x1B || cartridgeType == 0x1E;

        try {
            for (int i = 0; i < this.romBanks.length; i++) {
                int[] romBank = this.romBanks[i];
                int start = i * romBank.length;
                System.arraycopy(this.originalRom, start, romBank, 0, romBank.length);
            }
        } catch (Exception e) {
            throw new EmulatorException("Error initializing GameBoy cartridge ROM!", e);
        }

        this.romBankMask = ((1 << (32 - Integer.numberOfLeadingZeros(this.romBanks.length))) - 1) >> 1;
        this.ramBankMask = this.ramBanks == null ? 0 : ((1 << (32 - Integer.numberOfLeadingZeros(this.ramBanks.length))) - 1) >> 1;

        if (this.hasBattery) {
            this.readSaveData().ifPresent(saveData -> {
                if (this.ramBanks == null) {
                    return;
                }
                try {
                    for (int i = 0; i < this.ramBanks.length; i++) {
                        int[] bank = this.ramBanks[i];
                        System.arraycopy(saveData, i * bank.length, bank, 0, bank.length);
                    }
                } catch (Exception e) {
                    Logger.error("Error reading save data for GameBoy MBC5 cartridge: {}", e);
                }
            });
        }

    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            return this.romBanks[0][address];
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return this.romBanks[(((this.romBankUpper & 1) << 8) | this.romBankLower) & this.romBankMask][address - 0x4000];
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate == 0x0A && this.ramBanks != null) {
                int[] ramBank = this.ramBanks[(this.ramBankNumber & 0xF) & this.ramBankMask];
                address -= 0xA000;
                if (address < ramBank.length) {
                    return ramBank[address];
                } else {
                    return 0xFF;
                }
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC5 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            this.ramGate = value & 0xFF;
        } else if (address >= 0x2000 && address <= 0x2FFF) {
            this.romBankLower = value & 0xFF & this.romBankMask;
        } else if (address >= 0x3000 && address <= 0x3FFF) {
            this.romBankUpper = value & 1;
        } else if (address >= 0x4000 && address <= 0x5FFF) {
            this.ramBankNumber = value & 0xF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramGate == 0x0A && this.ramBanks != null) {
                int[] ramBank = this.ramBanks[(this.ramBankNumber & 0xF) & this.ramBankMask];
                address -= 0xA000;
                if (address < ramBank.length) {
                    ramBank[address] = value & 0xFF;
                }
            }
        }
    }

    @Override
    protected Optional<int[]> getSaveData() {
        return Optional.ofNullable(this.hasBattery ? this.ramBanks : null).map(banks -> Arrays.stream(banks).flatMapToInt(Arrays::stream).toArray());
    }

}
