package io.github.arkosammy12.jemu.core.util;

import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;

public final class ActionSignal {

    private final Runnable action;
    private final IntPriorityQueue timers = new IntHeapPriorityQueue();
    private long ticks;

    public ActionSignal(Runnable action) {
        this.action = action;
    }

    public void trigger(int delay) {
        this.timers.enqueue(Math.toIntExact(this.ticks + delay));
    }

    public void tick() {
        this.ticks++;
        while (!this.timers.isEmpty() && this.timers.firstInt() <= this.ticks) {
            this.action.run();
            this.timers.dequeueInt();
        }
    }

}
