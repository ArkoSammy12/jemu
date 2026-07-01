package io.github.arkosammy12.jemu.frontend.config.settings;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.audio.SoundDevice;

import java.util.Optional;

public interface AudioSettings {

    int getVolume();

    boolean getMute();

    SampleRate getSampleRate();

    Optional<SoundDevice> getSoundDevice();

}
