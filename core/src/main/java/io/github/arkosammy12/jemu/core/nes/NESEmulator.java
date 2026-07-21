package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.hardware.NMOS6502;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Optional;
import java.util.function.BooleanSupplier;

public class NESEmulator implements Emulator, NMOS6502.SystemBus, Resetable {

    private static final int NTSC_MASTER_CLOCK_FREQUENCY_HZ = 236_250_000 / 11;
    private static final int NTSC_CPU_CLOCK_DIVISOR = 12;
    private static final int NTSC_PPU_CLOCK_DIVISOR = 4;
    private static final int NTSC_FRAMERATE = 60;

    private static final int PAL_MASTER_CLOCK_FREQUENCY_HZ = (int) Math.round(26_601_712.5);
    private static final int PAL_CPU_CLOCK_DIVISOR = 16;
    private static final int PAL_PPU_CLOCK_DIVISOR = 5;
    private static final int PAL_FRAMERATE = 50;

    private final NESHost systemHost;
    private String loadedRomFileName = "";

    private final RP2A03<?> ricohCore;
    private final RP2C02<?> ppu;
    private final NESCPUBus<?> cpuBus;

    private NESCartridge<?> cartridge;

    private final TVSystem tvSystem;
    private final int iterationsPerFrame;

    private final Runnable runCycleFunction;
    private final Runnable resetRunCycleFunction;
    private Runnable currentRunCycleFunction;

    private final BooleanSupplier deassertRESSupplier;
    private final BooleanSupplier assertRESSupplier;
    private BooleanSupplier resLineLevelSupplier;

    private final int framerate;

    private final int cpuSubCycleDivisor;
    private int cpuDivisorCounter;

    private final int ppuSubCycleDivisor;
    private int ppuDivisorCounter;

    public NESEmulator(NESHost systemHost) {
        this.systemHost = systemHost;
        Optional<byte[]> optionalROM = systemHost.getRom();
        if (optionalROM.isEmpty()) {
            throw new MissingROMException(systemHost);
        }

        byte[] rom = optionalROM.get();
        this.cartridge = NESCartridge.getCartridge(this, INESFile.getINESFile(this, rom));
        systemHost.getRomPath().ifPresent(path -> {
            this.loadedRomFileName = path.getFileName().toString();
        });

        this.tvSystem = this.cartridge.getINESFile().getTVSystem();
        boolean deriveCyclesFromMasterClock;
        switch (this.tvSystem) {
            case NTSC, MULTIPLE_REGION -> {
                this.framerate = NTSC_FRAMERATE;
                // Calculated based on fixed ratio
                this.iterationsPerFrame = NTSC_MASTER_CLOCK_FREQUENCY_HZ / NTSC_CPU_CLOCK_DIVISOR / NTSC_FRAMERATE;
                this.cpuSubCycleDivisor = NTSC_CPU_CLOCK_DIVISOR / 2;
                this.ppuSubCycleDivisor = NTSC_PPU_CLOCK_DIVISOR / 2;
                deriveCyclesFromMasterClock = false;

                this.ricohCore = new RP2A03<>(this, this.iterationsPerFrame);
                this.ppu = new RP2C02<>(this);
            }
            case PAL -> {
                this.framerate = PAL_FRAMERATE;
                // Doubling master clock frequency to account of half-cycle stepping
                this.iterationsPerFrame = (PAL_MASTER_CLOCK_FREQUENCY_HZ * 2) / PAL_FRAMERATE;
                this.cpuSubCycleDivisor = PAL_CPU_CLOCK_DIVISOR;
                this.ppuSubCycleDivisor = PAL_PPU_CLOCK_DIVISOR;
                deriveCyclesFromMasterClock = true;

                this.ricohCore = new RP2A07<>(this, (PAL_MASTER_CLOCK_FREQUENCY_HZ) / PAL_CPU_CLOCK_DIVISOR / PAL_FRAMERATE);
                this.ppu = new RP2C07<>(this);
            }
            case DENDY -> throw new EmulatorException("Dendy NES system not supported");
            default -> throw new EmulatorException("%S NES system not supported".formatted(this.tvSystem));
        }

        this.cpuBus = new NESCPUBus<>(this);

        if (deriveCyclesFromMasterClock) {
            this.runCycleFunction = () -> {
                this.cpuDivisorCounter--;
                if (this.cpuDivisorCounter <= 0) {
                    this.ricohCore.cycleHalf();
                    this.cpuDivisorCounter = this.cpuSubCycleDivisor;
                }

                this.ppuDivisorCounter--;
                if (this.ppuDivisorCounter <= 0) {
                    this.ppu.cycleHalfDot();
                    this.ppuDivisorCounter = this.ppuSubCycleDivisor;
                }
            };
        } else {
            this.runCycleFunction = () -> {
                this.ricohCore.cycleHalf();
                this.ppu.cycleHalfDot();
                this.ppu.cycleHalfDot();
                this.ppu.cycleHalfDot();

                this.ricohCore.cycleHalf();
                this.ppu.cycleHalfDot();
                this.ppu.cycleHalfDot();
                this.ppu.cycleHalfDot();
            };
        }

        this.resetRunCycleFunction = () -> {

            // Hotswap cartridge of ROM filename changed
            systemHost.getRomPath().ifPresent(path -> systemHost.getRom().ifPresent(newRom -> {
                String romPathFilename = path.getFileName().toString();
                if (!this.loadedRomFileName.equals(romPathFilename)) {
                    if (this.cartridge != null) {
                        this.cartridge.save();
                    }
                    this.loadedRomFileName = romPathFilename;
                    this.cartridge = NESCartridge.getCartridge(this, INESFile.getINESFile(this, newRom));
                }
            }));

            this.ricohCore.reset();
            this.ppu.reset();
            this.runCycleFunction.run();
            this.currentRunCycleFunction = this.runCycleFunction;
        };

        this.currentRunCycleFunction = this.runCycleFunction;

        this.deassertRESSupplier = () -> false;
        this.assertRESSupplier = () -> {
            if (this.getRicohCore().getCpu().getHalfCyclePhase() == NMOS6502.Phase.PHI_2) {
                this.resLineLevelSupplier = this.deassertRESSupplier;
            }
            return true;
        };
        this.resLineLevelSupplier = this.deassertRESSupplier;
    }

    @Override
    public NESHost getHost() {
        return this.systemHost;
    }

    public RP2A03<?> getRicohCore() {
        return this.ricohCore;
    }

    public Processor getCpu() {
        return this.ricohCore.getCpu();
    }

    @Override
    public RP2C02<?> getVideoGenerator() {
        return this.ppu;
    }

    @Override
    public NESAPU<?> getAudioGenerator() {
        return this.ricohCore.getApu();
    }

    @Override
    public NESController<?> getSystemController() {
        return this.ricohCore.getController();
    }

    @Override
    public RP2A03<?> getBus() {
        return this.ricohCore;
    }

    public NESCPUBus<?> getCpuBus() {
        return this.cpuBus;
    }

    public NESCartridge<?> getCartridge() {
        return this.cartridge;
    }

    public TVSystem getTVSystem() {
        return this.tvSystem;
    }

    @Override
    public void executeFrame() {
        for (int i = 0; i < this.iterationsPerFrame; i++) {
            this.currentRunCycleFunction.run();
        }
    }

    @Override
    public void executeCycle() {
        this.currentRunCycleFunction.run();
    }

    @Override
    public int getFramerate() {
        return this.framerate;
    }

    @Override
    public boolean getIRQ() {
        return this.ricohCore.getIRQSignal() || this.cartridge.getIRQSignal();
    }

    @Override
    public boolean getNMI() {
        return this.ppu.getNMISignal();
    }

    @Override
    public boolean getRES() {
        return this.resLineLevelSupplier.getAsBoolean();
    }

    @Override
    public boolean getRDY() {
        return this.ricohCore.getRDYSignal();
    }

    @Override
    public void reset() {
        this.currentRunCycleFunction = this.resetRunCycleFunction;
        this.resLineLevelSupplier = this.assertRESSupplier;
    }

    @Override
    public void close() {
        try {
            if (this.cartridge != null) {
                this.cartridge.save();
            }
        } catch (Exception e) {
            throw new EmulatorException("Error releasing emulator resources!", e);
        }
    }

    public enum TVSystem {
        NTSC,
        PAL,
        DENDY,
        MULTIPLE_REGION
    }

}
