package io.github.arkosammy12.jemu.frontend.swing.events;

public sealed interface Event permits PauseEvent, ResetEvent, StepCycleEvent, StepFrameEvent, StopEvent {
}
