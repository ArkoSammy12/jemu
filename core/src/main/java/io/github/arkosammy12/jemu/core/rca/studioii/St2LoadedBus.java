package io.github.arkosammy12.jemu.core.rca.studioii;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

import static io.github.arkosammy12.jemu.core.rca.studioii.RCAStudioIIBus.SYSTEM_ROM;

public class St2LoadedBus implements Bus {

    private final byte[][] cartridgeRomPages = new byte[256][];
    private final byte[] ram = new byte[512];

    public St2LoadedBus(byte[] file) {
        if (file.length < 0x200) {
            throw new EmulatorException("Invalid .st2 ROM file (Must be at least 512 bytes)!");
        }
        if (file[0] != 'R' || file[1] != 'C' || file[2] != 'A' || file[3] != '2') {
            throw new EmulatorException("Invalid .st2 ROM file (bad magic)!");
        }
        int blocks = (int) file[4] & 0xFF;
        if (file.length != (blocks << 8)) {
            throw new EmulatorException("Invalid .st2 ROM file. Expected 0x%04X bytes, but found 0x%04X bytes instead!".formatted(blocks << 8, file.length));
        }
        if (blocks < 2 || blocks > 65) {
            throw new EmulatorException("Invalid .st2 ROM file. Invalid block number %04X. Must be between 2 and 65!".formatted(blocks));
        }
        for (int i = 0; i < (blocks - 1); i++) {
            int blockPage = (int) file[64 + i] & 0xFF;
            if (blockPage == 0x00) {
                continue;
            }
            byte[] page = new byte[256];
            System.arraycopy(file, 256 + (256 * i), page, 0, page.length);
            this.cartridgeRomPages[blockPage] = page;
        }
    }

    @Override
    public int readByte(int address) {
        address &= 0xFFFF;
        int page = address >>> 8;
        if (this.cartridgeRomPages[page] != null) {
            return (int) this.cartridgeRomPages[page][address & 0xFF] & 0xFF;
        } else if (address <= 0x7FF) {
            return (int) SYSTEM_ROM[address & 0x7FF] & 0xFF;
        } else {
            address &= 0x3FF;
            if (address <= 0x1FF) {
                return (int) this.ram[address & 0x1FF] & 0xFF;
            } else {
                return 0xFF;
            }
        }
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0xFFFF;
        if (address > 0x7FF && this.cartridgeRomPages[address >>> 8] == null) {
            address &= 0x3FF;
            if (address <= 0x1FF) {
                this.ram[address & 0x1FF] = (byte) value;
            }
        }
    }

}
