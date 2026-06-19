package io.github.arkosammy12.jemu.core.nintendo.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.cpu.NES6502;

import static io.github.arkosammy12.jemu.core.nintendo.nes.RP2C02.OAMDATA_ADDR;

public class RP2A03<E extends NESEmulator> implements Bus {

    public static final int SQ1_VOL_ADDR = 0x4000;
    public static final int SQ1_SWEEP_ADDR = 0x4001;
    public static final int SQ1_LO_ADDR = 0x4002;
    public static final int SQ1_HI_ADDR = 0x4003;
    public static final int SQ2_VOL_ADDR = 0x4004;
    public static final int SQ2_SWEEP_ADDR = 0x4005;
    public static final int SQ2_LO_ADDR = 0x4006;
    public static final int SQ2_HI_ADDR = 0x4007;
    public static final int TRI_LINEAR_ADDR = 0x4008;

    public static final int TRI_LO_ADDR = 0x400A;
    public static final int TRI_HI_ADDR = 0x400B;
    public static final int NOISE_VOL_ADDR = 0x400C;

    public static final int NOISE_LO_ADDR = 0x400E;
    public static final int NOISE_HI_ADDR = 0x400F;
    public static final int DMC_FREQ_ADDR = 0x4010;
    public static final int DMC_RAW_ADDR = 0x4011;
    public static final int DMC_START_ADDR = 0x4012;
    public static final int DMC_LEN_ADDR = 0x4013;

    public static final int OAMDMA_ADDR = 0x4014;
    public static final int SND_CHN_ADDR = 0x4015;
    public static final int JOY1_ADDR = 0x4016;
    public static final int JOY2_ADDR = 0x4017;

    private final E emulator;
    protected final NES6502 cpu;
    private final NESAPU<?> apu;
    private final NESController<?> controller;

    private int oamDmaTransferredBytes = 256;
    private int oamDmaSourceAddressHighByte;
    private int oamDmaCurrentData = -1;

    private APUHalfCycleType apuHalfCycleType = APUHalfCycleType.GET;

    private int scheduleDmcDmaHaltCountdown;
    private DmcDmaStep dmcDmaStep = DmcDmaStep.NONE;
    private DmcDmaStep dmcDmaStartStep = DmcDmaStep.DUMMY;
	private NESAPU.DmcDmaType dmcDmaType = NESAPU.DmcDmaType.RELOAD;
    private int dmcDmaAddress;
	private boolean dmcDmaRequestPending;
    private boolean dmcDmaHaltOnly;

    private int internalDataBus;
	
	private int joyOutputEnableAddress = -1;
    private int joyOutputEnableValue;

    public RP2A03(E emulator, int apuSampleBufferSize) {
        this.emulator = emulator;
        this.cpu = new NES6502(emulator);
        this.apu = new NESAPU<>(emulator, apuSampleBufferSize);
        this.controller = new NESController<>(emulator);
    }

    public NES6502 getCpu() {
        return this.cpu;
    }

    public NESAPU<?> getApu() {
        return this.apu;
    }

    public NESController<?> getController() {
        return this.controller;
    }

    public APUHalfCycleType getCurrentApuHalfCycleType() {
        return this.apuHalfCycleType;
    }

    @Override
    public int readByte(int address) {
        if (!this.isDmaUsingExternalBusOnThisCycle()) {
            int readByte = this.readByteRicohCore(address);
            if (readByte >= 0) {
                this.internalDataBus = readByte & 0xFF;
            }
        }

        return this.internalDataBus;
    }
	
    @Override
    public void writeByte(int address, int value) {
        this.releaseJoy1OutputEnable();
        this.internalDataBus = value;
        this.emulator.getCpuBus().writeByte(address, value);
    }

    private int readByteRicohCore(int address) {
        return address == SND_CHN_ADDR ? this.readByteRegister(address) : this.emulator.getCpuBus().readByte(address);
    }

	private void releaseJoy1OutputEnable() {
        this.joyOutputEnableAddress = -1;
    }

    private int readJoyWithOutputEnable(int address) {
        address &= 0xFFFF;

        if (this.joyOutputEnableAddress != address) {
            this.joyOutputEnableAddress = address;
            this.joyOutputEnableValue = switch (address) {
                case JOY1_ADDR -> this.controller.readJoy1();
                case JOY2_ADDR -> this.controller.readJoy2();
                default -> -1;
            };
        }

        return (this.joyOutputEnableValue & ~0xE0) | (this.internalDataBus & 0xE0);
    }

    private int readByteDMA(int address) {
        int combinedAddress = this.getCombinedRicohAddress(address);
        int activatedRegisterByte = -1;

        // The Ricoh register that responds to a DMA read is determined by combining the upper bits of the
        // CPU's current address bus value with the lower 5 bits of the DMA address. If this combined
        // address falls in $4000-$401F, that register is activated.
        if (combinedAddress >= 0x4000 && combinedAddress <= 0x401F) {
            activatedRegisterByte = this.readByteRegister(combinedAddress);
        }

        int readByte;
        if (address >= 0x4000 && address <= 0x401F) {

            // If DMA is trying to read from an internal Ricoh register, it can only do so if the
            // register was activated via the combined Ricoh and CPU address.
            // If no Ricoh register was activated, the final value is open bus.
            readByte = activatedRegisterByte;
		} else {

            // Otherwise, the DMA reads from the external bus as normal
            readByte = this.readByteRicohCore(address);

            if ((combinedAddress == JOY1_ADDR || combinedAddress == JOY2_ADDR) && activatedRegisterByte >= 0) {
                this.joyOutputEnableAddress = combinedAddress;
                this.joyOutputEnableValue = activatedRegisterByte;
            }

            if (activatedRegisterByte >= 0) {
                if ((combinedAddress == JOY1_ADDR || combinedAddress == JOY2_ADDR) && readByte >= 0) {

                    // Controller ports only drive bits 0-4; bits 5-7 are open bus. The final value
                    // merges the open bus bits from the external read with the controller bits.

                    // TODO: This value is what the external bus sees. The CPU on the other hand sees the regular bus conflict
                    // with the ANDing behavior of the driven bits
                    readByte = (readByte & 0xE0) | (activatedRegisterByte & 0x1F);
                } else {
                    // Otherwise, we ignore the value on the external bus and use the internal read from the activated Ricoh register
                    readByte = activatedRegisterByte;
                }
            }
        }

        if (readByte >= 0) {
            // Update the internal data bus if the final value is non-open bus
            this.internalDataBus = readByte & 0xFF;
            if (combinedAddress != SND_CHN_ADDR) {
                // The internal and external data buses are connected, so bus conflicts are visible on both,
                // except for $4015 reads, which only update the internal bus.
                this.emulator.getCpuBus().setExternalDataBus(this.internalDataBus);
            }
        }

        return this.internalDataBus;
    }

    public int readByteRegister(int address) {
        address &= 0xFFFF;

        if ((address >= SQ1_VOL_ADDR && address <= TRI_LINEAR_ADDR) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN_ADDR) {
            this.releaseJoy1OutputEnable();

            int ret = this.apu.readByte(address);
            if (address == SND_CHN_ADDR) {
                ret = (ret & ~0b00100000) | (this.internalDataBus & 0b00100000);
            }
            return ret;
        } else if (address == OAMDMA_ADDR) {
            this.releaseJoy1OutputEnable();
            return -1;
        } else if (address == JOY1_ADDR || address == JOY2_ADDR) {
            return this.readJoyWithOutputEnable(address);
        } else {
            this.releaseJoy1OutputEnable();
            return -1;
        }
    }

    public void writeByteRegister(int address, int value) {
		this.releaseJoy1OutputEnable();

        if ((address >= SQ1_VOL_ADDR && address <= TRI_LINEAR_ADDR) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN_ADDR || address == JOY2_ADDR) {
            this.apu.writeByte(address, value);
        } else if (address == OAMDMA_ADDR) {
            this.oamDmaSourceAddressHighByte = value & 0xFF;
            this.startOAMDMA();
        } else if (address == JOY1_ADDR) {
            this.controller.writeJoy1(value);
        }
    }

    public void cycleHalf() {
		switch (this.cpu.getHalfCyclePhase()) {
			case PHI_1 -> this.cpu.cycle();
			case PHI_2 -> this.onCPUPHI2();
		}
	}

    protected void onCPUPHI2() {
        boolean isHalted = this.cpu.isHalted();

        // We cycle the cartridge before the CPU to avoid any potential CPU writes to PPU registers from messing up
        // MMC3's PPU address tracking between a dot step and the cartridge being cycled, fixing MMC3's double IRQ counter clocking
        this.emulator.getCartridge().cycle();

        this.cpu.cycle();

        if (this.dmcDmaHaltOnly) {
            this.dmcDmaHaltOnly = false;
            this.dmcDmaRequestPending = false;
            this.dmcDmaStartStep = DmcDmaStep.DUMMY;
            this.dmcDmaType = NESAPU.DmcDmaType.RELOAD;
        }

        this.controller.cycle();

        if (this.scheduleDmcDmaHaltCountdown > 0) {
            this.scheduleDmcDmaHaltCountdown--;

            // TODO: On the RP2A07 (PAL), it waits until the fetch cycle of the 6502 via the SYNC pin
            if (this.scheduleDmcDmaHaltCountdown <= 0) {
                // If the start of DMC DMA coincides with the second-to-last or last OAM DMA GET, delay the former by an extra cycle
                if (this.apuHalfCycleType == APUHalfCycleType.GET && this.oamDmaCurrentData < 0 && (this.oamDmaTransferredBytes == 254 || this.oamDmaTransferredBytes == 255)) {
                    this.scheduleDmcDmaHaltCountdown = 1;
                } else {
                    this.startDMCDMA();
                }
            }
        }

        this.apu.cycleHalf();
        this.cycleDma(isHalted);

        this.apuHalfCycleType = this.apuHalfCycleType.getOpposite();
    }

    protected void startDMCDMA() {
		this.scheduleDmcDmaHaltCountdown = 0;

        if (this.dmcDmaStartStep == DmcDmaStep.NONE) {
            this.dmcDmaHaltOnly = true;
        } else {
            this.dmcDmaStep = this.dmcDmaStartStep;
        }

        this.dmcDmaStartStep = DmcDmaStep.DUMMY;
	}

    protected void startOAMDMA() {
        this.oamDmaTransferredBytes = 0;
    }

    private void cycleDma(boolean isHalted) {
        if (!isHalted || (this.oamDmaTransferredBytes >= 256 && this.dmcDmaStep == DmcDmaStep.NONE)) {
            return;
        }
		switch (this.apuHalfCycleType) {
			case GET -> {
				switch (this.dmcDmaStep) {
					case NONE -> this.tickOamDmaGetIfOngoing();
					case DUMMY -> {
						if (this.dmcDmaStartStep == DmcDmaStep.NONE) {
							this.dmcDmaStep = DmcDmaStep.NONE;
							this.dmcDmaRequestPending = false;
							this.dmcDmaStartStep = DmcDmaStep.DUMMY;
						} else if (this.oamDmaTransferredBytes < 256) {
							this.tickOamDmaGetIfOngoing();
							this.dmcDmaStep = DmcDmaStep.GET;
						} else {
							this.dmcDmaStep = DmcDmaStep.ALIGNMENT;
						}
					}
					// Needed so DMC DMA doesn't perform its read immediately after its dummy cycle whenever there's no OAM DMA by this point
					case ALIGNMENT -> this.dmcDmaStep = DmcDmaStep.GET;
					case GET -> {
						if (this.dmcDmaType != NESAPU.DmcDmaType.EXPLICIT_ABORT_0) {
							this.apu.writeDmcDma(this.readByteDMA(this.dmcDmaAddress));
						}

						this.dmcDmaStep = DmcDmaStep.NONE;
						this.dmcDmaRequestPending = false;
						this.dmcDmaType = NESAPU.DmcDmaType.RELOAD;
						this.dmcDmaStartStep = DmcDmaStep.DUMMY;

						NESAPU.DmcDmaType pendingImplicitDmaType = this.apu.getPendingImplicitDmcDmaType();
						if (pendingImplicitDmaType != null) {
							int pendingImplicitDmaAddress = this.apu.getPendingImplicitDmcDmaAddress();
							this.apu.clearPendingImplicitDmcDma();
                            switch (pendingImplicitDmaType) {
                                case IMPLICIT_UNEXPECTED_RELOAD -> this.triggerDmcDma(NESAPU.DmcDmaType.RELOAD, pendingImplicitDmaAddress);
                                case IMPLICIT_ABORT -> this.triggerExplicitStopDmcDma(NESAPU.DmcDmaType.EXPLICIT_ABORT_MINUS_2_OR_3, pendingImplicitDmaAddress);
                            }
						}
					}
				}
			}
			case PUT -> {
				switch (this.dmcDmaStep) {
					case DUMMY -> {
						boolean finalOamPut = this.oamDmaCurrentData >= 0 && this.oamDmaTransferredBytes == 255;
						if (!finalOamPut) {
							// If DMC DMA starts by this point in the OAM DMA transfer, this cycle is the DMC DMA alignment cycle.
							// Do not transition DMC DMA to its GET step yet
							if (!(this.dmcDmaRequestPending && this.oamDmaCurrentData >= 0 && this.oamDmaTransferredBytes == 254)) {
								this.dmcDmaStep = DmcDmaStep.GET;
							}
						}
					}
					case ALIGNMENT -> this.dmcDmaStep = DmcDmaStep.GET;
				}

				if (this.oamDmaCurrentData >= 0 && this.oamDmaTransferredBytes < 256) {
					this.emulator.getCpuBus().writeByte(OAMDATA_ADDR, this.oamDmaCurrentData);
					this.oamDmaTransferredBytes++;
					this.oamDmaCurrentData = -1;
				}
			}
		}
	}

    // Assumes that the CPU is cycled before the DMA units
	private boolean isDmaUsingExternalBusOnThisCycle() {
		return switch (this.apuHalfCycleType) {
			case GET -> switch (this.dmcDmaStep) {
				case NONE, DUMMY, ALIGNMENT -> this.isOamDmaGetCycle();
				case GET -> this.cpu.isHalted();
			};
			case PUT -> switch (this.dmcDmaStep) {
				case NONE, DUMMY, ALIGNMENT, GET -> this.isOamDmaPutCycle();
			};
		};
	}

    private boolean isOamDmaGetCycle() {
        return this.oamDmaTransferredBytes < 256 && this.cpu.isHalted();
    }

    private boolean isOamDmaPutCycle() {
        return this.oamDmaCurrentData >= 0 && this.oamDmaTransferredBytes < 256 && this.cpu.isHalted();
    }

    private void tickOamDmaGetIfOngoing() {
		if (this.oamDmaTransferredBytes < 256) {
			this.oamDmaCurrentData = this.readByteDMA((this.oamDmaSourceAddressHighByte << 8) | (this.oamDmaTransferredBytes & 0xFF));
		}
	}

    void triggerDmcDma(NESAPU.DmcDmaType dmcDmaType, int address) {
		if (dmcDmaType != NESAPU.DmcDmaType.LOAD && dmcDmaType != NESAPU.DmcDmaType.RELOAD) {
            return;
        }

        // Do not replace or stack an already pending or ongoing DMC DMA transfer.
		if (this.dmcDmaRequestPending || this.scheduleDmcDmaHaltCountdown > 0 || this.dmcDmaStep != DmcDmaStep.NONE || this.dmcDmaHaltOnly) {
			return;
		}

		this.dmcDmaRequestPending = true;
        this.dmcDmaStartStep = DmcDmaStep.DUMMY;
		this.dmcDmaType = dmcDmaType;
		this.dmcDmaAddress = address & 0xFFFF;
		this.scheduleDmcDmaHaltCountdown = switch (dmcDmaType) {
			case LOAD -> switch (this.apuHalfCycleType) {
				case GET -> 4;
				case PUT -> 3;
			};
			case RELOAD -> switch (this.apuHalfCycleType) {
				case GET -> 2;
				case PUT -> 1;
			};
            default -> 0;
		};
	}

    void triggerExplicitStopDmcDma(NESAPU.DmcDmaType dmcDmaType, int address) {

		if (dmcDmaType != NESAPU.DmcDmaType.EXPLICIT_ABORT_0 && dmcDmaType != NESAPU.DmcDmaType.EXPLICIT_ABORT_MINUS_1 && dmcDmaType != NESAPU.DmcDmaType.EXPLICIT_ABORT_MINUS_2_OR_3) {
			return;
		}

		// Explicit stop may replace a pending normal reload, but not an already-running
		// non-DUMMY DMA phase.
		if (this.dmcDmaStep != DmcDmaStep.NONE || this.dmcDmaHaltOnly) {
			if ((dmcDmaType == NESAPU.DmcDmaType.EXPLICIT_ABORT_0 || dmcDmaType == NESAPU.DmcDmaType.EXPLICIT_ABORT_MINUS_1) && this.dmcDmaStep == DmcDmaStep.DUMMY) {
				this.dmcDmaType = dmcDmaType;
				this.dmcDmaRequestPending = true;
			}
			return;
		}

		this.dmcDmaRequestPending = true;
		this.dmcDmaType = dmcDmaType;
		this.dmcDmaAddress = address & 0xFFFF;

		this.dmcDmaStartStep = switch (dmcDmaType) {
			case EXPLICIT_ABORT_0, EXPLICIT_ABORT_MINUS_1, LOAD, RELOAD, IMPLICIT_UNEXPECTED_RELOAD -> DmcDmaStep.DUMMY;
			case EXPLICIT_ABORT_MINUS_2_OR_3, IMPLICIT_ABORT -> DmcDmaStep.NONE;
        };

		this.scheduleDmcDmaHaltCountdown = switch (this.apuHalfCycleType) {
			case GET -> 2;
			case PUT -> 1;
		};
	}
	
    public boolean getIRQSignal() {
        return this.apu.getIRQSignal();
    }

    public boolean getRDYSignal() {
        return this.oamDmaTransferredBytes < 256 || this.dmcDmaStep != DmcDmaStep.NONE || this.dmcDmaHaltOnly;
    }

    private int getCombinedRicohAddress(int address) {
        return (this.cpu.getLastAddress() & 0xFFE0) | (address & 0x1F);
    }

    public enum APUHalfCycleType {
        GET,
        PUT;

        private APUHalfCycleType getOpposite() {
            return switch (this) {
                case GET -> PUT;
                case PUT -> GET;
            };
        }
    }

    private enum DmcDmaStep {
		NONE,
		DUMMY,
		// Alignment state needed for already finished OAM DMA while DMC DMA still needs an alignment cycle
		ALIGNMENT,
		GET
	}

}