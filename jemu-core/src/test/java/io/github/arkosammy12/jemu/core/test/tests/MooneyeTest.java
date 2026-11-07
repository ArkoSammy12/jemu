package io.github.arkosammy12.jemu.core.test.tests;

import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.core.test.gameboy.GameBoyTestHarness;
import io.github.arkosammy12.jemu.core.test.gameboy.TestRoms;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class MooneyeTest {

    private static final int TIMEOUT_FRAMES = 20 * 60;
    private static final String EXPECTED_FAILURES_RESOURCE = "gameboy/mooneye-expected-failures.txt";

    @TestFactory
    public Stream<DynamicTest> mooneye_acceptance() throws IOException {
        Optional<Path> acceptanceDirectory = TestRoms.resolveDirectory("mooneye-test-suite/acceptance");
        if (acceptanceDirectory.isEmpty()) {
            return Stream.empty();
        }
        Path acceptance = acceptanceDirectory.get();
        Set<String> expectedFailures = TestRoms.readExpectedFailures(EXPECTED_FAILURES_RESOURCE);

        List<Path> roms;
        try (Stream<Path> walk = Files.walk(acceptance)) {
            roms = walk.filter(path -> path.toString().endsWith(".gb")).sorted().toList();
        }

        return roms.stream().flatMap(rom -> modelsFor(rom).stream().map(model -> {
            String name = "%s (%s)".formatted(acceptance.relativize(rom).toString().replace(File.separatorChar, '/'), model);
            return DynamicTest.dynamicTest(name, () -> {
                GameBoyTestHarness.Result result;
                try (GameBoyTestHarness harness = new GameBoyTestHarness(rom, model)) {
                    result = harness.runMooneye(TIMEOUT_FRAMES);
                }
                Logger.info("{}: {}", name, result);
                boolean expectedToFail = expectedFailures.contains(name);
                if (result == GameBoyTestHarness.Result.PASSED && expectedToFail) {
                    fail("%s now passes; remove it from %s".formatted(name, EXPECTED_FAILURES_RESOURCE));
                } else if (result != GameBoyTestHarness.Result.PASSED && !expectedToFail) {
                    fail("%s: %s".formatted(name, result));
                }
            });
        }));
    }

    // Mooneye test suffixes with "G" are for DMG, and those with "C" are for CGB.
    // Tests with no suffix are for any model.
    private static Set<GameBoyHost.Model> modelsFor(Path rom) {
        String stem = rom.getFileName().toString();
        stem = stem.substring(0, stem.length() - ".gb".length());
        int dash = stem.lastIndexOf('-');
        if (dash < 0) {
            return EnumSet.allOf(GameBoyHost.Model.class);
        }
        String suffix = stem.substring(dash + 1);
        Set<GameBoyHost.Model> models = EnumSet.noneOf(GameBoyHost.Model.class);
        if (suffix.chars().allMatch(Character::isUpperCase)) {
            if (suffix.indexOf('G') >= 0) {
                models.add(GameBoyHost.Model.DMG);
            }
            if (suffix.indexOf('C') >= 0) {
                models.add(GameBoyHost.Model.CGB);
            }
        } else if (suffix.startsWith("dmg") && !suffix.equals("dmg0")) {
            models.add(GameBoyHost.Model.DMG);
        } else if (suffix.startsWith("cgb") && !suffix.equals("cgb0")) {
            models.add(GameBoyHost.Model.CGB);
        }
        return models;
    }

}
