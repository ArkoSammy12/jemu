package io.github.arkosammy12.jemu.core.test.gameboy;

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/// Use `-Djemu.gb.testRoms` to override the test ROM directory. Default is `target/test-roms`.
public final class TestRoms {

    private TestRoms() {
        throw new AssertionError();
    }

    public static Optional<Path> resolveDirectory(String relativePath) {
        String property = System.getProperty("jemu.gb.testRoms");
        if (property == null) {
            Logger.warn("Test ROMs not configured (-Djemu.gb.testRoms); skipping");
            return Optional.empty();
        }
        Path directory = Paths.get(property).resolve(relativePath);
        if (!Files.isDirectory(directory)) {
            Logger.warn("Test ROM directory not found at {}; skipping", directory);
            return Optional.empty();
        }
        return Optional.of(directory);
    }

    public static Set<String> readExpectedFailures(String resource) throws IOException {
        try (InputStream in = Objects.requireNonNull(TestRoms.class.getClassLoader().getResourceAsStream(resource), resource);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

}
