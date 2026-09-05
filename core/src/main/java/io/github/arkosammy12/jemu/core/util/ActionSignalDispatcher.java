package io.github.arkosammy12.jemu.core.util;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.function.IntConsumer;

public class ActionSignalDispatcher {

    private IntConsumer[] actions = new IntConsumer[0];
    private int[] delays = new int[0];

    private IntArrayFIFOQueue[][] buffers = new IntArrayFIFOQueue[0][];
    private int[] positions = new int[0];

    public int addSignal(int maximumDelay, IntConsumer action) {
        int index = this.buffers.length;
        IntConsumer[] newActions = new IntConsumer[this.actions.length + 1];
        IntArrayFIFOQueue[][] newBuffers = new IntArrayFIFOQueue[this.buffers.length + 1][];
        int[] newDelays = new int[this.delays.length + 1];
        int[] newPositions = new int[this.positions.length + 1];

        for (int i = 0; i < this.buffers.length; i++) {
            newActions[i] = this.actions[i];
            newBuffers[i] = this.buffers[i];
            newDelays[i] = this.delays[i];
            newPositions[i] = this.positions[i];
        }

        newActions[index] = action;
        IntArrayFIFOQueue[] newBuffer = new IntArrayFIFOQueue[maximumDelay + 1];
        for (int i = 0; i < newBuffer.length; i++) {
            newBuffer[i] = new IntArrayFIFOQueue();
        }
        newBuffers[index] = newBuffer;
        newDelays[index] = maximumDelay;
        newPositions[index] = 0;

        this.actions = newActions;
        this.buffers = newBuffers;
        this.delays = newDelays;
        this.positions = newPositions;

        return index;
    }

    public void trigger(int id, int value) {
        this.trigger(id, value, this.delays[id]);
    }

    public void trigger(int id, int value, int delay) {
        int index = (this.positions[id] + delay) % this.buffers[id].length;
        this.buffers[id][index].enqueue(value);
    }

    public void cancel(int id) {
        for (IntArrayFIFOQueue queue : this.buffers[id]) {
            queue.clear();
        }
    }

    public void tick() {
        for (int i = 0; i < this.buffers.length; i++) {
            this.positions[i] = (this.positions[i] + 1) % this.buffers[i].length;
            IntArrayFIFOQueue queue = this.buffers[i][this.positions[i]];
            IntConsumer action = this.actions[i];
            while (!queue.isEmpty()) {
                action.accept(queue.dequeueInt());
            }
        }
    }

    public void reset() {
        for (int i = 0; i < this.buffers.length; i++) {
            for (IntArrayFIFOQueue queue : this.buffers[i]) {
                queue.clear();
            }
            this.positions[i] = 0;
        }
    }

}
