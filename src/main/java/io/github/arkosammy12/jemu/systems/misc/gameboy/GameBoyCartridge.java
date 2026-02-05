package io.github.arkosammy12.jemu.systems.misc.gameboy;

import io.github.arkosammy12.jemu.systems.GameBoyEmulator;
import io.github.arkosammy12.jemu.systems.bus.Bus;

import java.util.Arrays;

public abstract class GameBoyCartridge implements Bus {

    public static final int CARTRIDGE_TYPE_ADDRESS = 0x0147;
    public static final int ROM_SIZE_ADDRESS = 0x0148;
    public static final int RAM_SIZE_ADDRESS = 0x0149;

    protected final int[] originalRom;

    protected final int cartridgeType;
    protected final int romBankAmount;
    protected final int ramBankAmount;

    protected final int[] rom0 = new int[0x4000];

    public GameBoyCartridge(GameBoyEmulator emulator, int cartridgeType) {
        int[] rom = emulator.getEmulatorSettings().getRom();
        this.originalRom = Arrays.copyOf(rom, rom.length);

        System.arraycopy(this.originalRom, 0, this.rom0, 0, this.rom0.length);

        this.cartridgeType = cartridgeType;
        this.romBankAmount = rom[ROM_SIZE_ADDRESS];
        this.ramBankAmount = rom[RAM_SIZE_ADDRESS];

    }

    public static GameBoyCartridge getCartridge(GameBoyEmulator emulator) {
        int cartridgeType = emulator.getEmulatorSettings().getRom()[CARTRIDGE_TYPE_ADDRESS];
        return switch (cartridgeType) {
            //case 0x01, 0x02, 0x03 -> new MBC1(emulator,  cartridgeType);
            default -> new MBC0(emulator, cartridgeType);
        };
    }

}
