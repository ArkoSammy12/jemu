package io.github.arkosammy12.jemu.core.atari.atari2600;

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

    private boolean joystick0Button;

    private boolean physicalJoystick1Up;
    private boolean physicalJoystick1Down;
    private boolean physicalJoystick1Left;
    private boolean physicalJoystick1Right;

    private boolean currentJoystick1Up;
    private boolean currentJoystick1Down;
    private boolean currentJoystick1Left;
    private boolean currentJoystick1Right;

    private boolean joystick1Button;

    public Atari2600Controller(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public void onActionPressed(Action action) {
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
            case JOYSTICK0_BUTTON -> {
                this.joystick0Button = true;
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
            case JOYSTICK1_BUTTON -> {
                this.joystick1Button = true;
                this.emulator.getTIA().setI5(false);
            }
        }
    }

    @Override
    public void onActionReleased(Action action) {
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
            case JOYSTICK0_BUTTON -> {
                this.joystick0Button = false;
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
            case JOYSTICK1_BUTTON -> {
                this.joystick1Button = false;
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
            case JOYSTICK0_BUTTON -> this.joystick0Button;

            case JOYSTICK1_UP -> this.currentJoystick1Up;
            case JOYSTICK1_DOWN -> this.currentJoystick1Down;
            case JOYSTICK1_LEFT -> this.currentJoystick1Left;
            case JOYSTICK1_RIGHT -> this.currentJoystick1Right;
            case JOYSTICK1_BUTTON -> this.joystick1Button;
        };
    }

    public enum Actions implements Action {
        GAME_SELECT("Game Select"),
        GAME_RESET("Game Reset"),

        JOYSTICK0_UP("Joystick 0 Up"),
        JOYSTICK0_DOWN("Joystick 0 Down"),
        JOYSTICK0_LEFT("Joystick 0 Left"),
        JOYSTICK0_RIGHT("Joystick 0 Right"),
        JOYSTICK0_BUTTON("Joystick 0 Button"),

        JOYSTICK1_UP("Joystick 1 Up"),
        JOYSTICK1_DOWN("Joystick 1 Down"),
        JOYSTICK1_LEFT("Joystick 1 Left"),
        JOYSTICK1_RIGHT("Joystick 1 Right"),
        JOYSTICK1_BUTTON("Joystick 1 Button"),

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
