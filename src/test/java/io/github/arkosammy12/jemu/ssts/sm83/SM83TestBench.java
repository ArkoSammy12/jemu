package io.github.arkosammy12.jemu.ssts.sm83;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SM83TestBench {

    SM83TestFile testFile;

    public SM83TestBench(Path filePath) throws IOException {
        Gson gson = new Gson();
        Type type = new TypeToken<List<SM83TestCase>>() {}.getType();
        this.testFile = new SM83TestFile(gson.fromJson(Files.readString(filePath), type));
    }

    public void runTest() {
        List<SM83TestCase> testCases = this.testFile.getTestCases();
        for (SM83TestCase testCase : testCases) {
            SM83TestCaseBench testCaseBench = new SM83TestCaseBench(testCase);
            testCaseBench.runTest();
        }
    }

}
