package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.audio.InternalMuteEvent;

public sealed interface MuteEvent extends AudioSettingChangedEvent permits InternalMuteEvent {

    boolean getMute();

}
