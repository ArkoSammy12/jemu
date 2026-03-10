package io.github.arkosammy12.jemu.frontend;

import java.util.Optional;

public interface SystemDescriptor {

    String getName();

    String getId();

    Optional<String[]> getFileExtensions();

}
