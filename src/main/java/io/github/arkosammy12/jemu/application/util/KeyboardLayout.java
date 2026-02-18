package io.github.arkosammy12.jemu.application.util;

import io.github.arkosammy12.jemu.application.io.Serializable;
import picocli.CommandLine;

public enum KeyboardLayout implements DisplayNameProvider, Serializable {
    QWERTY("Qwerty", "qwerty"),
    DVORAK("Dvorak", "dvorak"),
    AZERTY("Azerty", "azerty"),
    COLEMAK("Colemak", "colemak");

    private final String displayName;
    private final String identifier;

    KeyboardLayout(String displayName, String identifier) {
        this.displayName = displayName;
        this.identifier = identifier;
    }

    public static KeyboardLayout getKeyboardLayoutForIdentifier(String identifier) {
        for (KeyboardLayout layout : values()) {
            if (layout.identifier.equals(identifier)) {
                return layout;
            }
        }
        throw new IllegalArgumentException("Unknown keyboard layout identifier \"" + identifier + "\"!");
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String getSerializedString() {
        return this.identifier;
    }

    public static class Converter implements CommandLine.ITypeConverter<KeyboardLayout> {

        @Override
        public KeyboardLayout convert(String value) {
            return getKeyboardLayoutForIdentifier(value);
        }

    }

}
