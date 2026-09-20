package io.github.arkosammy12.jemu.core.commodore64.crt;

import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.ArrayList;
import java.util.List;

public class CRTFile {

    private final int cartridgeHardwareType;
    private final boolean exromLineStatus;
    private final boolean gameLineStatus;
    private final List<CHIPPacket> chipPackets ;

    public CRTFile(byte[] file) {
        if (file.length < 0x14) {
            throw new ROMInitializationException("CRT file is too short to contain a valid header!");
        }
        int fileHeaderLength = (Byte.toUnsignedInt(file[0x0010]) << 24) | (Byte.toUnsignedInt(file[0x0011]) << 16) | (Byte.toUnsignedInt(file[0x0012]) << 8) | Byte.toUnsignedInt(file[0x0013]);
        if (fileHeaderLength < 0x40) {
            throw new ROMInitializationException("CRT file header length is %d, but must be at least %d bytes!".formatted(fileHeaderLength, 0x40));
        }
        if (fileHeaderLength >= file.length) {
            throw new ROMInitializationException("CRT file header is cutoff!");
        }

        this.cartridgeHardwareType = (Byte.toUnsignedInt(file[0x0016]) << 8) | Byte.toUnsignedInt(file[0x0017]);
        this.exromLineStatus = (file[0x0018] & 1) == 0;
        this.gameLineStatus = (file[0x0019] & 1) == 0;

        ArrayList<CHIPPacket> chipPackets = new ArrayList<>();

        try {
            int chipPacketStartOffset = fileHeaderLength;
            while (chipPacketStartOffset < file.length) {
                if (chipPacketStartOffset + 0xF >= file.length) {
                    throw new ROMInitializationException("CHIP packet at offset %d has a cutoff header!".formatted(chipPacketStartOffset));
                }

                int totalPacketLength = (Byte.toUnsignedInt(file[chipPacketStartOffset + 0x0004]) << 24) | (Byte.toUnsignedInt(file[chipPacketStartOffset + 0x0005]) << 16) | (Byte.toUnsignedInt(file[chipPacketStartOffset + 0x0006]) << 8) | Byte.toUnsignedInt(file[chipPacketStartOffset + 0x0007]);
                CHIPType chipType = CHIPType.getCHIPTypeForIntValue((Byte.toUnsignedInt(file[chipPacketStartOffset + 0x0008]) << 8) | Byte.toUnsignedInt(file[chipPacketStartOffset + 0x0009]));
                int bankNumber = (Byte.toUnsignedInt(file[chipPacketStartOffset + 0x000A]) << 8) | Byte.toUnsignedInt(file[chipPacketStartOffset + 0x000B]);
                int startingLoadAddress = (Byte.toUnsignedInt(file[chipPacketStartOffset + 0x000C]) << 8) | Byte.toUnsignedInt(file[chipPacketStartOffset + 0x000D]);
                int romImageSizeBytes = (Byte.toUnsignedInt(file[chipPacketStartOffset + 0x000E]) << 8) | Byte.toUnsignedInt(file[chipPacketStartOffset + 0x000F]);

                if (romImageSizeBytes + 0x10 != totalPacketLength) {
                    throw new ROMInitializationException("CHIP packet at offset %d has a ROM image size header of %d, which is not equal to the total packet length header + 16, which is %d!".formatted(chipPacketStartOffset, romImageSizeBytes, totalPacketLength));
                }

                if (chipPacketStartOffset + totalPacketLength > file.length) {
                    throw new ROMInitializationException("CHIP packet at offset %d has cutoff ROM data!".formatted(chipPacketStartOffset));
                }

                byte[] romData = new byte[romImageSizeBytes];
                System.arraycopy(file, chipPacketStartOffset + 0x0010, romData, 0, romImageSizeBytes);
                chipPackets.add(new CHIPPacket(chipType, bankNumber, startingLoadAddress, romData));
                chipPacketStartOffset += totalPacketLength;
            }
        } catch (Exception e) {
            throw new ROMInitializationException("Failed to load .CRT file!", e);
        }

        if (chipPackets.isEmpty()) {
            throw new ROMInitializationException("CRT file has no valid CHIP packets!");
        }
        this.chipPackets = List.copyOf(chipPackets);
    }

    public int getCartridgeHardwareType() {
        return this.cartridgeHardwareType;
    }

    public boolean getEXROMLineStatus() {
        return this.exromLineStatus;
    }

    public boolean getGAMELineStatus() {
        return this.gameLineStatus;
    }

    public List<CHIPPacket> getChipPackets() {
        return this.chipPackets;
    }

}
