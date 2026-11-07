package io.github.arkosammy12.jemu.core.test.tests;

import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.core.test.gameboy.GameBoyTestHarness;
import io.github.arkosammy12.jemu.core.test.gameboy.TestRoms;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class BlarggTest {

    private static final int TIMEOUT_FRAMES = 60 * 60;
    private static final String EXPECTED_FAILURES_RESOURCE = "gameboy/blargg-expected-failures.txt";

    private static final List<Map.Entry<String, Set<GameBoyHost.Model>>> ROM_DIRECTORIES = List.of(
            Map.entry("cpu_instrs/individual", EnumSet.allOf(GameBoyHost.Model.class)),
            Map.entry("instr_timing", EnumSet.allOf(GameBoyHost.Model.class)),
            Map.entry("mem_timing/individual", EnumSet.allOf(GameBoyHost.Model.class)),
            Map.entry("mem_timing-2/rom_singles", EnumSet.allOf(GameBoyHost.Model.class)),
            Map.entry("dmg_sound/rom_singles", EnumSet.of(GameBoyHost.Model.DMG)),
            Map.entry("cgb_sound/rom_singles", EnumSet.of(GameBoyHost.Model.CGB)),
            Map.entry("oam_bug/rom_singles", EnumSet.of(GameBoyHost.Model.DMG)),
            Map.entry("interrupt_time", EnumSet.of(GameBoyHost.Model.CGB))
    );

    @TestFactory
    public Stream<DynamicTest> blargg() throws IOException {
        Optional<Path> blarggDirectory = TestRoms.resolveDirectory("blargg");
        if (blarggDirectory.isEmpty()) {
            return Stream.empty();
        }
        Path blargg = blarggDirectory.get();
        Set<String> expectedFailures = TestRoms.readExpectedFailures(EXPECTED_FAILURES_RESOURCE);

        return ROM_DIRECTORIES.stream().flatMap(entry -> {
            Path directory = blargg.resolve(entry.getKey());
            if (!Files.isDirectory(directory)) {
                return Stream.empty();
            }
            return listRoms(directory).stream().flatMap(rom -> entry.getValue().stream().map(model -> {
                String name = "%s (%s)".formatted(blargg.relativize(rom).toString().replace(File.separatorChar, '/'), model);
                return DynamicTest.dynamicTest(name, () -> {
                    GameBoyTestHarness.BlarggResult result;
                    try (GameBoyTestHarness harness = new GameBoyTestHarness(rom, model)) {
                        result = harness.runBlargg(TIMEOUT_FRAMES);
                    }
                    Logger.info("{}: {}", name, result.status());
                    boolean expectedToFail = expectedFailures.contains(name);
                    if (result.status() == GameBoyTestHarness.Result.PASSED && expectedToFail) {
                        fail("%s now passes; remove it from %s".formatted(name, EXPECTED_FAILURES_RESOURCE));
                    } else if (result.status() != GameBoyTestHarness.Result.PASSED && !expectedToFail) {
                        fail("%s: %s\n%s".formatted(name, result.status(), result.output()));
                    }
                });
            }));
        });
    }

    private static List<Path> listRoms(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.toString().endsWith(".gb")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
