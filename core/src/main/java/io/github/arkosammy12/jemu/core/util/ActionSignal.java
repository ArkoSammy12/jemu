package io.github.arkosammy12.jemu.core.util;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import java.util.function.IntConsumer;

public final class ActionSignal {

    private final IntConsumer action;

    private final int delay;
    private final IntArrayFIFOQueue[] buffer;
    private int position;

    public ActionSignal(int delay, IntConsumer action) {
        this.action = action;
        this.delay = delay;
        this.buffer = new IntArrayFIFOQueue[delay + 1];
        for (int i = 0; i < this.buffer.length; i++) {
            this.buffer[i] = new IntArrayFIFOQueue();
        }
    }

    public void trigger(int value) {
        int index = (this.position + this.delay) % this.buffer.length;
        this.buffer[index].enqueue(value);
    }

    public void tick() {
        this.position = (this.position + 1) % this.buffer.length;
        IntArrayFIFOQueue queue = this.buffer[this.position];
        while (!queue.isEmpty()) {
            this.action.accept(queue.dequeueInt());
        }
    }

    public void reset() {
        for (IntArrayFIFOQueue queue : this.buffer) {
            queue.clear();
        }
        this.position = 0;
    }

}
