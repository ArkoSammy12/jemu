package io.github.arkosammy12.jemu.app.system.chip8.database;

import java.util.Map;
import java.util.Optional;

record Hashes(Map<String, Integer> hashes) {

    public Optional<Integer> getIndexForHash(String hash) {
        if (hash == null) {
            return Optional.empty();
        }
        if (this.hashes == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.hashes.get(hash));
    }

}

