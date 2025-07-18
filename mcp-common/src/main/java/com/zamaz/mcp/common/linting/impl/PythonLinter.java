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
 * Python linter implementation using Ruff.
 * Ruff is a fast Python linter written in Rust that combines multiple tools.
 */
@Component
public class PythonLinter implements Linter {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonLinter.class);
    private static final String LINTER_NAME = "ruff";
    private static final Pattern RUFF_OUTPUT_PATTERN = Pattern.compile(
        "^([^:]+):(\\d+):(\\d+): ([A-Z]+\\d+) (.+)$"
    );
    
    @Override
    public String getName() {
        return LINTER_NAME;
    }
    
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("py", "pyi");
    }
    
    @Override
    public List<LintingIssue> lint(Path file, LintingContext context) {
        List<LintingIssue> issues = new ArrayList<>();
        
        try {
            // Build ruff command
            List<String> command = buildRuffCommand(file, context);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(context.getProjectRoot().toFile());
            
            Process process = processBuilder.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LintingIssue issue = parseRuffOutput(line, file);
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
                    logger.warn("Ruff error output: {}", line);
                }
            }
            
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.error("Ruff process timed out for file: {}", file);
            }
            
        } catch (Exception e) {
            logger.error("Error running Ruff on file: {}", file, e);
        }
        
        return issues;
    }
    
    private List<String> buildRuffCommand(Path file, LintingContext context) {
        List<String> command = new ArrayList<>();
        command.add("ruff");
        command.add("check");
        
        // Add configuration file if exists
        Path configFile = context.getProjectRoot().resolve("pyproject.toml");
        if (configFile.toFile().exists()) {
            command.add("--config");
            command.add(configFile.toString());
        }
        
        // Add output format for parsing
        command.add("--output-format");
        command.add("concise");
        
        // Add the file to lint
        command.add(file.toString());
        
        return command;
    }
    
    private LintingIssue parseRuffOutput(String line, Path file) {
        Matcher matcher = RUFF_OUTPUT_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }
        
        String filePath = matcher.group(1);
        int lineNumber = Integer.parseInt(matcher.group(2));
        int columnNumber = Integer.parseInt(matcher.group(3));
        String ruleCode = matcher.group(4);
        String message = matcher.group(5);
        
        // Map Ruff rule codes to severity
        LintingSeverity severity = mapRuffSeverity(ruleCode);
        
        return LintingIssue.builder()
            .file(filePath)
            .line(lineNumber)
            .column(columnNumber)
            .rule(ruleCode)
            .message(message)
            .severity(severity)
            .linter(LINTER_NAME)
            .category(mapRuffCategory(ruleCode))
            .fixable(isRuffFixable(ruleCode))
            .build();
    }
    
    private LintingSeverity mapRuffSeverity(String ruleCode) {
        // Security issues are errors
        if (ruleCode.startsWith("S")) {
            return LintingSeverity.ERROR;
        }
        
        // Errors and bugs are errors
        if (ruleCode.startsWith("E") || ruleCode.startsWith("F") || ruleCode.startsWith("B")) {
            return LintingSeverity.ERROR;
        }
        
        // Warnings
        if (ruleCode.startsWith("W")) {
            return LintingSeverity.WARNING;
        }
        
        // Everything else is info
        return LintingSeverity.INFO;
    }
    
    private String mapRuffCategory(String ruleCode) {
        if (ruleCode.startsWith("S")) return "security";
        if (ruleCode.startsWith("E")) return "error";
        if (ruleCode.startsWith("W")) return "warning";
        if (ruleCode.startsWith("F")) return "pyflakes";
        if (ruleCode.startsWith("B")) return "bugbear";
        if (ruleCode.startsWith("I")) return "imports";
        if (ruleCode.startsWith("N")) return "naming";
        if (ruleCode.startsWith("UP")) return "pyupgrade";
        if (ruleCode.startsWith("C4")) return "comprehensions";
        if (ruleCode.startsWith("T20")) return "print";
        if (ruleCode.startsWith("RUF")) return "ruff";
        if (ruleCode.startsWith("PL")) return "pylint";
        if (ruleCode.startsWith("Q")) return "quotes";
        if (ruleCode.startsWith("SIM")) return "simplify";
        if (ruleCode.startsWith("TID")) return "tidy-imports";
        if (ruleCode.startsWith("TCH")) return "type-checking";
        if (ruleCode.startsWith("PTH")) return "pathlib";
        if (ruleCode.startsWith("ERA")) return "eradicate";
        if (ruleCode.startsWith("ARG")) return "unused-arguments";
        
        return "general";
    }
    
    private boolean isRuffFixable(String ruleCode) {
        // Most formatting and import issues are fixable
        return ruleCode.startsWith("I") || 
               ruleCode.startsWith("Q") ||
               ruleCode.startsWith("UP") ||
               ruleCode.startsWith("W") ||
               ruleCode.startsWith("F541") ||
               ruleCode.startsWith("RUF");
    }
    
    /**
     * Run Ruff with auto-fix enabled.
     */
    public List<LintingIssue> lintAndFix(Path file, LintingContext context) {
        try {
            List<String> command = buildRuffCommand(file, context);
            // Replace "check" with "check --fix"
            int checkIndex = command.indexOf("check");
            if (checkIndex >= 0) {
                command.add(checkIndex + 1, "--fix");
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(context.getProjectRoot().toFile());
            
            Process process = processBuilder.start();
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                logger.error("Ruff auto-fix process timed out for file: {}", file);
            }
            
            // Re-run linting to get remaining issues
            return lint(file, context);
            
        } catch (Exception e) {
            logger.error("Error running Ruff auto-fix on file: {}", file, e);
            return lint(file, context);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("ruff", "--version").start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            logger.debug("Ruff not available: {}", e.getMessage());
            return false;
        }
    }
}