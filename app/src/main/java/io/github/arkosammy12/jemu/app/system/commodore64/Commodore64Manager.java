package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Commodore64Manager extends SystemManager {

    @Nullable
    private volatile Commodore64PanelSettings commodore64PanelSettings;

    public Commodore64Manager(Jemu jemu, SystemRegistry systemRegistry) {
        super(jemu, systemRegistry);
    }

    @Override
    public String getName() {
        return "Commodore 64";
    }

    @Override
    public String getId() {
        return "c64";
    }

    @Override
    public Collection<String> getFileExtensions() {
        return List.of();
    }

    @Override
    public SystemAdapter createSystem(boolean detectedAutomatically) throws Exception {
        return new Commodore64Adapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof Commodore64Adapter;
    }

    Commodore64Settings getEmulationSettings() {
        return this.systemRegistry.getEmulationSettings().getCommodore64Settings();
    }

    private Optional<Commodore64PanelSettings> getPanelSettings() {
        return Optional.ofNullable(this.commodore64PanelSettings);
    }

    @Override
    public Optional<? extends Function<? super EventPublisher, ? extends JPanel>> getSettingsWindowContents() {
        return Optional.of(eventPublisher -> {
            Commodore64PanelSettings commodore64PanelSettings = new Commodore64PanelSettings(this, eventPublisher);
            this.commodore64PanelSettings = commodore64PanelSettings;
            return commodore64PanelSettings;
        });
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) {
        super.onCoreSettingChangedEvent(coreSettingChangedEvent);
        this.getPanelSettings().ifPresent(commodore64PanelSettings -> commodore64PanelSettings.onEvent(coreSettingChangedEvent));
        switch (coreSettingChangedEvent) {
            case KernalRomPathSettingChangedEvent(Path path) -> this.getEmulationSettings().setKernalRomPath(path);
            case BasicRomPathSettingChangedEvent(Path path) -> this.getEmulationSettings().setBasicRomPath(path);
            case CharacterRomPathSettingChangedEvent(Path path) -> this.getEmulationSettings().setCharacterRomPath(path);
            default -> {}
        }
    }

    record KernalRomPathSettingChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }

    record BasicRomPathSettingChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }

    record CharacterRomPathSettingChangedEvent(@Nullable Path path) implements CoreSettingChangedEvent, Supplier<@Nullable Path> {

        @Override
        @Nullable
        public Path get() {
            return this.path();
        }

    }

}
