package io.github.arkosammy12.jemu.config;

import io.github.arkosammy12.jemu.config.initializers.ApplicationInitializer;
import io.github.arkosammy12.jemu.util.DisplayAngle;
import io.github.arkosammy12.jemu.util.KeyboardLayout;
import io.github.arkosammy12.jemu.util.System;
import io.github.wasabithumb.jtoml.JToml;
import io.github.wasabithumb.jtoml.key.TomlKey;
import io.github.wasabithumb.jtoml.value.TomlValue;
import io.github.wasabithumb.jtoml.value.array.TomlArray;
import io.github.wasabithumb.jtoml.value.primitive.TomlPrimitive;
import io.github.wasabithumb.jtoml.value.table.TomlTable;
import net.harawata.appdirs.AppDirsFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class DataManager implements ApplicationInitializer {

    public static final String RECENT_FILES = "file.recent_files";
    public static final String CURRENT_DIRECTORY = "file.current_directory";

    public static final String SYSTEM = "emulator.system";
    public static final String DISPLAY_ANGLE = "emulator.display_angle";

    public static final String VOLUME = "settings.volume";
    public static final String MUTED = "settings.muted";
    public static final String KEYBOARD_LAYOUT = "settings.keyboard_layout";
    public static final String SHOW_INFO_BAR = "settings.show_info_bar";

    public static final String SHOW_DEBUGGER = "debug.show_debugger";
    public static final String SHOW_DISASSEMBLER = "debug.show_disassembler";

    public static final String DEBUGGER_FOLLOW = "ui.debugger.debugger_follow";
    public static final String DISASSEMBLER_FOLLOW = "ui.disassembler.disassembler_follow";

    private static final Path APP_DIR = Path.of(AppDirsFactory.getInstance().getUserDataDir("jemu", null, null));
    private static final Path DATA_FILE = APP_DIR.resolve("data.toml");

    private final Map<String, Object> transientEntries = new ConcurrentHashMap<>();
    private final Map<String, Object> persistentEntries = new ConcurrentHashMap<>();

    public DataManager() {
        if (!Files.exists(DATA_FILE)) {
            Logger.warn("Found no existing data file at \"{}\"", DATA_FILE);
            return;
        }
        try {
            JToml jToml = JToml.jToml();
            TomlTable table = jToml.read(DATA_FILE);
            for (TomlKey key : table.keys()) {
                TomlValue value = table.get(key);
                if (value == null) {
                    continue;
                }
                if (value.isPrimitive()) {
                    TomlPrimitive primitive = value.asPrimitive();
                    if (primitive.isString()) {
                        this.persistentEntries.put(key.toString(), primitive.asString());
                    } else {
                        Logger.warn("Primitive value for key \"{}\" was not a string. This key will be skipped!", key);
                    }
                } else if (value.isArray()) {
                    TomlArray tomlArray = value.asArray();
                    List<String> list = new ArrayList<>();
                    for (TomlValue element : tomlArray) {
                        if (element.isPrimitive()) {
                            TomlPrimitive primitive = element.asPrimitive();
                            if (primitive.isString()) {
                                list.add(primitive.asString());
                            } else {
                                Logger.warn("Primitive value in array for key \"{}\" was not a string. This element will be skipped!", key);
                            }
                        } else {
                            Logger.warn("Non primitive value found in array for key: {}. This element will be skipped!", key);
                        }
                    }
                    this.persistentEntries.put(key.toString(), list.toArray(new String[tomlArray.size()]));
                } else {
                    Logger.warn("Non primitive value found for key: {}. This key will be skipped!", key);
                }
            }
        } catch (Exception e) {
            Logger.warn("Error loading data file from directory {}: {}", APP_DIR, e.getCause());
        }
    }

    public void putTransient(String key, Object value) {
        this.transientEntries.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTransient(String key, Class<T> clazz) {
        Object entry = this.transientEntries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!clazz.isInstance(entry)) {
            return Optional.empty();
        }
        return Optional.of((T) entry);
    }

    public <T> T getTransientOrCompute(String key, Class<T> clazz, Supplier<T> supplier) {
        return clazz.cast(
                this.transientEntries.compute(key, (_, v) -> {
                    if (clazz.isInstance(v)) {
                        return v;
                    }
                    return supplier.get();
                })
        );
    }


    public <T> void modifyTransientOrCompute(String key, Class<T> clazz, Supplier<T> supplier, UnaryOperator<T> modifier) {
        this.transientEntries.compute(key, (_, v) -> {
            if (clazz.isInstance(v)) {
                return modifier.apply(clazz.cast(v));
            }
            return supplier.get();
        });

    }

    public void putPersistent(String key, String value) {
        this.persistentEntries.put(key, value);
    }

    public void putPersistent(String key, String[] value) {
        this.persistentEntries.put(key, value);
    }

    public Optional<String> getPersistent(String key) {
        return Optional.ofNullable(this.persistentEntries.get(key) instanceof String str ? str : null);
    }

    public Optional<String[]> getPersistentArray(String key) {
        return Optional.ofNullable(this.persistentEntries.get(key) instanceof String[] str ? str : null);
    }

    public void save() {
        if (!Files.exists(APP_DIR)) {
            try {
                Files.createDirectories(APP_DIR);
            } catch (Exception e) {
                Logger.error("Error creating app data directories in {}: {}", APP_DIR, e.getCause());
            }
        }
        try {
            JToml jtoml = JToml.jToml();
            TomlTable table = TomlTable.create();
            for (Map.Entry<String, Object> entry : this.persistentEntries.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String str) {
                    table.put(entry.getKey(), str);
                } else if (value instanceof String[] arr) {
                    TomlArray tomlArray = TomlArray.create();
                    for (String e : arr) {
                        tomlArray.add(e);
                    }
                    table.put(entry.getKey(), tomlArray);
                } else {
                    throw new IllegalStateException("Found non string or non string array value: \"" + value.toString() + "\" for key \"" + entry.getKey() + "\" in persistent entries map!");
                }
            }
            jtoml.write(DATA_FILE, table);
        } catch (Exception e) {
            Logger.error("Error saving data to data file: {}", e);
        }
    }

    @Override
    public Optional<Path> getRomPath() {
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> getRawRom() {
        return Optional.empty();
    }

    @Override
    public Optional<List<Path>> getRecentFiles() {
        return this.getPersistentArray(RECENT_FILES).flatMap(recentFiles -> tryOptional(() -> Arrays.stream(recentFiles).map(Path::of).toList()));
    }

    @Override
    public Optional<String> getCurrentDirectory() {
        return this.getPersistent(CURRENT_DIRECTORY);
    }

    @Override
    public Optional<Integer> getVolume() {
        return this.getPersistent(VOLUME).flatMap(v -> tryOptional(() -> Integer.valueOf(v))).filter(i -> i >= 0 && i <= 100);
    }

    @Override
    public Optional<Boolean> getMuted() {
        return this.getPersistent(MUTED).flatMap(v -> tryOptional(() -> Boolean.valueOf(v)));
    }

    @Override
    public Optional<Boolean> getShowingInfoBar() {
        return this.getPersistent(SHOW_INFO_BAR).flatMap(v -> tryOptional(() -> Boolean.valueOf(v)));
    }

    @Override
    public Optional<Boolean> getShowingDebugger() {
        return this.getPersistent(SHOW_DEBUGGER).flatMap(v -> tryOptional(() -> Boolean.valueOf(v)));
    }

    @Override
    public Optional<Boolean> getShowingDisassembler() {
        return this.getPersistent(SHOW_DISASSEMBLER).flatMap(v -> tryOptional(() -> Boolean.valueOf(v)));
    }

    @Override
    public Optional<Boolean> getDebuggerFollowing() {
        return this.getPersistent(DEBUGGER_FOLLOW).flatMap(v -> tryOptional(() -> Boolean.valueOf(v)));
    }

    @Override
    public Optional<Boolean> getDisassemblerFollowing() {
        return this.getPersistent(DISASSEMBLER_FOLLOW).flatMap(v -> tryOptional(() -> Boolean.valueOf(v)));
    }

    @Override
    public Optional<DisplayAngle> getDisplayAngle() {
        return this.getPersistent(DISPLAY_ANGLE).flatMap(str -> tryOptional(() -> DisplayAngle.getDisplayAngleForIdentifier(str)));
    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayout() {
        return this.getPersistent(KEYBOARD_LAYOUT).map(str -> getEnumFromSerialized(KeyboardLayout.class, str));
    }

    @Override
    public Optional<System> getSystem() {
        return this.getPersistent(SYSTEM).map(str -> getEnumFromSerialized(System.class, str));
    }

    public static <T> Optional<T> tryOptional(Supplier<@Nullable T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Exception _) {
            return Optional.empty();
        }
    }

    @Nullable
    private static <E extends Enum<E> & Serializable> E getEnumFromSerialized(Class<E> enumClass, @NotNull String str) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getSerializedString().equals(str)) {
                return e;
            }
        }
        return null;
    }

    public enum BooleanValue implements Serializable {
        NULL("null"),
        TRUE("true"),
        FALSE("false");

        private final String serializedString;

        BooleanValue(String serializedString) {
            this.serializedString = serializedString;
        }

        private Optional<Boolean> mapToInternal() {
            return Optional.ofNullable(switch (this) {
                case NULL -> null;
                case TRUE -> true;
                case FALSE -> false;
            });
        }

        public static String toSerialized(@Nullable Boolean val) {
            BooleanValue value = switch (val) {
                case Boolean bool when bool -> TRUE;
                case null -> NULL;
                default -> FALSE;
            };
            return value.getSerializedString();
        }

        @Override
        public String getSerializedString() {
            return this.serializedString;
        }

    }

}
