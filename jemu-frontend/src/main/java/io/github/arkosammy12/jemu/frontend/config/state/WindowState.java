package io.github.arkosammy12.jemu.frontend.config.state;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.OptionalInt;

public class WindowState {

    @SerializedName("bounds")
    private Bounds bounds = new Bounds();

    @Nullable
    @SerializedName("extended_state")
    private Integer extendedState = null;

    public Bounds getBounds() {
        return this.bounds;
    }

    public void setExtendedState(int extendedState) {
        this.extendedState = extendedState;
    }

    public OptionalInt getExtendedState() {
        return this.extendedState == null ? OptionalInt.empty() : OptionalInt.of(this.extendedState);
    }

    public static class Bounds {

        @Nullable
        @SerializedName("x")
        private Integer x = null;

        @Nullable
        @SerializedName("y")
        private Integer y = null;

        @Nullable
        @SerializedName("width")
        private Integer width = null;

        @Nullable
        @SerializedName("height")
        private Integer height = null;

        public void setX(int x) {
            this.x = x;
        }

        public OptionalInt getX() {
            return this.x == null ? OptionalInt.empty() : OptionalInt.of(this.x);
        }

        public void setY(int y) {
            this.y = y;
        }

        public OptionalInt getY() {
            return this.y == null ? OptionalInt.empty() : OptionalInt.of(this.y);
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public OptionalInt getWidth() {
            return this.width == null ? OptionalInt.empty() : OptionalInt.of(this.width);
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public OptionalInt getHeight() {
            return this.height == null ? OptionalInt.empty() : OptionalInt.of(this.height);
        }

        public void setFromBounds(Rectangle bounds) {
            this.setX(bounds.x);
            this.setY(bounds.y);
            this.setWidth(bounds.width);
            this.setHeight(bounds.height);
        }

    }

}
