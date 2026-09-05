package io.github.arkosammy12.jemu.core.util;

public class BidirectionalPin {

    private final SystemBus systemBus;

    private boolean direction;
    private boolean outputLatch;

    public BidirectionalPin(SystemBus systemBus){
        this.systemBus = systemBus;
    }

    public boolean read(){
        return this.direction ? this.outputLatch : this.systemBus.getBit();
    }

    public void setDirection(boolean value) {
        this.direction = value;
    }

    public boolean getDirection() {
        return this.direction;
    }

    public void write(boolean value) {
        this.outputLatch = value;
    }

    public void clock() {
        if (this.direction) {
            this.systemBus.clockOutput();
        } else {
            this.systemBus.clockInput();
        }
    }

    public interface SystemBus {

        boolean getBit();

        default void clockOutput() {

        }

        default void clockInput() {

        }

    }

}
