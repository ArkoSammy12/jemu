package io.github.arkosammy12.jemu.app.system;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.util.KeyActionMap;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class SystemManager implements SystemDescriptor {

    protected final Jemu jemu;
    protected final SystemRegistry systemRegistry;
    protected final KeyActionMap<SystemController.Action> keyActionMap = new KeyActionMap<>();

    public SystemManager(Jemu jemu, SystemRegistry systemRegistry) {
        this.jemu = jemu;
        this.systemRegistry = systemRegistry;
    }

    public SystemRegistry getSystemRegistry() {
        return this.systemRegistry;
    }

    public abstract SystemAdapter createSystem(boolean detectedAutomatically) throws Exception;

    public abstract boolean manages(SystemAdapter systemAdapter);

    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) {

    }

    public Optional<List<SystemController.Action>> getActionsForKey(KeyAction keyAction) {
        return this.keyActionMap.get(keyAction);
    }

    public Optional<Map.Entry<KeyAction, List<SystemController.Action>>> getMappingsForKey(KeyAction keyAction) {
        return this.keyActionMap.getMapping(keyAction);
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
