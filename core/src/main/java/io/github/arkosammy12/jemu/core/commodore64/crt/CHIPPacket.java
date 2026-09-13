package io.github.arkosammy12.jemu.core.commodore64.crt;

public record CHIPPacket(CHIPType chipType, int bankNumber, int startingLoadAddress, byte[] romData) {}