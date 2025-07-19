package com.zamaz.mcp.configserver.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class CredentialScanner {

    private static final Logger logger = LoggerFactory.getLogger(CredentialScanner.class);

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            // Passwords
            Pattern.compile("password\\s*[:=]\\s*[^$\\{].*[^}]$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pwd\\s*[:=]\\s*[^$\\{].*[^}]$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pass\\s*[:=]\\s*[^$\\{].*[^}]$", Pattern.CASE_INSENSITIVE),
            
            // Secrets and keys
            Pattern.compile("secret\\s*[:=]\\s*[^$\\{].*[^}]$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("key\\s*[:=]\\s*[^$\\{].*[^}]$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("api[_-]?key\\s*[:=]\\s*[^$\\{].*[^}]$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("token\\s*[:=]\\s*[^$\\{].*[^}]$", Pattern.CASE_INSENSITIVE),
            
            // Private keys
            Pattern.compile("BEGIN\\s+PRIVATE\\s+KEY"),
            Pattern.compile("BEGIN\\s+RSA\\s+PRIVATE\\s+KEY"),
            Pattern.compile("BEGIN\\s+EC\\s+PRIVATE\\s+KEY"),
            
            // Connection strings with credentials
            Pattern.compile("jdbc:[^:]+://[^:]+:[^@]+@"),
            Pattern.compile("mongodb://[^:]+:[^@]+@"),
            Pattern.compile("redis://[^:]+:[^@]+@")
    );

    private static final List<Pattern> EXCLUSION_PATTERNS = List.of(
            Pattern.compile("\\{cipher\\}"),
            Pattern.compile("change-me", Pattern.CASE_INSENSITIVE),
            Pattern.compile("example", Pattern.CASE_INSENSITIVE),
            Pattern.compile("placeholder", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\$\\{[^}]+\\}")  // Environment variables
    );

    public static class ScanResult {
        private final Path file;
        private final int lineNumber;
        private final String line;
        private final String issue;

        public ScanResult(Path file, int lineNumber, String line, String issue) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.line = line;
            this.issue = issue;
        }

        public Path getFile() {
            return file;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getLine() {
            return line;
        }

        public String getIssue() {
            return issue;
        }
    }

    public List<ScanResult> scanFile(Path file) throws IOException {
        List<ScanResult> results = new ArrayList<>();
        
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return results;
        }
        
        List<String> lines = Files.readAllLines(file);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            
            // Skip if line matches exclusion patterns
            if (shouldExclude(line)) {
                continue;
            }
            
            // Check against sensitive patterns
            for (Pattern pattern : SENSITIVE_PATTERNS) {
                if (pattern.matcher(line).find()) {
                    results.add(new ScanResult(
                            file,
                            lineNumber,
                            line.trim(),
                            "Potential hardcoded credential detected"
                    ));
                    break;
                }
            }
        }
        
        return results;
    }

    public List<ScanResult> scanDirectory(Path directory) throws IOException {
        List<ScanResult> allResults = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isConfigurationFile)
                    .forEach(file -> {
                        try {
                            allResults.addAll(scanFile(file));
                        } catch (IOException e) {
                            logger.error("Error scanning file: {}", file, e);
                        }
                    });
        }
        
        return allResults;
    }

    private boolean shouldExclude(String line) {
        return EXCLUSION_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(line).find());
    }

    private boolean isConfigurationFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".yml") ||
               fileName.endsWith(".yaml") ||
               fileName.endsWith(".properties") ||
               fileName.endsWith(".json");
    }

    public void reportResults(List<ScanResult> results) {
        if (results.isEmpty()) {
            logger.info("No security issues found");
            return;
        }
        
        logger.warn("Found {} potential security issues:", results.size());
        
        results.forEach(result -> {
            logger.warn("File: {}, Line {}: {}",
                    result.getFile(),
                    result.getLineNumber(),
                    result.getIssue()
            );
            logger.warn("  > {}", result.getLine());
        });
    }
}