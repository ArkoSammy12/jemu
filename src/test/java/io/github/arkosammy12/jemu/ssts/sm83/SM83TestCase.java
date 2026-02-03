package io.github.arkosammy12.jemu.ssts.sm83;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SM83TestCase {

    @SerializedName("name")
    private String name;

    @SerializedName("initial")
    private SM83TestState initialState;

    @SerializedName("final")
    private SM83TestState finalState;

    @SerializedName("cycles")
    private List<List<Object>> cycles;

    public String getName() {
        return this.name;
    }

    public SM83TestState getInitialState() {
        return this.initialState;
    }

    public SM83TestState getFinalState() {
        return this.finalState;
    }

    public List<List<Object>> getCycles() {
        return this.cycles;
    }


}
