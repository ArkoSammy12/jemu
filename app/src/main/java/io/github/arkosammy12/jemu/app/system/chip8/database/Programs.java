package io.github.arkosammy12.jemu.app.system.chip8.database;

import java.util.List;
import java.util.Optional;

record Programs(List<ProgramEntry> programEntries) {

    public Optional<ProgramEntry> getProgramEntryAt(int index) {
        if (this.programEntries == null) {
            return Optional.empty();
        }
        if (index < 0 || index >= this.programEntries.size()) {
            return Optional.empty();
        }
        return Optional.of(this.programEntries.get(index));
    }

}
