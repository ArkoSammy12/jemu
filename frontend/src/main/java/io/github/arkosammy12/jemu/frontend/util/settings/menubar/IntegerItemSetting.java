package io.github.arkosammy12.jemu.frontend.util.settings.menubar;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.SpinnerIntegerSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class IntegerItemSetting<E extends Event & Supplier<Integer>> extends SpinnerIntegerSetting<E> implements MenuItemSetting {

    protected final JMenu jMenu;

    public IntegerItemSetting(EventPublisher eventPublisher, @NotNull String name, @NotNull Integer startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Integer, ? extends Event> eventSupplier) {
        super(eventPublisher, name, startingValue, minimumValue, maximumValue, eventClass, eventPredicate, eventSupplier);
        this.jMenu = new JMenu(name);
        this.jMenu.add(this.jSpinner);
    }

    @Override
    public void addToJMenu(JMenu jMenu) {
        jMenu.add(this.jMenu);
    }

}
