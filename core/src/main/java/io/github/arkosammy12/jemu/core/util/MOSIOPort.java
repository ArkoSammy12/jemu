package io.github.arkosammy12.jemu.core.util;

public final class MOSIOPort {

    private final PortOwner portOwner;
    private final InputSource inputSource;

    public MOSIOPort(PortOwner portOwner, InputSource inputSource) {
        this.portOwner = portOwner;
        this.inputSource = inputSource;
    }

    public int read() {
        int ddr = this.portOwner.getDataDirectionRegister();
        return (this.portOwner.getOutputLatch() & ddr) | (this.inputSource.getInputBits() & ~ddr);
    }

    public interface PortOwner {

        int getDataDirectionRegister();

        int getOutputLatch();

    }

    public interface InputSource {

        int getInputBits();

    }

    public static class DefaultPortOwner implements PortOwner {

        protected int dataDirectionRegister;
        protected int outputLatch;

        @Override
        public int getDataDirectionRegister() {
            return this.dataDirectionRegister;
        }

        public void setDataDirectionRegister(int value) {
            this.dataDirectionRegister = value & 0xFF;
        }

        @Override
        public int getOutputLatch() {
            return this.outputLatch;
        }

        public void setOutputLatch(int value) {
            this.outputLatch = value & 0xFF;
        }

    }

}
