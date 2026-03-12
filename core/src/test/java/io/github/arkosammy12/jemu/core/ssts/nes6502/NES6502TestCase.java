package io.github.arkosammy12.jemu.core.ssts.nes6502;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NES6502TestCase {

    @SerializedName("name")
    private String name;

    @SerializedName("initial")
    private NES6502TestState initialState;

    @SerializedName("final")
    private NES6502TestState finalState;

    @SerializedName("cycles")
    private List<List<Object>> cycles;

    public String getName() {
        return this.name;
    }

    public NES6502TestState getInitialState() {
        return this.initialState;
    }

    public NES6502TestState getFinalState() {
        return this.finalState;
    }

    public List<List<Object>> getCycles() {
        return this.cycles;
    }

}
