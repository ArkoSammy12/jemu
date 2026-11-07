package io.github.arkosammy12.jemu.frontend.gui.system;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public interface SystemCatalog {

    Collection<? extends SystemDescriptor> getSystemDescriptors();

    Optional<JsonElement> serializeSettings();

    void deserializeSettings(@NotNull JsonElement settings);

}
