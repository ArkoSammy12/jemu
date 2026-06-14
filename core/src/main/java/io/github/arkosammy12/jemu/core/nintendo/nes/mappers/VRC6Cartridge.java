package io.github.arkosammy12.jemu.core.nintendo.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nintendo.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nintendo.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.nintendo.nes.ines.NES20File;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nintendo.nes.RP2C02.*;
import static io.github.arkosammy12.jemu.core.nintendo.nes.RP2C02.CIRAM_END;

public class VRC6Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private static final double NES_PULSE_FULL_VOLUME = 95.88 / ((8128.0 / 15.0) + 100.0);
    private static final double VRC6_PULSE_FULL_VOLUME = 15.0 / 61.0;

    // TODO: Eventually make this user configurable
    private static final double VRC6_WEIGHT = NES_PULSE_FULL_VOLUME / VRC6_PULSE_FULL_VOLUME;

    private final byte[] programROM;
    private final byte[] programRAM;
    private final byte[] characterROM;
    private final byte[] characterRAM;

    private final int a0Bit;
    private final int a1Bit;

    private final VRC4Cartridge.VRCIRQEngine vrcirqEngine = new VRC4Cartridge.VRCIRQEngine();
    private final PulseChannel pulseChannel1 = new PulseChannel();
    private final PulseChannel pulseChannel2 = new PulseChannel();
    private final SawtoothChannel sawtoothChannel = new SawtoothChannel();

    private int prgSelect16K;
    private int prgSelect8K;
    private int ppuBankingStyle;

    private NametableArrangement nametableArrangement = NametableArrangement.HORIZONTAL;

    private int R0;
    private int R1;
    private int R2;
    private int R3;
    private int R4;
    private int R5;
    private int R6;
    private int R7;

    // Frequency control registers
    private boolean halt;
    private boolean frequency16x;
    private boolean frequency256x;

    public VRC6Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        byte[] programRomData = iNESFile.getProgramRom();
        this.programROM = Arrays.copyOf(programRomData, programRomData.length);

        int programRamSize = iNESFile.getProgramRamSize();
        if (iNESFile instanceof NES20File nes20File) {
            programRamSize += nes20File.getNonVolatileProgramRamSizeBytes();
        }

        this.programRAM = programRamSize > 0 ? new byte[programRamSize] : null;

        Optional<byte[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterROM = null;
            int characterRamSize = iNESFile.getCharacterRamSize();
            if (iNESFile instanceof NES20File nes20File) {
                characterRamSize += nes20File.getNonVolatileCharacterRamSizeBytes();
            }
            this.characterRAM = new byte[characterRamSize];
        } else {
            byte[] characterRomData = characterRomOptional.get();
            this.characterROM = Arrays.copyOf(characterRomData, characterRomData.length);
            this.characterRAM = null;
        }

        this.a0Bit = switch (iNESFile.getMapperNumber()) {
            case 24 -> 0;
            case 26 -> 1;
            default -> throw new EmulatorException("Invalid mapper number %d for VRC6!".formatted(this.iNESFile.getMapperNumber()));
        };

        this.a1Bit = switch (iNESFile.getMapperNumber()) {
            case 24 -> 1;
            case 26 -> 0;
            default -> throw new EmulatorException("Invalid mapper number %d for VRC6!".formatted(this.iNESFile.getMapperNumber()));
        };

        this.restoreSaveData(this.programRAM, this.characterRAM);

    }

   @Override
	public int readBytePPU(int address) {
		this.observePPUAddress(address);
        if (address >= CHR_START && address <= CHR_END) {
            return this.readChrMappedAddress(this.mapChrAddress(address));
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            if (this.usesROMNametables()) {
                return this.readChrMappedAddress(this.mapVRC6NametableChrAddress(address));
            } else {
                return this.readByteVRAM(this.mapNametableAddress(address));
            }
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES VRC6 cartridge PPU read address $%04X!".formatted(address));
        }
	}

    @Override
	public void writeBytePPU(int address, int value) {
		this.observePPUAddress(address);
        if (address >= 0x0000 && address <= 0x1FFF) {
            this.writeChrMappedAddress(this.mapChrAddress(address), value);
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            if (this.usesROMNametables()) {
                this.writeChrMappedAddress(this.mapVRC6NametableChrAddress(address), value);
            } else {
                this.writeByteVRAM(this.mapNametableAddress(address), value);
            }
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES VRC6 cartridge PPU write address $%04X!".formatted(address));
        }
	}

    @Override
    public int readByte(int address) {
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
            if (this.programRAM != null && this.isPrgRamEnabled()) {
                this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] = (byte) value;
            }
        } else if (address >= 0x8000 && address <= 0x8FFF) {
            if (this.getRegisterSlot(address) <= 3) {
                this.prgSelect16K = value & 0xF;
            }
        } else if (address >= 0x9000 && address <= 0x9FFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.pulseChannel1.writeVolume(value);
                case 1 -> this.pulseChannel1.writeLO(value);
                case 2 -> this.pulseChannel1.writeHI(value);
                case 3 -> {
                    this.halt = (value & 1) != 0;
                    this.frequency16x = (value & (1 << 1)) != 0;
                    this.frequency256x = (value & (1 << 2)) != 0;
                }
            }
        } else if (address >= 0xA000 && address <= 0xAFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.pulseChannel2.writeVolume(value);
                case 1 -> this.pulseChannel2.writeLO(value);
                case 2 -> this.pulseChannel2.writeHI(value);
            }
        } else if (address >= 0xB000 && address <= 0xBFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.sawtoothChannel.writeVolume(value);
                case 1 -> this.sawtoothChannel.writeLO(value);
                case 2 -> this.sawtoothChannel.writeHI(value);
                case 3 -> {
					this.ppuBankingStyle = value & 0xBF;

					// Keep this only as a harmless fallback/default. VRC6 nametable mapping
					// below does not depend on nametableArrangement anymore.
					this.nametableArrangement = switch (value & 0x0C) {
						case 0x00 -> NametableArrangement.VERTICAL;
						case 0x04 -> NametableArrangement.HORIZONTAL;
						case 0x08 -> NametableArrangement.SINGLE_SCREEN_LOWER_BANK;
						case 0x0C -> NametableArrangement.SINGLE_SCREEN_UPPER_BANK;
						default -> throw new EmulatorException("Invalid VRC6 mirroring bits!");
					};
				}
            }
        } else if (address >= 0xC000 && address <= 0xCFFF) {
            if (this.getRegisterSlot(address) <= 3) {
                this.prgSelect8K = value & 0x1F;
            }
        } else if (address >= 0xD000 && address <= 0xDFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.R0 = value & 0xFF;
                case 1 -> this.R1 = value & 0xFF;
                case 2 -> this.R2 = value & 0xFF;
                case 3 -> this.R3 = value & 0xFF;
            }
        } else if (address >= 0xE000 && address <= 0xEFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.R4 = value & 0xFF;
                case 1 -> this.R5 = value & 0xFF;
                case 2 -> this.R6 = value & 0xFF;
                case 3 -> this.R7 = value & 0xFF;
            }
        } else if (address >= 0xF000 && address <= 0xFFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.vrcirqEngine.writeIRQLatch(value);
                case 1 -> this.vrcirqEngine.writeIRQControl(value);
                case 2 -> this.vrcirqEngine.writeIRQAcknowledge();
            }
        }
    }

    protected int getRegisterSlot(int address) {
        return ((address >> this.a0Bit) & 1) | (((address >> this.a1Bit) & 1) << 1);
    }

    private int mapChrAddress(int address) {
        address &= 0x1FFF;
        return switch (this.ppuBankingStyle & 0x03) {
            case 0 -> {
                if (address <= 0x03FF) {
                    yield (address & 0x3FF) | (this.R0 << 10);
                } else if (address <= 0x07FF) {
                    yield (address & 0x3FF) | (this.R1 << 10);
                } else if (address <= 0x0BFF) {
                    yield (address & 0x3FF) | (this.R2 << 10);
                } else if (address <= 0x0FFF) {
                    yield (address & 0x3FF) | (this.R3 << 10);
                } else if (address <= 0x13FF) {
                    yield (address & 0x3FF) | (this.R4 << 10);
                } else if (address <= 0x17FF) {
                    yield (address & 0x3FF) | (this.R5 << 10);
                } else if (address <= 0x1BFF) {
                    yield (address & 0x3FF) | (this.R6 << 10);
                } else {
                    yield (address & 0x3FF) | (this.R7 << 10);
                }
            }
            case 1 -> {
                int reg;
                if (address <= 0x07FF) {
                    reg = this.R0;
                } else if (address <= 0x0FFF) {
                    reg = this.R1;
                } else if (address <= 0x17FF) {
                    reg = this.R2;
                } else {
                    reg = this.R3;
                }
                int a10 = this.passPPUA10() ? (address >>> 10) & 1 : (reg & 1);
                yield (address & 0x3FF) | (a10 << 10) | ((reg >> 1) << 11);
            }
            case 2, 3 -> {
                if (address <= 0x03FF) {
                    yield (address & 0x3FF) | (this.R0 << 10);
                } else if (address <= 0x07FF) {
                    yield (address & 0x3FF) | (this.R1 << 10);
                } else if (address <= 0x0BFF) {
                    yield (address & 0x3FF) | (this.R2 << 10);
                } else if (address <= 0x0FFF) {
                    yield (address & 0x3FF) | (this.R3 << 10);
                } else {
                    int reg = address <= 0x17FF ? this.R4 : this.R5;
                    int a10 = this.passPPUA10() ? (address >>> 10) & 1 : (reg & 1);
                    yield (address & 0x3FF) | (a10 << 10) | ((reg >> 1) << 11);
                }
            }
            default -> throw new EmulatorException("Invalid PPU banking style bits for CHR banking!");
        };
    }

   @Override
	protected int mapNametableAddress(int address) {
		address = 0x2000 | (address & 0x0FFF);

		int fine = address & 0x03FF;
		int bank = this.selectVRC6NametableBank(address);

		// CIRAM is only 2 KiB. The selected nametable bank only contributes A10.
		return ((bank & 1) << 10) | fine;
	}

	private int mapVRC6NametableChrAddress(int address) {
		address = 0x2000 | (address & 0x0FFF);

		int fine = address & 0x03FF;
		int bank = this.selectVRC6NametableBank(address);

		return (bank << 10) | fine;
	}

	private int selectVRC6NametableBank(int address) {
		address = 0x2000 | (address & 0x0FFF);

		int slot = (address >> 10) & 3;
		int mode = this.ppuBankingStyle & 0x0F;

		int reg;
		int forceLsb = -1;

		if (this.passPPUA10()) {
			switch (mode & 3) {
				case 0 -> {
					// Mode 0: normal CIRAM mirroring equivalents, but expressed
					// as R6/R7 plus forced low bit so ROM nametables work too.
					switch (mode) {
						case 0x0 -> {
							reg = slot < 2 ? this.R6 : this.R7;
							forceLsb = slot & 1;
						}
						case 0x4 -> {
							reg = (slot & 1) == 0 ? this.R6 : this.R7;
							forceLsb = slot >= 2 ? 1 : 0;
						}
						case 0x8 -> {
							reg = slot < 2 ? this.R6 : this.R7;
							forceLsb = 0;
						}
						case 0xC -> {
							reg = (slot & 1) == 0 ? this.R6 : this.R7;
							forceLsb = 1;
						}
						default -> throw new EmulatorException("Invalid VRC6 mode 0 nametable mode!");
					}
				}

				case 1 -> {
					// Mode 1: 4-screen style, R4/R5/R6/R7.
					reg = switch (slot) {
						case 0 -> this.R4;
						case 1 -> this.R5;
						case 2 -> this.R6;
						case 3 -> this.R7;
						default -> throw new EmulatorException("Invalid VRC6 nametable slot!");
					};
				}

				case 2 -> {
					// Mode 2: R6/R7 vertical or horizontal style.
					if (mode == 0x2 || mode == 0xA) {
						reg = (slot & 1) == 0 ? this.R6 : this.R7;
					} else {
						reg = slot < 2 ? this.R6 : this.R7;
					}
				}

				case 3 -> {
					// Mode 3: intended for ROM nametables; force even/odd page.
					switch (mode) {
						case 0x3 -> {
							reg = (slot & 1) == 0 ? this.R6 : this.R7;
							forceLsb = slot >= 2 ? 1 : 0;
						}
						case 0x7 -> {
							reg = slot < 2 ? this.R6 : this.R7;
							forceLsb = slot & 1;
						}
						case 0xB -> {
							reg = (slot & 1) == 0 ? this.R6 : this.R7;
							forceLsb = 1;
						}
						case 0xF -> {
							reg = slot < 2 ? this.R6 : this.R7;
							forceLsb = 0;
						}
						default -> throw new EmulatorException("Invalid VRC6 mode 3 nametable mode!");
					}
				}

				default -> throw new EmulatorException("Invalid VRC6 nametable mode!");
			}
		} else {
			reg = this.selectVRC6NametableRegisterWithA10Latched(mode, slot);
		}

		if (forceLsb >= 0) {
			reg = (reg & ~1) | forceLsb;
		}

		return reg & 0xFF;
	}

	private int selectVRC6NametableRegisterWithA10Latched(int mode, int slot) {
		return switch (slot) {
			case 0 -> switch (mode) {
				case 0x1, 0x5, 0x9, 0xD -> this.R4;
				default -> this.R6;
			};
			case 1 -> switch (mode) {
				case 0x1, 0x5, 0x9, 0xD -> this.R5;
				case 0x0, 0x6, 0x7, 0x8, 0xE, 0xF -> this.R6;
				default -> this.R7;
			};
			case 2 -> switch (mode) {
				case 0x0, 0x6, 0x7, 0x8, 0xE, 0xF -> this.R7;
				default -> this.R6;
			};
			case 3 -> this.R7;
			default -> throw new EmulatorException("Invalid VRC6 nametable slot %d!".formatted(slot));
		};
	}

	private boolean usesROMNametables() {
		return (this.ppuBankingStyle & (1 << 4)) != 0;
	}

	private int readChrMappedAddress(int mappedAddress) {
		if (this.characterRAM != null) {
			return (int) this.characterRAM[mappedAddress % this.characterRAM.length] & 0xFF;
		}
		return (int) this.characterROM[mappedAddress % this.characterROM.length] & 0xFF;
	}

	private void writeChrMappedAddress(int mappedAddress, int value) {
		if (this.characterRAM != null) {
			this.characterRAM[mappedAddress % this.characterRAM.length] = (byte) value;
		}
	}

    protected int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (address <= 0x3FFF) {
            return (address & 0x3FFF) | (this.prgSelect16K << 14);
        } else if (address <= 0x5FFF) {
            return (address & 0x1FFF) | (this.prgSelect8K << 13);
        } else {
            return (address & 0x1FFF) | (0b11111 << 13);
        }
    }

    private int mapPrgRamAddress(int address) {
        return address & 0x1FFF;
    }

    private boolean isPrgRamEnabled() {
        return (this.ppuBankingStyle & (1 << 7)) != 0;
    }

    private boolean passPPUA10() {
        return (this.ppuBankingStyle & (1 << 5)) != 0;
    }

    @Override
    public void cycle() {
        this.vrcirqEngine.cycle();
        this.pulseChannel1.clock();
        this.pulseChannel2.clock();
        this.sawtoothChannel.clock();
    }

    @Override
    public boolean getIRQSignal() {
        return this.vrcirqEngine.getIRQSignal();
    }

    @Override
    public double mixAPUAudio(double apuOutput) {
        return apuOutput + (VRC6_WEIGHT * ((double) (this.pulseChannel1.getDigitalOutput() + this.pulseChannel2.getDigitalOutput() + this.sawtoothChannel.getDigitalOutput()) / 61.0));
    }

    private boolean getHalt() {
        return this.halt;
    }

    private boolean getFrequency16x() {
        return this.frequency16x;
    }

    private boolean getFrequency256x() {
        return this.frequency256x;
    }

    @Override
    protected Optional<byte[]> getPrgRam() {
        return Optional.ofNullable(this.programRAM);
    }

    @Override
    protected Optional<byte[]> getChrRam() {
        return Optional.ofNullable(this.characterRAM);
    }

    private abstract class WaveformChannel {

        private int dividerReload;
        private boolean enabled;

        protected int divider;
        protected int step;

        protected abstract void writeVolume(int value);

        protected void writeLO(int value) {
            this.dividerReload = (this.dividerReload & 0b1111_0000_0000) | (value & 0xFF);
        }

        protected void writeHI(int value) {
            this.enabled = (value & (1 << 7)) != 0;
            this.dividerReload = (this.dividerReload & 0b0000_1111_1111) | ((value & 0b1111) << 8);
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        protected boolean getEnabled() {
            return this.enabled;
        }

        protected int getDividerReload() {
            if (getHalt()) {
                return dividerReload;
            } else if (getFrequency256x()) {
                return dividerReload >> 8;
            } else if (getFrequency16x()) {
                return dividerReload >> 4;
            }
            return dividerReload;
        }

        protected abstract void clock();

        protected abstract int getDigitalOutput();

    }

    private class PulseChannel extends WaveformChannel {

        private int currentVolume;
        private int dutyCycle;
        private boolean mode;

        @Override
        protected void writeHI(int value) {
            super.writeHI(value);
            if (!this.getEnabled()) {
                this.divider = 0;
                this.step = 0;
            }
        }

        @Override
        protected void writeVolume(int value) {
            this.currentVolume = value & 0b1111;
            this.dutyCycle = (value >>> 4) & 0b111;
            this.mode = (value & (1 << 7)) != 0;
        }

        private int getCurrentVolume() {
            return this.currentVolume;
        }

        private int getDutyCycle() {
            return this.dutyCycle;
        }

        private boolean getMode() {
            return this.mode;
        }

        @Override
        protected void clock() {
            if (!this.getEnabled() || getHalt()) {
                return;
            }

            this.divider--;
            if (this.divider < 0) {
                this.divider = this.getDividerReload();
                this.step = (this.step - 1) & 0xF;
            }
        }

        @Override
        protected int getDigitalOutput() {
            if (!this.getEnabled()) {
                return 0;
            } else if (this.getMode()) {
                return this.getCurrentVolume();
            } else if (this.step <= this.getDutyCycle()) {
                return this.getCurrentVolume();
            } else {
                return 0;
            }
        }

    }

    private class SawtoothChannel extends WaveformChannel {

        private int sawVolume;

        private int accumulator;
        private boolean subClock = true;

        @Override
        protected void writeVolume(int value) {
            this.sawVolume = value & 0x3F;
        }

        @Override
        protected void writeHI(int value) {
            super.writeHI(value);
            if (!this.getEnabled()) {
                this.accumulator = 0;
                this.subClock = true;
            }
        }

        @Override
        protected void clock() {
            if (!this.getEnabled() || getHalt()) {
                return;
            }
            this.divider--;
            if (this.divider < 0) {
                this.divider = this.getDividerReload();
                this.subClock = !this.subClock;
                if (this.subClock) {
                    this.step++;
                    if (this.step >= 7) {
                        this.accumulator = 0;
                        this.step = 0;
                    } else {
                        this.accumulator = (this.accumulator + this.sawVolume) & 0xFF;
                    }
                }
            }
        }

        @Override
        protected int getDigitalOutput() {
            if (this.getEnabled()) {
                return (this.accumulator >>> 3) & 0b11111;
            } else {
                return 0;
            }
        }
    }

}
