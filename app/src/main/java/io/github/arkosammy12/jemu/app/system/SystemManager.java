package io.github.arkosammy12.jemu.app.system;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.util.exceptions.SystemRedirectException;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;
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

    public SystemRegistry getSystemRegistry() {
        return this.systemRegistry;
    }

    public abstract SystemAdapter createSystem(boolean detectedAutomatically) throws Exception;

    public abstract boolean manages(SystemAdapter systemAdapter);

    public void onCoreSettingChangedEvent(CoreSettingChangeEvent coreSettingChangeEvent) {

    }

    public static byte @Nullable [] loadFromResources(Class<?> clazz, String path) throws Exception {
        try (InputStream in = clazz.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            } else {
                return in.readAllBytes();
            }
        }
    }

    public static String getSha1Hash(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
    }

}
