package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalMuteEvent;

public sealed interface MuteEvent extends AudioSettingChangeEvent permits InternalMuteEvent {

    boolean getMute();

}
