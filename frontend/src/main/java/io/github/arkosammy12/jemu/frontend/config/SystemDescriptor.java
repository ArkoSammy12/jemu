package io.github.arkosammy12.jemu.frontend.config;

import java.util.Collection;

public interface SystemDescriptor {

    String getName();

    String getId();

    Collection<String> getFileExtensions();

}
