package io.github.arkosammy12.jemu.frontend.util.settings.menubar;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.EnumUISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EnumMenuItemSetting<V extends Enum<V> & DisplayNamerProvider, E extends Event & Supplier<V>> extends EnumUISetting<V, E> implements MenuItemSetting {

    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final JMenu jMenu;
    private final Map<V, JRadioButtonMenuItem> buttonMap = new HashMap<>();

    public EnumMenuItemSetting(EventPublisher eventPublisher, @NotNull String name, @NotNull V startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super V, ? extends Event> eventSupplier) {
        super(eventPublisher, name, startingValue, eventClass, eventPredicate, eventSupplier);
        this.jMenu = new JMenu(name);
        for (V enumVariant : startingValue.getDeclaringClass().getEnumConstants()) {
            JRadioButtonMenuItem enumSettingButton = new JRadioButtonMenuItem(enumVariant.getDisplayName());
            this.buttonGroup.add(enumSettingButton);
            this.buttonMap.put(enumVariant, enumSettingButton);

            enumSettingButton.setSelected(startingValue == enumVariant);
            enumSettingButton.addActionListener(_ -> this.eventPublisher.publishEvent(eventSupplier.apply(enumVariant)));
            this.jMenu.add(enumSettingButton);
        }
    }

    @Override
    public void setValue(V value) {
        SwingUtilities.invokeLater(() -> {
            this.buttonGroup.clearSelection();
            JRadioButtonMenuItem button = this.buttonMap.get(value);
            if (button != null) {
                button.setSelected(true);
            }
        });
    }

    @Override
    public void addToJMenu(JMenu jMenu) {
        jMenu.add(this.jMenu);
    }

}
