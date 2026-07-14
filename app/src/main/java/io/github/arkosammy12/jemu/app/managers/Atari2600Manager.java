package io.github.arkosammy12.jemu.app.managers;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.Atari2600Adapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600SystemHost;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import java.io.InputStream;
import java.util.*;

public class Atari2600Manager implements SystemManager {

    private final Map<String, Atari2600Database.Entry> databaseMap;

    public Atari2600Manager() {
        Map<String, Atari2600Database.Entry> map = new HashMap<>();
        dbInit: try {
            byte[] bytes = loadFromResources(this.getClass(), "/system/atari2600/vcs_cart_db/db.json");
            if (bytes == null) {
                Logger.error("Atari 2600 database file not found!");
                break dbInit;
            }
            String json = new String(bytes);
            Atari2600Database db = new Gson().fromJson(json, Atari2600Database.class);
            for (Atari2600Database.Entry entry : db.getRoms()) {
                map.put(entry.getSha1(), entry);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to load Atari 2600 database!");
        }
        this.databaseMap = Map.copyOf(map);
    }

    @Override
    public SystemAdapter createSystem(Jemu jemu, System system) throws LineUnavailableException {
        return new Atari2600Adapter(jemu, system, this);
    }

    @Override
    public String getName() {
        return "Atari 2600";
    }

    @Override
    public String getId() {
        return "atari-2600";
    }

    @Override
    public Optional<String[]> getFileExtensions() {
        return Optional.of(new String[] {"a26"});
    }

    public Optional<Atari2600Database.Entry> getDatabaseEntryForRom(byte[] rom) {
        try {
            return Optional.ofNullable(this.databaseMap.get(SystemManager.getSha1Hash(rom)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static byte @Nullable [] loadFromResources(Class<?> clazz, String path) throws Exception {
        try (InputStream in = clazz.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            } else {
                return in.readAllBytes();
            }
        }
    }

    public static class Atari2600Database {

        @SerializedName("schemaVersion")
        private int schemaVersion;

        @SerializedName("generated")
        private String generated;

        @SerializedName("roms")
        private List<Entry> roms;

        private List<Entry> getRoms() {
            return this.roms;
        }

        public static class Entry implements Atari2600SystemHost.CartridgeInfo {
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
            public Optional<Atari2600Cartridge.CartridgeType> getCartridgeType() {
                return Arrays.stream(Atari2600Cartridge.CartridgeType.values()).filter(cartridgeType -> cartridgeType.getName().equals(this.cartType)).findFirst();
            }

            @Override
            public Optional<Atari2600Emulator.TVFormat> getTVFormat() {
                return Arrays.stream(Atari2600Emulator.TVFormat.values()).filter(cartridgeType -> cartridgeType.getName().equals(this.tvFormat)).findFirst();
            }

        }

    }

}
