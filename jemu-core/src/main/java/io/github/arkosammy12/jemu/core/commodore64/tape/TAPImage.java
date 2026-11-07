package io.github.arkosammy12.jemu.core.commodore64.tape;

import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.ArrayList;
import java.util.List;

public class TAPImage implements TapeImage {

    private final int[] pulseLengths;

    TAPImage(byte[] bytes) {
        if (bytes.length < 0x14) {
            throw new ROMInitializationException("Inserted .TAP image is not long enough to contain a valid header! Found length is %d bytes".formatted(bytes.length));
        }

        int tapVersion = (int) bytes[0xC] & 0xFF;
        if (tapVersion != 0 && tapVersion != 1) {
            throw new ROMInitializationException("TAP version %d is not supported! Supported values are 0 or 1".formatted(tapVersion));
        }

        int computerPlatform = (int) bytes[0xD] & 0xFF;
        if (computerPlatform != 0) {
            throw new ROMInitializationException("TAP computer platform value %d is not supported! Supported value is 0 (C64)".formatted(computerPlatform));
        }

        int videoStandard = (int) bytes[0xE] & 0xFF;
        if (videoStandard != 0) {
            throw new ROMInitializationException("TAP video standard value %d is not supported! Supported value is 0 (PAL)".formatted(videoStandard));
        }

        int dataSize = (((int) bytes[0x13] & 0xFF) << 24) | (((int) bytes[0x12] & 0xFF) << 16) | (((int) bytes[0x11] & 0xFF) << 8) | ((int) bytes[0x10] & 0xFF);

        if (0x14 + dataSize > bytes.length) {
            throw new ROMInitializationException("Inserted .TAP file size of %d does not fit the provided file data size of %d!".formatted(bytes.length, dataSize));
        }

        List<Integer> pulseLengths = new ArrayList<>();
        int filePointer = 0x14;
        while (filePointer < bytes.length) {
            int dataByte = (int) bytes[filePointer] & 0xFF;
            if (dataByte == 0) {
                switch (tapVersion) {
                    case 0 -> {
                        pulseLengths.add(255 * 8);
                        filePointer++;
                    } case 1 -> {
                        if (filePointer + 3 >= bytes.length) {
                            throw new ROMInitializationException("Inserted version .TAP contains truncated overflow byte ($00) value at EOF!");
                        }
                        pulseLengths.add((((int) bytes[filePointer + 3] & 0xFF) << 16) | ((((int) bytes[filePointer + 2] & 0xFF) << 8)) | ((int) bytes[filePointer + 1] & 0xFF));
                        filePointer += 4;
                    }
                }
            } else {
                pulseLengths.add(dataByte * 8);
                filePointer++;
            }
        }

        this.pulseLengths = new int[pulseLengths.size()];
        for (int i = 0; i < pulseLengths.size(); i++) {
            this.pulseLengths[i] = pulseLengths.get(i);
        }
    }

    @Override
    public int getPulseLength(int index) {
        if (index < this.pulseLengths.length) {
            return this.pulseLengths[index];
        } else {
            return -1;
        }
    }

}
