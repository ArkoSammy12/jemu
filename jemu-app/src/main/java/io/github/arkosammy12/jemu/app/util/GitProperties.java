package io.github.arkosammy12.jemu.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GitProperties {

    public static final String COMMIT_ID;

    static {
        Properties props = new Properties();
        try (InputStream is = GitProperties.class.getResourceAsStream("/git.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // git.properties unavailable, falls back to "unknown"
        }
        COMMIT_ID = props.getProperty("git.commit.id.full", "unknown");
    }

}
