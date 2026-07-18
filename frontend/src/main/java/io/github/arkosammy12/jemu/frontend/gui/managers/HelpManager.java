package io.github.arkosammy12.jemu.frontend.gui.managers;

import org.jetbrains.annotations.NotNull;

public interface HelpManager {

    void setProjectName(@NotNull String projectName);

    void setAuthorString(@NotNull String authorString);

    void setVersionString(@NotNull String versionString);

    void setCommitIDString(@NotNull String commitIdString);

    void setBuildDateString(@NotNull String buildDateString);

    void setProjectSourceLink(@NotNull String projectSourceLink);

    void setProjectBugReportLink(@NotNull String projectBugReportLink);

}
