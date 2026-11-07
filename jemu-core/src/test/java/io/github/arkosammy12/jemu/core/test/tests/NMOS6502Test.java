package io.github.arkosammy12.jemu.core.test.tests;

import io.github.arkosammy12.jemu.core.test.ssts.nes6502.NES6502TestBench;
import io.github.arkosammy12.jemu.core.test.ssts.nmos6502.NMOS6502TestBench;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class NMOS6502Test {

    @Test
    public void nmos6502_ssts() {
        URL url = NMOS6502Test.class.getClassLoader().getResource("submodules/tests/65x02-ssts/6502/v1");
        if (url == null) {
            Logger.warn("SST files for NMOS6502 CPU not found!");
            return;
        }
        try (Stream<Path> testFilePaths = Files.list(Paths.get(url.toURI()))) {
            Logger.info("Running SSTs for NMOS6502 CPU");
            testFilePaths.forEach(path -> {
                try {
                    NMOS6502TestBench testBench = new NMOS6502TestBench(path);
                    testBench.runTest();
                } catch (IOException e) {
                    Logger.error("Exception running NMOS6502 SSTs: {}", e);
                }
            });
        } catch (IOException | URISyntaxException e) {
            Logger.error("Exception running NMOS6502 SSTs: {}", e);
        }
    }

}
