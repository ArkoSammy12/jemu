package io.github.arkosammy12.jemu.core.commodore64.crt;

public enum CHIPType {
    ROM(0),
    RAM(1),
    FLASH_ROM(2),
    EEPROM(3);

    private final int intValue;

    CHIPType(int intValue) {
        this.intValue = intValue;
    }

    public static CHIPType getCHIPTypeForIntValue(int intValue) {
        for (CHIPType chipType : CHIPType.values()) {
            if (chipType.intValue == intValue) {
                return chipType;
            }
        }
        throw new IllegalArgumentException("Unknown CHIP packet chip type value %d!".formatted(intValue));
    }

}
