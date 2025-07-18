package com.zamaz.mcp.common.linting.incremental;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzes git diffs to determine which files have changed.
 */
public class GitDiffAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(GitDiffAnalyzer.class);

    /**
     * Get files that have changed between two commits.
     */
    public Set<Path> getChangedFiles(Path projectRoot, String fromCommit, String toCommit) {
        try {
            logger.debug("Getting changed files from {} to {} in {}", fromCommit, toCommit, projectRoot);
            
            ProcessBuilder pb = new ProcessBuilder(
                "git", "diff", "--name-only", fromCommit + ".." + toCommit
            );
            pb.directory(projectRoot.toFile());
            
            Process process = pb.start();
            
            Set<Path> changedFiles = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        Path filePath = projectRoot.resolve(Paths.get(line.trim()));
                        changedFiles.add(filePath);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = readProcessError(process);
                throw new RuntimeException("Git diff failed with exit code " + exitCode + ": " + error);
            }
            
            logger.debug("Found {} changed files", changedFiles.size());
            return changedFiles;
            
        } catch (Exception e) {
            logger.error("Error getting changed files from git", e);
            throw new RuntimeException("Failed to get changed files from git", e);
        }
    }

    /**
     * Get files that have changed in the working directory compared to the index.
     */
    public Set<Path> getWorkingDirectoryChanges(Path projectRoot) {
        try {
            logger.debug("Getting working directory changes in {}", projectRoot);
            
            // Get both staged and unstaged changes
            Set<Path> changedFiles = new HashSet<>();
            
            // Staged changes
            changedFiles.addAll(getGitDiffFiles(projectRoot, "--cached"));
            
            // Unstaged changes
            changedFiles.addAll(getGitDiffFiles(projectRoot));
            
            // Untracked files
            changedFiles.addAll(getUntrackedFiles(projectRoot));
            
            logger.debug("Found {} changed files in working directory", changedFiles.size());
            return changedFiles;
            
        } catch (Exception e) {
            logger.error("Error getting working directory changes", e);
            throw new RuntimeException("Failed to get working directory changes", e);
        }
    }

    /**
     * Get files that have changed between the current branch and another branch.
     */
    public Set<Path> getChangedFilesBetweenBranches(Path projectRoot, String baseBranch, String targetBranch) {
        return getChangedFiles(projectRoot, baseBranch, targetBranch);
    }

    /**
     * Get files changed in a specific commit.
     */
    public Set<Path> getChangedFilesInCommit(Path projectRoot, String commit) {
        try {
            logger.debug("Getting changed files in commit {} in {}", commit, projectRoot);
            
            ProcessBuilder pb = new ProcessBuilder(
                "git", "diff-tree", "--no-commit-id", "--name-only", "-r", commit
            );
            pb.directory(projectRoot.toFile());
            
            Process process = pb.start();
            
            Set<Path> changedFiles = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        Path filePath = projectRoot.resolve(Paths.get(line.trim()));
                        changedFiles.add(filePath);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = readProcessError(process);
                throw new RuntimeException("Git diff-tree failed with exit code " + exitCode + ": " + error);
            }
            
            logger.debug("Found {} changed files in commit {}", changedFiles.size(), commit);
            return changedFiles;
            
        } catch (Exception e) {
            logger.error("Error getting changed files in commit {}", commit, e);
            throw new RuntimeException("Failed to get changed files in commit " + commit, e);
        }
    }

    /**
     * Check if the current directory is a git repository.
     */
    public boolean isGitRepository(Path projectRoot) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--git-dir");
            pb.directory(projectRoot.toFile());
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
            
        } catch (Exception e) {
            logger.debug("Error checking if directory is git repository: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the current git branch name.
     */
    public String getCurrentBranch(Path projectRoot) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "branch", "--show-current");
            pb.directory(projectRoot.toFile());
            
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String branch = reader.readLine();
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Failed to get current branch");
                }
                
                return branch != null ? branch.trim() : "HEAD";
            }
            
        } catch (Exception e) {
            logger.warn("Error getting current branch: {}", e.getMessage());
            return "HEAD";
        }
    }

    private Set<Path> getGitDiffFiles(Path projectRoot, String... additionalArgs) throws IOException, InterruptedException {
        String[] baseCommand = {"git", "diff", "--name-only"};
        String[] fullCommand = Arrays.copyOf(baseCommand, baseCommand.length + additionalArgs.length);
        System.arraycopy(additionalArgs, 0, fullCommand, baseCommand.length, additionalArgs.length);
        
        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.directory(projectRoot.toFile());
        
        Process process = pb.start();
        
        Set<Path> changedFiles = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    Path filePath = projectRoot.resolve(Paths.get(line.trim()));
                    changedFiles.add(filePath);
                }
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = readProcessError(process);
            throw new RuntimeException("Git diff failed with exit code " + exitCode + ": " + error);
        }
        
        return changedFiles;
    }

    private Set<Path> getUntrackedFiles(Path projectRoot) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "ls-files", "--others", "--exclude-standard");
        pb.directory(projectRoot.toFile());
        
        Process process = pb.start();
        
        Set<Path> untrackedFiles = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    Path filePath = projectRoot.resolve(Paths.get(line.trim()));
                    untrackedFiles.add(filePath);
                }
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = readProcessError(process);
            throw new RuntimeException("Git ls-files failed with exit code " + exitCode + ": " + error);
        }
        
        return untrackedFiles;
    }

    private String readProcessError(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "Unable to read error stream";
        }
    }
}