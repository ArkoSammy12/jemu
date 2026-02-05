package io.github.arkosammy12.jemu.disassembler;

import io.github.arkosammy12.jemu.systems.Emulator;
import io.github.arkosammy12.jemu.systems.bus.BusView;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntSupplier;

public abstract class AbstractDisassembler<E extends Emulator> implements Disassembler {

    private static final int RANGE_ADDRESS_FLAG = 0x8000_0000;

    protected final E emulator;
    private volatile boolean enabled = false;
    private volatile boolean running = true;
    private volatile IntSupplier programCounterSupplier = null;

    private final Thread disassemblerThread;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = this.readWriteLock.readLock();
    private final Lock writeLock = this.readWriteLock.writeLock();

    private final BlockingQueue<Integer> addressQueue = new MpscBlockingConsumerArrayQueue<>(200000);
    private final NavigableMap<Integer, Entry> entries = new ConcurrentSkipListMap<>();
    private final IntArrayList addressOrdinalList = new IntArrayList();

    private final Set<Integer> breakpoints = ConcurrentHashMap.newKeySet();
    private final Collection<Integer> immutableBreakpointsView = Collections.unmodifiableSet(this.breakpoints);
    private final AtomicInteger lastSeenPC = new AtomicInteger(-1);

    private int currentStaticDisassemblerPointer = 0;
    private boolean staticDisassemblyFinished = false;

    public AbstractDisassembler(E emulator) {
        this.emulator = emulator;
        this.disassemblerThread = new Thread(this::disassemblerLoop, "jemu-disassembler-thread");
        this.disassemblerThread.setDaemon(true);
        this.disassemblerThread.start();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public int getSize() {
        this.readLock.lock();
        try {
            return this.addressOrdinalList.size();
        } finally {
            this.readLock.unlock();
        }
    }

    public void setProgramCounterSupplier(IntSupplier supplier) {
        this.programCounterSupplier = supplier;
    }

    @Override
    public Optional<IntSupplier> getProgramCounterSupplier() {
        return Optional.ofNullable(this.programCounterSupplier);
    }

    @Override
    public int getOrdinalForAddress(int address) {
        this.readLock.lock();
        try {
            return this.addressOrdinalList.indexOf(address);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    @Nullable
    public Disassembler.Entry getEntry(int ordinal) {
        this.readLock.lock();
        try {
            if (ordinal < 0 || ordinal >= addressOrdinalList.size()) {
                return null;
            }
            Entry entry = this.entries.get(this.addressOrdinalList.get(ordinal));
            if (entry == null) {
                return null;
            }
            this.validateEntry(entry);
            return entry;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void addBreakpoint(int address) {
        this.breakpoints.add(address);
    }

    @Override
    public void removeBreakpoint(int address) {
        this.breakpoints.remove(address);
    }

    @Override
    public boolean checkBreakpoint(int address) {
        int previousPC = this.lastSeenPC.getAndSet(address);
        if (!this.isEnabled()) {
            return false;
        }
        if (this.breakpoints.isEmpty()) {
            return false;
        }
        if (previousPC == address) {
            return false;
        }
        return this.breakpoints.contains(address);
    }

    @Override
    public void clearBreakpoints() {
        this.breakpoints.clear();
    }

    @Override
    public Collection<Integer> getCurrentBreakpoints() {
        return this.immutableBreakpointsView;
    }

    @Override
    public void close() throws IOException {
        this.running = false;
        this.disassemblerThread.interrupt();
        try {
            this.disassemblerThread.join();
        } catch (InterruptedException _) {}
    }

    public void disassemble(int address) {
        if (!this.isEnabled()) {
            return;
        }
        this.addressQueue.offer(address);
    }

    public void disassembleRange(int address, int range, boolean addressIsProgramCounter) {
        if (!this.isEnabled()) {
            return;
        }
        int currentAddress = address;
        if (addressIsProgramCounter) {
            this.addressQueue.offer(address);
            currentAddress += this.getLengthForInstructionAt(address);
        }
        for (int i = 0; i < range; i++) {
            this.addressQueue.offer(currentAddress | RANGE_ADDRESS_FLAG);
            currentAddress += this.getLengthForInstructionAt(currentAddress);
        }
    }

    protected abstract int getLengthForInstructionAt(int address);

    protected abstract int getBytecodeForInstructionAt(int address);

    protected abstract String getTextForInstructionAt(int address);

    private void disassemblerLoop() {
        while (this.running) {
            try {
                if (this.staticDisassemblyFinished) {
                    this.disassembleDynamic(this.addressQueue.take());
                } else {
                    Integer address = this.addressQueue.poll();
                    if (address == null) {
                        this.disassembleStatic();
                    } else {
                        this.disassembleDynamic(address);
                    }
                }
            } catch (InterruptedException e) {
                if (!this.running) {
                    return;
                }
            }
        }
    }

    private void disassembleStatic() {
        BusView bus = this.emulator.getBusView();
        if (this.currentStaticDisassemblerPointer >= bus.getMemorySize()) {
            this.staticDisassemblyFinished = true;
            return;
        }
        int length = this.getLengthForInstructionAt(this.currentStaticDisassemblerPointer);
        if (!this.isAddressCovered(this.currentStaticDisassemblerPointer, length, null)) {
            this.addEntry(new Entry(this.currentStaticDisassemblerPointer, length, this.getBytecodeForInstructionAt(this.currentStaticDisassemblerPointer), Entry.Type.STATIC));
        }
        this.currentStaticDisassemblerPointer += length;
    }

    private void disassembleDynamic(int address) {
        boolean isRangeAddress = (address & RANGE_ADDRESS_FLAG) != 0;
        address = isRangeAddress ? address & ~RANGE_ADDRESS_FLAG : address;
        int length = this.getLengthForInstructionAt(address);
        int bytecode = this.getBytecodeForInstructionAt(address);

        // Add a new entry if one doesn't currently exist at address, unless we are adding
        // a range entry, and it is currently covered by trace entries.
        Entry entry = this.entries.get(address);
        if (entry == null) {
            if (!isRangeAddress || !this.isAddressCovered(address, length, Entry.Type.RANGE)) {
                this.addEntry(new Entry(address, length, bytecode, isRangeAddress ? Entry.Type.RANGE : Entry.Type.TRACE));
            }
            return;
        }

        // If the structure changed, overwrite the existing entry with a new entry, unless we are attempting to overwrite
        // with a range entry, and it is currently covered by trace entries.
        if ((entry.getLength() != length || entry.getAddress() != address) && (!isRangeAddress || !this.isAddressCovered(address, length, Entry.Type.RANGE))) {
            this.addEntry(new Entry(address, length, bytecode, isRangeAddress ? Entry.Type.RANGE : Entry.Type.TRACE));
            return;
        }

        // If the structure didn't change, or if we were trying to overwrite trace entries with range entries,
        // check if the current entry isn't a trace entry and that we were trying to add a range entry, in which case just update its bytecode
        if (isRangeAddress && entry.getType() != Entry.Type.TRACE) {
            entry.setBytecode(bytecode);
            return;
        }

        // Otherwise, refresh the current entry and mark it as a trace
        entry.setAddress(address);
        entry.setLength(length);
        entry.setBytecode(bytecode);
        entry.setType(Entry.Type.TRACE);
    }

    private void addEntry(Entry entry) {
        this.writeLock.lock();
        try {
            int address = entry.getAddress();
            int length = entry.getLength();
            this.removeOverlappingEntries(address, length);
            this.entries.put(address, entry);
            this.addOrdinalAddress(address);
        } finally {
            this.writeLock.unlock();
        }
    }

    private void removeOverlappingEntries(int beginAddress, int length) {
        int endAddress = beginAddress + length;

        Map.Entry<Integer, Entry> lower = entries.floorEntry(beginAddress);
        if (lower != null) {
            Entry lowerEntry = lower.getValue();
            int lowerAddressEnd = lowerEntry.getAddress() + lowerEntry.getLength();
            if (lowerAddressEnd > beginAddress) {
                this.removeEntry(lowerEntry);
            }
        }

        Map.Entry<Integer, Entry> higher = entries.ceilingEntry(beginAddress);
        while (higher != null && higher.getKey() < endAddress) {
            this.removeEntry(higher.getValue());
            higher = this.entries.higherEntry(higher.getKey());
        }
    }

    private void removeEntry(Entry entry) {
        int start = entry.getAddress();
        int end = start + entry.getLength();

        this.entries.remove(start);
        this.addressOrdinalList.removeIf(e -> e >= start && e < end);
    }

    private void addOrdinalAddress(int address) {
        int idx = Collections.binarySearch(this.addressOrdinalList, address);
        if (idx < 0) {
            this.addressOrdinalList.add(-idx - 1, address);
        }
    }

    private void validateEntry(Entry entry) {
        int address = entry.getAddress();
        int length = entry.getLength();
        int bytecode = entry.getByteCode();

        int currentLength = this.getLengthForInstructionAt(address);
        int currentBytecode = this.getBytecodeForInstructionAt(address);

        if (length != currentLength) {
            this.disassemble(address);
        } else if (bytecode != currentBytecode) {
            entry.setBytecode(currentBytecode);
        }
        if (entry.getText() == null) {
            entry.setText(this.getTextForInstructionAt(address));
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAddressCovered(int address, int length, @Nullable Entry.Type allowOverWriteUpTo) {
        for (int i = address; i < address + length; i++) {
            Map.Entry<Integer, Entry> entry = this.entries.floorEntry(i);
            if (entry == null) {
                continue;
            }
            Entry overlappingEntry = entry.getValue();
            int overlappingEnd = entry.getKey() + overlappingEntry.getLength();
            if (overlappingEnd <= i) {
                continue;
            }
            if (allowOverWriteUpTo != null && allowOverWriteUpTo.canOverwrite(overlappingEntry.getType())) {
                continue;
            }
            return true;
        }
        return false;
    }

    protected static class Entry implements Disassembler.Entry {

        private volatile Type type;
        private volatile int instructionAddress;
        private volatile int length;
        private volatile int bytecode;
        private volatile String text;

        private Entry(int address, int length, int bytecode, @NotNull Type type) {
            this.instructionAddress = address;
            this.length = length;
            this.bytecode = bytecode;
            this.type = type;
        }

        private void setAddress(int address) {
            if (this.instructionAddress != address) {
                this.text = null;
                this.instructionAddress = address;
            }
        }

        @Override
        public int getAddress() {
            return instructionAddress;
        }

        private void setLength(int length) {
            if (this.length != length) {
                this.text = null;
                this.length = length;
            }
        }

        @Override
        public int getLength() {
            return length;
        }

        private void setBytecode(int bytecode) {
            if (this.bytecode != bytecode) {
                this.text = null;
                this.bytecode = bytecode;
            }
        }

        @Override
        public int getByteCode() {
            return bytecode;
        }

        private void setText(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        private void setType(@NotNull Type type) {
            this.type = type;
        }

        @NotNull
        private Type getType() {
            return this.type;
        }

        private enum Type {
            STATIC(0),
            RANGE(1),
            TRACE(2);

            private final int strength;

            Type(int strength) {
                this.strength = strength;
            }

            private boolean canOverwrite(Type other) {
                return this.strength >= other.strength;
            }
        }

    }

}
