package io.github.arkosammy12.jemu.app.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.arkosammy12.jemu.app.gui.MainWindow;
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
    private Configurations configurations = new Configurations();

    public ConfigurationManager(@Nullable Path settingsFileDirectoryPath) {
        this.settingsFilePath = settingsFileDirectoryPath == null ? null : settingsFileDirectoryPath.resolve("settings.json");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }

    @ApiStatus.Internal
    public Configurations getConfig() {
        return this.configurations;
    }

    public void read(MainWindow mainWindow) {
        if (this.settingsFilePath == null) {
            return;
        }
        Configurations configuration = null;
        try {
            String json = Files.readString(this.settingsFilePath);
            configuration = this.gson.fromJson(json, Configurations.class);
        } catch (Exception e) {
            Logger.error("Error reading configuration file: {}", e);
        }
        if (configuration != null) {
            this.configurations = configuration;
        }
        this.configurations.getPreferenceSettings().getEmulatorSettings().getEmulationSettings().ifPresent(jsonElement -> mainWindow.getSystemCatalog().deserializeSettings(jsonElement));
    }

    public void save(MainWindow mainWindow) {
        if (this.settingsFilePath == null) {
            return;
        }
        try {
            mainWindow.getSystemCatalog().serializeSettings().ifPresent(jsonElement -> this.configurations.getPreferenceSettings().getEmulatorSettings().setEmulationSettings(jsonElement));
            String json = this.gson.toJson(this.configurations);
            Files.writeString(this.settingsFilePath, json);
        } catch (Exception e) {
            Logger.error("Error saving configurations to file: {}", e);
        }
    }

}
