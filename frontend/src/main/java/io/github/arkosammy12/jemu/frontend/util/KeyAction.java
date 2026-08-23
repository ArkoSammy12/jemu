package io.github.arkosammy12.jemu.frontend.util;

import java.awt.event.KeyEvent;

public record KeyAction(int keyCode, KeyLocation keyLocation, ModifierKey controlKey, ModifierKey shiftKey, ModifierKey altKey) {

    public static final KeyAction A = KeyAction.fromKeyCode(KeyEvent.VK_A);
    public static final KeyAction B = KeyAction.fromKeyCode(KeyEvent.VK_B);
    public static final KeyAction C = KeyAction.fromKeyCode(KeyEvent.VK_C);
    public static final KeyAction D = KeyAction.fromKeyCode(KeyEvent.VK_D);
    public static final KeyAction E = KeyAction.fromKeyCode(KeyEvent.VK_E);
    public static final KeyAction F = KeyAction.fromKeyCode(KeyEvent.VK_F);
    public static final KeyAction G = KeyAction.fromKeyCode(KeyEvent.VK_G);
    public static final KeyAction H = KeyAction.fromKeyCode(KeyEvent.VK_H);
    public static final KeyAction I = KeyAction.fromKeyCode(KeyEvent.VK_I);
    public static final KeyAction J = KeyAction.fromKeyCode(KeyEvent.VK_J);
    public static final KeyAction K = KeyAction.fromKeyCode(KeyEvent.VK_K);
    public static final KeyAction L = KeyAction.fromKeyCode(KeyEvent.VK_L);
    public static final KeyAction M = KeyAction.fromKeyCode(KeyEvent.VK_M);
    public static final KeyAction N = KeyAction.fromKeyCode(KeyEvent.VK_N);
    public static final KeyAction O = KeyAction.fromKeyCode(KeyEvent.VK_O);
    public static final KeyAction P = KeyAction.fromKeyCode(KeyEvent.VK_P);
    public static final KeyAction Q = KeyAction.fromKeyCode(KeyEvent.VK_Q);
    public static final KeyAction R = KeyAction.fromKeyCode(KeyEvent.VK_R);
    public static final KeyAction S = KeyAction.fromKeyCode(KeyEvent.VK_S);
    public static final KeyAction T = KeyAction.fromKeyCode(KeyEvent.VK_T);
    public static final KeyAction U = KeyAction.fromKeyCode(KeyEvent.VK_U);
    public static final KeyAction V = KeyAction.fromKeyCode(KeyEvent.VK_V);
    public static final KeyAction W = KeyAction.fromKeyCode(KeyEvent.VK_W);
    public static final KeyAction X = KeyAction.fromKeyCode(KeyEvent.VK_X);
    public static final KeyAction Y = KeyAction.fromKeyCode(KeyEvent.VK_Y);
    public static final KeyAction Z = KeyAction.fromKeyCode(KeyEvent.VK_Z);

    public static final KeyAction NUM_1 = KeyAction.fromKeyCode(KeyEvent.VK_1);
    public static final KeyAction NUM_2 = KeyAction.fromKeyCode(KeyEvent.VK_2);
    public static final KeyAction NUM_3 = KeyAction.fromKeyCode(KeyEvent.VK_3);
    public static final KeyAction NUM_4 = KeyAction.fromKeyCode(KeyEvent.VK_4);
    public static final KeyAction NUM_5 = KeyAction.fromKeyCode(KeyEvent.VK_5);
    public static final KeyAction NUM_6 = KeyAction.fromKeyCode(KeyEvent.VK_6);
    public static final KeyAction NUM_7 = KeyAction.fromKeyCode(KeyEvent.VK_7);
    public static final KeyAction NUM_8 = KeyAction.fromKeyCode(KeyEvent.VK_8);
    public static final KeyAction NUM_9 = KeyAction.fromKeyCode(KeyEvent.VK_9);
    public static final KeyAction NUM_0 = KeyAction.fromKeyCode(KeyEvent.VK_0);

    public static final KeyAction BACK_QUOTE = KeyAction.fromKeyCode(KeyEvent.VK_BACK_QUOTE);
    public static final KeyAction MINUS = KeyAction.fromKeyCode(KeyEvent.VK_MINUS);
    public static final KeyAction EQUALS = KeyAction.fromKeyCode(KeyEvent.VK_EQUALS);
    public static final KeyAction BACK_SPACE = KeyAction.fromKeyCode(KeyEvent.VK_BACK_SPACE);
    public static final KeyAction TAB = KeyAction.fromKeyCode(KeyEvent.VK_TAB);
    public static final KeyAction OPEN_BRACKET = KeyAction.fromKeyCode(KeyEvent.VK_OPEN_BRACKET);
    public static final KeyAction CLOSE_BRACKET = KeyAction.fromKeyCode(KeyEvent.VK_CLOSE_BRACKET);
    public static final KeyAction BACK_SLASH = KeyAction.fromKeyCode(KeyEvent.VK_BACK_SLASH);
    public static final KeyAction CAPS_LOCK = KeyAction.fromKeyCode(KeyEvent.VK_CAPS_LOCK);
    public static final KeyAction SEMICOLON = KeyAction.fromKeyCode(KeyEvent.VK_SEMICOLON);
    public static final KeyAction QUOTE = KeyAction.fromKeyCode(KeyEvent.VK_QUOTE);
    public static final KeyAction DEAD_ACUTE = KeyAction.fromKeyCode(KeyEvent.VK_DEAD_ACUTE);
    public static final KeyAction ENTER = KeyAction.fromKeyCode(KeyEvent.VK_ENTER);
    public static final KeyAction SHIFT = KeyAction.fromKeyCode(KeyEvent.VK_SHIFT);
    public static final KeyAction COMMA = KeyAction.fromKeyCode(KeyEvent.VK_COMMA);
    public static final KeyAction PERIOD = KeyAction.fromKeyCode(KeyEvent.VK_PERIOD);
    public static final KeyAction SLASH = KeyAction.fromKeyCode(KeyEvent.VK_SLASH);
    public static final KeyAction ALT = KeyAction.fromKeyCode(KeyEvent.VK_ALT);
    public static final KeyAction CONTROL = KeyAction.fromKeyCode(KeyEvent.VK_CONTROL);
    public static final KeyAction LEFT_SHIFT = SHIFT.withKeyLocation(KeyLocation.LEFT);
    public static final KeyAction RIGHT_SHIFT = SHIFT.withKeyLocation(KeyLocation.RIGHT);

    public static final KeyAction EXCLAMATION_POINT = NUM_1.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction AT = NUM_2.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction NUMERAL = NUM_3.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction DOLLAR_SIGN = NUM_4.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction PERCENTAGE_SIGN = NUM_5.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction CIRCUMFLEX = NUM_6.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction AMPERSAND = NUM_7.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction ASTERISK = NUM_8.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction OPEN_PARENTHESIS = NUM_9.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction CLOSE_PARENTHESIS = NUM_0.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction UNDERSCORE = MINUS.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction PLUS = EQUALS.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction BRACELEFT = OPEN_BRACKET.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction BRACERIGHT = CLOSE_BRACKET.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction COLON = SEMICOLON.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction DOUBLE_QUOTES = QUOTE.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction LESS_THAN = COMMA.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction GREATER_THAN = PERIOD.withShiftKey(ModifierKey.PRESSED);
    public static final KeyAction QUESTION_MARK = SLASH.withShiftKey(ModifierKey.PRESSED);

    public static final KeyAction SPACE = KeyAction.fromKeyCode(KeyEvent.VK_SPACE);

    public static final KeyAction NUMPAD_1 = NUM_1.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_2 = NUM_2.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_3 = NUM_3.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_4 = NUM_4.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_5 = NUM_5.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_6 = NUM_6.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_7 = NUM_7.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_8 = NUM_8.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_9 = NUM_9.withKeyLocation(KeyLocation.NUMPAD);
    public static final KeyAction NUMPAD_0 = NUM_0.withKeyLocation(KeyLocation.NUMPAD);

    public KeyAction {
        if (this.keyCode() < 0) {
            throw new IllegalArgumentException("Invalid keyCode %d!".formatted(this.keyCode()));
        }
    }

    public KeyAction withKeyCode(int keycode) {
        return new KeyAction(keycode, this.keyLocation(), this.controlKey(), this.shiftKey(), this.altKey());
    }

    public KeyAction withKeyLocation(KeyLocation keyLocation) {
        return new KeyAction(this.keyCode(), keyLocation, this.controlKey(), this.shiftKey(), this.altKey());
    }

    public KeyAction withControlKey(ModifierKey controlKey) {
        return new KeyAction(this.keyCode(), this.keyLocation(), controlKey, this.shiftKey(), this.altKey());
    }

    public KeyAction withShiftKey(ModifierKey shiftKey) {
        return new KeyAction(this.keyCode(), this.keyLocation(), this.controlKey(), shiftKey, this.altKey());
    }

    public KeyAction withAltKey(ModifierKey altKey) {
        return new KeyAction(this.keyCode(), this.keyLocation(), this.controlKey(), this.shiftKey(), altKey);
    }

    public boolean matches(KeyAction other) {
        if (this.keyCode() != other.keyCode()) {
            return false;
        }
        if (this.keyLocation() != KeyLocation.UNSPECIFIED && this.keyLocation() != other.keyLocation()) {
            return false;
        }
        if (this.controlKey() != ModifierKey.UNSPECIFIED && this.controlKey() != other.controlKey()) {
            return false;
        }
        if (this.shiftKey() != ModifierKey.UNSPECIFIED && this.shiftKey() != other.shiftKey()) {
            return false;
        }
        return this.altKey() == ModifierKey.UNSPECIFIED || this.altKey() == other.altKey();
    }

    public boolean isOfKeyEvent(KeyEvent keyEvent) {
        if (this.keyCode() != keyEvent.getKeyCode()) {
            return false;
        }
        if (this.keyLocation() != KeyLocation.UNSPECIFIED && this.keyLocation() != KeyLocation.fromKeyEventLocation(keyEvent.getKeyLocation())) {
            return false;
        }
        if (this.controlKey() != ModifierKey.UNSPECIFIED && this.controlKey() != ModifierKey.fromIsDownValue(keyEvent.isControlDown())) {
            return false;
        }
        if (this.shiftKey() != ModifierKey.UNSPECIFIED && this.shiftKey() != ModifierKey.fromIsDownValue(keyEvent.isShiftDown())) {
            return false;
        }
        return this.altKey() == ModifierKey.UNSPECIFIED || this.altKey() == ModifierKey.fromIsDownValue(keyEvent.isAltDown());
    }

    public static KeyAction fromKeyEvent(KeyEvent keyEvent) {
        return new KeyAction(keyEvent.getKeyCode(), KeyLocation.fromKeyEventLocation(keyEvent.getKeyLocation()), ModifierKey.fromIsDownValue(keyEvent.isControlDown()), ModifierKey.fromIsDownValue(keyEvent.isShiftDown()), ModifierKey.fromIsDownValue(keyEvent.isAltDown()));
    }

    public static KeyAction fromKeyCode(int keyCode) {
        return new KeyAction(keyCode, KeyLocation.UNSPECIFIED, ModifierKey.UNSPECIFIED, ModifierKey.UNSPECIFIED, ModifierKey.UNSPECIFIED);
    }

    @Override
    public String toString() {
        String string = KeyEvent.getKeyText(this.keyCode());
        if (this.altKey() == ModifierKey.PRESSED && this.keyCode() != KeyEvent.VK_ALT) {
            string = "Alt+" + string;
        }
        if (this.shiftKey == ModifierKey.PRESSED && this.keyCode() != KeyEvent.VK_SHIFT) {
            string = "Shift+" + string;
        }
        if (this.controlKey() == ModifierKey.PRESSED && this.keyCode() != KeyEvent.VK_CONTROL) {
            string = "Ctrl+" + string;
        }
        string = string + switch (this.keyLocation) {
            case UNKNOWN, STANDARD, UNSPECIFIED -> "";
            case RIGHT -> " (Right)";
            case LEFT -> " (Left)";
            case NUMPAD -> " (Numpad)";
        };
        return string;
    }

    public enum KeyLocation {
        UNKNOWN,
        STANDARD,
        NUMPAD,
        LEFT,
        RIGHT,
        UNSPECIFIED
        ;

        private static KeyLocation fromKeyEventLocation(int keyLocation) {
            return switch (keyLocation) {
                case KeyEvent.KEY_LOCATION_LEFT -> KeyLocation.LEFT;
                case KeyEvent.KEY_LOCATION_RIGHT -> KeyLocation.RIGHT;
                case KeyEvent.KEY_LOCATION_NUMPAD -> KeyLocation.NUMPAD;
                case KeyEvent.KEY_LOCATION_STANDARD -> KeyLocation.STANDARD;
                case KeyEvent.KEY_LOCATION_UNKNOWN -> KeyLocation.UNKNOWN;
                default -> KeyLocation.UNSPECIFIED;
            };
        }

    }

    public enum ModifierKey {
        PRESSED,
        UNPRESSED,
        UNSPECIFIED;

        public static ModifierKey fromIsDownValue(boolean isDown) {
            return isDown ? PRESSED : UNPRESSED;
        }

    }

}
