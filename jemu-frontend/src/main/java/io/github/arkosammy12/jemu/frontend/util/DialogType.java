package io.github.arkosammy12.jemu.frontend.util;

import javax.swing.*;

public enum DialogType {
    INFORMATION(JOptionPane.INFORMATION_MESSAGE),
    WARNING(JOptionPane.WARNING_MESSAGE),
    ERROR(JOptionPane.ERROR_MESSAGE);

    private final int jOptionPaneMessageTypeId;

    DialogType(int jOptionPaneMessageTypeId) {
        this.jOptionPaneMessageTypeId = jOptionPaneMessageTypeId;
    }

    public int getJOptionPaneMessageTypeId() {
        return this.jOptionPaneMessageTypeId;
    }

}