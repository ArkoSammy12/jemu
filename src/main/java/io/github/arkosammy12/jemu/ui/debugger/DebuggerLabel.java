package io.github.arkosammy12.jemu.ui.debugger;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class DebuggerLabel<T> extends JLabel {

    private final DebuggerSchema.TextEntry<T> textEntry;

    private T state = null;
    private boolean stateChanged = false;
    private Color lastForegroundColor = UIManager.getColor("Table.foreground");

    public DebuggerLabel(DebuggerSchema.TextEntry<T> textEntry) {
        super(textEntry.getName().orElse(""));
        this.textEntry = textEntry;
    }

    private boolean stateHasChanged() {
        return this.stateChanged;
    }

    public void update() {
        T oldState = this.state;
        Optional<Supplier<T>> stateUpdaterOptional = this.textEntry.getStateUpdater();
        if (stateUpdaterOptional.isEmpty()) {
            this.stateChanged = false;
            return;
        }
        T newState = stateUpdaterOptional.get().get();
        this.state = newState;

        String name = this.textEntry.getName().orElse("");
        if (newState == null) {
            this.setText(name);
            this.stateChanged = false;
            return;
        }
        this.stateChanged =!Objects.equals(oldState, newState);

        String text = "";
        if (!name.isEmpty()) {
            text += name;
        }
        text += ": " + this.textEntry.getToStringFunction().orElse(Object::toString).apply(this.state);

        if (!text.equals(this.getText())) {
            this.setText(text);
        }
    }

    public Color getForegroundColor() {
        return this.lastForegroundColor;
    }

    public void updateForegroundColor() {
        this.lastForegroundColor = this.stateHasChanged() ? Color.YELLOW : UIManager.getColor("Table.foreground");
    }

}
