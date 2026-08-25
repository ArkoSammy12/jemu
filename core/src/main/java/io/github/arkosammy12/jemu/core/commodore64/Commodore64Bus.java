package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.util.ByteSizes.*;

public class Commodore64Bus<E extends Commodore64Emulator> implements Bus {

    private final E emulator;

    private final byte @Nullable [] prgFile;

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

        Optional<byte[]> optionalROM = emulator.getHost().getRom();
        Optional<Path> optionalROMPath = emulator.getHost().getRomPath();
        if (optionalROM.isPresent() && optionalROMPath.isPresent()) {
            Path path = optionalROMPath.get();
            String extension = FilenameUtils.getExtension(path.toString());
            if (!"prg".equalsIgnoreCase(extension)) {
                throw new ROMInitializationException("Non .prg files are not supported for HLE!");
            }
            byte[] bytes = optionalROM.get();
            this.prgFile = Arrays.copyOf(bytes, bytes.length);

        } else {
            this.prgFile = null;
        }
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

    public void patchPrgFile() {
        if (this.prgFile == null) {
            return;
        }

        int start = (this.prgFile[0] & 0xFF) | ((this.prgFile[1] & 0xFF) << 8);
        int end = (start + this.prgFile.length - 2) & 0xFFFF;

        System.arraycopy(this.prgFile, 2, this.ram, start, Math.min(this.prgFile.length - 2, 0xFFFF + 1 - start));
        this.ram[0x2B] = (byte) (start & 0xFF);
        this.ram[0xAC] = (byte) (start & 0xFF);
        this.ram[0x2C] = (byte) ((start >>> 8) & 0xFF);
        this.ram[0xAD] = (byte) ((start >>> 8) & 0xFF);
        this.ram[0x2D] = (byte) (end & 0xFF);
        this.ram[0x2F] = (byte) (end & 0xFF);
        this.ram[0x31] = (byte) (end & 0xFF);
        this.ram[0xAE] = (byte) (end & 0xFF);
        this.ram[0x2E] = (byte) ((end >>> 8) & 0xFF);
        this.ram[0x30] = (byte) ((end >>> 8) & 0xFF);
        this.ram[0x32] = (byte) ((end >>> 8) & 0xFF);
        this.ram[0xAF] = (byte) ((end >>> 8) & 0xFF);

        if (((int)this.ram[0x00C6] & 0xFF) == 0) {
            if (start == 0x0801) {
                this.ram[0x0277] = (byte) 'R';
                this.ram[0x0278] = (byte) 'U';
                this.ram[0x0279] = (byte) 'N';
                this.ram[0x027a] = (byte) 0x0d;  // Return

                // Publish the characters last, so the KERNAL cannot see a partial command.
                this.ram[0x00c6] = 4;
            } else {
                String text = "SYS %s\r".formatted(Integer.toString(start));
                for (int i = 0; i < text.length(); i++) {
                    this.ram[0x0277 + i] = (byte) text.charAt(i);
                }
                this.ram[0x00C6] = (byte) text.length();
            }
        }
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
                        // IO 1
                        yield this.combineWithDataBus(0, 0x00);
                    } else {
                        // IO 2
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
                        // IO 1
                    } else {
                        // IO 2
                    }
                }
            }
        } else {
            this.ram[address] = (byte) value;
        }
    }

    public int readVIC2(int address) {
        address = (address & 0x3FFF) | ((~this.emulator.getCIA2IOPortA().read() & 0b11) << 14);
        int ret;
        if ((address >= 0x1000 && address <= 0x1FFF) || (address >= 0x9000 && address <= 0x9FFF)) {
            ret = (int) this.characterROM[address & 0xFFF] & 0xFF;
        } else {
            ret = (int) this.ram[address] & 0xFF;
        }
        ret |= (((int) this.colorRAM[address & 0x3FF] & 0xF) << 8);
        this.dataBus = ret;
        return ret;
    }

    public int combineWithDataBus(int value, int validBitsMask) {
        return (value & validBitsMask & 0xFF) | (this.dataBus & ~validBitsMask);
    }

}
