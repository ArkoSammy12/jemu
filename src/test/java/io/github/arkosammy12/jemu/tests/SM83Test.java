package io.github.arkosammy12.jemu.tests;

import io.github.arkosammy12.jemu.ssts.sm83.SM83TestBench;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SM83Test {

    @Test
    public void sm83_ssts() {
        URL url = SM83Test.class.getClassLoader().getResource("ssts/sm83/v1");
        try (Stream<Path> testFilePaths = Files.list(Paths.get(url.toURI()))) {
            testFilePaths.forEach(path -> {
                try {
                    SM83TestBench testBench = new SM83TestBench(path);
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
