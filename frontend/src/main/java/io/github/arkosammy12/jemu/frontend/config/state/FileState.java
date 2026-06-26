package io.github.arkosammy12.jemu.frontend.config.state;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileState {

    @SerializedName("recent_files")
    private volatile List<String> recentFilePaths = new ArrayList<>();

    @Nullable
    @SerializedName("current_directory")
    private volatile String currentDirectoryPath = null;

    public void setRecentFilePaths(Collection<Path> recentFilePaths)  {
        this.recentFilePaths = recentFilePaths.stream().map(Path::toString).toList();
    }

    public List<Path> getRecentFilePaths() {
        return this.recentFilePaths.stream().map(Path::of).collect(Collectors.toList());
    }

    public void setCurrentDirectoryPath(Path currentDirectoryPath) {
        this.currentDirectoryPath = currentDirectoryPath.toString();
    }

    public Optional<Path> getCurrentDirectoryPath() {
        return Optional.ofNullable(this.currentDirectoryPath).map(Path::of);
    }

}
