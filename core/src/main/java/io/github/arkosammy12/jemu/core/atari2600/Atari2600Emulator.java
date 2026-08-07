package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.hardware.MOS6532;
import io.github.arkosammy12.jemu.core.hardware.NMOS6507;

import static io.github.arkosammy12.jemu.core.atari2600.Atari2600Controller.Actions.*;

public class Atari2600Emulator implements Emulator, NMOS6507.SystemBus, MOS6532.SystemBus, TelevisionInterfaceAdaptor.SystemBus {

    private static final int NTSC_CPU_CLOCK_SPEED = 1193182;
    private static final int NTSC_FRAMERATE = 60;

    private static final int PAL_CPU_CLOCK_SPEED = 1182298;
    private static final int PAL_FRAMERATE = 50;

    private final Atari2600SystemHost systemHost;
    private final TVFormat tvFormat;

    private final NMOS6507 cpu;
    private final TelevisionInterfaceAdaptor<?> tia;
    private final MOS6532<?> pia;
    private final Atari2600Bus<?> bus;
    private final Atari2600Controller<?> controller;
    private final Atari2600Cartridge<?> cartridge;

    private final int framerate;
    private final int iterationsPerFrame;

    public Atari2600Emulator(Atari2600SystemHost systemHost) {
        this.systemHost = systemHost;
        this.tvFormat = this.systemHost.getTVFormatOverride().or(() -> this.systemHost.getCartridgeInfo().flatMap(Atari2600SystemHost.CartridgeInfo::getTVFormat)).orElse(TVFormat.NTSC);

        int clockSpeed;
        switch (this.tvFormat) {
            case NTSC -> {
                clockSpeed = NTSC_CPU_CLOCK_SPEED;
                this.framerate = NTSC_FRAMERATE;
            }
            case PAL, SECAM -> {
                clockSpeed = PAL_CPU_CLOCK_SPEED;
                this.framerate = PAL_FRAMERATE;
            }
            case NTSC50 -> {
                clockSpeed = NTSC_CPU_CLOCK_SPEED;
                this.framerate = PAL_FRAMERATE;
            }
            case PAL60, SECAM60 -> {
                clockSpeed = PAL_CPU_CLOCK_SPEED;
                this.framerate = NTSC_FRAMERATE;
            }
            default -> throw new EmulatorException("Atari 2600 TV format %s is not supported!".formatted(this.tvFormat.getName()));
        }

        this.iterationsPerFrame = clockSpeed / this.framerate;

        this.controller = new Atari2600Controller<>(this);
        this.cpu = new NMOS6507(this);
        this.tia = new TelevisionInterfaceAdaptor<>(this, this.iterationsPerFrame);
        this.pia = new MOS6532<>(this);
        this.bus = new Atari2600Bus<>(this);
        this.cartridge = Atari2600Cartridge.getCartridge(this);
    }

    public TVFormat getTVFormat() {
        return this.tvFormat;
    }

    @Override
    public Atari2600Bus<?> getBus() {
        return this.bus;
    }

    @Override
    public Atari2600SystemHost getHost() {
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

    public TelevisionInterfaceAdaptor<?> getTIA() {
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
        this.cartridge.cycle();
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
        int ret = this.systemHost.getRightDifficulty() ? 1 << 7 : 0;
        ret |= this.systemHost.getLeftDifficulty() ? 1 << 6 : 0;
        ret |= this.systemHost.getColorSwitch() ? 1 << 3 : 0;
        ret |= this.controller.isActionPressed(GAME_SELECT) ? 0 : 1 << 1;
        ret |= this.controller.isActionPressed(GAME_RESET) ? 0 : 1;
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

    @Override
    public void close() throws Exception {

    }

    public enum TVFormat {
        NTSC("NTSC"),
        PAL("PAL"),
        SECAM("SECAM"),
        NTSC50("NTSC50"),
        PAL60("PAL60"),
        SECAM60("SECAM60");

        private final String name;

        TVFormat(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

    }

}
