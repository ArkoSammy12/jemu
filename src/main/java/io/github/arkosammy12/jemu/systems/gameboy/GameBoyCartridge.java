package io.github.arkosammy12.jemu.systems.gameboy;

import io.github.arkosammy12.jemu.systems.common.Bus;

import java.util.Arrays;

public abstract class GameBoyCartridge implements Bus {

    public static final int CARTRIDGE_TYPE_ADDRESS = 0x0147;
    public static final int ROM_SIZE_ADDRESS = 0x0148;
    public static final int RAM_SIZE_ADDRESS = 0x0149;

    protected final int[] originalRom;

    protected final int cartridgeType;
    protected final int romSizeHeader;
    protected final int ramSizeHeader;

    public GameBoyCartridge(GameBoyEmulator emulator, int cartridgeType) {
        int[] rom = emulator.getEmulatorSettings().getRom();
        this.originalRom = Arrays.copyOf(rom, rom.length);
        this.cartridgeType = cartridgeType;
        this.romSizeHeader = rom[ROM_SIZE_ADDRESS];
        this.ramSizeHeader = rom[RAM_SIZE_ADDRESS];
    }

    public static GameBoyCartridge getCartridge(GameBoyEmulator emulator) {
        int cartridgeType = emulator.getEmulatorSettings().getRom()[CARTRIDGE_TYPE_ADDRESS];
        return switch (cartridgeType) {
            case 0x01, 0x02, 0x03 -> new MBC1(emulator, cartridgeType);
            case 0x05, 0x06 -> new MBC2(emulator, cartridgeType);
            case 0x0F, 0x10, 0x11, 0x12, 0x13 -> new MBC3(emulator, cartridgeType);
            case 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E -> new MBC5(emulator, cartridgeType);
            default -> new MBC0(emulator, cartridgeType);
        };
    }

    public void cycle() {

    }

}
