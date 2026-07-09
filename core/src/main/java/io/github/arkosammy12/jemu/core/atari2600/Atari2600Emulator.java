package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.hardware.MOS6532;
import io.github.arkosammy12.jemu.core.hardware.NMOS6507;

import static io.github.arkosammy12.jemu.core.atari2600.Atari2600Controller.Actions.*;

public class Atari2600Emulator implements Emulator, NMOS6507.SystemBus, MOS6532.SystemBus, TIA.SystemBus {

    private static final int NTSC_CPU_CLOCK_SPEED = 1193182;
    private static final int NTSC_FRAMERATE = 60;

    private static final int PAL_CPU_CLOCK_SPEED = 1182298;
    private static final int PAL_FRAMERATE = 50;

    private final SystemHost systemHost;

    private final NMOS6507 cpu;
    private final TIA<?> tia;
    private final MOS6532<?> pia;
    private final Atari2600Bus<?> bus;
    private final Atari2600Controller<?> controller;
    private final Atari2600Cartridge<?> cartridge;

    private final int framerate;
    private final int iterationsPerFrame;

    public Atari2600Emulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.framerate = NTSC_FRAMERATE;
        this.iterationsPerFrame = NTSC_CPU_CLOCK_SPEED / this.framerate;

        this.controller = new Atari2600Controller<>(this);
        this.cpu = new NMOS6507(this);
        this.tia = new TIA<>(this, this.iterationsPerFrame);
        this.pia = new MOS6532<>(this);
        this.bus = new Atari2600Bus<>(this);
        this.cartridge = Atari2600Cartridge.getCartridge(this);

    }

    @Override
    public Atari2600Bus<?> getBus() {
        return this.bus;
    }

    @Override
    public SystemHost getHost() {
        return this.systemHost;
    }

    @Override
    public VideoGenerator getVideoGenerator() {
        return this.tia;
    }

    @Override
    public AudioGenerator getAudioGenerator() {
        return this.tia;
    }

    @Override
    public SystemController getSystemController() {
        return this.controller;
    }

    public TIA<?> getTIA() {
        return this.tia;
    }

    public MOS6532<?> getPIA() {
        return this.pia;
    }

    public Atari2600Cartridge<?> getCartridge() {
        return this.cartridge;
    }

    @Override
    public void executeFrame() {
        for (int i = 0; i < this.iterationsPerFrame; i++) {
            this.runCycle();
        }
    }

    @Override
    public void executeCycle() {
        this.runCycle();
    }

    private void runCycle() {
        this.cpu.cycle();
        this.cpu.cycle();
        this.tia.cycle();
        this.pia.cycle();
    }

    @Override
    public int getFramerate() {
        return this.framerate;
    }

    @Override
    public boolean getIRQ() {
        return this.pia.getIRQSignal();
    }

    @Override
    public boolean getNMI() {
        return false;
    }

    @Override
    public boolean getRES() {
        return false;
    }

    @Override
    public boolean getRDY() {
        return this.tia.getRDYSignal();
    }

    @Override
    public int readSWCHA(int ddrA) {
        int ret = this.controller.isActionPressed(JOYSTICK1_UP) ? 0 : 1;
        ret |= this.controller.isActionPressed(JOYSTICK1_DOWN) ? 0 : 1 << 1;
        ret |= this.controller.isActionPressed(JOYSTICK1_LEFT) ? 0 : 1 << 2;
        ret |= this.controller.isActionPressed(JOYSTICK1_RIGHT) ? 0 : 1 << 3;
        ret |= this.controller.isActionPressed(JOYSTICK0_UP) ? 0 : 1 << 4;
        ret |= this.controller.isActionPressed(JOYSTICK0_DOWN) ? 0 : 1 << 5;
        ret |= this.controller.isActionPressed(JOYSTICK0_LEFT) ? 0 : 1 << 6;
        ret |= this.controller.isActionPressed(JOYSTICK0_RIGHT) ? 0 : 1 << 7;
        return ret;
    }

    @Override
    public int readSWCHB(int ddrB) {
        int ret = this.getP1Difficulty() ? 1 << 7 : 0;
        ret |= this.getP0Difficulty() ? 1 << 6 : 0;
        ret |= this.getColor() ? 1 << 3 : 0;
        ret |= this.getGameSelect() ? 1 << 1 : 0;
        ret |= this.getGameReset() ? 1 : 0;
        return ret;
    }

    @Override
    public void writeSWCHA(int value, int ddrA) {

    }

    @Override
    public void writeSWCHB(int value, int ddrB) {

    }

    @Override
    public boolean getI0() {
        return false;
    }

    @Override
    public boolean getI1() {
        return false;
    }

    @Override
    public boolean getI2() {
        return false;
    }

    @Override
    public boolean getI3() {
        return false;
    }

    @Override
    public boolean getI4() {
        return !this.controller.isActionPressed(JOYSTICK0_FIRE);
    }

    @Override
    public boolean getI5() {
        return !this.controller.isActionPressed(JOYSTICK1_FIRE);
    }

    @Override
    public int combineWithDataBus(int value, int validBitsMask) {
        return this.getBus().combineWithDataBus(value, validBitsMask);
    }

    private boolean getP1Difficulty() {
        return false;
    }

    private boolean getP0Difficulty() {
        return false;
    }

    private boolean getColor() {
        return true;
    }

    private boolean getGameSelect() {
        return !this.controller.isActionPressed(GAME_SELECT);
    }

    private boolean getGameReset() {
        return !this.controller.isActionPressed(GAME_RESET);
    }

    @Override
    public void close() throws Exception {

    }

}
