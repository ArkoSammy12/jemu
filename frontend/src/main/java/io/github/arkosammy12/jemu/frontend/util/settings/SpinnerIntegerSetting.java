package io.github.arkosammy12.jemu.frontend.util.settings;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class SpinnerIntegerSetting<E extends Event & Supplier<Integer>> extends IntegerUISetting<E> {

    protected final JSpinner jSpinner;

    private final ChangeListener changeListener;

    public SpinnerIntegerSetting(MainWindow mainWindow, @NotNull String name, @NotNull Integer startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Integer, ? extends Event> eventSupplier) {
        super(mainWindow, name, startingValue, minimumValue, maximumValue, eventClass, eventPredicate, eventSupplier);
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel();
        if (this.minimumValue != null) {
            spinnerNumberModel.setMinimum(this.minimumValue);
        }
        if (this.maximumValue != null) {
            spinnerNumberModel.setMaximum(this.maximumValue);
        }
        spinnerNumberModel.setValue(startingValue);

        this.jSpinner = new JSpinner(spinnerNumberModel);

        this.changeListener = _ -> {
            if (jSpinner.getValue() instanceof Integer value) {
                this.mainWindow.publishEvent(eventSupplier.apply(value));
            }
        };

        this.jSpinner.addChangeListener(changeListener);
    }

    public JSpinner getJSpinner() {
        return this.jSpinner;
    }

    @Override
    public void setValue(Integer value) {
        SwingUtilities.invokeLater(() -> {
            this.jSpinner.removeChangeListener(this.changeListener);
            this.jSpinner.setValue(value);
            this.jSpinner.addChangeListener(this.changeListener);
        });
    }

}
