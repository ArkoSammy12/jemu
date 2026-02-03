package io.github.arkosammy12.jemu.tests;

import io.github.arkosammy12.jemu.ssts.cdp1802.CDP1802TestBench;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class CDP1802Test {

    @Test
    public void cdp1802_ssts() {
        URL url = SM83Test.class.getClassLoader().getResource("ssts/cdp1802/v1");
        try (Stream<Path> testFilePaths = Files.list(Paths.get(url.toURI()))) {
            testFilePaths.forEach(path -> {
                try {
                    CDP1802TestBench testBench = new CDP1802TestBench(path);
                    testBench.runTest();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
