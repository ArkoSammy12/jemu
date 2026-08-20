package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Controller;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Host;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Optional;

public class Commodore64Adapter extends SystemAdapter implements Commodore64Host {

    private String romTitle;
    private final Commodore64Manager commodore64Manager;

    public Commodore64Adapter(Jemu jemu, Commodore64Manager systemManager) throws LineUnavailableException {
        super(jemu, systemManager);
        this.commodore64Manager = systemManager;
    }

    @Override
    protected Emulator createEmulator() {
        return new Commodore64Emulator(this);
    }

    @Override
    protected SystemController.Action getActionForKeyCode(int keyCode, int keyLocation) {
        return switch (keyCode) {
            case KeyEvent.VK_A -> Commodore64Controller.KeyboardMatrix.KEY_A;
            case KeyEvent.VK_B -> Commodore64Controller.KeyboardMatrix.KEY_B;
            case KeyEvent.VK_C -> Commodore64Controller.KeyboardMatrix.KEY_C;
            case KeyEvent.VK_D -> Commodore64Controller.KeyboardMatrix.KEY_D;
            case KeyEvent.VK_E -> Commodore64Controller.KeyboardMatrix.KEY_E;
            case KeyEvent.VK_F -> Commodore64Controller.KeyboardMatrix.KEY_F;
            case KeyEvent.VK_G -> Commodore64Controller.KeyboardMatrix.KEY_G;
            case KeyEvent.VK_H -> Commodore64Controller.KeyboardMatrix.KEY_H;
            case KeyEvent.VK_I -> Commodore64Controller.KeyboardMatrix.KEY_I;
            case KeyEvent.VK_J -> Commodore64Controller.KeyboardMatrix.KEY_J;
            case KeyEvent.VK_K -> Commodore64Controller.KeyboardMatrix.KEY_K;
            case KeyEvent.VK_L -> Commodore64Controller.KeyboardMatrix.KEY_L;
            case KeyEvent.VK_M -> Commodore64Controller.KeyboardMatrix.KEY_M;
            case KeyEvent.VK_N -> Commodore64Controller.KeyboardMatrix.KEY_N;
            case KeyEvent.VK_O -> Commodore64Controller.KeyboardMatrix.KEY_O;
            case KeyEvent.VK_P -> Commodore64Controller.KeyboardMatrix.KEY_P;
            case KeyEvent.VK_Q -> Commodore64Controller.KeyboardMatrix.KEY_Q;
            case KeyEvent.VK_R -> Commodore64Controller.KeyboardMatrix.KEY_R;
            case KeyEvent.VK_S -> Commodore64Controller.KeyboardMatrix.KEY_S;
            case KeyEvent.VK_T -> Commodore64Controller.KeyboardMatrix.KEY_T;
            case KeyEvent.VK_U -> Commodore64Controller.KeyboardMatrix.KEY_U;
            case KeyEvent.VK_V -> Commodore64Controller.KeyboardMatrix.KEY_V;
            case KeyEvent.VK_W -> Commodore64Controller.KeyboardMatrix.KEY_W;
            case KeyEvent.VK_X -> Commodore64Controller.KeyboardMatrix.KEY_X;
            case KeyEvent.VK_Y -> Commodore64Controller.KeyboardMatrix.KEY_Y;
            case KeyEvent.VK_Z -> Commodore64Controller.KeyboardMatrix.KEY_Z;

            case KeyEvent.VK_0 -> Commodore64Controller.KeyboardMatrix.KEY_ZERO;
            case KeyEvent.VK_1 -> Commodore64Controller.KeyboardMatrix.KEY_ONE_EXCLAMATION_POINT;
            case KeyEvent.VK_2 -> Commodore64Controller.KeyboardMatrix.KEY_TWO_QUOTES;
            case KeyEvent.VK_3 -> Commodore64Controller.KeyboardMatrix.KEY_THREE_NUMERAL;
            case KeyEvent.VK_4 -> Commodore64Controller.KeyboardMatrix.KEY_FOUR_DOLLAR_SIGN;
            case KeyEvent.VK_5 -> Commodore64Controller.KeyboardMatrix.KEY_FIVE_PERCENT_SIGN;
            case KeyEvent.VK_6 -> Commodore64Controller.KeyboardMatrix.KEY_SIX_AMPERSAND;
            case KeyEvent.VK_7 -> Commodore64Controller.KeyboardMatrix.KEY_SEVEN_TILE;
            case KeyEvent.VK_8 -> Commodore64Controller.KeyboardMatrix.KEY_EIGHT_OPEN_PARENTHESIS;
            case KeyEvent.VK_9 -> Commodore64Controller.KeyboardMatrix.KEY_NINE_OPEN_PARENTHESIS;

            case KeyEvent.VK_MINUS -> Commodore64Controller.KeyboardMatrix.KEY_MINUS;
            case KeyEvent.VK_CLOSE_BRACKET -> Commodore64Controller.KeyboardMatrix.KEY_PLUS;
            case KeyEvent.VK_EQUALS -> Commodore64Controller.KeyboardMatrix.KEY_EQUALS;
            case KeyEvent.VK_SEMICOLON -> Commodore64Controller.KeyboardMatrix.KEY_COLON_OPENING_BRACKET;
            case KeyEvent.VK_QUOTE -> Commodore64Controller.KeyboardMatrix.KEY_SEMICOLON_CLOSING_BRACKET;
            case KeyEvent.VK_COMMA -> Commodore64Controller.KeyboardMatrix.KEY_COMMA_LESS_THAN;
            case KeyEvent.VK_PERIOD -> Commodore64Controller.KeyboardMatrix.KEY_PERIOD_GREATER_THAN;
            case KeyEvent.VK_SLASH -> Commodore64Controller.KeyboardMatrix.KEY_SLASH_QUESTION_MARK;
            case KeyEvent.VK_SPACE -> Commodore64Controller.KeyboardMatrix.KEY_SPACE;
            case KeyEvent.VK_BACK_SPACE -> Commodore64Controller.KeyboardMatrix.KEY_INST_DEL;
            case KeyEvent.VK_ENTER -> Commodore64Controller.KeyboardMatrix.KEY_RETURN;

            case KeyEvent.VK_SHIFT -> keyLocation == KeyEvent.KEY_LOCATION_LEFT ? Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT : Commodore64Controller.KeyboardMatrix.KEY_RIGHT_SHIFT;
            case KeyEvent.VK_CAPS_LOCK -> Commodore64Controller.KeyboardSpecialKey.SHIFT_LOCK;
            case KeyEvent.VK_TAB -> Commodore64Controller.KeyboardMatrix.KEY_CTRL;
            case KeyEvent.VK_BACK_QUOTE -> Commodore64Controller.KeyboardMatrix.KEY_LEFT_ARROW;

            case KeyEvent.VK_BACK_SLASH -> Commodore64Controller.KeyboardSpecialKey.RESTORE;

            case KeyEvent.VK_OPEN_BRACKET -> Commodore64Controller.KeyboardMatrix.KEY_AT;
            //case KeyEvent.VK_CLOSE_BRACKET -> Commodore64Controller.KeyboardMatrix.KEY_ASTERISK;

            // TODO: Map remaining keys: Pound, CLR/HOME, Fx buttons, CRSR buttons, Commodore key, Run/Stop, Up arrow, asterisk

            default -> null;
        };
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<Path> getKernalROMPath() {
        return commodore64Manager.getEmulationSettings().getKernalRomPath();
    }

    @Override
    public Optional<Path> getBASICRomPath() {
        return commodore64Manager.getEmulationSettings().getBasicRomPath();
    }

    @Override
    public Optional<Path> getCharacterROMPath() {
        return commodore64Manager.getEmulationSettings().getCharacterRomPath();
    }

    @Override
    public int getRB8ForPaletteIndex(int paletteIndex) {
        return this.commodore64Manager.getEmulationSettings().getVICIIPalette().getRGB8ForPaletteIndex(paletteIndex);
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
