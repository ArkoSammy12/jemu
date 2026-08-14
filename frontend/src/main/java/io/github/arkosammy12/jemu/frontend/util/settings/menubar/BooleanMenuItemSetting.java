package io.github.arkosammy12.jemu.frontend.util.settings.menubar;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.BooleanUISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BooleanMenuItemSetting<E extends Event & Supplier<Boolean>> extends BooleanUISetting<E> implements MenuItemSetting {

    private final JRadioButtonMenuItem jRadioButtonMenuItem;

    public BooleanMenuItemSetting(EventPublisher eventPublisher, @NotNull String name, @NotNull Boolean startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Boolean, ? extends Event> eventSupplier) {
        super(eventPublisher, name, startingValue, eventClass, eventPredicate, eventSupplier);
        this.jRadioButtonMenuItem = new JRadioButtonMenuItem(name);
        this.jRadioButtonMenuItem.setSelected(startingValue);
        this.jRadioButtonMenuItem.addActionListener(_ ->  this.eventPublisher.publishEvent(eventSupplier.apply(this.jRadioButtonMenuItem.isSelected())));
    }

    @Override
    public void setValue(Boolean value) {
        SwingUtilities.invokeLater(() -> this.jRadioButtonMenuItem.setSelected(value));
    }

    @Override
    public void addToJMenu(JMenu jMenu) {
        jMenu.add(this.jRadioButtonMenuItem);
    }

}
