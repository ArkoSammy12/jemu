package io.github.arkosammy12.jemu.ssts.sm83;

import java.util.List;

public class SM83TestFile {

    private final List<SM83TestCase> testCases;

    public SM83TestFile(List<SM83TestCase> testCases) {
        this.testCases = testCases;
    }

    public List<SM83TestCase> getTestCases() {
        return this.testCases;
    }

}
