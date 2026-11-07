package io.github.arkosammy12.jemu.frontend.util.settings.panel;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.SpinnerIntegerSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class IntegerPanelSpinnerSetting<E extends Event & Supplier<Integer>> extends SpinnerIntegerSetting<E> implements PanelSetting {

    private final JLabel primaryJLabel;

    @Nullable
    private final JLabel secondaryJLabel;

    public IntegerPanelSpinnerSetting(EventPublisher eventPublisher, @NotNull String name, @Nullable String secondaryLabel, @NotNull Integer startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Integer, ? extends Event> eventSupplier) {
        super(eventPublisher, name, startingValue, minimumValue, maximumValue, eventClass, eventPredicate, eventSupplier);
        this.primaryJLabel = new JLabel(name);
        this.secondaryJLabel = secondaryLabel == null ? null : new JLabel(secondaryLabel);
    }

    @Override
    public void addToJPanel(JPanel jPanel, Object... constraints) {
        jPanel.add(this.primaryJLabel, constraints.length >= 1 ? constraints[0] : null);
        jPanel.add(this.jSpinner, constraints.length >= 2 ? constraints[1] : null);
        if (this.secondaryJLabel != null) {
            jPanel.add(this.secondaryJLabel, constraints.length >= 3 ? constraints[2] : null);
        }
    }

}
