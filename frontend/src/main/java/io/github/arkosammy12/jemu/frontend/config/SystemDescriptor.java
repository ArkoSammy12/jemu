package io.github.arkosammy12.jemu.frontend.config;

import java.util.Collection;
import java.util.Optional;

public interface SystemDescriptor {

    String getName();

    String getId();

    Collection<String> getFileExtensions();

    default Optional<Category> getCategory() {
        return Optional.empty();
    }

    interface Category {

        String getName();

    }

}
