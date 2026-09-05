package io.github.arkosammy12.jemu.app.util;

import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;

import java.util.*;

public class KeyActionMap<V extends SystemController.Action> {

    private final Map<Integer, List<Map.Entry<KeyAction, List<V>>>> map = new HashMap<>();

    @SafeVarargs
    public final void put(KeyAction action, V value, V... extraValues) {
        List<V> values = new ArrayList<>();
        values.add(value);
        values.addAll(List.of(extraValues));
        this.map.computeIfAbsent(action.keyCode(), _ -> new ArrayList<>()).add(Map.entry(action, List.copyOf(values)));
    }

    public Optional<List<V>> get(KeyAction input) {
        List<Map.Entry<KeyAction, List<V>>> bucket = this.map.get(input.keyCode());
        if (bucket == null) {
            return Optional.empty();
        }
        for (Map.Entry<KeyAction, List<V>> entry : bucket) {
            if (entry.getKey().matches(input)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public Optional<Map.Entry<KeyAction, List<V>>> getMapping(KeyAction input) {
        List<Map.Entry<KeyAction, List<V>>> bucket = this.map.get(input.keyCode());
        if (bucket == null) {
            return Optional.empty();
        }
        for (Map.Entry<KeyAction, List<V>> entry : bucket) {
            if (entry.getKey().matches(input)) {
                return Optional.of(Map.entry(entry.getKey(), entry.getValue()));
            }
        }
        return Optional.empty();
    }

}

