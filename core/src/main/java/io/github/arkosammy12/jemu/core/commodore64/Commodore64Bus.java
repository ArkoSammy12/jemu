package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.arkosammy12.jemu.core.util.ByteSizes.*;

public class Commodore64Bus<E extends Commodore64Emulator> implements Bus {

    private final E emulator;

    private final byte[] kernalROM = new byte[KB_8];
    private final byte[] basicROM = new byte[KB_8];
    private final byte[] characterROM = new byte[KB_4];

    private final byte[] ram = new byte[KB_64];
    private final byte[] colorRAM = new byte[KB_1];

    private int dataBus;

    public Commodore64Bus(E emulator) {
        this.emulator = emulator;
        this.tryLoadROM(emulator.getHost().getKernalROMPath().orElse(null), this.kernalROM, "Kernal");
        this.tryLoadROM(emulator.getHost().getBASICRomPath().orElse(null), this.basicROM, "BASIC");
        this.tryLoadROM(emulator.getHost().getCharacterROMPath().orElse(null), this.characterROM, "Character");
    }

    private void tryLoadROM(@Nullable Path sourcePath, byte[] destination, String romName) {
        if (sourcePath == null) {
            throw new MissingROMException("Missing Commodore 64 %s ROM image!".formatted(romName));
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(sourcePath);
        } catch (Exception e) {
            throw new ROMInitializationException("Failed to read Commodore 64 %s ROM image!".formatted(romName), e);
        }

        if (bytes.length != destination.length) {
            throw new ROMInitializationException("Expected Commodore 64 %s ROM image to be %d bytes long, but was %d bytes instead!".formatted(romName, destination.length, bytes.length));
        }
        System.arraycopy(bytes, 0, destination, 0, destination.length);
    }

    @Override
    public int readByte(int address) {
        int ret;
        if (address >= 0xA000 && address <= 0xBFFF) {
            ret = switch (this.emulator.getCPUIOPort().read() & 0b111) {
                case 3, 7 -> (int) this.basicROM[address & 0x1FFF] & 0xFF;
                default -> (int) this.ram[address] & 0xFF;
            };
        } else if (address >= 0xD000 && address <= 0xDFFF) {
            ret = switch (this.emulator.getCPUIOPort().read() & 0b111) {
                case 0, 4 -> (int) this.ram[address] & 0xFF;
                case 1, 2, 3 -> (int) this.characterROM[address & 0xFFF] & 0xFF;
                default -> {
                    if (address <= 0xD3FF) {
                        yield this.emulator.getVideoGenerator().readByte(address);
                    } else if (address <= 0xD7FF) {
                        yield this.emulator.getAudioGenerator().readByte(address);
                    } else if (address <= 0xDBFF) {
                        yield this.combineWithDataBus((int) this.colorRAM[address & 0x3FF] & 0xFF, 0x0F);
                    } else if (address <= 0xDCFF) {
                        yield this.emulator.getCIA1().readByte(address);
                    } else if (address <= 0xDDFF) {
                        yield this.emulator.getCIA2().readByte(address);
                    } else if (address <= 0xDEFF) {
                        yield this.combineWithDataBus(0, 0x00);
                    } else {
                        yield this.combineWithDataBus(0, 0x00);
                    }
                }
            };
        } else if (address >= 0xE000 && address <= 0xFFFF) {
            ret = switch (this.emulator.getCPUIOPort().read() & 0b111) {
                case 0, 1, 4, 5 -> (int) this.ram[address] & 0xFF;
                default -> (int) this.kernalROM[address & 0x1FFF] & 0xFF;
            };
        } else {
            ret = (int) this.ram[address] & 0xFF;
        }
        this.dataBus = ret;
        return this.dataBus;
    }

    @Override
    public void writeByte(int address, int value) {
        value &= 0xFF;
        this.dataBus = value;
        if (address >= 0xD000 && address <= 0xDFFF) {
            switch (this.emulator.getCPUIOPort().read() & 0b111) {
                case 0, 1, 2, 3, 4 -> this.ram[address] = (byte) value;
                default -> {
                    if (address <= 0xD3FF) {
                        this.emulator.getVideoGenerator().writeByte(address, value);
                    } else if (address <= 0xD7FF) {
                        this.emulator.getAudioGenerator().writeByte(address, value);
                    } else if (address <= 0xDBFF) {
                        this.colorRAM[address & 0x3FF] = (byte) value;
                    } else if (address <= 0xDCFF) {
                        this.emulator.getCIA1().writeByte(address, value);
                    } else if (address <= 0xDDFF) {
                        this.emulator.getCIA2().writeByte(address, value);
                    } else if (address <= 0xDEFF) {

                    } else {

                    }
                }
            }
        } else {
            this.ram[address] = (byte) value;
        }
    }

    public int readVIC2(int address) {
        address = address | ((~this.emulator.getCIA2IOPort().read() & 0b11) << 14);
        int ret;
        if ((address >= 0x1000 && address <= 0x1FFF) || (address >= 0x9000 && address <= 0x9FFF)) {
            ret = (int) this.characterROM[address & 0xFFF] & 0xFF;
        } else {
            ret = (int) this.ram[address] & 0xFF;
        }
        return ret | (((int) this.colorRAM[address & 0x3FF] & 0xF) << 8);
    }

    public int combineWithDataBus(int value, int validBitsMask) {
        return (value & validBitsMask & 0xFF) | (this.dataBus & ~validBitsMask);
    }

}
