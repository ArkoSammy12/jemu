package io.github.arkosammy12.jemu.core.common;

public interface SystemController {

    void pressAction(Action action);

    void releaseAction(Action action);

    interface Action {

        String getLabel();

    }

}
