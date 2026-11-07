package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.atari2600.cartridges.*;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.Optional;

public abstract class Atari2600Cartridge<E extends Atari2600Emulator> {

    protected final E emulator;
    protected final byte[] rom;

    public Atari2600Cartridge(E emulator) {
        this.emulator = emulator;
        Optional<byte[]> rom = emulator.getHost().getRom();
        if (rom.isEmpty()) {
            throw new MissingROMException(emulator);
        }
        this.rom = rom.get();
    }

    public int readByte(int address, int dataBus) {
        return (int) this.rom[this.mapROMAddress(address) % this.rom.length] & 0xFF;
    }

    public void writeByte(int address, int value) {

    }

    public void cycle() {

    }

    protected abstract int mapROMAddress(int address);

    protected final int getLastROMBankNumber(int bankWindowSize, int bankNumberMask) {
        return ((this.rom.length / bankWindowSize) - 1) & bankNumberMask;
    }

    public static <E extends Atari2600Emulator> Atari2600Cartridge<E> getCartridge(E emulator) {
        Type cartridgeType = emulator.getHost().getCartridgeTypeOverride().or(() -> emulator.getHost().getCartridgeInfo().flatMap(Atari2600SystemHost.CartridgeInfo::getCartridgeType)).orElse(Type.CART_4K);
        return switch (cartridgeType) {
            case CART_0840 -> new Cartridge0840<>(emulator);
            case CART_3E -> new Cartridge3E<>(emulator);
            case CART_3F -> new Cartridge3F<>(emulator);
            case CART_2K -> new Cartridge2K<>(emulator);
            case CART_4K -> new Cartridge4K<>(emulator);
            case CART_F4 -> new CartridgeF4<>(emulator);
            case CART_F6 -> new CartridgeF6<>(emulator);
            case CART_F8 -> new CartridgeF8<>(emulator);
            case CART_FA -> new CartridgeFA<>(emulator);
            case CART_FE -> new CartridgeFE<>(emulator);
            default -> throw new ROMInitializationException("Unimplemented Atari 2600 cartridge type \"%s\"!".formatted(cartridgeType.getName()));
        };
    }

    public enum Type {
        CART_2K("2K"),
        CART_4K("4K"),
        CART_4KSC("4KSC"),
        CART_F4("F4"),
        CART_F4SC("F4SC"),
        CART_F6("F6"),
        CART_F6SC("F6SC"),
        CART_F8("F8"),
        CART_F8SC("F8SC"),
        CART_F0("F0"),
        CART_FA("FA"),
        CART_FA2("FA2"),
        CART_FE("FE"),
        CART_E0("E0"),
        CART_E7("E7"),
        CART_EF("EF"),
        CART_EFSC("EFSC"),
        CART_3E("3E"),
        CART_3EPLUS("3E+"),
        CART_3F("3F"),
        CART_0840("0840"),
        CART_4A50("4A50"),
        CART_AR("AR"),
        CART_CV("CV"),
        CART_UA("UA"),
        CART_SB("SB"),
        CART_WD("WD"),
        CART_X07("X07"),
        CART_MDM("MDM"),
        CART_MVC("MVC"),
        CART_BF("BF"),
        CART_BFSC("BFSC"),
        CART_DF("DF"),
        CART_DFSC("DFSC"),
        CART_DPC("DPC"),
        CART_DPCPLUS("DPC+"),
        CART_CDF("CDF"),
        CART_GL("GL"),
        CART_TVBOY("TVBoy");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

    }

}
