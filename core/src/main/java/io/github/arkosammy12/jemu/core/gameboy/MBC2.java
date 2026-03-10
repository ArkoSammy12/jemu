package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Optional;

public class MBC2 extends GameBoyCartridge {

    private static final int A8_MASK = 1 << 8;

    private final int[][] romBanks;
    private final int @Nullable [] sRam;
    private final boolean hasBattery;

    private final int romBankMask;

    private int romBankNumber = 0xF1;
    private int ramGate = 0xF0;

    public MBC2(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);

        this.romBanks = switch (this.romSizeHeader) {
            case 0x00 -> new int[2][0x4000];
            case 0x01 -> new int[4][0x4000];
            case 0x02 -> new int[8][0x4000];
            case 0x03 -> new int[16][0x4000];
            default -> throw new EmulatorException("Incompatible ROM size header $%02X for MBC2 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };

        if (cartridgeType == 0x06) {
            this.sRam = new int[512];
            this.hasBattery = true;
        } else {
            this.sRam = null;
            this.hasBattery = false;
        }

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

        if (this.hasBattery) {
            this.readSaveData().ifPresent(saveData -> {
                try {
                    System.arraycopy(saveData, 0, this.sRam, 0, Math.min(saveData.length, this.sRam.length));
                } catch (Exception e) {
                    Logger.error("Error reading save data for GameBoy MBC2 cartridge: {}", e);
                }
            });
        }

    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            return this.romBanks[0][address];
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return this.romBanks[(this.romBankNumber & 0xF) & this.romBankMask][address - 0x4000];
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if ((this.ramGate & 0xF) == 0b1010 && this.sRam != null) {
                return this.sRam[address & 0x1FF] | 0xF0;
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC2 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            if ((address & A8_MASK) == 0) {
                this.ramGate = value & 0xF;
            } else {
                int effectiveBankNumber = value & 0xF;
                if (effectiveBankNumber == 0) {
                    effectiveBankNumber = 1;
                }
                this.romBankNumber = effectiveBankNumber & this.romBankMask;
            }
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if ((this.ramGate & 0xF) == 0b1010 && this.sRam != null) {
                this.sRam[address & 0x1FF] = value & 0xFF;
            }
        }
    }

    @Override
    protected Optional<int[]> getSaveData() {
        return Optional.ofNullable(this.hasBattery ? this.sRam : null);
    }

}
