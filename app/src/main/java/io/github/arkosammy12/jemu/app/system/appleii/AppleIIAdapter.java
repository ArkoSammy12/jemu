package io.github.arkosammy12.jemu.app.system.appleii;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.appleii.AppleIIEmulator;
import io.github.arkosammy12.jemu.core.appleii.AppleIISystemHost;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.util.Optional;

public class AppleIIAdapter extends SystemAdapter implements AppleIISystemHost {

    public AppleIIAdapter(Jemu jemu, SystemManager systemManager) throws LineUnavailableException {
        super(jemu, systemManager);
    }

    @Override
    protected Emulator createEmulator() {
        return new AppleIIEmulator(this);
    }

    @Override
    protected @Nullable SystemController.Action getActionForKeyCode(int keyCode) {
        return null;
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.empty();
    }
}
