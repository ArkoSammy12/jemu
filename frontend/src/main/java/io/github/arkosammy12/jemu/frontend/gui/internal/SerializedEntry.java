package io.github.arkosammy12.jemu.frontend.gui.internal;

import java.util.function.Consumer;
import java.util.function.Supplier;


public record SerializedEntry(String key, Supplier<String> serializer, Consumer<String> deserializer) {}
