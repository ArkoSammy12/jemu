package io.github.arkosammy12.jemu.core.util;

import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;

public final class ActionSignal {

    private final Runnable action;
    private final LongPriorityQueue timers = new LongHeapPriorityQueue();
    private long ticks;

    public ActionSignal(Runnable action) {
        this.action = action;
    }

    public void trigger(int delay) {
        this.timers.enqueue(this.ticks + delay);
    }

    public void tick() {
        this.ticks++;
        while (!this.timers.isEmpty() && this.timers.firstLong() <= this.ticks) {
            this.action.run();
            this.timers.dequeueLong();
        }
    }

}
