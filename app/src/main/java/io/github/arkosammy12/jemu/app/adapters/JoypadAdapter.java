package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.core.common.SystemController;
import net.java.games.input.Component;
import net.java.games.input.Event;

public class JoypadAdapter {
    private static final float BUTTON_PRESSED_VALUE = 1.0f;
    private static final float AXIS_PRESSED_THRESHOLD = 0.5f;

    private final AbstractSystemAdapter systemAdapter;

    public JoypadAdapter(AbstractSystemAdapter adapter) {
        this.systemAdapter = adapter;
    }

    public void joypadEvent(Event e) {
        Component comp = e.getComponent();
        Component.Identifier id = comp.getIdentifier();

        SystemController.Action action = systemAdapter.getActionForJoypadEvent(e);

        if (action == null) {
            return;
        }

        if (id instanceof Component.Identifier.Button) {
            if (e.getValue() == BUTTON_PRESSED_VALUE) {
                systemAdapter.getEmulator().getSystemController().onActionPressed(action);
            }
            else {
                systemAdapter.getEmulator().getSystemController().onActionReleased(action);
            }
        }
        else if (id instanceof Component.Identifier.Axis) {
            if (e.getValue() >= AXIS_PRESSED_THRESHOLD) {
                systemAdapter.getEmulator().getSystemController().onActionPressed(action);
            }
            else {
                systemAdapter.getEmulator().getSystemController().onActionReleased(action);
            }
        }
    }
}
