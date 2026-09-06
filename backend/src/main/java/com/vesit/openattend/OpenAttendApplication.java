package com.vesit.openattend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class OpenAttendApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(OpenAttendApplication.class, args);
    }

    private static void loadDotEnv() {
        File[] candidates = new File[] {
            new File(".env"),
            new File("../.env"),
            new File(System.getProperty("user.dir"), ".env")
        };

        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = trimmed.indexOf('=');
                        if (eqIdx > 0) {
                            String key = trimmed.substring(0, eqIdx).trim();
                            String val = trimmed.substring(eqIdx + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }

                            // If key is DATABASE_URL, parse standard postgresql:// format to JDBC
                            if ("DATABASE_URL".equals(key)) {
                                handleDatabaseUrl(val);
                            }
                        }
                    }
                    break;
                } catch (Exception e) {
                    System.err.println("Notice: Could not load .env file: " + e.getMessage());
                }
            }
        }
    }

    private static void handleDatabaseUrl(String rawUrl) {
        try {
            if (rawUrl.startsWith("postgresql://") || rawUrl.startsWith("postgres://")) {
                URI uri = new URI(rawUrl.replace("postgresql://", "http://").replace("postgres://", "http://"));
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    if (System.getProperty("DB_USERNAME") == null && System.getenv("DB_USERNAME") == null) {
                        System.setProperty("DB_USERNAME", parts[0]);
                        System.setProperty("spring.datasource.username", parts[0]);
                    }
                    if (System.getProperty("DB_PASSWORD") == null && System.getenv("DB_PASSWORD") == null) {
                        System.setProperty("DB_PASSWORD", parts[1]);
                        System.setProperty("spring.datasource.password", parts[1]);
                    }
                }
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String query = uri.getQuery();
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null ? "?" + query : "");
                System.setProperty("DATABASE_URL", jdbcUrl);
                System.setProperty("spring.datasource.url", jdbcUrl);
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not parse DATABASE_URL: " + e.getMessage());
        }
    }
}

