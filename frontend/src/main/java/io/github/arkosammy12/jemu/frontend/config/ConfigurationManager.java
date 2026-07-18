package io.github.arkosammy12.jemu.frontend.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.arkosammy12.jemu.frontend.config.internal.InternalConfigurations;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationManager {

    @Nullable
    private final Path settingsFilePath;
    private final Gson gson;

    @NotNull
    private InternalConfigurations internalConfigurations = new InternalConfigurations();

    public ConfigurationManager(@Nullable Path settingsFileDirectoryPath) {
        this.settingsFilePath = settingsFileDirectoryPath == null ? null : settingsFileDirectoryPath.resolve("settings.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
    }

    @ApiStatus.Internal
    public InternalConfigurations getConfig() {
        return this.internalConfigurations;
    }

    public void read(MainWindow mainWindow) {
        if (this.settingsFilePath == null) {
            return;
        }
        InternalConfigurations internalConfigurations = null;
        try {
            String json = Files.readString(this.settingsFilePath);
            internalConfigurations = this.gson.fromJson(json, InternalConfigurations.class);
        } catch (Exception e) {
            Logger.error("Error reading configuration file: {}", e);
        }
        if (internalConfigurations != null) {
            this.internalConfigurations = internalConfigurations;
        }
        this.internalConfigurations.getInternalPreferenceSettings().getInternalEmulatorSettings().getEmulationSettings().ifPresent(jsonElement -> mainWindow.getSystemCatalog().deserializeSettings(jsonElement));
    }

    public void save(MainWindow mainWindow) {
        if (this.settingsFilePath == null) {
            return;
        }
        try {
            mainWindow.getSystemCatalog().serializeSettings().ifPresent(jsonElement -> this.internalConfigurations.getInternalPreferenceSettings().getInternalEmulatorSettings().setEmulationSettings(jsonElement));
            String json = this.gson.toJson(this.internalConfigurations);
            Files.writeString(this.settingsFilePath, json);
        } catch (Exception e) {
            Logger.error("Error saving configurations to file: {}", e);
        }
    }

}
