package io.github.arkosammy12.jemu.disassembler;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;
import java.util.function.IntSupplier;

public interface Disassembler extends Closeable {

    void setEnabled(boolean enabled);

    boolean isEnabled();

    int getSize();

    Optional<IntSupplier> getProgramCounterSupplier();

    int getOrdinalForAddress(int address);

    @Nullable
    Entry getEntry(int ordinal);

    void addBreakpoint(int address);

    void removeBreakpoint(int address);

    boolean checkBreakpoint(int address);

    void clearBreakpoints();

    Collection<Integer> getCurrentBreakpoints();

    interface Entry {

        int getAddress();

        int getLength();

        int getByteCode();

        String getText();

    }

}
