package io.github.arkosammy12.jemu.systems.gameboy;

import io.github.arkosammy12.jemu.exceptions.EmulatorException;

import java.util.Arrays;

public class MBC2 extends GameBoyCartridge {

    private static final int A8_MASK = 1 << 8;

    private final int[][] romBanks;
    private final int[] sRam = new int[512];

    private int romBankNumber = 0xF1;
    private int ramGate = 0xF0;

    private final int romBankMask;

    public MBC2(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);

        this.romBanks = switch (this.romSizeHeader) {
            case 0x00 -> new int[2][0x4000];
            case 0x01 -> new int[4][0x4000];
            case 0x02 -> new int[8][0x4000];
            case 0x03 -> new int[16][0x4000];
            default -> throw new EmulatorException("Incompatible ROM size header \"0x%02X\" for MBC2 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };

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
        Arrays.fill(this.sRam, 0xF0);

    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            return this.romBanks[0][address];
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return this.romBanks[(this.romBankNumber & 0xF) & this.romBankMask][address - 0x4000];
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if ((this.ramGate & 0xF) == 0b1010) {
                return this.sRam[address & 0x1FF];
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC2 cartridge address 0x\"%04X\"!".formatted(address));
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
            if ((this.ramGate & 0xF) == 0b1010) {
                this.sRam[address & 0x1FF] = 0xF0 | (value & 0xF);
            }
        }
    }
}
