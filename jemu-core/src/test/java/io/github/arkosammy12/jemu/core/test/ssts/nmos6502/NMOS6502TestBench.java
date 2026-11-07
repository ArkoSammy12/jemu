package io.github.arkosammy12.jemu.core.test.ssts.nmos6502;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NMOS6502TestBench {

    private final io.github.arkosammy12.jemu.core.test.ssts.nmos6502.NMOS6502TestFile testFile;

    public NMOS6502TestBench(Path filePath) throws IOException {
        Gson gson = new Gson();
        Type type = new TypeToken<List<NMOS6502TestCase>>() {}.getType();
        this.testFile = new NMOS6502TestFile(gson.fromJson(Files.readString(filePath), type));
    }

    public void runTest() {
        List<NMOS6502TestCase> testCases = this.testFile.testCases();
        for (NMOS6502TestCase testCase : testCases) {
            NMOS6502TestCaseBench testCaseBench = new NMOS6502TestCaseBench(testCase);
            testCaseBench.runTest();
        }
    }

}
