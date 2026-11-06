package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.util.ActionSignal;

import java.util.Optional;
import java.util.function.Supplier;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class MMC3Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    protected static final int A12 = 1 << 12;

    private final Supplier<NametableArrangement> nametableArrangementSupplier;

    private NametableArrangement nametableArrangement = NametableArrangement.HORIZONTAL;
    protected int irqCounterReload;
    protected boolean irqEnabled;

    // Bank select registers
    private int bankSelectIndex;
    private boolean prgRomBankMode;
    private boolean chrA12Inversion;

    // PRG RAM protect registers
    private boolean enablePrgRam;
    private boolean allowPrgRamWrites;

    private int R0;
    private int R1;
    private int R2;
    private int R3;
    private int R4;
    private int R5;
    private int R6;
    private int R7;

    protected int irqCounter;
    protected boolean irqReload;
    private boolean irqSignal;
    protected int previousPPUAddress;
    protected int cyclesDown = 3;

    protected final ActionSignal setIRQSignal;

    public MMC3Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
        if (iNESFile.hasAlternativeNametableLayout()) {
            this.nametableArrangementSupplier = () -> NametableArrangement.FOUR_SCREEN;
        } else {
            this.nametableArrangementSupplier = () -> this.nametableArrangement;
        }
        this.setIRQSignal = new ActionSignal(4, _ -> this.irqSignal = true);
    }

    @Override
    protected VRAMSize getVRAMSize() {
        return this.iNESFile.hasAlternativeNametableLayout() ? VRAMSize.KB_4 : VRAMSize.KB_2;
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
            throw new EmulatorException("Invalid NES MMC3 cartridge PPU read address $%04X!".formatted(address));
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
            throw new EmulatorException("Invalid NES MMC3 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, this.nametableArrangementSupplier.get());
    }

    @Override
    public int readByte(int address, int dataBus) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null && this.isPrgRamEnabled()) {
                return (int) this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] & 0xFF;
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
            if (this.programRAM != null && this.allowPrgRamWrites() && this.isPrgRamEnabled()) {
                this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] = (byte) value;
            }
        } else if (address >= 0x8000 && address <= 0x9FFF) {
            if ((address & 1) == 0) {
                this.setBankSelect(value);
            } else {
                switch (this.bankSelectIndex) {
                    case 0 -> this.R0 = value & 0xFE;
                    case 1 -> this.R1 = value & 0xFE;
                    case 2 -> this.R2 = value & 0xFF;
                    case 3 -> this.R3 = value & 0xFF;
                    case 4 -> this.R4 = value & 0xFF;
                    case 5 -> this.R5 = value & 0xFF;
                    case 6 -> this.R6 = value & 0xFF; // Support oversized MMC3
                    case 7 -> this.R7 = value & 0xFF; // Support oversized MMC3
                }
            }
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            if ((address & 1) == 0) {
                this.nametableArrangement = (value & 1) != 0 ? NametableArrangement.VERTICAL : NametableArrangement.HORIZONTAL;
            } else {
                this.setPrgRamProtect(value);
            }
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            if ((address & 1) == 0) {
                this.irqCounterReload = value & 0xFF;
            } else {
                this.irqCounter = 0;
                this.irqReload = true;
            }
        } else if (address >= 0xE000 && address <= 0xFFFF) {
            if ((address & 1) == 0) {
                this.irqEnabled = false;
                this.irqSignal = false;
            } else {
                this.irqEnabled = true;
            }
        }
    }

    @Override
    public void cycle() {
        if ((this.previousPPUAddress & A12) == 0) {
            // Cap the cyclesDown counter to 10, since we just care that it is at least 4 when clocking the scanline counter.
            // This way we prevent a potential integer overflow.
            if (this.cyclesDown < 10) {
                this.cyclesDown++;
            }
        } else {
            this.cyclesDown = 0;
        }
    }

    @Override
    public void onPPUHalfDot() {
        this.setIRQSignal.tick();
    }

    protected void setBankSelect(int value) {
        this.bankSelectIndex = value & 0b111;
        this.prgRomBankMode = (value & (1 << 6)) != 0;
        this.chrA12Inversion = (value & (1 << 7)) != 0;
    }

    protected void setPrgRamProtect(int value) {
        this.enablePrgRam = (value & (1 << 7)) != 0;
        this.allowPrgRamWrites = (value & (1 << 6)) == 0;
    }

    protected boolean isPrgRamEnabled() {
        return this.enablePrgRam;
    }

    protected boolean allowPrgRamWrites() {
        return this.allowPrgRamWrites;
    }

    private int mapChrAddress(int address) {
        address &= 0x1FFF;
        if (this.chrA12Inversion) {
            if (address <= 0x03FF) {
                return (address & 0x3FF) | (this.R2 << 10);
            } else if (address <= 0x07FF) {
                return (address & 0x3FF) | (this.R3 << 10);
            } else if (address <= 0x0BFF) {
                return (address & 0x3FF) | (this.R4 << 10);
            } else if (address <= 0x0FFF) {
                return (address & 0x3FF) | (this.R5 << 10);
            } else if (address <= 0x17FF) {
                return (address & 0x7FF) | (this.R0 << 10);
            } else {
                return (address & 0x7FF) | (this.R1 << 10);
            }
        } else {
            if (address <= 0x07FF) {
                return (address & 0x7FF) | (this.R0 << 10);
            } else if (address <= 0x0FFF) {
                return (address & 0x7FF) | (this.R1 << 10);
            } else if (address <= 0x13FF) {
                return (address & 0x3FF) | (this.R2 << 10);
            } else if (address <= 0x17FF) {
                return (address & 0x3FF) | (this.R3 << 10);
            } else if (address <= 0x1BFF) {
                return (address & 0x3FF) | (this.R4 << 10);
            } else {
                return (address & 0x3FF) | (this.R5 << 10);
            }
        }
    }

    private int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (this.prgRomBankMode) {
            if (address <= 0x1FFF) {
                return (address & 0x1FFF) | (0xFE << 13);
            } else if (address <= 0x3FFF) {
                return (address & 0x1FFF) | (this.R7 << 13);
            } else if (address <= 0x5FFF) {
                return (address & 0x1FFF) | (this.R6 << 13);
            } else {
                return (address & 0x1FFF) | (0xFF << 13);
            }
        } else {
            if (address <= 0x1FFF) {
                return (address & 0x1FFF) | (this.R6 << 13);
            } else if (address <= 0x3FFF) {
                return (address & 0x1FFF) | (this.R7 << 13);
            } else if (address <= 0x5FFF) {
                return (address & 0x1FFF) | (0xFE << 13);
            } else {
                return (address & 0x1FFF) | (0xFF << 13);
            }
        }
    }

    protected int mapPrgRamAddress(int address) {
        return address & 0x1FFF;
    }


    // If making changes to this, also change in MMC6 if needed
    @Override
    public void observePPUAddress(int address) {
        if (address != this.previousPPUAddress) {
            if ((address & A12) != 0 && (this.previousPPUAddress & A12) == 0 && this.cyclesDown >= 4) {
                if (this.irqCounter <= 0 || this.irqReload) {
                    this.irqCounter = this.irqCounterReload;
                    this.irqReload = false;
                } else {
                    this.irqCounter--;
                }

                if (this.irqCounter <= 0 && this.irqEnabled) {
                    this.setIRQSignal.trigger(0);
                }
            }
            this.previousPPUAddress = address & 0xFFFF;
        }
    }

    @Override
    public boolean getIRQSignal() {
        return this.irqSignal;
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
