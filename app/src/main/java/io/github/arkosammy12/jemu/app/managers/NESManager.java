package io.github.arkosammy12.jemu.app.managers;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.NESAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.CartridgeInfo;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.sound.sampled.LineUnavailableException;
import java.nio.file.Path;
import java.util.*;

public class NESManager implements SystemManager {

    private final Map<String, NES20Database.Game> databaseMap;

    public NESManager() {
        Map<String, NES20Database.Game> map = new HashMap<>();
        try {
            XmlMapper xmlMapper = new XmlMapper();
            NES20Database nes20Database = xmlMapper.readValue(Path.of("app", "src", "main", "resources", "system", "nes", "nes20db", "nes20db.xml"), NES20Database.class);
            for (NES20Database.Game game : nes20Database.getGames()) {
                map.put(game.getRom().getSha1().toLowerCase(), game);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to load NES 2.0 database!");
        }
        this.databaseMap = Map.copyOf(map);
    }

    @Override
    public SystemAdapter createSystem(Jemu jemu, System system) throws LineUnavailableException {
        return new NESAdapter(jemu, system, this);
    }

    @Override
    public String getName() {
        return "Nintendo Entertainment System";
    }

    @Override
    public String getId() {
        return "nes";
    }

    @Override
    public Optional<String[]> getFileExtensions() {
        return Optional.of(new String[] {"nes"});
    }

    public Optional<CartridgeInfo> findDatabaseEntryFromNesFile(byte[] nesFile, int totalRomSize, boolean hasByteTrainer) {
        try {

            if (totalRomSize < 0) {
                throw new IllegalArgumentException("Total rom size cannot be negative!");
            }

            byte[] trustedSizeHeadersHashData = new byte[totalRomSize + (hasByteTrainer ? 512 : 0)];
            if (trustedSizeHeadersHashData.length <= nesFile.length - 16) {
                java.lang.System.arraycopy(nesFile, 16, trustedSizeHeadersHashData, 0, trustedSizeHeadersHashData.length);
                String trustedSizeHeadersHash = SystemManager.getSha1Hash(trustedSizeHeadersHashData);
                NES20Database.Game trustedSizeHeaderEntry = this.databaseMap.get(trustedSizeHeadersHash);
                if (trustedSizeHeaderEntry != null) {
                    return Optional.of(trustedSizeHeaderEntry);
                }
            }

            if (nesFile.length >= 16) {
                byte[] allDataMinusHeader = new byte[nesFile.length - 16];
                java.lang.System.arraycopy(nesFile, 16, allDataMinusHeader, 0, allDataMinusHeader.length);
                String allDataMinusHeaderHash = SystemManager.getSha1Hash(allDataMinusHeader);
                return Optional.ofNullable(this.databaseMap.get(allDataMinusHeaderHash));
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @JsonRootName(value = "nes20db")
    private static class NES20Database {

        @JacksonXmlProperty(localName = "date", isAttribute = true)
        private String date;

        @JacksonXmlProperty(localName = "game")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Game> games;

        private List<Game> getGames() {
            return this.games;
        }

        private static class Game implements CartridgeInfo {

            @JacksonXmlProperty(localName = "prgrom")
            private CartridgeRomSection prgRom;

            @Nullable
            @JacksonXmlProperty(localName = "chrrom")
            private CartridgeRomSection chrRom;

            @JacksonXmlProperty(localName = "rom")
            private Rom rom;

            @Nullable
            @JacksonXmlProperty(localName = "prgram")
            private Memory prgRam;

            @Nullable
            @JacksonXmlProperty(localName = "prgnvram")
            private Memory prgNvRam;

            @Nullable
            @JacksonXmlProperty(localName = "chrram")
            private Memory chrRam;

            @Nullable
            @JacksonXmlProperty(localName = "chrnvram")
            private Memory chrNvRam;

            @Nullable
            @JacksonXmlProperty(localName = "miscrom")
            private MiscRom miscRom;

            @JacksonXmlProperty(localName = "pcb")
            private Pcb pcb;

            @JacksonXmlProperty(localName = "console")
            private Console console;

            @JacksonXmlProperty(localName = "expansion")
            private Expansion expansion;

            private Rom getRom() {
                return this.rom;
            }

            @Override
            public int getProgramRomSize() {
                return this.prgRom.getSize();
            }

            @Override
            public int getCharacterRomSize() {
                return this.chrRom == null ? 0 : this.chrRom.getSize();
            }

            @Override
            public int getProgramRamSize() {
                return this.prgRam == null ? 0 : this.prgRam.getSize();
            }

            @Override
            public int getNonVolatileProgramRamSize() {
                return this.prgNvRam == null ? 0 : this.prgNvRam.getSize();
            }

            @Override
            public int getCharacterRamSize() {
                return this.chrRam == null ? 0 : this.chrRam.getSize();
            }

            @Override
            public int getNonVolatileCharacterRamSize() {
                return this.chrNvRam == null ? 0 : this.chrNvRam.getSize();
            }

            @Override
            public int getMapperNumber() {
                return this.pcb.getMapper();
            }

            @Override
            public int getSubmapperNumber() {
                return this.pcb.getSubmapper();
            }

            @Override
            public boolean getNametableArrangement() {
                return switch (this.getMapperNumber()) {
                    case 30 -> switch (this.pcb.getMirroring()) {
                        case "V", "4" -> true;
                        default -> false;
                    };
                    case 218 -> switch (this.pcb.getMirroring()) {
                        case "V", "1" -> true;
                        default -> false;
                    };
                    default -> Objects.equals(this.pcb.getMirroring(), "V");
                };
            }

            @Override
            public boolean hasAlternativeNametableLayout() {
                return switch (this.getMapperNumber()) {
                    case 30 -> switch (this.pcb.getMirroring()) {
                        case "1", "4" -> true;
                        default -> false;
                    };
                    case 218 -> switch (this.pcb.getMirroring()) {
                        case "0", "1" -> true;
                        default -> false;
                    };
                    default -> Objects.equals(this.pcb.getMirroring(), "4");
                };
            }

            @Override
            public boolean hasBattery() {
                return this.pcb.getBattery();
            }

            @Override
            public NESEmulator.TVSystem getTVSystem() {
                return switch (this.console.getRegion()) {
                    case 1 -> NESEmulator.TVSystem.PAL;
                    case 2 -> NESEmulator.TVSystem.MULTIPLE_REGION;
                    case 3 -> NESEmulator.TVSystem.DENDY;
                    default -> NESEmulator.TVSystem.NTSC;
                };
            }

            private static class Memory {

                @JacksonXmlProperty(localName = "size", isAttribute = true)
                private int size;

                protected int getSize() {
                    return this.size;
                }

            }

            private static class Rom extends Memory {

                @JacksonXmlProperty(localName = "crc32", isAttribute = true)
                private String crc32; // In uppercase

                @JacksonXmlProperty(localName = "sha1", isAttribute = true)
                private String sha1; // In uppercase

                private String getSha1() {
                    return this.sha1;
                }

            }

            private static class CartridgeRomSection extends Rom {

                @JacksonXmlProperty(localName = "sum16", isAttribute = true)
                private String sum16;

            }

            private static class MiscRom extends Rom {

                @JacksonXmlProperty(localName = "number", isAttribute = true)
                private int number;

            }

            private static class Pcb {

                @JacksonXmlProperty(localName = "mapper", isAttribute = true)
                private int mapper;

                @JacksonXmlProperty(localName = "submapper", isAttribute = true)
                private int submapper;

                @JacksonXmlProperty(localName = "mirroring", isAttribute = true)
                private String mirroring;

                @JacksonXmlProperty(localName = "battery", isAttribute = true)
                private int battery;

                protected int getMapper() {
                    return this.mapper;
                }

                protected int getSubmapper() {
                    return this.submapper;
                }

                protected String getMirroring() {
                    return this.mirroring;
                }

                private boolean getBattery() {
                    return this.battery != 0;
                }

            }

            private static class Console {

                @JacksonXmlProperty(localName = "type", isAttribute = true)
                private int type;

                @JacksonXmlProperty(localName = "region", isAttribute = true)
                private int region;

                protected int getRegion() {
                    return this.region;
                }

            }

            private static class Expansion {

                @JacksonXmlProperty(localName = "type", isAttribute = true)
                private int type;

            }

        }

    }

}
