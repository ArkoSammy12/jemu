package io.github.arkosammy12.jemu.app.config.settings;

import io.github.arkosammy12.jemu.app.audio.SampleRate;
import io.github.arkosammy12.jemu.app.audio.SoundDevice;

import java.util.Optional;

public interface AudioSettings {

    int getVolume();

    boolean getMute();

    SampleRate getSampleRate();

    int getLatencyMs();

    Optional<SoundDevice> getSoundDevice();

}
