package io.github.arkosammy12.jemu.app.drivers;

import de.gurkenlabs.input4j.InputComponent;
import de.gurkenlabs.input4j.InputDevice;
import de.gurkenlabs.input4j.InputDevicePlugin;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.app.adapters.AbstractSystemAdapter;

import de.gurkenlabs.input4j.InputDevices;
import de.gurkenlabs.input4j.components.XInput;
import io.github.arkosammy12.jemu.core.common.SystemController;
import org.tinylog.Logger;

import java.io.IOException;

import static io.github.arkosammy12.jemu.app.Jemu.tryJoinSafely;

// input4j not working as expected; limited to 1 controller
// TODO: find better library or work around
public class JoypadDriver implements AutoCloseable {

    private static final float JOYSTICK_ENGAGED_THRESH = 0.5f;

    // Keep JOYSTICK_DISENGAGED_THRESH <= JOYSTICK_ENGAGED_THRESH.
    // If JOYSTICK_DISENGAGED_THRESH < JOYSTICK_ENGAGED_THRESH there is a hysteresis effect which seems more natural
    // when using a joystick in place of a DPAD
    private static final float JOYSTICK_DISENGAGED_THRESH = 0.45f;

    private boolean joyUp = false;
    private boolean joyDown = false;
    private boolean joyLeft = false;
    private boolean joyRight = false;

    private final AbstractSystemAdapter systemAdapter;

    private final Thread pollThread;
    private final Object joypadLock = new Object();
    private volatile boolean running = true;

    private InputDevicePlugin inputDevicePlugin;
    private InputDevice device;

    public JoypadDriver(AbstractSystemAdapter systemAdapter) {
        this.systemAdapter = systemAdapter;

        this.pollThread = new Thread(this::pollLoop, "%s-joypad-poll-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.pollThread.setDaemon(true);
        this.pollThread.start();
    }

    public void poll() {
        synchronized (this.joypadLock) {
            this.joypadLock.notify();
        }
    }

    private void pollLoop() {
        while (this.running) {
            try {
                synchronized (this.joypadLock) {
                    this.joypadLock.wait();
                    if (!this.running) {
                        break;
                    }

                    this.tryInitDevices();
                    this.tryPoll();
                }
            } catch (InterruptedException e) {}
        }

        this.closeJoypadDevicePlugin();
    }

    private void deRegisterDevice(InputDevice device) {
        Logger.info("Deregistering device: " + device);

        // DT 01/06/2026:
        // Only support 1 device until a better library/solution is found
        if (this.device != device) {
            return;
        }

        this.device.clearButtonPressedListeners(XInput.DPAD_UP);
        this.device.clearButtonPressedListeners(XInput.DPAD_DOWN);
        this.device.clearButtonPressedListeners(XInput.DPAD_LEFT);
        this.device.clearButtonPressedListeners(XInput.DPAD_RIGHT);
        this.device.clearButtonPressedListeners(XInput.START);
        this.device.clearButtonPressedListeners(XInput.BACK);
        this.device.clearButtonPressedListeners(XInput.X);
        this.device.clearButtonPressedListeners(XInput.A);

        this.device.clearButtonReleasedListeners(XInput.DPAD_UP);
        this.device.clearButtonReleasedListeners(XInput.DPAD_DOWN);
        this.device.clearButtonReleasedListeners(XInput.DPAD_LEFT);
        this.device.clearButtonReleasedListeners(XInput.DPAD_RIGHT);
        this.device.clearButtonReleasedListeners(XInput.START);
        this.device.clearButtonReleasedListeners(XInput.BACK);
        this.device.clearButtonReleasedListeners(XInput.X);
        this.device.clearButtonReleasedListeners(XInput.A);

        this.device.clearAxisChangedListeners(XInput.LEFT_THUMB_X);
        this.device.clearAxisChangedListeners(XInput.LEFT_THUMB_Y);

        this.device = null;
    }

    private void registerDevice(InputDevice device) {
        Logger.info("Registering device: " + device);

        // DT 01/06/2026:
        // Only support 1 device until a better library/solution is found
        if (this.device != null) {
            return;
        }

        this.device = device;

        this.device.onButtonPressed(XInput.DPAD_UP, () -> pressButton(XInput.DPAD_UP));
        this.device.onButtonPressed(XInput.DPAD_DOWN, () -> pressButton(XInput.DPAD_DOWN));
        this.device.onButtonPressed(XInput.DPAD_LEFT, () -> pressButton(XInput.DPAD_LEFT));
        this.device.onButtonPressed(XInput.DPAD_RIGHT, () -> pressButton(XInput.DPAD_RIGHT));
        this.device.onButtonPressed(XInput.START, () -> pressButton(XInput.START));
        this.device.onButtonPressed(XInput.BACK, () -> pressButton(XInput.BACK));
        this.device.onButtonPressed(XInput.X, () -> pressButton(XInput.X));
        this.device.onButtonPressed(XInput.A, () -> pressButton(XInput.A));

        this.device.onButtonReleased(XInput.DPAD_UP, () -> releaseButton(XInput.DPAD_UP));
        this.device.onButtonReleased(XInput.DPAD_DOWN, () -> releaseButton(XInput.DPAD_DOWN));
        this.device.onButtonReleased(XInput.DPAD_LEFT, () -> releaseButton(XInput.DPAD_LEFT));
        this.device.onButtonReleased(XInput.DPAD_RIGHT, () -> releaseButton(XInput.DPAD_RIGHT));
        this.device.onButtonReleased(XInput.START, () -> releaseButton(XInput.START));
        this.device.onButtonReleased(XInput.BACK, () -> releaseButton(XInput.BACK));
        this.device.onButtonReleased(XInput.X, () -> releaseButton(XInput.X));
        this.device.onButtonReleased(XInput.A, () -> releaseButton(XInput.A));

        this.device.onAxisChanged(XInput.LEFT_THUMB_X, this::xAxisChange);
        this.device.onAxisChanged(XInput.LEFT_THUMB_Y, this::yAxisChange);
    }

    private void pressButton(InputComponent.ID id) {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);
        if (action != null) {
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        }
    }

    private void releaseButton(InputComponent.ID id) {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);
        if (action != null) {
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }
    }

    private void xAxisChange(float val) {
        if (val >= JOYSTICK_ENGAGED_THRESH && !joyRight) {
            joyRight = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_RIGHT);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        } else if (val < JOYSTICK_DISENGAGED_THRESH && joyRight) {
            joyRight = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_RIGHT);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }

        if (val <= -JOYSTICK_ENGAGED_THRESH && !joyLeft) {
            joyLeft = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_LEFT);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        } else if (val > -JOYSTICK_DISENGAGED_THRESH && joyLeft) {
            joyLeft = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_LEFT);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }
    }

    private void yAxisChange(float val) {
        if (val >= JOYSTICK_ENGAGED_THRESH && !joyUp) {
            joyUp = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_UP);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        } else if (val < JOYSTICK_DISENGAGED_THRESH && joyUp) {
            joyUp = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_UP);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }

        if (val <= -JOYSTICK_ENGAGED_THRESH && !joyDown) {
            joyDown = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_DOWN);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        } else if (val > -JOYSTICK_DISENGAGED_THRESH && joyDown) {
            joyDown = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_DOWN);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }
    }

    private void tryPoll(){
        if (this.device != null) {
            this.device.poll();
        }
    }

    private void tryInitDevices() {
        if (this.device != null && this.inputDevicePlugin != null) {
            return;
        }

        if (this.inputDevicePlugin != null) {
            this.closeJoypadDevicePlugin();
        }

        this.inputDevicePlugin = InputDevices.init();

        if (inputDevicePlugin.getAll().isEmpty()) {
            this.closeJoypadDevicePlugin();
            return;
        }

        // DT 01/06/2026:
        // input4j with default plugin (windows) seems to never call onDeviceConnected() when a controller is connected.
        // The only reliable way to test for connection is to check all devices after a call to init().
        // Unfortunately this limits the number of controllers usable by this library to 1, as it is not feasible to
        // constantly call init() and re-initialize already connected controllers.
        //
        // Since onDeviceConnected() does nothing, we must call registerDevice() on the first found controller

        //this.inputDevicePlugin.onDeviceConnected(this::registerCallbacks);
        this.inputDevicePlugin.getAll().stream().findFirst().ifPresent(this::registerDevice);
        this.inputDevicePlugin.onDeviceDisconnected(this::deRegisterDevice);
    }

    private void closeJoypadDevicePlugin(){
        if (this.inputDevicePlugin != null) {
            try {
                this.inputDevicePlugin.close();
            } catch (IOException e) {
                Logger.error("Error releasing joypad driver resources: {}", e);
            }

            this.inputDevicePlugin = null;
        }
    }

    @Override
    public void close() {
        this.running = false;
        synchronized (this.joypadLock) {
            this.joypadLock.notifyAll();
        }
        tryJoinSafely(this.pollThread);
    }

}
