package io.github.arkosammy12.jemu.app.system.nes;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.CartridgeInfo;
import org.jetbrains.annotations.Nullable;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;
import java.util.Objects;

@JsonRootName(value = "nes20db")
class NES20Database {

    @JacksonXmlProperty(localName = "date", isAttribute = true)
    private String date;

    @JacksonXmlProperty(localName = "game")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Game> games;

    List<Game> getGames() {
        return this.games;
    }

    static class Game implements CartridgeInfo {

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

        Rom getRom() {
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

        static class Rom extends Memory {

            @JacksonXmlProperty(localName = "crc32", isAttribute = true)
            private String crc32; // In uppercase

            @JacksonXmlProperty(localName = "sha1", isAttribute = true)
            private String sha1; // In uppercase

            String getSha1() {
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