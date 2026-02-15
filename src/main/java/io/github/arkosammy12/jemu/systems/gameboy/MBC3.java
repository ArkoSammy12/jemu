package io.github.arkosammy12.jemu.systems.gameboy;

public class MBC3 extends GameBoyCartridge {

    public MBC3(GameBoyEmulator emulator, int cartridgeType) {
        super(emulator, cartridgeType);
    }

    @Override
    public int readByte(int address) {
        return 0;
    }

    @Override
    public void writeByte(int address, int value) {

    }
}
