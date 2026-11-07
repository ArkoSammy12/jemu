package io.github.arkosammy12.jemu.core.util;

import java.util.Arrays;

public final class ShiftRegister {

    private final int[] elements;
    private final int elementMask;
    private int head;

    private int currentSize;

    public ShiftRegister(int size, int elementSizeInBits) {
        if (elementSizeInBits < 1) {
            throw new IllegalArgumentException("Shift register element size in bits cannot be 0 or negative!");
        } else if (elementSizeInBits > 32) {
            throw new IllegalArgumentException("Shift register element size in bits cannot be greater than 32!");
        }
        this.elements = new int[size];
        this.elementMask = (int) ((1L << elementSizeInBits) - 1);
        this.currentSize = this.elements.length;
    }

    public int shiftHead(int shiftInTail) {
        int headValue = this.elements[this.head];
        this.elements[this.head] = shiftInTail & this.elementMask;
        this.head = (this.head + 1) % this.elements.length;
        this.decrementSize();
        return headValue;
    }

    public int shiftTail(int shiftInHead) {
        int tail = this.get(this.elements.length - 1);
        this.head = (this.head + this.elements.length - 1) % this.elements.length;
        this.elements[this.head] = shiftInHead & this.elementMask;
        this.decrementSize();
        return tail;
    }

    public void set(int index, int value) {
        this.elements[(this.head + index) % this.elements.length] = value & this.elementMask;
    }

    public int get(int index) {
        return this.elements[(this.head + index) % this.elements.length];
    }

    public boolean isEmpty() {
        return this.currentSize == 0;
    }

    public void clear() {
        Arrays.fill(this.elements, 0);
        this.head = 0;
        this.currentSize = 0;
    }

    public void setFull() {
        this.currentSize = this.elements.length;
    }

    private void decrementSize() {
        if (this.currentSize > 0) {
            this.currentSize--;
        }
    }

}
