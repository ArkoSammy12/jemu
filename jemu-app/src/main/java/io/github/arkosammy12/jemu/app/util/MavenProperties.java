package io.github.arkosammy12.jemu.app.util;

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class MavenProperties {

    public static final String VERSION;
    public static final String ARTIFACT_ID;
    public static final String BUILD_DATE;
    public static final String AUTHOR;

    static {
        Properties props = new Properties();
        try (InputStream is = MavenProperties.class.getResourceAsStream("/app.properties")) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Could not load app.properties", e);
        }
        VERSION = props.getProperty("version", "unknown");
        ARTIFACT_ID = props.getProperty("artifact.id", "jemu");
        BUILD_DATE = props.getProperty("build.date", "unknown");
        AUTHOR = props.getProperty("author", "unknown");
    }

    private MavenProperties() {}

    public static final class Provider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[] { MavenProperties.VERSION };
        }
    }

}