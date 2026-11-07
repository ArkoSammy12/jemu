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

/**
 * Locates the compiled Game Boy test ROMs fetched into {@code target/test-roms} at
 * build time (see the download-maven-plugin execution in the core pom), laid out as
 * in c-sp/game-boy-test-roms. {@code -Djemu.gb.testRoms} overrides the location.
 */
public final class TestRoms {

    private TestRoms() {
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

    /**
     * Reads a known-failure list from a test resource: one ROM path per line, with
     * blank lines and {@code #} comments ignored.
     */
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
