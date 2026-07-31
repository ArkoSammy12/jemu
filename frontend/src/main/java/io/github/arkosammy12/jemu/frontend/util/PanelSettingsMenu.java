package io.github.arkosammy12.jemu.frontend.util;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.settings.UISetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.EnumPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.IntegerPanelSpinnerSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.PathUISetting;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PanelSettingsMenu extends JPanel {

    private final MainWindow mainWindow;
    protected final Collection<UISetting<?, ?>> settings = new ArrayList<>();
    protected final JPanel innerPanel = new JPanel();

    public PanelSettingsMenu(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.setLayout(new MigLayout());

        JScrollPane scrollPane = new JScrollPane(this.innerPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.add(scrollPane, "grow, push");
        this.innerPanel.setLayout(new MigLayout());
    }

    public void addHeader(String text) {
        this.innerPanel.add(new JLabel("<html><h1>%s</h1></html>".formatted(text)), "wrap");
    }

    public void addEmptyLine() {
        this.innerPanel.add(new JLabel(""), "spanx, wrap");
    }

    public <E extends Event & Supplier<Boolean>> BooleanPanelSetting<E> addBooleanSetting(String name, boolean startingValue, @Nullable Class<E> classEvent, @Nullable Predicate<E> eventPredicate, Function<? super Boolean, ? extends Event> eventSupplier) {
        BooleanPanelSetting<E> booleanPanelSetting = new BooleanPanelSetting<>(this.mainWindow, name, startingValue, classEvent, eventPredicate, eventSupplier);
        booleanPanelSetting.addToJPanel(this.innerPanel, "wrap");
        this.settings.add(booleanPanelSetting);
        return booleanPanelSetting;
    }

    public <E extends Event & Supplier<Integer>> IntegerPanelSpinnerSetting<E> addIntegerSetting(String name, @Nullable String secondaryLabel, int startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, @Nullable Class<E> classEvent, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Integer, ? extends Event> eventSupplier) {
        IntegerPanelSpinnerSetting<E> spinnerIntegerSetting = new IntegerPanelSpinnerSetting<>(this.mainWindow, name, secondaryLabel, startingValue, minimumValue, maximumValue, classEvent, eventPredicate, eventSupplier);
        this.addIntegerSetting(spinnerIntegerSetting);
        return spinnerIntegerSetting;
    }

    public <E extends Event & Supplier<Integer>> void addIntegerSetting(IntegerPanelSpinnerSetting<E> integerSetting) {
        integerSetting.addToJPanel(this.innerPanel, null, "split 2", "growx, wrap");
        this.settings.add(integerSetting);
    }

    public <V extends Enum<V> & DisplayNamerProvider, E extends Event & Supplier<V>> EnumPanelSetting<V, E> addEnumSetting(String name, @NotNull V startingValue, @Nullable Class<E> classEvent, @Nullable Predicate<E> eventPredicate, Function<? super V, ? extends Event> eventSupplier) {
        EnumPanelSetting<V, E> enumPanelSetting = new EnumPanelSetting<>(this.mainWindow, name, startingValue, classEvent, eventPredicate, eventSupplier);
        enumPanelSetting.addToJPanel(this.innerPanel, null, "growx, wrap");
        this.settings.add(enumPanelSetting);
        return enumPanelSetting;
    }

    public <E extends Event & Supplier<Path>> PathUISetting<E> addPathSetting(PathUISetting.PathSelectionMode pathSelectionMode, @NotNull String name, @NotNull Path startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Path, ? extends Event> eventSupplier) {
        PathUISetting<E> pathUISetting = new PathUISetting<>(this.mainWindow, pathSelectionMode, name, startingValue, eventClass, eventPredicate, eventSupplier);
        pathUISetting.addToJPanel(this.innerPanel, null, "growx", "growx, wrap");
        this.settings.add(pathUISetting);
        return pathUISetting;
    }

    public void onEvent(Event event) {
        for (UISetting<?, ?> setting : this.settings) {
            setting.onEvent(event);
        }
    }

}
