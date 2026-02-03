package io.github.arkosammy12.jemu.ssts.cdp1802;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.arkosammy12.jemu.ssts.sm83.SM83TestCase;
import io.github.arkosammy12.jemu.ssts.sm83.SM83TestCaseBench;
import io.github.arkosammy12.jemu.ssts.sm83.SM83TestFile;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CDP1802TestBench {

    private final CDP1802TestFile testFile;

    public CDP1802TestBench(Path filePath) throws IOException {
        Gson gson = new Gson();
        Type type = new TypeToken<List<CDP1802TestCase>>() {}.getType();
        this.testFile = new CDP1802TestFile(gson.fromJson(Files.readString(filePath), type));
    }

    public void runTest() {
        List<CDP1802TestCase> testCases = this.testFile.testCases();
        for (CDP1802TestCase testCase : testCases) {
            CDP1802TestCaseBench testCaseBench = new CDP1802TestCaseBench(testCase);
            testCaseBench.runTest();
        }
    }

}
