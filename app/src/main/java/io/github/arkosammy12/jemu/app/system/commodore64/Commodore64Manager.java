package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Controller;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class Commodore64Manager extends SystemManager {

    @Nullable
    private volatile Commodore64MenuBarSettings commodore64MenuBarSettings;

    @Nullable
    private volatile Commodore64PanelSettings commodore64PanelSettings;

    public Commodore64Manager(Jemu jemu, SystemRegistry systemRegistry) {
        super(jemu, systemRegistry);

        this.keyActionMap.put(KeyAction.A, Commodore64Controller.KeyboardMatrix.KEY_A);
        this.keyActionMap.put(KeyAction.B, Commodore64Controller.KeyboardMatrix.KEY_B);
        this.keyActionMap.put(KeyAction.C, Commodore64Controller.KeyboardMatrix.KEY_C);
        this.keyActionMap.put(KeyAction.D, Commodore64Controller.KeyboardMatrix.KEY_D);
        this.keyActionMap.put(KeyAction.E, Commodore64Controller.KeyboardMatrix.KEY_E);
        this.keyActionMap.put(KeyAction.F, Commodore64Controller.KeyboardMatrix.KEY_F);
        this.keyActionMap.put(KeyAction.G, Commodore64Controller.KeyboardMatrix.KEY_G);
        this.keyActionMap.put(KeyAction.H, Commodore64Controller.KeyboardMatrix.KEY_H);
        this.keyActionMap.put(KeyAction.I, Commodore64Controller.KeyboardMatrix.KEY_I);
        this.keyActionMap.put(KeyAction.J, Commodore64Controller.KeyboardMatrix.KEY_J);
        this.keyActionMap.put(KeyAction.K, Commodore64Controller.KeyboardMatrix.KEY_K);
        this.keyActionMap.put(KeyAction.L, Commodore64Controller.KeyboardMatrix.KEY_L);
        this.keyActionMap.put(KeyAction.M, Commodore64Controller.KeyboardMatrix.KEY_M);
        this.keyActionMap.put(KeyAction.N, Commodore64Controller.KeyboardMatrix.KEY_N);
        this.keyActionMap.put(KeyAction.O, Commodore64Controller.KeyboardMatrix.KEY_O);
        this.keyActionMap.put(KeyAction.P, Commodore64Controller.KeyboardMatrix.KEY_P);
        this.keyActionMap.put(KeyAction.Q, Commodore64Controller.KeyboardMatrix.KEY_Q);
        this.keyActionMap.put(KeyAction.R, Commodore64Controller.KeyboardMatrix.KEY_R);
        this.keyActionMap.put(KeyAction.S, Commodore64Controller.KeyboardMatrix.KEY_S);
        this.keyActionMap.put(KeyAction.T, Commodore64Controller.KeyboardMatrix.KEY_T);
        this.keyActionMap.put(KeyAction.U, Commodore64Controller.KeyboardMatrix.KEY_U);
        this.keyActionMap.put(KeyAction.V, Commodore64Controller.KeyboardMatrix.KEY_V);
        this.keyActionMap.put(KeyAction.W, Commodore64Controller.KeyboardMatrix.KEY_W);
        this.keyActionMap.put(KeyAction.X, Commodore64Controller.KeyboardMatrix.KEY_X);
        this.keyActionMap.put(KeyAction.Y, Commodore64Controller.KeyboardMatrix.KEY_Y);
        this.keyActionMap.put(KeyAction.Z, Commodore64Controller.KeyboardMatrix.KEY_Z);

        this.keyActionMap.put(KeyAction.BACK_QUOTE.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_LEFT_ARROW);
        this.keyActionMap.put(KeyAction.DEAD_GRAVE.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_LEFT_ARROW);
        this.keyActionMap.put(KeyAction.BACK_QUOTE.withShiftKey(KeyAction.ModifierKey.PRESSED), Commodore64Controller.KeyboardMatrix.KEY_POUND);
        this.keyActionMap.put(KeyAction.DEAD_GRAVE.withShiftKey(KeyAction.ModifierKey.PRESSED), Commodore64Controller.KeyboardMatrix.KEY_POUND);
        this.keyActionMap.put(KeyAction.NUM_1.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_ONE_EXCLAMATION_POINT);
        this.keyActionMap.put(KeyAction.NUM_2.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_TWO_QUOTES);
        this.keyActionMap.put(KeyAction.NUM_3.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_THREE_NUMERAL);
        this.keyActionMap.put(KeyAction.NUM_4.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_FOUR_DOLLAR_SIGN);
        this.keyActionMap.put(KeyAction.NUM_5.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_FIVE_PERCENT_SIGN);
        this.keyActionMap.put(KeyAction.NUM_6.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_SIX_AMPERSAND);
        this.keyActionMap.put(KeyAction.NUM_7.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_SEVEN_BACK_QUOTE);
        this.keyActionMap.put(KeyAction.NUM_8.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_EIGHT_OPEN_PARENTHESIS);
        this.keyActionMap.put(KeyAction.NUM_9.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_NINE_CLOSE_PARENTHESIS);
        this.keyActionMap.put(KeyAction.NUM_0.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_ZERO);

        this.keyActionMap.put(KeyAction.EXCLAMATION_POINT, Commodore64Controller.KeyboardMatrix.KEY_ONE_EXCLAMATION_POINT, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.AT, Commodore64Controller.KeyboardMatrix.KEY_AT);
        this.keyActionMap.put(KeyAction.NUMERAL, Commodore64Controller.KeyboardMatrix.KEY_THREE_NUMERAL, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.DOLLAR_SIGN, Commodore64Controller.KeyboardMatrix.KEY_FOUR_DOLLAR_SIGN, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.PERCENTAGE_SIGN, Commodore64Controller.KeyboardMatrix.KEY_FIVE_PERCENT_SIGN, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.CIRCUMFLEX, Commodore64Controller.KeyboardMatrix.KEY_UP_ARROW);
        this.keyActionMap.put(KeyAction.AMPERSAND, Commodore64Controller.KeyboardMatrix.KEY_SIX_AMPERSAND, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.ASTERISK, Commodore64Controller.KeyboardMatrix.KEY_ASTERISK);
        this.keyActionMap.put(KeyAction.OPEN_PARENTHESIS, Commodore64Controller.KeyboardMatrix.KEY_EIGHT_OPEN_PARENTHESIS, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.CLOSE_PARENTHESIS, Commodore64Controller.KeyboardMatrix.KEY_NINE_CLOSE_PARENTHESIS, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);

        this.keyActionMap.put(KeyAction.MINUS.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_MINUS);
        this.keyActionMap.put(KeyAction.EQUALS.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_EQUALS);
        this.keyActionMap.put(KeyAction.PLUS, Commodore64Controller.KeyboardMatrix.KEY_PLUS);
        this.keyActionMap.put(KeyAction.BACK_SPACE, Commodore64Controller.KeyboardMatrix.KEY_INST_DEL);
        this.keyActionMap.put(KeyAction.TAB, Commodore64Controller.KeyboardMatrix.KEY_COMMODORE);
        this.keyActionMap.put(KeyAction.BACK_SLASH, Commodore64Controller.KeyboardMatrix.KEY_CLR_HOME);
        this.keyActionMap.put(KeyAction.CAPS_LOCK, Commodore64Controller.KeyboardSpecialKey.SHIFT_LOCK);
        this.keyActionMap.put(KeyAction.COLON, Commodore64Controller.KeyboardMatrix.KEY_COLON_OPENING_BRACKET);
        this.keyActionMap.put(KeyAction.OPEN_BRACKET.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_COLON_OPENING_BRACKET, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.SEMICOLON.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_SEMICOLON_CLOSING_BRACKET);
        this.keyActionMap.put(KeyAction.CLOSE_BRACKET.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_SEMICOLON_CLOSING_BRACKET, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.ENTER, Commodore64Controller.KeyboardMatrix.KEY_RETURN);
        this.keyActionMap.put(KeyAction.QUOTE.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_SEVEN_BACK_QUOTE, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.DOUBLE_QUOTES, Commodore64Controller.KeyboardMatrix.KEY_TWO_QUOTES, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.DEAD_ACUTE.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_SEVEN_BACK_QUOTE, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.DEAD_ACUTE.withShiftKey(KeyAction.ModifierKey.PRESSED), Commodore64Controller.KeyboardMatrix.KEY_TWO_QUOTES, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);

        this.keyActionMap.put(KeyAction.SHIFT.withKeyLocation(KeyAction.KeyLocation.LEFT), Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.SHIFT.withKeyLocation(KeyAction.KeyLocation.RIGHT), Commodore64Controller.KeyboardMatrix.KEY_RIGHT_SHIFT);
        this.keyActionMap.put(KeyAction.COMMA.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_COMMA_LESS_THAN);
        this.keyActionMap.put(KeyAction.LESS_THAN, Commodore64Controller.KeyboardMatrix.KEY_COMMA_LESS_THAN, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.PERIOD.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_PERIOD_GREATER_THAN);
        this.keyActionMap.put(KeyAction.GREATER_THAN, Commodore64Controller.KeyboardMatrix.KEY_PERIOD_GREATER_THAN, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.SLASH.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_SLASH_QUESTION_MARK);
        this.keyActionMap.put(KeyAction.QUESTION_MARK, Commodore64Controller.KeyboardMatrix.KEY_SLASH_QUESTION_MARK, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);

        this.keyActionMap.put(KeyAction.CONTROL, Commodore64Controller.KeyboardMatrix.KEY_CTRL);

        this.keyActionMap.put(KeyAction.F1.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F1_F2);
        this.keyActionMap.put(KeyAction.F2.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F1_F2, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.F3.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F3_F4);
        this.keyActionMap.put(KeyAction.F4.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F3_F4, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.F5.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F5_F6);
        this.keyActionMap.put(KeyAction.F6.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F5_F6, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.F7.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F7_F8);
        this.keyActionMap.put(KeyAction.F8.withShiftKey(KeyAction.ModifierKey.UNPRESSED), Commodore64Controller.KeyboardMatrix.KEY_F7_F8, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);

        this.keyActionMap.put(KeyAction.ESCAPE, Commodore64Controller.KeyboardMatrix.KEY_RUN_STOP);
        this.keyActionMap.put(KeyAction.F12, Commodore64Controller.KeyboardSpecialKey.RESTORE);

        this.keyActionMap.put(KeyAction.UP_ARROW, Commodore64Controller.KeyboardMatrix.KEY_CRSR_UP_DOWN, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.DOWN_ARROW, Commodore64Controller.KeyboardMatrix.KEY_CRSR_UP_DOWN);
        this.keyActionMap.put(KeyAction.LEFT_ARROW, Commodore64Controller.KeyboardMatrix.KEY_CRSR_LEFT_RIGHT, Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
        this.keyActionMap.put(KeyAction.RIGHT_ARROW, Commodore64Controller.KeyboardMatrix.KEY_CRSR_LEFT_RIGHT);

        this.keyActionMap.put(KeyAction.SPACE, Commodore64Controller.KeyboardMatrix.KEY_SPACE);

    }

    @Override
    public String getName() {
        return "Commodore 64";
    }

    @Override
    public String getId() {
        return "c64";
    }

    @Override
    public Collection<String> getFileExtensions() {
        return List.of("prg");
    }

    @Override
    public SystemAdapter createSystem(boolean detectedAutomatically) throws Exception {
        return new Commodore64Adapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof Commodore64Adapter;
    }

    Commodore64Settings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getCommodore64Settings();
    }

    private Optional<Commodore64MenuBarSettings> getMenuBarSettings() {
        return Optional.ofNullable(this.commodore64MenuBarSettings);
    }

    private Optional<Commodore64PanelSettings> getPanelSettings() {
        return Optional.ofNullable(this.commodore64PanelSettings);
    }

    @Override
    public Optional<? extends Function<? super EventPublisher, ? extends JMenu>> getSettingsMenuBarContents() {
        return Optional.of(eventPublisher -> {
            Commodore64MenuBarSettings commodore64MenuBarSettings = new Commodore64MenuBarSettings(this, eventPublisher);
            this.commodore64MenuBarSettings = commodore64MenuBarSettings;
            return commodore64MenuBarSettings;
        });
    }

    @Override
    public Optional<? extends Function<? super EventPublisher, ? extends JPanel>> getSettingsWindowContents() {
        return Optional.of(eventPublisher -> {
            Commodore64PanelSettings commodore64PanelSettings = new Commodore64PanelSettings(this, eventPublisher);
            this.commodore64PanelSettings = commodore64PanelSettings;
            return commodore64PanelSettings;
        });
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangedEvent);
        this.getMenuBarSettings().ifPresent(commodore64MenuBarSettings -> commodore64MenuBarSettings.onEvent(coreSettingChangedEvent));
        this.getPanelSettings().ifPresent(commodore64PanelSettings -> commodore64PanelSettings.onEvent(coreSettingChangedEvent));
        switch (coreSettingChangedEvent) {
            case KernalRomPathSettingChangedEvent(Path path) -> this.getEmulationSettings().setKernalRomPath(path);
            case BasicRomPathSettingChangedEvent(Path path) -> this.getEmulationSettings().setBasicRomPath(path);
            case CharacterRomPathSettingChangedEvent(Path path) -> this.getEmulationSettings().setCharacterRomPath(path);
            case VICIIPaletteSettingChangedEvent(Commodore64Settings.VICIIPalette viciiPalette) -> this.getEmulationSettings().setVICIIPalette(viciiPalette);
            default -> {}
        }
    }

    record KernalRomPathSettingChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }

    record BasicRomPathSettingChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }

    record CharacterRomPathSettingChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }

    record VICIIPaletteSettingChangedEvent(@NotNull Commodore64Settings.VICIIPalette viciiPalette) implements CoreSettingChangedEvent, Supplier<Commodore64Settings.VICIIPalette> {

        @Override
        public Commodore64Settings.VICIIPalette get() {
            return this.viciiPalette;
        }

    }

}
