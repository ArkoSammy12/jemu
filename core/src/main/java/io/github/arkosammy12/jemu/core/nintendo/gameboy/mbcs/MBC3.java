package io.github.arkosammy12.jemu.core.nintendo.gameboy.mbcs;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyCartridge;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyEmulator;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.Optional;

public class MBC3 extends GameBoyCartridge {

    private final byte[][] romBanks;
    private final byte @Nullable [][] ramBanks;

    protected byte @Nullable [] saveData;

    private final int romBankMask;
    private final int ramBankMask;
    private final boolean hasBattery;

    private int romBankNumber = 1;
    protected int ramBankNumber;
    protected boolean ramRTCEnable;
    private final boolean mbc30;

    public MBC3(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);

        boolean mbc30 = false;

        this.romBanks = switch (this.romSizeHeader) {
            case 0x00 -> new byte[2][0x4000];
            case 0x01 -> new byte[4][0x4000];
            case 0x02 -> new byte[8][0x4000];
            case 0x03 -> new byte[16][0x4000];
            case 0x04 -> new byte[32][0x4000];
            case 0x05 -> new byte[64][0x4000];
            case 0x06 -> new byte[128][0x4000];
            case 0x07 -> {
                mbc30 = true;
                yield new byte[256][0x4000];
            }
            default -> throw new EmulatorException("Incompatible ROM size header $%02X for MBC3 GameBoy cartridge type!".formatted(this.romSizeHeader));
        };

        if (cartridgeType == 0x10 || cartridgeType == 0x12 || cartridgeType == 0x13) {
            this.ramBanks = switch (this.ramSizeHeader) {
                case 0x00 -> null;
                case 0x01 -> new byte[1][0x800];
                case 0x02 -> new byte[1][0x2000];
                case 0x03 -> new byte[4][0x2000];
                case 0x05 -> {
                    mbc30 = true;
                    yield new byte[8][0x2000];
                }
                default -> throw new EmulatorException("Incompatible RAM size header $%02X for MBC3 GameBoy cartridge type!".formatted(this.ramSizeHeader));
            };
        } else {
            this.ramBanks = null;
        }

        this.hasBattery = cartridgeType == 0x0F || cartridgeType == 0x10 || cartridgeType == 0x13;

        try {
            for (int i = 0; i < this.romBanks.length; i++) {
                byte[] romBank = this.romBanks[i];
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
                this.saveData = saveData;
                if (this.ramBanks == null) {
                    return;
                }
                try {
                    for (int i = 0; i < this.ramBanks.length; i++) {
                        byte[] bank = this.ramBanks[i];
                        System.arraycopy(saveData, i * bank.length, bank, 0, bank.length);
                    }
                } catch (Exception e) {
                    Logger.error("Error reading save data for GameBoy MBC3 cartridge: {}", e);
                }
            });
        }
        this.mbc30 = mbc30;
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x0000 && address <= 0x3FFF) {
            return (int) this.romBanks[0][address] & 0xFF;
        } else if (address >= 0x4000 && address <= 0x7FFF) {
            return (int) this.romBanks[this.romBankNumber & this.romBankMask][address - 0x4000] & 0xFF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramBankNumber <= 0x07 && this.ramRTCEnable && this.ramBanks != null) {
                byte[] ramBank = this.ramBanks[this.ramBankNumber & this.ramBankMask];
                address -= 0xA000;
                if (address < ramBank.length) {
                    return (int) ramBank[address] & 0xFF;
                } else {
                    return 0xFF;
                }
            } else {
                return 0xFF;
            }
        } else {
            throw new EmulatorException("Invalid GameBoy MBC3 cartridge read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            this.ramRTCEnable = (value & 0xF) == 0xA;
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            this.romBankNumber = value & (this.mbc30 ? 0xFF : 0x7F);
            if (this.romBankNumber == 0) {
                this.romBankNumber = 1;
            }
            this.romBankNumber &= this.romBankMask;
        } else if (address >= 0x4000 && address <= 0x5FFF) {
            this.ramBankNumber = value & 0xF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramBankNumber <= 0x07 && this.ramRTCEnable && this.ramBanks != null) {
                byte[] ramBank = this.ramBanks[this.ramBankNumber & this.ramBankMask];
                address -= 0xA000;
                if (address < ramBank.length) {
                    ramBank[address] = (byte) value;
                }
            }
        }
    }

    @Override
    protected Optional<byte[]> getSaveData() {
        return Optional.ofNullable(this.hasBattery ? this.ramBanks : null).map(GameBoyCartridge::toFlatByteArray);
    }

}
