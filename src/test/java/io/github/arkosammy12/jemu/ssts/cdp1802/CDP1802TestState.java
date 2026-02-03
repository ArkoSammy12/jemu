package io.github.arkosammy12.jemu.ssts.cdp1802;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CDP1802TestState {

    @SerializedName("r")
    private List<Integer> registers;

    @SerializedName("p")
    private int P;

    @SerializedName("x")
    private int X;

    @SerializedName("n")
    private int N;

    @SerializedName("i")
    private int I;

    @SerializedName("t")
    private int T;

    @SerializedName("d")
    private int D;

    @SerializedName("df")
    private int DF;

    @SerializedName("ie")
    private int interruptEnable;

    @SerializedName("q")
    private int Q;

    @SerializedName("ram")
    private List<List<Integer>> ram;

    public int getR(int index) {
        return this.registers.get(index);
    }

    public int getP() {
        return this.P;
    }

    public int getX() {
        return this.X;
    }

    public int getN() {
        return this.N;
    }

    public int getI() {
        return this.I;
    }

    public int getT() {
        return this.T;
    }

    public int getD() {
        return this.D;
    }

    public int getDF() {
        return this.DF;
    }

    public int getIE() {
        return this.interruptEnable;
    }

    public int getQ() {
        return this.Q;
    }

    public List<List<Integer>> getRam() {
        return this.ram;
    }

}



