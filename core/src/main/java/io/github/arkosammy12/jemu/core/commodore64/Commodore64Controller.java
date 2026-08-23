package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class Commodore64Controller implements SystemController {

    private final int[] columnBits = new int[8];
    private final int[] rowBits = new int[8];

    private boolean restoreKey;
    private ShiftLockGate shiftLockGate = ShiftLockGate.OPEN;

    @Override
    public void pressAction(Action action) {
        if (!(action instanceof Actions commodore64Action)) {
            return;
        }

        switch (commodore64Action) {
            case KeyboardMatrix keyboardMatrixKey -> {
                this.columnBits[keyboardMatrixKey.getRow()] |= 1 << keyboardMatrixKey.getColumn();
                this.rowBits[keyboardMatrixKey.getColumn()] |= 1 << keyboardMatrixKey.getRow();
            }
            case KeyboardSpecialKey keyboardSpecialKey -> {
                if (keyboardSpecialKey == KeyboardSpecialKey.RESTORE) {
                    this.restoreKey = true;
                }
            }

        }
    }

    @Override
    public void releaseAction(Action action) {
        if (!(action instanceof Actions commodore64Action)) {
            return;
        }

        switch (commodore64Action) {
            case KeyboardMatrix keyboardMatrixKey -> {
                this.columnBits[keyboardMatrixKey.getRow()] &= ~(1 << keyboardMatrixKey.getColumn());
                this.rowBits[keyboardMatrixKey.getColumn()] &= ~(1 << keyboardMatrixKey.getRow());
            }
            case KeyboardSpecialKey keyboardSpecialKey -> {
                switch (keyboardSpecialKey) {
                    case RESTORE -> this.restoreKey = false;
                    case SHIFT_LOCK -> this.shiftLockGate = this.shiftLockGate.getOpposite();
                }
            }

        }

    }

    public int getColumnBits(int selectMask) {
        int ret = 0;
        for (int i = 0, mask = 0b1; i < 8; i++, mask <<= 1) {
            if ((selectMask & mask) != 0) {
                ret |= this.columnBits[i];
                if (i == KeyboardMatrix.KEY_LEFT_SHIFT.getRow()) {
                    ret |= this.shiftLockGate.getColumnBit();
                }
            }
        }
        return ret;
    }

    public int getRowBits(int selectMask) {
        int ret = 0;
        for (int i = 0, mask = 0b1; i < 8; i++, mask <<= 1) {
            if ((selectMask & mask) != 0) {
                ret |= this.rowBits[i];
                if (i == KeyboardMatrix.KEY_LEFT_SHIFT.getColumn()) {
                    ret |= this.shiftLockGate.getRowBit();
                }
            }
        }
        return ret;
    }

    public boolean getRestoreKey() {
        return this.restoreKey;
    }

    public sealed interface Actions extends SystemController.Action permits KeyboardMatrix, KeyboardSpecialKey {}

    public enum KeyboardMatrix implements Actions {
        KEY_INST_DEL(0, 0, "Key INST | DEL"),
        KEY_THREE_NUMERAL(0, 1, "Key 3 | #"),
        KEY_FIVE_PERCENT_SIGN(0, 2, "Key 5 | %"),
        KEY_SEVEN_BACK_QUOTE(0, 3, "Key 7 | `"),
        KEY_NINE_CLOSE_PARENTHESIS(0, 4, "Key 9 | )"),
        KEY_PLUS(0, 5, "Key +"),
        KEY_POUND(0, 6, "Key £"),
        KEY_ONE_EXCLAMATION_POINT(0, 7, "Key 1 | !"),

        KEY_RETURN(1, 0, "Key RETURN"),
        KEY_W(1, 1, "Key W"),
        KEY_R(1, 2, "Key R"),
        KEY_Y(1, 3, "Key Y"),
        KEY_I(1, 4, "Key I"),
        KEY_P(1, 5, "Key P"),
        KEY_ASTERISK(1, 6, "Key *"),
        KEY_LEFT_ARROW(1, 7, "Key Left Arrow"),

        KEY_CRSR_LEFT_RIGHT(2, 0, "Key CRSR Left Right"),
        KEY_A(2, 1, "Key A"),
        KEY_D(2, 2, "Key D"),
        KEY_G(2, 3, "Key G"),
        KEY_J(2, 4, "Key J"),
        KEY_L(2, 5, "Key L"),
        KEY_SEMICOLON_CLOSING_BRACKET(2, 6, "Key ; | ]"),
        KEY_CTRL(2, 7, "Key CTRL"),

        KEY_F7_F8(3, 0, "Key F7 | F8"),
        KEY_FOUR_DOLLAR_SIGN(3, 1, "Key 4 | $"),
        KEY_SIX_AMPERSAND(3, 2, "Key 6 | &"),
        KEY_EIGHT_OPEN_PARENTHESIS(3, 3, "Key 8 | ("),
        KEY_ZERO(3, 4, "Key 0"),
        KEY_MINUS(3, 5, "Key -"),
        KEY_CLR_HOME(3, 6, "Key CLR | HOME"),
        KEY_TWO_QUOTES(3, 7, "2 | \""),

        KEY_F1_F2(4, 0, "Key F1 | F2"),
        KEY_Z(4, 1, "Key Z"),
        KEY_C(4, 2, "Key C"),
        KEY_B(4, 3, "Key B"),
        KEY_M(4, 4, "Key M"),
        KEY_PERIOD_GREATER_THAN(4, 5, "Key . | >"),
        KEY_RIGHT_SHIFT(4, 6, "Key Right Shift"),
        KEY_SPACE(4, 7, "Key Spacebar"),

        KEY_F3_F4(5, 0, "Key F3 | F4"),
        KEY_S(5, 1, "Key S"),
        KEY_F(5, 2, "Key F"),
        KEY_H(5, 3, "Key H"),
        KEY_K(5, 4, "Key K"),
        KEY_COLON_OPENING_BRACKET(5, 5, "Key : | ["),
        KEY_EQUALS(5, 6, "Key ="),
        KEY_COMMODORE(5, 7, "Key Commodore"),

        KEY_F5_F6(6, 0, "Key F5 | F6"),
        KEY_E(6, 1, "Key E"),
        KEY_T(6, 2, "Key T"),
        KEY_U(6, 3, "Key U"),
        KEY_O(6, 4, "Key O"),
        KEY_AT(6, 5, "Key @"),
        KEY_UP_ARROW(6, 6, "Key Up Arrow"),
        KEY_Q(6, 7, "Key Q"),

        KEY_CRSR_UP_DOWN(7, 0, "Key CRSR Up Down"),
        KEY_LEFT_SHIFT(7, 1, "Key Left Shift"),
        KEY_X(7, 2, "Key X"),
        KEY_V(7, 3, "Key V"),
        KEY_N(7, 4, "Key N"),
        KEY_COMMA_LESS_THAN(7, 5, "Key , | <"),
        KEY_SLASH_QUESTION_MARK(7, 6, "Key / | ?"),
        KEY_RUN_STOP(7, 7, "Key RUN | STOP")
        ;

        private final String label;
        private final int row;
        private final int column;

        KeyboardMatrix(int row, int column, String label) {
            this.label = label;
            this.row = row;
            this.column = column;
        }

        private int getColumn() {
            return this.column;
        }

        private int getRow() {
            return this.row;
        }

        @Override
        public String getLabel() {
            return this.label;
        }

    }

    public enum KeyboardSpecialKey implements Actions {
        SHIFT_LOCK("Key Shift Lock"),
        RESTORE("Key Restore");

        private final String label;

        KeyboardSpecialKey(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return this.label;
        }

    }

    private enum ShiftLockGate {
        OPEN,
        CLOSED
        ;

        private int getColumnBit() {
            return switch (this) {
                case OPEN -> 0;
                case CLOSED -> 1 << KeyboardMatrix.KEY_LEFT_SHIFT.getColumn();
            };
        }

        private int getRowBit() {
            return switch (this) {
                case OPEN -> 0;
                case CLOSED -> 1 << KeyboardMatrix.KEY_LEFT_SHIFT.getRow();
            };
        }

        private ShiftLockGate getOpposite() {
            return switch (this) {
                case OPEN -> CLOSED;
                case CLOSED -> OPEN;
            };
        }

    }

}
