package io.github.arkosammy12.jemu.backend.common;

import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import io.github.arkosammy12.jemu.backend.drivers.VideoDriver;

import java.util.Optional;

public interface SystemHost {

    byte[] getRom();

    String getSystemName();

    Optional<String> getRomTitle();

    Optional<VideoDriver> getVideoDriver();

    Optional<AudioDriver> getAudioDriver();

    static int[] byteToIntArray(byte[] byteArray) {
        int[] arr = new int[byteArray.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = byteArray[i] & 0xFF;
        }
        return arr;
    }

}
