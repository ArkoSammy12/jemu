package io.github.arkosammy12.jemu.core.tests;

import io.github.arkosammy12.jemu.core.ssts.nes6502.NES6502TestBench;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class NES6502Test {

    @Test
    public void nes6502_ssts() {
        URL url = NES6502Test.class.getClassLoader().getResource("submodules/tests/65x02-ssts/nes6502/v1");
        try (Stream<Path> testFilePaths = Files.list(Paths.get(url.toURI()))) {
            testFilePaths.forEach(path -> {
                try {
                    NES6502TestBench testBench = new NES6502TestBench(path);
                    testBench.runTest();
                } catch (IOException e) {
                    Logger.error("Exception running NES6502 SSTs: {}", e);
                }
            });
        } catch (IOException | URISyntaxException e) {
            Logger.error("Exception running NES6502 SSTs: {}", e);
        }
    }

}
