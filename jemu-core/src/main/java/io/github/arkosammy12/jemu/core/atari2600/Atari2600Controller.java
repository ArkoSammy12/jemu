package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class Atari2600Controller<E extends Atari2600Emulator> implements SystemController {

    private final E emulator;

    private boolean gameSelectButton;
    private boolean gameResetButton;

    private boolean physicalJoystick0Up;
    private boolean physicalJoystick0Down;
    private boolean physicalJoystick0Left;
    private boolean physicalJoystick0Right;

    private boolean currentJoystick0Up;
    private boolean currentJoystick0Down;
    private boolean currentJoystick0Left;
    private boolean currentJoystick0Right;

    private boolean joystick0FireButton;

    private boolean physicalJoystick1Up;
    private boolean physicalJoystick1Down;
    private boolean physicalJoystick1Left;
    private boolean physicalJoystick1Right;

    private boolean currentJoystick1Up;
    private boolean currentJoystick1Down;
    private boolean currentJoystick1Left;
    private boolean currentJoystick1Right;

    private boolean joystick1FireButton;

    public Atari2600Controller(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public void pressAction(Action action) {
        if (!(action instanceof Atari2600Controller.Actions atari2600Action)) {
            return;
        }
        switch (atari2600Action) {
            case GAME_SELECT -> this.gameSelectButton = true;
            case GAME_RESET -> this.gameResetButton = true;
            case JOYSTICK0_UP -> {
                this.physicalJoystick0Up = true;
                if (!this.physicalJoystick0Down) {
                    this.currentJoystick0Up = true;
                }
            }
            case JOYSTICK0_DOWN -> {
                this.physicalJoystick0Down = true;
                if (!this.physicalJoystick0Up) {
                    this.currentJoystick0Down = true;
                }
            }
            case JOYSTICK0_LEFT -> {
                this.physicalJoystick0Left = true;
                if (!this.physicalJoystick0Right) {
                    this.currentJoystick0Left = true;
                }
            }
            case JOYSTICK0_RIGHT -> {
                this.physicalJoystick0Right = true;
                if (!this.physicalJoystick0Left) {
                    this.currentJoystick0Right = true;
                }
            }
            case JOYSTICK0_FIRE -> {
                this.joystick0FireButton = true;
                this.emulator.getTIA().setI4(false);
            }

            case JOYSTICK1_UP -> {
                this.physicalJoystick1Up = true;
                if (!this.physicalJoystick1Down) {
                    this.currentJoystick1Up = true;
                }
            }
            case JOYSTICK1_DOWN -> {
                this.physicalJoystick1Down = true;
                if (!this.physicalJoystick1Up) {
                    this.currentJoystick1Down = true;
                }
            }
            case JOYSTICK1_LEFT -> {
                this.physicalJoystick1Left = true;
                if (!this.physicalJoystick1Right) {
                    this.currentJoystick1Left = true;
                }
            }
            case JOYSTICK1_RIGHT -> {
                this.physicalJoystick1Right = true;
                if (!this.physicalJoystick1Left) {
                    this.currentJoystick1Right = true;
                }
            }
            case JOYSTICK1_FIRE -> {
                this.joystick1FireButton = true;
                this.emulator.getTIA().setI5(false);
            }
        }
    }

    @Override
    public void releaseAction(Action action) {
        if (!(action instanceof Atari2600Controller.Actions atari2600Action)) {
            return;
        }
        switch (atari2600Action) {
            case GAME_SELECT -> this.gameSelectButton = false;
            case GAME_RESET -> this.gameResetButton = false;
            case JOYSTICK0_UP -> {
                this.physicalJoystick0Up = false;
                this.currentJoystick0Up = false;
                if (this.physicalJoystick0Down) {
                    this.currentJoystick0Down = true;
                }
            }
            case JOYSTICK0_DOWN -> {
                this.physicalJoystick0Down = false;
                this.currentJoystick0Down = false;
                if (this.physicalJoystick0Up) {
                    this.currentJoystick0Up = true;
                }
            }
            case JOYSTICK0_LEFT -> {
                this.physicalJoystick0Left = false;
                this.currentJoystick0Left = false;
                if (this.physicalJoystick0Right) {
                    this.currentJoystick0Right = true;
                }
            }
            case JOYSTICK0_RIGHT -> {
                this.physicalJoystick0Right = false;
                this.currentJoystick0Right = false;
                if (this.physicalJoystick0Left) {
                    this.currentJoystick0Left = true;
                }
            }
            case JOYSTICK0_FIRE -> {
                this.joystick0FireButton = false;
                this.emulator.getTIA().setI4(true);
            }

            case JOYSTICK1_UP -> {
                this.physicalJoystick1Up = false;
                this.currentJoystick1Up = false;
                if (this.physicalJoystick1Down) {
                    this.currentJoystick1Down = true;
                }
            }
            case JOYSTICK1_DOWN -> {
                this.physicalJoystick1Down = false;
                this.currentJoystick1Down = false;
                if (this.physicalJoystick1Up) {
                    this.currentJoystick1Up = true;
                }
            }
            case JOYSTICK1_LEFT -> {
                this.physicalJoystick1Left = false;
                this.currentJoystick1Left = false;
                if (this.physicalJoystick1Right) {
                    this.currentJoystick1Right = true;
                }
            }
            case JOYSTICK1_RIGHT -> {
                this.physicalJoystick1Right = false;
                this.currentJoystick1Right = false;
                if (this.physicalJoystick1Left) {
                    this.currentJoystick1Left = true;
                }
            }
            case JOYSTICK1_FIRE -> {
                this.joystick1FireButton = false;
                this.emulator.getTIA().setI5(true);
            }
        }
    }

    public boolean isActionPressed(Atari2600Controller.Actions action) {
        return switch (action) {
            case GAME_SELECT -> this.gameSelectButton;
            case GAME_RESET -> this.gameResetButton;

            case JOYSTICK0_UP -> this.currentJoystick0Up;
            case JOYSTICK0_DOWN -> this.currentJoystick0Down;
            case JOYSTICK0_LEFT -> this.currentJoystick0Left;
            case JOYSTICK0_RIGHT -> this.currentJoystick0Right;
            case JOYSTICK0_FIRE -> this.joystick0FireButton;

            case JOYSTICK1_UP -> this.currentJoystick1Up;
            case JOYSTICK1_DOWN -> this.currentJoystick1Down;
            case JOYSTICK1_LEFT -> this.currentJoystick1Left;
            case JOYSTICK1_RIGHT -> this.currentJoystick1Right;
            case JOYSTICK1_FIRE -> this.joystick1FireButton;
        };
    }

    public enum Actions implements Action {
        GAME_SELECT("Game Select"),
        GAME_RESET("Game Reset"),

        JOYSTICK0_UP("Left Joystick Up"),
        JOYSTICK0_DOWN("Left Joystick Down"),
        JOYSTICK0_LEFT("Left Joystick Left"),
        JOYSTICK0_RIGHT("Left Joystick Right"),
        JOYSTICK0_FIRE("Left Joystick Fire"),

        JOYSTICK1_UP("Right Joystick 1 Up"),
        JOYSTICK1_DOWN("Right Joystick Down"),
        JOYSTICK1_LEFT("Right Joystick Left"),
        JOYSTICK1_RIGHT("Right Joystick Right"),
        JOYSTICK1_FIRE("Right Joystick Fire"),

        ;

        private final String label;

        Actions(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return this.label;
        }
    }

}
