package io.github.arkosammy12.jemu.core.chip8.bus;

import io.github.arkosammy12.jemu.core.chip8.Chip8Emulator;
import io.github.arkosammy12.jemu.core.chip8.Chip8Host;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.Optional;

public class Chip8Bus implements Bus {

    private static final int MEMORY_BOUNDS_MASK = 0xFFF;
    private static final int MEMORY_SIZE = MEMORY_BOUNDS_MASK + 1;
    private static final int PROGRAM_START = 0x200;

    protected final byte[] memory;
    protected final int memoryBoundsMask;

    public Chip8Bus(Chip8Emulator emulator) {
        Optional<byte[]> optionalRom = emulator.getHost().getRom();
        if (optionalRom.isEmpty()) {
            throw new MissingROMException(emulator);
        }
        this.memoryBoundsMask = this.getMemoryBoundsMask();
        this.memory = new byte[this.getMemorySize()];
        try {
            byte[] rom = optionalRom.get();
            System.arraycopy(rom, 0, this.memory, this.getProgramStart(), rom.length);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ROMInitializationException("ROM size too big for selected CHIP-8 variant %s!".formatted(emulator.getHost().getSystemName()));
        } catch (Exception e) {
            throw new ROMInitializationException("Failed to initialize memory for CHIP-8 variant %s".formatted(emulator.getHost().getSystemName()), e);
        }
    }

    public int getMemorySize() {
        return MEMORY_SIZE;
    }

    public int getMemoryBoundsMask() {
        return MEMORY_BOUNDS_MASK;
    }

    public int getProgramStart() {
        return PROGRAM_START;
    }

    public void loadFont(Chip8Host.SpriteFont spriteFont) {
        try {
            spriteFont.getSmallFont().ifPresent(smallFont -> {
                for (int i = 0; i < smallFont.length; i++) {
                    byte[] slice = smallFont[i];
                    int sliceLength = slice.length;
                    int offset = spriteFont.getSmallFontBeginOffset() + (sliceLength * i);
                    System.arraycopy(slice, 0, this.memory, offset, sliceLength);
                }
            });
            spriteFont.getBigFont().ifPresent(bigFont -> {
                for (int i = 0; i < bigFont.length; i++) {
                    byte[] slice = bigFont[i];
                    int sliceLength = slice.length;
                    int offset = spriteFont.getBigFontBeginOffset() + (sliceLength * i);
                    System.arraycopy(slice, 0, this.memory, offset, sliceLength);
                }
            });
        } catch (Exception e) {
            throw new EmulatorException("Failed to initialize CHIP-8 font in memory", e);
        }
    }

    @Override
    public int readByte(int address) {
        return (int) this.memory[address & this.memoryBoundsMask] & 0xFF;
    }

    @Override
    public void writeByte(int address, int value) {
        this.memory[address & this.memoryBoundsMask] = (byte) value;
    }

}
