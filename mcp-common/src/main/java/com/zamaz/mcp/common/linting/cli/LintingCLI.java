package com.zamaz.mcp.common.linting.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zamaz.mcp.common.linting.LintingConfiguration;
import com.zamaz.mcp.common.linting.LintingContext;
import com.zamaz.mcp.common.linting.LintingEngine;
import com.zamaz.mcp.common.linting.LintingResult;
import com.zamaz.mcp.common.linting.LintingSeverity;
import com.zamaz.mcp.common.linting.QualityThresholds;
import com.zamaz.mcp.common.linting.ReportFormat;
import com.zamaz.mcp.common.linting.impl.LintingEngineImpl;

/**
 * Command-line interface for the linting engine.
 */
public class LintingCLI {

    private static final Logger logger = LoggerFactory.getLogger(LintingCLI.class);

    private final LintingEngine lintingEngine;

    public LintingCLI() {
        this.lintingEngine = new LintingEngineImpl();
    }

    public static void main(String[] args) {
        LintingCLI cli = new LintingCLI();
        int exitCode = cli.run(args);
        System.exit(exitCode);
    }

    public int run(String[] args) {
        try {
            CLIOptions options = parseArguments(args);

            if (options.isHelp()) {
                printHelp();
                return 0;
            }

            if (options.isVersion()) {
                printVersion();
                return 0;
            }

            if (options.isListLinters()) {
                printAvailableLinters();
                return 0;
            }

            LintingContext context = createLintingContext(options);
            LintingResult result = executeLinting(options, context);

            printResults(result, options);

            return result.isSuccessful() ? 0 : 1;

        } catch (Exception e) {
            logger.error("Error running linting CLI", e);
            System.err.println("Error: " + e.getMessage());
            return 2;
        }
    }

    private CLIOptions parseArguments(String[] args) {
        CLIOptions options = new CLIOptions();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-h":
                case "--help":
                    options.setHelp(true);
                    break;

                case "-v":
                case "--version":
                    options.setVersion(true);
                    break;

                case "--list-linters":
                    options.setListLinters(true);
                    break;

                case "-p":
                case "--project":
                    if (i + 1 < args.length) {
                        options.setProjectPath(args[++i]);
                    }
                    break;

                case "-s":
                case "--service":
                    if (i + 1 < args.length) {
                        options.setServiceName(args[++i]);
                    }
                    break;

                case "-f":
                case "--files":
                    List<String> files = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        files.add(args[++i]);
                    }
                    options.setFiles(files);
                    break;

                case "--format":
                    if (i + 1 < args.length) {
                        options.setOutputFormat(args[++i]);
                    }
                    break;

                case "--output":
                    if (i + 1 < args.length) {
                        options.setOutputFile(args[++i]);
                    }
                    break;

                case "--parallel":
                    options.setParallel(true);
                    break;

                case "--no-parallel":
                    options.setParallel(false);
                    break;

                case "--auto-fix":
                    options.setAutoFix(true);
                    break;

                case "--exclude":
                    List<String> excludes = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        excludes.add(args[++i]);
                    }
                    options.setExcludePatterns(excludes);
                    break;

                case "--config":
                    if (i + 1 < args.length) {
                        options.setConfigFile(args[++i]);
                    }
                    break;

                case "--verbose":
                    options.setVerbose(true);
                    break;

                case "--quiet":
                    options.setQuiet(true);
                    break;

                default:
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    // Treat as project path if no other path specified
                    if (options.getProjectPath() == null) {
                        options.setProjectPath(arg);
                    }
                    break;
            }
        }

        // Set defaults
        if (options.getProjectPath() == null) {
            options.setProjectPath(".");
        }

        if (options.getOutputFormat() == null) {
            options.setOutputFormat("console");
        }

        return options;
    }

    private LintingContext createLintingContext(CLIOptions options) {
        Path projectRoot = Paths.get(options.getProjectPath()).toAbsolutePath();

        // Create default configuration
        LintingConfiguration config = LintingConfiguration.builder()
                .thresholds(new QualityThresholds())
                .parallelExecution(options.isParallel())
                .maxThreads(Runtime.getRuntime().availableProcessors())
                .build();

        return LintingContext.builder()
                .projectRoot(projectRoot)
                .configuration(config)
                .excludePatterns(options.getExcludePatterns())
                .parallelExecution(options.isParallel())
                .autoFix(options.isAutoFix())
                .build();
    }

    private LintingResult executeLinting(CLIOptions options, LintingContext context) {
        if (options.getServiceName() != null) {
            return lintingEngine.lintService(options.getServiceName(), context);
        } else if (options.getFiles() != null && !options.getFiles().isEmpty()) {
            List<Path> filePaths = options.getFiles().stream()
                    .map(Paths::get)
                    .collect(Collectors.toList());
            return lintingEngine.lintFiles(filePaths, context);
        } else {
            return lintingEngine.lintProject(context);
        }
    }

    private void printResults(LintingResult result, CLIOptions options) {
        ReportFormat format = parseReportFormat(options.getOutputFormat());
        String report = result.generateReport(format);

        if (options.getOutputFile() != null) {
            try {
                java.nio.file.Files.write(Paths.get(options.getOutputFile()), report.getBytes());
                if (!options.isQuiet()) {
                    System.out.println("Report written to: " + options.getOutputFile());
                }
            } catch (Exception e) {
                logger.error("Failed to write report to file", e);
                System.err.println("Failed to write report to file: " + e.getMessage());
            }
        } else {
            System.out.println(report);
        }

        if (!options.isQuiet()) {
            printSummary(result);
        }
    }

    private void printSummary(LintingResult result) {
        System.out.println();
        System.out.println("=== Summary ===");
        System.out.printf("Files processed: %d%n", result.getFilesProcessed());
        System.out.printf("Total issues: %d%n", result.getIssues().size());
        System.out.printf("Errors: %d%n", result.getIssuesBySeverity(LintingSeverity.ERROR).size());
        System.out.printf("Warnings: %d%n", result.getIssuesBySeverity(LintingSeverity.WARNING).size());
        System.out.printf("Duration: %dms%n", result.getDurationMs());
        System.out.printf("Status: %s%n", result.isSuccessful() ? "PASSED" : "FAILED");

        Object qualityScore = result.getMetrics().get("qualityScore");
        if (qualityScore instanceof Number) {
            System.out.printf("Quality Score: %.1f/100%n", ((Number) qualityScore).doubleValue());
        }
    }

    private ReportFormat parseReportFormat(String format) {
        switch (format.toLowerCase()) {
            case "json":
                return ReportFormat.JSON;
            case "html":
                return ReportFormat.HTML;
            case "xml":
                return ReportFormat.XML;
            case "markdown":
            case "md":
                return ReportFormat.MARKDOWN;
            case "console":
            default:
                return ReportFormat.CONSOLE;
        }
    }

    private void printHelp() {
        System.out.println("MCP Linting Tool");
        System.out.println();
        System.out.println("Usage: lint [OPTIONS] [PROJECT_PATH]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help              Show this help message");
        System.out.println("  -v, --version           Show version information");
        System.out.println("  --list-linters          List available linters");
        System.out.println("  -p, --project PATH      Project root path (default: current directory)");
        System.out.println("  -s, --service NAME      Lint specific service/module");
        System.out.println("  -f, --files FILE...     Lint specific files");
        System.out.println("  --format FORMAT         Output format (console, json, html, xml, markdown)");
        System.out.println("  --output FILE           Write report to file");
        System.out.println("  --parallel              Enable parallel execution (default)");
        System.out.println("  --no-parallel           Disable parallel execution");
        System.out.println("  --auto-fix              Automatically fix issues where possible");
        System.out.println("  --exclude PATTERN...    Exclude file patterns");
        System.out.println("  --config FILE           Configuration file path");
        System.out.println("  --verbose               Verbose output");
        System.out.println("  --quiet                 Quiet output (errors only)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  lint                    # Lint current project");
        System.out.println("  lint /path/to/project   # Lint specific project");
        System.out.println("  lint -s mcp-llm         # Lint specific service");
        System.out.println("  lint -f src/main.java   # Lint specific file");
        System.out.println("  lint --format json      # Output JSON report");
        System.out.println("  lint --auto-fix         # Fix issues automatically");
    }

    private void printVersion() {
        System.out.println("MCP Linting Tool v1.0.0");
        System.out.println("Built with Java " + System.getProperty("java.version"));
    }

    private void printAvailableLinters() {
        System.out.println("Available Linters:");
        System.out.println();

        Map<String, List<String>> linters = Map.of(
                "Java", Arrays.asList("Checkstyle", "SpotBugs", "PMD"),
                "TypeScript/JavaScript", Arrays.asList("ESLint", "Prettier"),
                "YAML", Arrays.asList("yamllint"),
                "JSON", Arrays.asList("JSON Schema Validator"),
                "Markdown", Arrays.asList("markdownlint", "Link Checker"),
                "Docker", Arrays.asList("hadolint"));

        linters.forEach((fileType, linterList) -> {
            System.out.printf("  %s:%n", fileType);
            linterList.forEach(linter -> System.out.printf("    - %s%n", linter));
            System.out.println();
        });
    }

    private static class CLIOptions {
        private boolean help = false;
        private boolean version = false;
        private boolean listLinters = false;
        private String projectPath;
        private String serviceName;
        private List<String> files;
        private String outputFormat;
        private String outputFile;
        private boolean parallel = true;
        private boolean autoFix = false;
        private List<String> excludePatterns = new ArrayList<>();
        private String configFile;
        private boolean verbose = false;
        private boolean quiet = false;

        // Getters and setters
        public boolean isHelp() {
            return help;
        }

        public void setHelp(boolean help) {
            this.help = help;
        }

        public boolean isVersion() {
            return version;
        }

        public void setVersion(boolean version) {
            this.version = version;
        }

        public boolean isListLinters() {
            return listLinters;
        }

        public void setListLinters(boolean listLinters) {
            this.listLinters = listLinters;
        }

        public String getProjectPath() {
            return projectPath;
        }

        public void setProjectPath(String projectPath) {
            this.projectPath = projectPath;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public List<String> getFiles() {
            return files;
        }

        public void setFiles(List<String> files) {
            this.files = files;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public void setOutputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
        }

        public String getOutputFile() {
            return outputFile;
        }

        public void setOutputFile(String outputFile) {
            this.outputFile = outputFile;
        }

        public boolean isParallel() {
            return parallel;
        }

        public void setParallel(boolean parallel) {
            this.parallel = parallel;
        }

        public boolean isAutoFix() {
            return autoFix;
        }

        public void setAutoFix(boolean autoFix) {
            this.autoFix = autoFix;
        }

        public List<String> getExcludePatterns() {
            return excludePatterns;
        }

        public void setExcludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
        }

        public String getConfigFile() {
            return configFile;
        }

        public void setConfigFile(String configFile) {
            this.configFile = configFile;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean isQuiet() {
            return quiet;
        }

        public void setQuiet(boolean quiet) {
            this.quiet = quiet;
        }
    }
}
