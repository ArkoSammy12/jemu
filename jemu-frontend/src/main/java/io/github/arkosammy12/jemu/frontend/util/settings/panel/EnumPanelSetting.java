package io.github.arkosammy12.jemu.frontend.util.settings.panel;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.EnumUISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EnumPanelSetting<V extends Enum<V> & DisplayNamerProvider, E extends Event & Supplier<V>> extends EnumUISetting<V, E> implements PanelSetting {

    private final JLabel jLabel;
    private final JComboBox<V> jComboBox;
    private final ActionListener actionListener;

    @SuppressWarnings("unchecked")
    public EnumPanelSetting(EventPublisher eventPublisher, @NotNull String name, @NotNull V startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super V, ? extends Event> eventSupplier) {
        super(eventPublisher, name, startingValue, eventClass, eventPredicate, eventSupplier);
        this.jLabel = new JLabel(name + ": ");
        this.jComboBox = new JComboBox<>(startingValue.getDeclaringClass().getEnumConstants());
        this.jComboBox.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DisplayNamerProvider provider) {
                    this.setText(provider.getDisplayName());
                }
                return this;
            }

        });
        this.jComboBox.setSelectedItem(startingValue);

        this.actionListener = _ -> {
            Object selectedItem = this.jComboBox.getSelectedItem();
            if (this.enumClass.isInstance(selectedItem)) {
                this.eventPublisher.publishEvent(eventSupplier.apply((V) selectedItem));
            }
        };

        this.jComboBox.addActionListener(this.actionListener);
    }

    @Override
    public void addToJPanel(JPanel jPanel, Object... constraints) {
        jPanel.add(this.jLabel, constraints.length >= 1 ? constraints[0] : null);
        jPanel.add(this.jComboBox, constraints.length >= 2 ? constraints[1] : null);
    }

    @Override
    public void setValue(V value) {
        SwingUtilities.invokeLater(() -> {
            this.jComboBox.removeActionListener(this.actionListener);
            this.jComboBox.setSelectedItem(value);
            this.jComboBox.addActionListener(this.actionListener);
        });
    }

}
