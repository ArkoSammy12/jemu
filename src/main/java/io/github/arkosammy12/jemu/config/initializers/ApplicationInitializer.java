package io.github.arkosammy12.jemu.config.initializers;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ApplicationInitializer extends EmulatorInitializer {

    Optional<List<Path>> getRecentFiles();

    Optional<String> getCurrentDirectory();

    Optional<Integer> getVolume();

    Optional<Boolean> getMuted();

    Optional<Boolean> getShowingInfoBar();

    Optional<Boolean> getShowingDebugger();

    Optional<Boolean> getShowingDisassembler();

    Optional<Boolean> getDebuggerFollowing();

    Optional<Boolean> getDisassemblerFollowing();

}
