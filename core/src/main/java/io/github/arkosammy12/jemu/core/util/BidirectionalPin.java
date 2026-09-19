package io.github.arkosammy12.jemu.core.util;

public class BidirectionalPin {

    private final PortOwner portOwner;
    private final InputSource inputSource;

    public BidirectionalPin(PortOwner portOwner, InputSource inputSource) {
        this.portOwner = portOwner;
        this.inputSource = inputSource;
    }

    public boolean read(){
        return this.portOwner.getDirection() ? this.portOwner.getOutput() : this.inputSource.getInput();
    }

    public interface PortOwner {

        boolean getDirection();

        boolean getOutput();

    }

    public interface InputSource {

        boolean getInput();

    }

    public interface SystemBus {

        boolean getBit();

    }

    public static class DefaultPortOwner implements PortOwner {

        private boolean direction;
        private boolean output;

        public void setDirection(boolean direction) {
            this.direction = direction;
        }

        @Override
        public boolean getDirection() {
            return this.direction;
        }

        public void setOutput(boolean output) {
            this.output = output;
        }

        @Override
        public boolean getOutput() {
            return this.output;
        }

    }

}
