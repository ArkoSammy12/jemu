package io.github.arkosammy12.jemu.core.test.ssts.nes6502;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NES6502TestBench {

    private final NES6502TestFile testFile;

    public NES6502TestBench(Path filePath) throws IOException {
        Gson gson = new Gson();
        Type type = new TypeToken<List<NES6502TestCase>>() {}.getType();
        this.testFile = new NES6502TestFile(gson.fromJson(Files.readString(filePath), type));
    }

    public void runTest() {
        List<NES6502TestCase> testCases = this.testFile.testCases();
        for (NES6502TestCase testCase : testCases) {
            NES6502TestCaseBench testCaseBench = new NES6502TestCaseBench(testCase);
            testCaseBench.runTest();
        }
    }

}
