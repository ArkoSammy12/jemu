package io.github.arkosammy12.jemu.core.commodore64.tape;

import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class T64Image implements TapeImage {

    private static final double PAL_SHORT_HALF_PERIOD_S = 182.7 * 0.000001;
    private static final double PAL_MEDIUM_HALF_PERIOD_S = 265.7 * 0.000001;
    private static final double PAL_LONG_HALF_PERIOD_S = 348.8 * 0.000001;

    private static final double PAL_SHORT_FULL_PERIOD_S = PAL_SHORT_HALF_PERIOD_S * 2;
    private static final double PAL_MEDIUM_FULL_PERIOD_S = PAL_MEDIUM_HALF_PERIOD_S * 2;
    private static final double PAL_LONG_FULL_PERIOD_S = PAL_LONG_HALF_PERIOD_S * 2;

    private static final int PAL_SHORT_PULSE_CYCLES = (int) (PAL_SHORT_FULL_PERIOD_S * Commodore64Emulator.PAL_CPU_FREQUENCY_HZ);
    private static final int PAL_MEDIUM_PULSE_CYCLES = (int) (PAL_MEDIUM_FULL_PERIOD_S * Commodore64Emulator.PAL_CPU_FREQUENCY_HZ);
    private static final int PAL_LONG_PULSE_CYCLES = (int) (PAL_LONG_FULL_PERIOD_S * Commodore64Emulator.PAL_CPU_FREQUENCY_HZ);

    private static final int PAL_SHORT_PULSE_10S_AMOUNT = (int) (10.0 / PAL_SHORT_FULL_PERIOD_S);
    private static final int PAL_SHORT_PULSE_2S_AMOUNT = (int) (2.0 / PAL_SHORT_FULL_PERIOD_S);

    private final int[] pulseLengths;

    public T64Image(Commodore64Emulator emulator, byte[] bytes) {
        if (!(readByte(bytes, 0x00) == 0x43 && readByte(bytes, 0x01) == 0x36 && readByte(bytes, 0x02) == 0x34)) {
            throw new ROMInitializationException("The inserted T64 image does not contain the string \"C64\" at the beginning of the file!");
        }

        // T64 format version (v1.0: $0100, v1.1: $0101, v2.0: $0200)
        // TODO: Maybe check the tape version for something?
        int t64FormatVersion = (readByte(bytes, 0x20) << 8) | readByte(bytes, 0x21);
        int maximumDirectoryEntries = (readByte(bytes, 0x23) << 8) | readByte(bytes, 0x22);
        int usedDirectoryEntries = (readByte(bytes, 0x25) << 8) | readByte(bytes, 0x24);

        if (usedDirectoryEntries > maximumDirectoryEntries) {
            throw new ROMInitializationException("The inserted T64 image has %d used directory entries, which is greater than the maximum directory entries value of %d!".formatted(usedDirectoryEntries, maximumDirectoryEntries));
        }

        boolean loadToBASICStart = emulator.getHost().loadT64ToBASICStart();
        int foundUsedEntries = 0;
        List<Integer> pulseLengths = new ArrayList<>();
        for (int i = 0; i < maximumDirectoryEntries; i++) {

            int directoryEntryOffset = 64 + (i * 32);

            // 0 = free (usually)
            // 1 = Normal tape file
            // 3 = Memory Snapshot, v .9, uncompressed
            // 2-255 = Reserved (for memory snapshots)
            int entryType = readByte(bytes, directoryEntryOffset);
            if (entryType == 0) {
                continue;
            } else if (entryType != 1) {
                throw new ROMInitializationException("T64 contains a non 1 file entry of %d, which is not supported!".formatted(entryType));
            }
            foundUsedEntries++;

            // CBM file type (0x82 for PRG, 0x81 for SEQ, etc). 0 indicates the special C64S "FRZ" session snapshot.
            // >>> In reality any value that is not a $00 is  seen  as  a
            //     PRG file. When this value is a $00 (and the previous  byte  at
            //     $40 is >1), then the file is a special T64 "FRZ" (frozen) C64s
            //     session snapshot.
            // From http://unusedino.de/ec64/technical/formats/t64.html
            int cbmFileType = readByte(bytes, directoryEntryOffset + 1);
            if (cbmFileType == 0x00) {
                throw new ROMInitializationException("Found unsupported T64 \"FRZ\" C64s session snapshot directory entry at offset %d!".formatted(directoryEntryOffset));
            }

            int startAddressLow = readByte(bytes, directoryEntryOffset + 2);
            int startAddressHigh = readByte(bytes, directoryEntryOffset + 3);
            int endAddressLow = readByte(bytes, directoryEntryOffset + 4);
            int endAddressHigh = readByte(bytes, directoryEntryOffset + 5);
            long containerFileRawOffset = ((long) readByte(bytes, directoryEntryOffset + 0xB) << 24) | ((long) readByte(bytes, directoryEntryOffset + 0xA) << 16) | ((long) readByte(bytes, directoryEntryOffset + 0x9) << 8) | readByte(bytes, directoryEntryOffset + 0x8);

            if (containerFileRawOffset > Integer.MAX_VALUE || containerFileRawOffset > bytes.length) {
                throw new ROMInitializationException("T64 entry offset out of range: " + containerFileRawOffset);
            }

            int containerFileOffset = (int) containerFileRawOffset;

            byte[] headerBlock = new byte[192];

            // Just doing what VICE says it does in https://vice-emu.sourceforge.io/vice_17.html#SEC406 regarding the CBM header type :v
            headerBlock[0] = (byte) (loadToBASICStart && foundUsedEntries == 1 ? 0x01 : 0x03);
            headerBlock[1] = (byte) startAddressLow;
            headerBlock[2] = (byte) startAddressHigh;
            headerBlock[3] = (byte) endAddressLow;
            headerBlock[4] = (byte) endAddressHigh;
            for (int j = 0; j < 16; j++) {
                headerBlock[j + 5] = (byte) readByte(bytes, directoryEntryOffset + 0x10 + j);
            }
            for (int j = 21; j < 192; j++) {
                headerBlock[j] = (byte) 0x20;
            }

            this.addDataBlock(pulseLengths, headerBlock, foundUsedEntries == 1, false);

            int startAddress = (startAddressHigh << 8) | startAddressLow;
            int endAddress = (endAddressHigh << 8) | endAddressLow;
            int size = endAddress - startAddress;
            int containerFileEndOffset = containerFileOffset + size;

            if (containerFileEndOffset > bytes.length) {
                throw new ROMInitializationException("Inserted TAP64 contains at offset %d is too big to fit in file!".formatted(directoryEntryOffset));
            }

            this.addDataBlock(pulseLengths, Arrays.copyOfRange(bytes, containerFileOffset, containerFileEndOffset), false, foundUsedEntries == usedDirectoryEntries);
        }

        if (foundUsedEntries != usedDirectoryEntries) {
            throw new ROMInitializationException("Inserted T64 image declares %d used directory entries, but found %d used entries instead!".formatted(usedDirectoryEntries, foundUsedEntries));
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

    private void addDataBlock(List<Integer> pulseLengths, byte[] payload, boolean isFirst, boolean isLast) {
        if (isFirst) {
            this.addLeader10Seconds(pulseLengths);
        } else {
            this.addLeader2Seconds(pulseLengths);
        }

        this.addFirstCountdownSequence(pulseLengths);
        int checksum = 0x00;
        for (byte b : payload) {
            int value = b & 0xFF;
            checksum ^= value;
            this.addByte(pulseLengths, value);
        }
        this.addByte(pulseLengths, checksum);
        this.addLongPulse(pulseLengths);
        this.add60PulsesSynchronization(pulseLengths);

        this.addSecondCountdownSequence(pulseLengths);
        for (byte b : payload) {
            this.addByte(pulseLengths, b & 0xFF);
        }
        this.addByte(pulseLengths, checksum);
        if (isLast) {
            this.addEndOfDataMarker(pulseLengths);
        }
        this.addLongPulse(pulseLengths);
        this.add60PulsesSynchronization(pulseLengths);
    }

    private void addLeader10Seconds(List<Integer> pulseLengths) {
        for (int i = 0; i < PAL_SHORT_PULSE_10S_AMOUNT; i++) {
            this.addShortPulse(pulseLengths);
        }
    }

    private void addLeader2Seconds(List<Integer> pulseLengths) {
        for (int i = 0; i < PAL_SHORT_PULSE_2S_AMOUNT; i++) {
            this.addShortPulse(pulseLengths);
        }
    }

    private void add60PulsesSynchronization(List<Integer> pulseLengths) {
        for (int i = 0; i < 60; i++) {
            this.addShortPulse(pulseLengths);
        }
    }

    private void addFirstCountdownSequence(List<Integer> pulseLengths) {
        for (int i = 0x89; i >= 0x81; i--) {
            this.addByte(pulseLengths, i);
        }
    }

    private void addSecondCountdownSequence(List<Integer> pulseLengths) {
        for (int i = 0x09; i >= 0x01; i--) {
            this.addByte(pulseLengths, i);
        }
    }

    private void addByte(List<Integer> pulseLengths, int value) {
        value &= 0xFF;
        this.addByteMarker(pulseLengths);
        boolean parity = true;
        for (int i = 1; i <= 0x80; i <<= 1) {
            if ((value & i) != 0) {
                parity = !parity;
                this.addBit1(pulseLengths);
            } else {
                this.addBit0(pulseLengths);
            }
        }
        if (parity) {
            this.addBit1(pulseLengths);
        } else {
            this.addBit0(pulseLengths);
        }
    }

    private void addBit0(List<Integer> pulseLengths) {
        this.addShortPulse(pulseLengths);
        this.addMediumPulse(pulseLengths);
    }

    private void addBit1(List<Integer> pulseLengths) {
        this.addMediumPulse(pulseLengths);
        this.addShortPulse(pulseLengths);
    }

    private void addByteMarker(List<Integer> pulseLengths) {
        this.addLongPulse(pulseLengths);
        this.addMediumPulse(pulseLengths);
    }

    private void addEndOfDataMarker(List<Integer> pulseLengths) {
        this.addLongPulse(pulseLengths);
        this.addShortPulse(pulseLengths);
    }

    private void addShortPulse(List<Integer> pulseLengths) {
        pulseLengths.add(PAL_SHORT_PULSE_CYCLES);
    }

    private void addMediumPulse(List<Integer> pulseLengths) {
        pulseLengths.add(PAL_MEDIUM_PULSE_CYCLES);
    }

    private void addLongPulse(List<Integer> pulseLengths) {
        pulseLengths.add(PAL_LONG_PULSE_CYCLES);
    }

    private static int readByte(byte[] bytes, int index) {
        return (int) bytes[index] & 0xFF;
    }

}
