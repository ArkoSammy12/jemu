package io.github.arkosammy12.jemu.app.system.chip8.database;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

class Quirks {

    @SerializedName(value = "shift")
    private Boolean shift;

    @SerializedName(value = "memoryLeaveIUnchanged")
    private Boolean memoryLeaveIUnchanged;

    @SerializedName(value = "memoryIncrementByX")
    private Boolean memoryIncrementByX;

    @SerializedName(value = "wrap")
    private Boolean wrap;

    @SerializedName(value = "jump")
    private Boolean jump;

    @SerializedName(value = "vblank")
    private Boolean vblank;

    @SerializedName(value = "logic")
    private Boolean logic;

    Optional<Boolean> getShift() {
        return Optional.ofNullable(this.shift);
    }

    Optional<Boolean> getMemoryLeaveIUnchanged() {
        return Optional.ofNullable(this.memoryLeaveIUnchanged);
    }

    Optional<Boolean> getMemoryIncrementByX() {
        return Optional.ofNullable(this.memoryIncrementByX);
    }

    Optional<Boolean> getWrap() {
        return Optional.ofNullable(this.wrap);
    }

    Optional<Boolean> getJump() {
        return Optional.ofNullable(this.jump);
    }

    Optional<Boolean> getVBlank() {
        return Optional.ofNullable(this.vblank);
    }

    Optional<Boolean> getLogic() {
        return Optional.ofNullable(this.logic);
    }

}
