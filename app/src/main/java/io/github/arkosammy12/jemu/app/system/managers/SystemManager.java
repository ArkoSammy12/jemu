package io.github.arkosammy12.jemu.app.system.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.gui.system.builder.SystemSettingsBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

public abstract class SystemManager implements SystemDescriptor {

    protected final Jemu jemu;
    protected final SystemRegistry systemRegistry;

    public SystemManager(Jemu jemu, SystemRegistry systemRegistry) {
        this.jemu = jemu;
        this.systemRegistry = systemRegistry;
    }

    public abstract SystemAdapter createSystem() throws Exception;

    public abstract boolean manages(SystemAdapter systemAdapter);

    public SystemSettingsBuilder buildSystemSettings(SystemSettingsBuilder systemSettingsBuilder) {
        return systemSettingsBuilder;
    }

    public void onCoreSettingEvent(CoreSettingChangeEvent coreSettingChangeEvent) {

    }

    protected byte @Nullable [] loadFromResources(String path) throws Exception {
        try (InputStream in = this.getClass().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            } else {
                return in.readAllBytes();
            }
        }
    }

    static String getSha1Hash(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
    }

}
