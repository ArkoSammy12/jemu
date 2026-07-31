package io.github.arkosammy12.jemu.frontend.util;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.settings.SpinnerIntegerSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.UISetting;
import io.github.arkosammy12.jemu.frontend.util.settings.menubar.BooleanMenuItemSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.menubar.EnumMenuItemSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.menubar.IntegerItemSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MenuBarSettingsMenu extends JMenu {

    private final MainWindow mainWindow;
    protected final Collection<UISetting<?, ?>> settings = new ArrayList<>();
    private final Collection<MenuBarSettingsMenu> subMenus = new ArrayList<>();

    public MenuBarSettingsMenu(MainWindow mainWindow, String name) {
        this.mainWindow = mainWindow;
        this.setText(name);
    }

    public MenuBarSettingsMenu addMenu(String name) {
        MenuBarSettingsMenu menu = new MenuBarSettingsMenu(this.mainWindow, name);
        this.add(menu);
        this.subMenus.add(menu);
        return menu;
    }

    public <E extends Event & Supplier<Boolean>> BooleanMenuItemSetting<E> addBooleanSetting(String name, boolean startingValue, @Nullable Class<E> classEvent, @Nullable Predicate<E> eventPredicate, Function<? super Boolean, ? extends Event> eventSupplier) {
        BooleanMenuItemSetting<E> booleanMenuItemSetting = new BooleanMenuItemSetting<>(this.mainWindow, name, startingValue, classEvent, eventPredicate, eventSupplier);
        booleanMenuItemSetting.addToJMenu(this);
        this.settings.add(booleanMenuItemSetting);
        return booleanMenuItemSetting;
    }

    public <E extends Event & Supplier<Integer>> SpinnerIntegerSetting<E> addIntegerSetting(String name, int startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, @Nullable Class<E> classEvent, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Integer, ? extends Event> eventSupplier) {
        IntegerItemSetting<E> spinnerIntegerSetting = new IntegerItemSetting<>(this.mainWindow, name, startingValue, minimumValue, maximumValue, classEvent, eventPredicate, eventSupplier);
        this.addIntegerSetting(spinnerIntegerSetting);
        return spinnerIntegerSetting;
    }

    public <E extends Event & Supplier<Integer>> void addIntegerSetting(IntegerItemSetting<E> integerItemSetting) {
        integerItemSetting.addToJMenu(this);
        this.settings.add(integerItemSetting);
    }

    public <V extends Enum<V> & DisplayNamerProvider, E extends Event & Supplier<V>> EnumMenuItemSetting<V, E> addEnumSetting(String name, @NotNull V startingValue, @Nullable Class<E> classEvent, @Nullable Predicate<E> eventPredicate, Function<? super V, ? extends Event> eventSupplier) {
        EnumMenuItemSetting<V, E> enumMenuItemSetting = new EnumMenuItemSetting<>(this.mainWindow, name, startingValue, classEvent, eventPredicate, eventSupplier);
        enumMenuItemSetting.addToJMenu(this);
        this.settings.add(enumMenuItemSetting);
        return enumMenuItemSetting;
    }

    public void onEvent(Event event) {
        for (UISetting<?, ?> setting : this.settings) {
            setting.onEvent(event);
        }
        for (MenuBarSettingsMenu subMenu : this.subMenus) {
            subMenu.onEvent(event);
        }
    }

}
