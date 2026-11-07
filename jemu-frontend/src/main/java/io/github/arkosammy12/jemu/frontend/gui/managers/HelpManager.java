package io.github.arkosammy12.jemu.frontend.gui.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

public interface HelpManager {

    void setHelpDialogContentsSupplier(Function<? super JFrame, ? extends @Nullable JPanel> helpDialogContentsSupplier);

    void setProjectSourceLink(@NotNull String projectSourceLink);

    void setProjectBugReportLink(@NotNull String projectBugReportLink);

}
