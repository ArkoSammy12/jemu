package io.github.arkosammy12.jemu.core.ssts.nes6502;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NES6502TestState {

    @SerializedName("pc")
    private int programCounter;

    @SerializedName("s")
    private int stackPointer;

    @SerializedName("a")
    private int accumulator;

    @SerializedName("x")
    private int X;

    @SerializedName("y")
    private int Y;

    @SerializedName("p")
    private int processorStatus;

    @SerializedName("ram")
    private List<List<Integer>> ram;

    public int getPC() {
        return this.programCounter;
    }

    public int getSP() {
        return this.stackPointer;
    }

    public int getA() {
        return this.accumulator;
    }

    public int getX() {
        return this.X;
    }

    public int getY() {
        return this.Y;
    }

    public int getP() {
        return this.processorStatus;
    }

    public List<List<Integer>> getRam() {
        return this.ram;
    }

}
