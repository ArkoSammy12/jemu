package io.github.arkosammy12.jemu.core.test.ssts.nmos6502;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NMOS6502TestCase {

    @SerializedName("name")
    private String name;

    @SerializedName("initial")
    private NMOS6502TestState initialState;

    @SerializedName("final")
    private NMOS6502TestState finalState;

    @SerializedName("cycles")
    private List<List<Object>> cycles;

    public String getName() {
        return this.name;
    }

    public NMOS6502TestState getInitialState() {
        return this.initialState;
    }

    public NMOS6502TestState getFinalState() {
        return this.finalState;
    }

    public List<List<Object>> getCycles() {
        return this.cycles;
    }

}
