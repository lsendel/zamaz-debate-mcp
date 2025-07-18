package com.zamaz.mcp.common.linting;

import java.nio.file.Path;
import java.util.List;

/**
 * Main interface for the linting engine that orchestrates all linting operations.
 */
public interface LintingEngine {
    
    /**
     * Lint the entire project with all configured linters.
     *
     * @param context the linting context containing configuration and settings
     * @return aggregated linting results
     */
    LintingResult lintProject(LintingContext context);
    
    /**
     * Lint a specific service/module.
     *
     * @param serviceName the name of the service to lint
     * @param context the linting context
     * @return linting results for the service
     */
    LintingResult lintService(String serviceName, LintingContext context);
    
    /**
     * Lint specific files.
     *
     * @param files list of file paths to lint
     * @param context the linting context
     * @return linting results for the files
     */
    LintingResult lintFiles(List<Path> files, LintingContext context);
    
    /**
     * Get available linters for a given file type.
     *
     * @param fileExtension the file extension (e.g., "java", "ts", "yml")
     * @return list of available linter names
     */
    List<String> getAvailableLinters(String fileExtension);
}