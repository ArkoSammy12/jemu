package io.github.arkosammy12.jemu.core.nes.ines;

import io.github.arkosammy12.jemu.core.nes.NESEmulator;

public interface CartridgeInfo {

    int getProgramRomSize();

    int getCharacterRomSize();

    int getProgramRamSize();

    int getNonVolatileProgramRamSize();

    int getCharacterRamSize();

    int getNonVolatileCharacterRamSize();

    int getMapperNumber();

    int getSubmapperNumber();

    boolean getNametableArrangement();

    boolean hasAlternativeNametableLayout();

    boolean hasBattery();

    NESEmulator.TVSystem getTVSystem();

}
