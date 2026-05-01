package io.github.arkosammy12.jemu.core.util;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;

import java.util.function.IntConsumer;

public final class ActionSignal {

    private final IntConsumer action;
    private final Long2IntMap pendingValues = new Long2IntOpenHashMap();
    private final LongPriorityQueue timers = new LongHeapPriorityQueue();
    private long ticks;

    public ActionSignal(IntConsumer action) {
        this.action = action;
    }

    public void trigger(int delay, int value) {
        long fireAt = this.ticks + delay;
        this.timers.enqueue(fireAt);
        this.pendingValues.put(fireAt, value);
    }

    public void tick() {
        this.ticks++;
        while (!this.timers.isEmpty() && this.timers.firstLong() <= this.ticks) {
            long fireAt = this.timers.dequeueLong();
            this.action.accept(this.pendingValues.remove(fireAt));
        }
    }

}
