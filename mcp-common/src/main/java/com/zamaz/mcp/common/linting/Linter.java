package com.zamaz.mcp.common.linting;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for individual linter implementations.
 */
public interface Linter {
    
    /**
     * Get the name of this linter.
     *
     * @return the linter name (e.g., "checkstyle", "eslint", "ruff")
     */
    String getName();
    
    /**
     * Get the file extensions supported by this linter.
     *
     * @return list of supported file extensions (without dots)
     */
    List<String> getSupportedExtensions();
    
    /**
     * Lint a single file.
     *
     * @param file the file to lint
     * @param context the linting context
     * @return list of linting issues found
     */
    List<LintingIssue> lint(Path file, LintingContext context);
    
    /**
     * Check if this linter is available on the system.
     *
     * @return true if the linter is installed and available
     */
    boolean isAvailable();
}