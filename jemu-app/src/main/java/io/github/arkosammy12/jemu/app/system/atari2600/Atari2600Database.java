package io.github.arkosammy12.jemu.app.system.atari2600;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600SystemHost;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class Atari2600Database {

    @SerializedName("schemaVersion")
    private int schemaVersion;

    @SerializedName("generated")
    private String generated;

    @SerializedName("roms")
    private List<Atari2600Database.Entry> roms;

    List<Atari2600Database.Entry> getRoms() {
        return this.roms;
    }

    static class Entry implements Atari2600SystemHost.CartridgeInfo {

        @SerializedName("title")
        private String title;

        @SerializedName("publisher")
        private String publisher;

        @SerializedName("year")
        private int year;

        @SerializedName("cartType")
        private String cartType;

        @SerializedName("tvFormat")
        private String tvFormat;

        @SerializedName("verification")
        private String verification;

        @SerializedName("verificationNote")
        private String verificationNote;

        @SerializedName("size")
        private int size;

        @SerializedName("layout")
        private String layout;

        @SerializedName("offset")
        private int offset;

        @SerializedName("length")
        private int length;

        @SerializedName("crc32")
        private String crc32;

        @SerializedName("md5")
        private String md5;

        @SerializedName("sha1")
        private String sha1;

        @SerializedName("sha256")
        private String sha256;

        @SerializedName("notes")
        private String notes;

        public String getSha1() {
            return this.sha1;
        }

        @Override
        public Optional<Atari2600Cartridge.Type> getCartridgeType() {
            return Arrays.stream(Atari2600Cartridge.Type.values()).filter(cartridgeType -> cartridgeType.getName().equals(this.cartType)).findFirst();
        }

        @Override
        public Optional<Atari2600Emulator.TVFormat> getTVFormat() {
            return Arrays.stream(Atari2600Emulator.TVFormat.values()).filter(cartridgeType -> cartridgeType.getName().equals(this.tvFormat)).findFirst();
        }

    }

}
