package io.github.arkosammy12.jemu.app.util.exceptions;

import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;

public class SystemRedirectException extends RuntimeException {

    private final SystemDescriptor targetSystemDescriptor;

    public SystemRedirectException(SystemDescriptor targetSystemDescriptor) {
        this.targetSystemDescriptor = targetSystemDescriptor;
    }

    public SystemDescriptor getTargetSystemDescriptor() {
        return this.targetSystemDescriptor;
    }

}
