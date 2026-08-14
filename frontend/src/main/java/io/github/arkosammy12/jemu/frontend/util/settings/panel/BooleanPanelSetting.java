package io.github.arkosammy12.jemu.frontend.util.settings.panel;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.BooleanUISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BooleanPanelSetting<E extends Event & Supplier<Boolean>> extends BooleanUISetting<E> implements PanelSetting {

    private final JCheckBox jCheckBox = new JCheckBox();

    public BooleanPanelSetting(EventPublisher eventPublisher, @NotNull String name, @NotNull Boolean startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Boolean, ? extends Event> eventSupplier) {
        super(eventPublisher, name, startingValue, eventClass, eventPredicate, eventSupplier);
        this.jCheckBox.setText(name);
        this.jCheckBox.setSelected(startingValue);
        this.jCheckBox.addActionListener(_ -> this.eventPublisher.publishEvent(eventSupplier.apply(this.jCheckBox.isSelected())));
    }

    public JCheckBox getJCheckBox() {
        return this.jCheckBox;
    }

    @Override
    public void setValue(Boolean value) {
        SwingUtilities.invokeLater(() -> this.jCheckBox.setSelected(value));
    }

    @Override
    public void addToJPanel(JPanel jPanel, Object... constraints) {
        jPanel.add(this.jCheckBox, constraints.length >= 1 ? constraints[0] : null);
    }


}
