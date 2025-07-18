package com.zamaz.mcp.common.linting.impl;

import com.zamaz.mcp.common.linting.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shell script linter implementation using ShellCheck.
 * ShellCheck is a shell script static analysis tool that detects bugs and security issues.
 */
@Component
public class ShellLinter implements Linter {
    
    private static final Logger logger = LoggerFactory.getLogger(ShellLinter.class);
    private static final String LINTER_NAME = "shellcheck";
    private static final Pattern SHELLCHECK_OUTPUT_PATTERN = Pattern.compile(
        "^([^:]+):(\\d+):(\\d+): ([a-z]+): (.+) \\[SC(\\d+)\\]$"
    );
    
    @Override
    public String getName() {
        return LINTER_NAME;
    }
    
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("sh", "bash", "ksh", "zsh");
    }
    
    @Override
    public List<LintingIssue> lint(Path file, LintingContext context) {
        List<LintingIssue> issues = new ArrayList<>();
        
        try {
            // Build shellcheck command
            List<String> command = buildShellCheckCommand(file, context);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(context.getProjectRoot().toFile());
            
            Process process = processBuilder.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LintingIssue issue = parseShellCheckOutput(line, file);
                    if (issue != null) {
                        issues.add(issue);
                    }
                }
            }
            
            // Read error stream
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    logger.warn("ShellCheck error output: {}", line);
                }
            }
            
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.error("ShellCheck process timed out for file: {}", file);
            }
            
        } catch (Exception e) {
            logger.error("Error running ShellCheck on file: {}", file, e);
        }
        
        return issues;
    }
    
    private List<String> buildShellCheckCommand(Path file, LintingContext context) {
        List<String> command = new ArrayList<>();
        command.add("shellcheck");
        
        // Add configuration file if exists
        Path configFile = context.getProjectRoot().resolve(".shellcheckrc");
        if (configFile.toFile().exists()) {
            command.add("--rcfile");
            command.add(configFile.toString());
        }
        
        // Add output format for parsing
        command.add("--format");
        command.add("gcc");
        
        // Enable all checks
        command.add("--enable=all");
        
        // Add external sources support
        command.add("--external-sources");
        
        // Add severity level (include all)
        command.add("--severity");
        command.add("info");
        
        // Add the file to lint
        command.add(file.toString());
        
        return command;
    }
    
    private LintingIssue parseShellCheckOutput(String line, Path file) {
        Matcher matcher = SHELLCHECK_OUTPUT_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }
        
        String filePath = matcher.group(1);
        int lineNumber = Integer.parseInt(matcher.group(2));
        int columnNumber = Integer.parseInt(matcher.group(3));
        String severityStr = matcher.group(4);
        String message = matcher.group(5);
        String errorCode = "SC" + matcher.group(6);
        
        // Map ShellCheck severity
        LintingSeverity severity = mapShellCheckSeverity(severityStr, errorCode);
        
        return LintingIssue.builder()
            .file(filePath)
            .line(lineNumber)
            .column(columnNumber)
            .rule(errorCode)
            .message(message)
            .severity(severity)
            .linter(LINTER_NAME)
            .category(mapShellCheckCategory(errorCode))
            .fixable(false) // ShellCheck doesn't have auto-fix
            .build();
    }
    
    private LintingSeverity mapShellCheckSeverity(String severityStr, String errorCode) {
        // ShellCheck severity levels
        switch (severityStr.toLowerCase()) {
            case "error":
                return LintingSeverity.ERROR;
            case "warning":
                return LintingSeverity.WARNING;
            case "info":
            case "style":
                return LintingSeverity.INFO;
            default:
                // Check error code for security issues
                int code = Integer.parseInt(errorCode.substring(2));
                if (isSecurityIssue(code)) {
                    return LintingSeverity.ERROR;
                }
                return LintingSeverity.WARNING;
        }
    }
    
    private boolean isSecurityIssue(int code) {
        // Security-related ShellCheck codes
        return (code >= 2000 && code < 2100) || // Command injection, etc.
               (code >= 2086 && code <= 2089) || // Unquoted variables
               code == 2155 || // Declare and assign separately
               code == 2162 || // read without -r
               code == 2181 || // Check exit code directly
               code == 2206 || // Quote to prevent word splitting
               code == 2207;   // Prefer mapfile or read -a
    }
    
    private String mapShellCheckCategory(String errorCode) {
        int code = Integer.parseInt(errorCode.substring(2));
        
        if (code >= 1000 && code < 2000) return "syntax";
        if (code >= 2000 && code < 2100) return "security";
        if (code >= 2100 && code < 2200) return "style";
        if (code >= 2200 && code < 2300) return "info";
        
        return "general";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("shellcheck", "--version").start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            logger.debug("ShellCheck not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a file is a shell script by examining its shebang line.
     */
    public boolean isShellScript(Path file) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(file);
            if (!lines.isEmpty()) {
                String firstLine = lines.get(0).trim();
                return firstLine.startsWith("#!") && 
                       (firstLine.contains("/sh") || 
                        firstLine.contains("/bash") || 
                        firstLine.contains("/ksh") || 
                        firstLine.contains("/zsh"));
            }
        } catch (Exception e) {
            logger.debug("Error checking if file is shell script: {}", file, e);
        }
        return false;
    }
}