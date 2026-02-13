package io.github.arkosammy12.jemu.systems.cosmacvip;

public interface IODevice {

    default void cycle() { }

    default DmaStatus getDmaStatus() {
        return DmaStatus.NONE;
    }

    default boolean isInterrupting() {
        return false;
    }

    default void doDmaOut(int dmaOutAddress, int value) { }

    default int doDmaIn(int dmaInAddress) {
        // Data bus lines are pulled up on the VIP
        return 0xFF;
    }

    default boolean isOutputPort(int port) {
        return false;
    }

    default void onOutput(int port, int value) { }

    default boolean isInputPort(int port) {
        return false;
    }

    default int onInput(int port) {
        // Data bus lines are pulled up on the VIP
        return 0xFF;
    }

    enum DmaStatus {
        NONE,
        IN,
        OUT

    }

}
