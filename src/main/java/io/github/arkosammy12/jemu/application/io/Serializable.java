package io.github.arkosammy12.jemu.application.io;

import org.jetbrains.annotations.Nullable;

public interface Serializable {

    String getSerializedString();

    static <T extends Serializable> String serialize(@Nullable T obj) {
        return obj == null ? "null" : obj.getSerializedString();
    }

}
