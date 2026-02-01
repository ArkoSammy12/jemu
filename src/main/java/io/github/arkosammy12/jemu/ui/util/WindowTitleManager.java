package io.github.arkosammy12.jemu.ui.util;

import io.github.arkosammy12.jemu.main.MainWindow;
import org.apache.commons.collections4.list.GrowthList;

import javax.swing.*;
import java.util.List;

public final class WindowTitleManager {

    private final MainWindow mainWindow;
    private final List<String> sections = new GrowthList<>();

    public WindowTitleManager(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void setSection(int index, String text) {
        while (this.sections.size() <= index) {
            this.sections.add("");
        }
        this.sections.set(index, text);
        this.updateTitle();
    }

    private void updateTitle() {
        SwingUtilities.invokeLater(() -> this.mainWindow.setTitle(String.join(" | ", sections)));
    }

    public void clear() {
        this.sections.clear();
        this.updateTitle();
    }
}
