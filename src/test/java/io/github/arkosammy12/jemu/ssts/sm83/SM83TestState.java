package io.github.arkosammy12.jemu.ssts.sm83;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SM83TestState {

    @SerializedName("pc")
    private int programCounter;

    @SerializedName("sp")
    private int stackPointer;

    @SerializedName("a")
    private int A;

    @SerializedName("b")
    private int B;

    @SerializedName("c")
    private int C;

    @SerializedName("d")
    private int D;

    @SerializedName("e")
    private int E;

    @SerializedName("f")
    private int F;

    @SerializedName("h")
    private int H;

    @SerializedName("l")
    private int L;

    @SerializedName("ime")
    private int interruptMasterEnable;

    @SerializedName("ei")
    private int interruptEnable;

    @SerializedName("ram")
    private List<List<Integer>> ram;

    public int getPC() {
        return this.programCounter;
    }

    public int getSP() {
        return this.stackPointer;
    }

    public int getA() {
        return this.A;
    }

    public int getB() {
        return this.B;
    }

    public int getC() {
        return this.C;
    }

    public int getD() {
        return this.D;
    }

    public int getE() {
        return this.E;
    }

    public int getF() {
        return this.F;
    }

    public int getH() {
        return this.H;
    }

    public int getL() {
        return this.L;
    }

    public int getIME() {
        return this.interruptMasterEnable;
    }

    public int getIE() {
        return this.interruptEnable;
    }

    public List<List<Integer>> getRam() {
        return this.ram;
    }

}
