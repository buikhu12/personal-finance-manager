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
                            setIfAbsent(key, value);
                            mapAliases(key, value);
                        }
                    }
                    break;
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void mapAliases(String key, String value) {
        switch (key) {
            case "server.port" -> setIfAbsent("PORT", value);
            case "PORT" -> setIfAbsent("server.port", value);

            case "spring.datasource.url" -> setIfAbsent("DB_URL", value);
            case "DB_URL" -> setIfAbsent("spring.datasource.url", value);

            case "spring.datasource.driver-class-name" -> setIfAbsent("DB_DRIVER", value);
            case "DB_DRIVER" -> setIfAbsent("spring.datasource.driver-class-name", value);

            case "spring.datasource.username" -> setIfAbsent("DB_USERNAME", value);
            case "DB_USERNAME" -> setIfAbsent("spring.datasource.username", value);

            case "spring.datasource.password" -> setIfAbsent("DB_PASSWORD", value);
            case "DB_PASSWORD" -> setIfAbsent("spring.datasource.password", value);

            case "spring.jpa.hibernate.ddl-auto" -> setIfAbsent("JPA_DDL_AUTO", value);
            case "JPA_DDL_AUTO" -> setIfAbsent("spring.jpa.hibernate.ddl-auto", value);

            case "spring.jpa.show-sql" -> setIfAbsent("JPA_SHOW_SQL", value);
            case "JPA_SHOW_SQL" -> setIfAbsent("spring.jpa.show-sql", value);

            case "spring.jpa.database-platform" -> setIfAbsent("JPA_DATABASE_PLATFORM", value);
            case "JPA_DATABASE_PLATFORM" -> setIfAbsent("spring.jpa.database-platform", value);

            case "spring.h2.console.enabled" -> setIfAbsent("H2_CONSOLE_ENABLED", value);
            case "H2_CONSOLE_ENABLED" -> setIfAbsent("spring.h2.console.enabled", value);

            case "logging.level.org.springframework.security" -> setIfAbsent("LOG_SECURITY", value);
            case "LOG_SECURITY" -> setIfAbsent("logging.level.org.springframework.security", value);

            case "logging.level.org.hibernate.SQL" -> setIfAbsent("LOG_SQL", value);
            case "LOG_SQL" -> setIfAbsent("logging.level.org.hibernate.SQL", value);
        }
    }

    private static void setIfAbsent(String key, String value) {
        if (System.getProperty(key) == null && System.getenv(key) == null) {
            System.setProperty(key, value);
        }
    }
}
