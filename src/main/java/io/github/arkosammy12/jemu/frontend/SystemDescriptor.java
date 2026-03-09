package io.github.arkosammy12.jemu.frontend;

import java.util.Optional;

public interface SystemDescriptor {

    String getName();

    Optional<String[]> getFileExtensions();

}
