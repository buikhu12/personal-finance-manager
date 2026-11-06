package com.vishnu.financemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class FinanceManagerApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(FinanceManagerApplication.class, args);
    }

    private static void loadDotenv() {
        Path[] candidates = new Path[]{
                Paths.get(".env"),
                Paths.get(System.getProperty("user.dir", "."), ".env")
        };

        for (Path path : candidates) {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = trimmed.indexOf('=');
                        if (eqIdx > 0) {
                            String key = trimmed.substring(0, eqIdx).trim();
                            String value = trimmed.substring(eqIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                    break;
                } catch (IOException ignored) {
                }
            }
        }
    }
}
