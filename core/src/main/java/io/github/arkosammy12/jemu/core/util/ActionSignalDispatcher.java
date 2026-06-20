package io.github.arkosammy12.jemu.core.util;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class ActionSignalDispatcher {

    private final List<Signal> signals = new ArrayList<>();
    private long ticks;

    public int addSignal(IntConsumer action) {
        int id = this.signals.size();
        this.signals.add(new Signal(action));
        return id;
    }

    public void trigger(int id, int delay, int value) {
        Signal signal = this.signals.get(id);
        long fireAt = this.ticks + (long) delay;
        signal.timers.enqueue(fireAt);
        signal.pendingValues.put(fireAt, value);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void tick() {
        this.ticks++;
        for (int i = 0; i < this.signals.size(); i++) {
            Signal signal = this.signals.get(i);
            while (!signal.timers.isEmpty() && signal.timers.firstLong() <= this.ticks) {
                signal.action.accept(signal.pendingValues.remove(signal.timers.dequeueLong()));
            }
        }
    }

    public void reset() {
        this.ticks = 0;
        this.signals.forEach(Signal::reset);
    }

    private static final class Signal {

        private final IntConsumer action;
        private final LongPriorityQueue timers = new LongHeapPriorityQueue();
        private final Long2IntMap pendingValues = new Long2IntOpenHashMap();

        private Signal(IntConsumer action) {
            this.action = action;
        }

        private void reset() {
            this.timers.clear();
            this.pendingValues.clear();
        }

    }

}
