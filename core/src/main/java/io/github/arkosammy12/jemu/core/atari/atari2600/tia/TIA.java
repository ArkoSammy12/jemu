package io.github.arkosammy12.jemu.core.atari.atari2600.tia;

import io.github.arkosammy12.jemu.core.atari.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TIA<E extends Atari2600Emulator> implements Bus, VideoGenerator, AudioGenerator {

    private static final int AUDIO_CPU_CLK_DIVISOR = 38;

    static final int CXM0P = 0x00;
    static final int CXM1P = 0x01;
    static final int CXP0FB = 0x02;
    static final int CXP1FB = 0x03;
    static final int CXM0FB = 0x04;
    static final int CXM1FB = 0x05;
    static final int CXBLPF = 0x06;
    static final int CXPPMM = 0x07;
    static final int INPT0 = 0x08;
    static final int INPT1 = 0x09;
    static final int INPT2 = 0x0A;
    static final int INPT3 = 0x0B;
    static final int INPT4 = 0x0C;
    static final int INPT5 = 0x0D;

    static final int VSYNC = 0x00;
    static final int VBLANK = 0x01;
    static final int WSYNC = 0x02;
    static final int RSYNC = 0x03;
    static final int NUSIZ0 = 0x04;
    static final int NUSIZ1 = 0x05;
    static final int COLUP0 = 0x06;
    static final int COLUP1 = 0x07;
    static final int COLUPF = 0x08;
    static final int COLUBK = 0x09;
    static final int CTRLPF = 0x0A;
    static final int REFP0 = 0x0B;
    static final int REFP1 = 0x0C;
    static final int PF0 = 0x0D;
    static final int PF1 = 0x0E;
    static final int PF2 = 0x0F;
    static final int RESP0 = 0x10;
    static final int RESP1 = 0x11;
    static final int RESM0 = 0x12;
    static final int RESM1 = 0x13;
    static final int RESBL = 0x14;
    static final int AUDC0 = 0x15;
    static final int AUDC1 = 0x16;
    static final int AUDF0 = 0x17;
    static final int AUDF1 = 0x18;
    static final int AUDV0 = 0x19;
    static final int AUDV1 = 0x1A;
    static final int GRP0 = 0x1B;
    static final int GRP1 = 0x1C;
    static final int ENAM0 = 0x1D;
    static final int ENAM1 = 0x1E;
    static final int ENABL = 0x1F;
    static final int HMP0 = 0x20;
    static final int HMP1 = 0x21;
    static final int HMM0 = 0x22;
    static final int HMM1 = 0x23;
    static final int HMBL = 0x24;
    static final int VDELP0 = 0x25;
    static final int VDELP1 = 0x26;
    static final int VDELBL = 0x27;
    static final int RESMP0 = 0x28;
    static final int RESMP1 = 0x29;
    static final int HMOVE = 0x2A;
    static final int HMCLR = 0x2B;
    static final int CXCLR = 0x2C;

    private final E emulator;

    private final TIAVideo<E> video;
    private final TIAAudio<E> audio;

    private int audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;

    public TIA(E emulator) {
        this.emulator = emulator;
        this.video = new TIAVideo<>(emulator);
        this.audio = new TIAAudio<>(emulator);

    }

    @Override
    public boolean isStereo() {
        return this.audio.isStereo();
    }

    @Override
    public @NotNull SampleSize getBytesPerSample() {
        return this.audio.getBytesPerSample();
    }

    @Override
    public Optional<SampleFrame> getSampleFrame() {
        return this.audio.getSampleFrame();
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.audio.getSampleFrameResampler();
    }

    @Override
    public int getImageWidth() {
        return this.video.getImageWidth();
    }

    @Override
    public int getImageHeight() {
        return this.video.getImageHeight();
    }

    @Override
    public double getPixelAspectRatio() {
        return this.video.getPixelAspectRatio();
    }

    @Override
    public int readByte(int address) {
        address &= 0xF;
        return switch (address) {
            case CXM0P, CXM1P, CXP0FB, CXP1FB, CXM0FB, CXM1FB, CXBLPF, CXPPMM -> this.video.readByte(address);
            case INPT0 -> this.emulator.getBus().combineWithDataBus(0x80, 0x80);
            case INPT1 -> this.emulator.getBus().combineWithDataBus(0x80, 0x80);
            case INPT2 -> this.emulator.getBus().combineWithDataBus(0x80, 0x80);
            case INPT3 -> this.emulator.getBus().combineWithDataBus(0x80, 0x80);
            case INPT4 -> this.emulator.getBus().combineWithDataBus(0x80, 0x80);
            case INPT5 -> this.emulator.getBus().combineWithDataBus(0x80, 0x80);
            default -> this.emulator.getBus().combineWithDataBus(0x80, 0x80);
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x3F;
        switch (address) {
            case VSYNC, VBLANK, WSYNC, RSYNC, NUSIZ0, NUSIZ1, COLUP0, COLUP1, COLUPF, COLUBK, CTRLPF, REFP0, REFP1,
                 PF0, PF1, PF2, RESP0, RESP1, RESM0, RESM1, RESBL, GRP0, GRP1, ENAM0, ENAM1, ENABL, HMP0, HMP1, HMM0,
                 HMM1, HMBL, VDELP0, VDELP1, VDELBL, RESMP0, RESMP1, HMOVE, HMCLR, CXCLR -> this.video.writeByte(address, value);
            case AUDC0, AUDC1, AUDF0, AUDF1, AUDV0, AUDV1 -> this.audio.writeByte(address, value);
        }
    }

    public void cycle() {
        this.video.clock();
        this.video.clock();
        this.video.clock();

        this.audioClockDivisor--;
        if (this.audioClockDivisor <= 0) {
            this.audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;
            this.audio.clock();
        }
    }

    public boolean getRDYSignal() {
        return this.video.getRDYSignal();
    }

}
