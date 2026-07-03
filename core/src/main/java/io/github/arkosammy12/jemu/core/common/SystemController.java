package io.github.arkosammy12.jemu.core.common;

public interface SystemController {

    void onActionPressed(Action action);

    void onActionReleased(Action action);

    interface Action {

        String getLabel();

    }

}
