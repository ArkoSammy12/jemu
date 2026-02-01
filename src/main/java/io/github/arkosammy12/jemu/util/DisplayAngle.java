package io.github.arkosammy12.jemu.util;

import io.github.arkosammy12.jemu.config.Serializable;
import picocli.CommandLine;

public enum DisplayAngle implements DisplayNameProvider, Serializable {
    DEG_0("No rotation", 0, "0"),
    DEG_90("90 degrees", 90, "90"),
    DEG_180("180 degrees", 180, "180"),
    DEG_270("270 degrees", 270, "270");

    private final String displayName;
    private final String identifier;
    private final int intValue;

    DisplayAngle(String displayName, int intValue, String identifier) {
        this.displayName = displayName;
        this.intValue = intValue;
        this.identifier = identifier;
    }

    public int getIntValue() {
        return this.intValue;
    }

    public static DisplayAngle getDisplayAngleForIdentifier(String identifier) {
        for (DisplayAngle displayAngle : DisplayAngle.values()) {
            if (displayAngle.identifier.equals(identifier)) {
                return displayAngle;
            }
        }
        throw new IllegalArgumentException("Unknown display angle identifier: " + identifier + "!");
    }

    public static DisplayAngle getDisplayAngleForIntValue(int intValue) {
        for (DisplayAngle displayAngle : DisplayAngle.values()) {
            if (intValue == displayAngle.intValue) {
                return displayAngle;
            }
        }
        throw new IllegalArgumentException("Invalid display angle value \"" + intValue + "\"!");
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String getSerializedString() {
        return this.identifier;
    }

    public static class Converter implements CommandLine.ITypeConverter<DisplayAngle> {

        @Override
        public DisplayAngle convert(String value) {
            return getDisplayAngleForIdentifier(value);
        }

    }

}
