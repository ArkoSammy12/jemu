package io.github.arkosammy12.jemu.core.common;

import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;

import java.nio.file.Path;
import java.util.Optional;

public interface SystemHost {

    Optional<byte[]> getRom();

    Optional<Path> getRomPath();

    String getSystemName();

    Optional<String> getRomTitle();

    Optional<? extends VideoDriver> getVideoDriver();

    Optional<? extends AudioDriver> getAudioDriver();

    static int[] byteToIntArray(byte[] byteArray) {
        int[] arr = new int[byteArray.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (int) byteArray[i] & 0xFF;
        }
        return arr;
    }

    static byte[] intToByteArray(int[] intArray) {
        byte[] arr = new byte[intArray.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) intArray[i];
        }
        return arr;
    }

    static byte[][] intToByteArray(int[][] intArray) {
        byte[][] outerArray = new byte[intArray.length][];
        for (int i = 0; i < outerArray.length; i++) {
            byte[] innerArray = new byte[intArray[i].length];
            for (int j = 0; j < innerArray.length; j++) {
                innerArray[j] = (byte) intArray[i][j];
            }
            outerArray[i] = innerArray;
        }
        return outerArray;
    }

}
