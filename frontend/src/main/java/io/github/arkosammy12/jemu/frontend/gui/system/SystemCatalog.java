package io.github.arkosammy12.jemu.frontend.gui.system;

import com.google.gson.JsonElement;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.SystemSettingsBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public interface SystemCatalog {

    Collection<? extends SystemDescriptor> getSystemDescriptors();

    void buildSystemSettings(SystemSettingsBuilder systemSettingsBuilder);

    Optional<JsonElement> serializeSettings();

    void deserializeSettings(@NotNull JsonElement settings);

}
