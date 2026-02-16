package io.github.arkosammy12.jemu.systems.gameboy;

public class MBC3 extends GameBoyCartridge {

    private static final int CLOCK_FREQUENCY = 32768;

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
