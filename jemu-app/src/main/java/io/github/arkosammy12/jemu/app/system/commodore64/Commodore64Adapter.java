package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.drivers.GlueVideoDriver;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Controller;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Host;
import io.github.arkosammy12.jemu.core.commodore64.tape.Commodore1531;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.frontend.events.CoreSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;
import io.github.arkosammy12.jemu.frontend.util.InputListener;
import org.apache.commons.io.FilenameUtils;

import javax.sound.sampled.LineUnavailableException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class Commodore64Adapter extends SystemAdapter implements Commodore64Host {

    private String romTitle;
    private final Commodore64Manager commodore64Manager;

    public Commodore64Adapter(Jemu jemu, Commodore64Manager systemManager) throws LineUnavailableException {
        super(jemu, systemManager);
        this.commodore64Manager = systemManager;
    }

    @Override
    protected Emulator createEmulator() {
        return new Commodore64Emulator(this);
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<Path> getKernalROMPath() {
        return this.commodore64Manager.getEmulationSettings().getKernalRomPath();
    }

    @Override
    public Optional<Path> getBASICRomPath() {
        return this.commodore64Manager.getEmulationSettings().getBasicRomPath();
    }

    @Override
    public Optional<Path> getCharacterROMPath() {
        return this.commodore64Manager.getEmulationSettings().getCharacterRomPath();
    }

    @Override
    public Optional<Path> getTapeImagePath() {
        return this.commodore64Manager.getEmulationSettings().getTapeImagePath();
    }

    @Override
    public int getRGB8ForPaletteIndex(int paletteIndex) {
        return this.commodore64Manager.getEmulationSettings().getVICIIPalette().getRGB8ForPaletteIndex(paletteIndex);
    }

    @Override
    public boolean swapJoysticks() {
        return this.commodore64Manager.getEmulationSettings().getSwapJoysticks();
    }

    @Override
    public boolean connectDatasette() {
        return this.commodore64Manager.getEmulationSettings().getConnectDatasette();
    }

    @Override
    public boolean loadT64ToBASICStart() {
        return this.commodore64Manager.getEmulationSettings().getLoadT64toBASICStart();
    }

    @Override
    protected GlueVideoDriver createVideoDriver(Emulator emulator) {
        return new Commodore64VideoDriver(this.jemu, emulator.getVideoGenerator());
    }

    @Override
    protected InputListener createInputListener() {
        return new InputListener() {

            private final Map<SystemController.Action, Integer> pressedActions = new HashMap<>();
            private final Map<Integer, Boolean> keyActionEdgeTracker = new HashMap<>();
            private int blockShiftKeyCode = -1;

            {
                for (Commodore64Controller.KeyboardMatrix keyboardMatrixAction : Commodore64Controller.KeyboardMatrix.values()) {
                    pressedActions.put(keyboardMatrixAction, 0);
                }
                for (Commodore64Controller.KeyboardSpecialKey specialKeyAction : Commodore64Controller.KeyboardSpecialKey.values()) {
                    pressedActions.put(specialKeyAction, 0);
                }
                for (Commodore64Controller.Peripheral peripheralAction : Commodore64Controller.Peripheral.values()) {
                    pressedActions.put(peripheralAction, 0);
                }
                for (Commodore64Controller.Datasette datasetteAction : Commodore64Controller.Datasette.values()) {
                    pressedActions.put(datasetteAction, 0);
                }
            }

            @Override
            public void onKeyActionPressed(KeyAction keyAction) {
                getEmulator().map(Emulator::getSystemController).ifPresent(systemController -> commodore64Manager.getMappingsForKey(keyAction).ifPresent(actions -> {
                    boolean mapsToShift = false;
                    boolean isNewPress = !this.keyActionEdgeTracker.computeIfAbsent(keyAction.keyCode(), _ -> false);
                    for (SystemController.Action action : actions.getValue()) {
                        if (isNewPress) {
                            Integer pressedAmount = this.pressedActions.get(action);
                            if (pressedAmount != null) {
                                pressedAmount++;
                                this.pressedActions.put(action, pressedAmount);
                                if (pressedAmount == 1) {
                                    systemController.pressAction(action);
                                }
                            }
                        }
                        switch (action) {
                            case Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT, Commodore64Controller.KeyboardMatrix.KEY_RIGHT_SHIFT -> mapsToShift = true;
                            default -> {}
                        }
                    }
                    if (keyAction.shiftKey() == KeyAction.ModifierKey.PRESSED && actions.getKey().shiftKey() == KeyAction.ModifierKey.PRESSED && !mapsToShift) {
                        this.blockShiftKeyCode = keyAction.keyCode();
                    }
                    this.updateShiftKeys(systemController);
                    this.keyActionEdgeTracker.put(keyAction.keyCode(), true);
                }));
            }

            @Override
            public void onKeyActionReleased(KeyAction keyAction) {
                getEmulator().map(Emulator::getSystemController).ifPresent(systemController -> {
                    this.keyActionEdgeTracker.put(keyAction.keyCode(), false);
                    Consumer<List<SystemController.Action>> actionsConsumer = actions -> actions.forEach(action -> {
                        Integer pressedAmount = this.pressedActions.get(action);
                        if (pressedAmount != null && pressedAmount > 0) {
                            pressedAmount--;
                            this.pressedActions.put(action, pressedAmount);
                            if (pressedAmount <= 0) {
                                systemController.releaseAction(action);
                            }
                        }
                    });
                    commodore64Manager.getActionsForKey(keyAction.withShiftKey(KeyAction.ModifierKey.UNPRESSED)).ifPresent(actionsConsumer);
                    commodore64Manager.getActionsForKey(keyAction.withShiftKey(KeyAction.ModifierKey.PRESSED)).ifPresent(actionsConsumer);
                    if (keyAction.keyCode() == this.blockShiftKeyCode) {
                        this.blockShiftKeyCode = -1;
                    }
                    this.updateShiftKeys(systemController);
                });
            }

            @Override
            public boolean onFilesDropped(EventPublisher eventPublisher, Collection<Path> paths) {
                Optional<Path> firstPath = paths.stream().findFirst();
                if (firstPath.isEmpty()) {
                    return false;
                } else {
                    Path path = firstPath.get();
                    return switch (FilenameUtils.getExtension(path.toString()).toLowerCase()) {
                        case Commodore1531.T64_FILE_EXTENSION, Commodore1531.TAP_FILE_EXTENSION -> {
                            eventPublisher.publishEvent(new Commodore64Manager.TapeImagePathChangedEvent(path));
                            yield true;
                        }
                        default -> false;
                    };
                }
            }

            private void updateShiftKeys(SystemController systemController) {
                if (this.blockShiftKeyCode > 0) {
                    systemController.releaseAction(Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
                    systemController.releaseAction(Commodore64Controller.KeyboardMatrix.KEY_RIGHT_SHIFT);
                } else {
                    Integer leftShiftCounter = this.pressedActions.get(Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
                    Integer rightShiftCounter = this.pressedActions.get(Commodore64Controller.KeyboardMatrix.KEY_RIGHT_SHIFT);
                    if (leftShiftCounter != null && leftShiftCounter > 0) {
                        systemController.pressAction(Commodore64Controller.KeyboardMatrix.KEY_LEFT_SHIFT);
                    }
                    if (rightShiftCounter != null && rightShiftCounter > 0) {
                        systemController.pressAction(Commodore64Controller.KeyboardMatrix.KEY_RIGHT_SHIFT);
                    }
                }
            }

        };
    }

    @Override
    public void onFrame() {
        super.onFrame();
        if (this.emulator instanceof Commodore64Emulator commodore64Emulator && this.videoDriver instanceof Commodore64VideoDriver commodore64VideoDriver) {
            commodore64VideoDriver.setTapeCounter(commodore64Emulator.getDatasette().getTapeCounter());
            commodore64VideoDriver.setMotorActive(commodore64Emulator.getMOTOR());
        }
    }

    @Override
    public void onCoreSettingChangedEvent(CoreSettingChangedEvent coreSettingChangedEvent) throws LineUnavailableException {
        super.onCoreSettingChangedEvent(coreSettingChangedEvent);
        if (this.emulator instanceof Commodore64Emulator commodore64Emulator) {
            switch (coreSettingChangedEvent) {
                case Commodore64Manager.TapeImagePathChangedEvent(Path path) -> commodore64Emulator.updateTapeImage(path);
                case Commodore64Manager.DatasetteButtonPressedEvent(Commodore64Controller.Datasette datasetteButton) -> commodore64Emulator.getSystemController().releaseAction(datasetteButton);
                default -> {}
            }
        }
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
