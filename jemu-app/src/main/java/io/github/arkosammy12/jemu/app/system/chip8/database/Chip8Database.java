package io.github.arkosammy12.jemu.app.system.chip8.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.arkosammy12.jemu.app.system.chip8.BuiltInChip8Palette;
import io.github.arkosammy12.jemu.app.system.chip8.Chip8Variant;
import io.github.arkosammy12.jemu.core.chip8.Chip8Host;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static io.github.arkosammy12.jemu.app.system.SystemManager.getSha1Hash;
import static io.github.arkosammy12.jemu.app.system.SystemManager.loadFromResources;
import static io.github.arkosammy12.jemu.app.system.chip8.Chip8Variant.*;

public class Chip8Database {

    @Nullable
    private final Hashes hashes;

    @Nullable
    private final Platforms platforms;

    @Nullable
    private final Programs programs;

    public Chip8Database() {
        Hashes hashes = null;
        Platforms platforms = null;
        Programs programs = null;
        dbInit: try {
            byte[] hashesBytes = loadFromResources(this.getClass(), "/system/chip8/chip-8-database/database/sha1-hashes.json");
            if (hashesBytes == null) {
                Logger.error("CHIP-8 sha1-hashes.json database file not found!");
                break dbInit;
            }

            byte[] programListBytes = loadFromResources(this.getClass(), "/system/chip8/chip-8-database/database/programs.json");
            if (programListBytes == null) {
                Logger.error("CHIP-8 programs.json database file not found!");
                break dbInit;
            }

            byte[] platformListBytes = loadFromResources(this.getClass(), "/system/chip8/chip-8-database/database/platforms.json");
            if (platformListBytes == null) {
                Logger.error("CHIP-8 platforms.json database file not found!");
                break dbInit;
            }

            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Integer>>() {}.getType();
            hashes = new Hashes(gson.fromJson(new String(hashesBytes), mapType));

            Type programListType = new TypeToken<List<ProgramEntry>>() {}.getType();
            programs = new Programs(gson.fromJson(new String(programListBytes), programListType));

            Type platformListType = new TypeToken<List<PlatformEntry>>() {}.getType();
            platforms = new Platforms(gson.fromJson(new String(platformListBytes), platformListType));
        } catch (Exception e) {
            Logger.error(e, "Failed to CHIP-8 database!");
        }
        this.hashes = hashes;
        this.platforms = platforms;
        this.programs = programs;
    }

    public Optional<Entry> getEntryForRom(byte[] rom) {
        if (this.hashes == null || this.platforms == null || this.programs == null) {
            return Optional.empty();
        }

        ProgramEntry programEntry = null;
        RomEntry romEntry = null;
        PlatformEntry platformEntry = null;

        db: try {
            String sha1 = getSha1Hash(rom);
            Optional<Integer> indexOptional = this.hashes.getIndexForHash(sha1);
            if (indexOptional.isEmpty()) {
                break db;
            }
            int index = indexOptional.get();

            Optional<ProgramEntry> programEntryOptional = this.programs.getProgramEntryAt(index);
            if (programEntryOptional.isEmpty()) {
                break db;
            }
            programEntry = programEntryOptional.get();

            Optional<RomEntry> romEntryOptional = programEntry.getRomEntries().flatMap(romEntries -> Optional.ofNullable(romEntries.get(sha1)));
            if (romEntryOptional.isEmpty()) {
                break db;
            }
            romEntry = romEntryOptional.get();

            Optional<List<PlatformEntry>> platformsOptional = this.platforms.getPlatformEntries();
            if (platformsOptional.isEmpty()) {
                break db;
            }

            List<PlatformEntry> platformEntryList = platformsOptional.get();
            Function<String, @Nullable PlatformEntry> platformIdFunction = platformId -> {
                for (PlatformEntry platformElement : platformEntryList) {
                    Optional<String> idOptional = platformElement.getId();
                    if (idOptional.isEmpty()) {
                        continue;
                    }
                    String id = idOptional.get();
                    if (id.equals(platformId)) {
                        return platformElement;
                    }
                }
                return null;
            };

            Optional<Map.Entry<String, Quirks>> quirkyPlatformsId = romEntry.getQuirkyPlatforms()
                    .orElse(new HashMap<>())
                    .entrySet()
                    .stream()
                    .findFirst();
            if (quirkyPlatformsId.isPresent()) {
                platformEntry = platformIdFunction.apply(quirkyPlatformsId.get().getKey());
            } else {
                Optional<String> platformId = romEntry.getPlatforms().map(List::getFirst);
                if (platformId.isPresent()) {
                    platformEntry = platformIdFunction.apply(platformId.get());
                }
            }

        } catch (Exception _) {

        }

        if (platformEntry == null && romEntry == null && programEntry == null) {
            return Optional.empty();
        } else {
            return Optional.of(new Entry(programEntry, romEntry, platformEntry));
        }
    }

    public static class Entry {

        @Nullable
        private final String romName;

        @Nullable
        private final Integer ipf;

        @Nullable
        private final Chip8Host.ColorPalette colorPalette;

        @Nullable
        private final VideoGenerator.DisplayOrientation displayOrientation;

        @Nullable
        private final String platformId;

        @Nullable
        private final Boolean doVFReset;

        @Nullable
        private final Chip8Host.MemoryIncrementQuirk memoryIncrementQuirk;

        @Nullable
        private final Boolean doDisplayWait;

        @Nullable
        private final Boolean doClipping;

        @Nullable
        private final Boolean doShiftVXInPlace;

        @Nullable
        private final Boolean doJumpWithVX;

        private Entry(@Nullable ProgramEntry programEntry, @Nullable RomEntry romEntry, @Nullable PlatformEntry platformEntry) {
            this.romName = Optional.ofNullable(programEntry).flatMap(ProgramEntry::getTitle).orElse(null);
            this.ipf = Optional.ofNullable(romEntry).flatMap(RomEntry::getTickRate).orElse(null);
            this.colorPalette =  Optional.ofNullable(romEntry)
                    .flatMap(RomEntry::getColors)
                    .flatMap(RomEntry.Colors::getPixels)
                    .map(pixels -> {
                        if (pixels.isEmpty()) {
                            return null;
                        } else {
                            int[][] customPixelColors = new int[pixels.size()][3];
                            for (int i = 0; i < pixels.size(); i++) {
                                String hex = pixels.get(i);
                                customPixelColors[i][0] = Integer.parseInt(hex.substring(1, 3), 16);
                                customPixelColors[i][1] = Integer.parseInt(hex.substring(3, 5), 16);
                                customPixelColors[i][2] = Integer.parseInt(hex.substring(5, 7), 16);
                            }
                            return new CustomColorPalette(BuiltInChip8Palette.CADMIUM, customPixelColors);
                        }
                    }).orElse(null);
            this.displayOrientation = Optional.ofNullable(romEntry)
                    .flatMap(RomEntry::getScreenRotation)
                    .flatMap(Entry::getDisplayOrientationForIntValue)
                    .orElse(null);

            this.platformId = Optional.ofNullable(platformEntry)
                    .flatMap(PlatformEntry::getId)
                    .orElse(null);

            this.doVFReset = getQuirk(romEntry, platformEntry, Quirks::getLogic).orElse(null);
            this.memoryIncrementQuirk = getQuirk(romEntry, platformEntry, Quirks::getMemoryLeaveIUnchanged)
                    .map(leaveIUnchanged -> leaveIUnchanged
                            ? Chip8Host.MemoryIncrementQuirk.NO_INCREMENT
                            : getQuirk(romEntry, platformEntry, Quirks::getMemoryIncrementByX)
                            .map(incrementByX -> incrementByX
                                    ? Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X
                                    : Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1)
                            .orElse(null))
                    .orElse(null);
            this.doDisplayWait = getQuirk(romEntry, platformEntry, Quirks::getVBlank).orElse(null);
            this.doClipping = getQuirk(romEntry, platformEntry, Quirks::getWrap).map(value -> !value).orElse(null);
            this.doShiftVXInPlace = getQuirk(romEntry, platformEntry, Quirks::getShift).orElse(null);
            this.doJumpWithVX = getQuirk(romEntry, platformEntry, Quirks::getJump).orElse(null);
        }

        public Optional<String> getRomName() {
            return Optional.ofNullable(this.romName);
        }

        public Optional<Integer> getIpf() {
            return Optional.ofNullable(this.ipf);
        }

        public Optional<Chip8Host.ColorPalette> getColorPalette() {
            return Optional.ofNullable(this.colorPalette);
        }

        public Optional<VideoGenerator.DisplayOrientation> getDisplayOrientation() {
            return Optional.ofNullable(this.displayOrientation);
        }

        public Optional<String> getPlatformId() {
            return Optional.ofNullable(this.platformId);
        }

        public Optional<Boolean> doVFReset() {
            return Optional.ofNullable(this.doVFReset);
        }

        public Optional<Chip8Host.MemoryIncrementQuirk> getMemoryIncrementQuirk() {
            return Optional.ofNullable(this.memoryIncrementQuirk);
        }

        public Optional<Boolean> doDisplayWait() {
            return Optional.ofNullable(this.doDisplayWait);
        }

        public Optional<Boolean> doClipping() {
            return Optional.ofNullable(this.doClipping);
        }

        public Optional<Boolean> doShiftVXInPlace() {
            return Optional.ofNullable(this.doShiftVXInPlace);
        }

        public Optional<Boolean> doJumpWithVX() {
            return Optional.ofNullable(this.doJumpWithVX);
        }

        private <T> Optional<T> getQuirk(@Nullable RomEntry romEntry, @Nullable PlatformEntry platformEntry, Function<Quirks, Optional<T>> getter) {
            return Optional.ofNullable(romEntry)
                    .flatMap(RomEntry::getQuirkyPlatforms)
                    .flatMap(quirkyPlatforms ->
                            Optional.ofNullable(platformEntry)
                                    .flatMap(PlatformEntry::getId)
                                    .flatMap(platformId -> Optional.ofNullable(quirkyPlatforms.get(platformId)))
                    )
                    .flatMap(getter)
                    .or(() -> Optional.ofNullable(platformEntry)
                            .flatMap(PlatformEntry::getQuirks)
                            .flatMap(getter)
                    );
        }

        private static Optional<VideoGenerator.DisplayOrientation> getDisplayOrientationForIntValue(int value) {
            return Optional.ofNullable(switch (value) {
                case 0 -> VideoGenerator.DisplayOrientation.DEG_0;
                case 90 -> VideoGenerator.DisplayOrientation.DEG_90;
                case 180 -> VideoGenerator.DisplayOrientation.DEG_180;
                case 270 -> VideoGenerator.DisplayOrientation.DEG_270;
                default -> null;
            });
        }

    }

}
