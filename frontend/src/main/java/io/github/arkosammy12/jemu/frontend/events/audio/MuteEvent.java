package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalMuteEvent;

public sealed interface MuteEvent extends AudioSettingChangedEvent permits InternalMuteEvent {

    boolean getMute();

}
