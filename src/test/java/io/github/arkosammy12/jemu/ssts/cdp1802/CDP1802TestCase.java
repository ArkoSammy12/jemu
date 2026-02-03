package io.github.arkosammy12.jemu.ssts.cdp1802;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CDP1802TestCase {

    @SerializedName("name")
    private String name;

    @SerializedName("initial")
    private CDP1802TestState initialState;

    @SerializedName("final")
    private CDP1802TestState finalState;

    @SerializedName("cycles")
    private List<List<Object>> cycles;

    public String getName() {
        return this.name;
    }

    public CDP1802TestState getInitialState() {
        return this.initialState;
    }

    public CDP1802TestState getFinalState() {
        return this.finalState;
    }

    public List<List<Object>> getCycles() {
        return this.cycles;
    }

}
