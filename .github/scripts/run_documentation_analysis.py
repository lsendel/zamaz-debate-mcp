#!/usr/bin/env python3
"""
Simple runner script for the documentation analysis system.
This script provides an easy way to run the analysis with various options.
"""

import argparse
import logging
import os
import sys
from pathlib import Path

# Add the current directory to the Python path
sys.path.insert(0, os.path.dirname(__file__))

from analyzers.documentation_analyzer import DocumentationAnalysisSystem


def setup_logging(verbose: bool = False):
    """Set up logging configuration."""
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[logging.StreamHandler(sys.stdout), logging.FileHandler("documentation_analysis.log")],
    )


def main():
    """Main function."""
    parser = argparse.ArgumentParser(
        description="Run comprehensive documentation analysis",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Basic analysis
  python run_documentation_analysis.py /path/to/project

  # Analysis with custom output directory
  python run_documentation_analysis.py /path/to/project -o ./my_analysis

  # Generate only HTML report
  python run_documentation_analysis.py /path/to/project --format html

  # Search documentation
  python run_documentation_analysis.py /path/to/project --search "API endpoints"

  # Verbose output
  python run_documentation_analysis.py /path/to/project -v
        """,
    )

    parser.add_argument("project_path", help="Path to the project to analyze")

    parser.add_argument(
        "--output-dir",
        "-o",
        default="./documentation_analysis_output",
        help="Output directory for analysis results (default: ./documentation_analysis_output)",
    )

    parser.add_argument(
        "--format",
        "-f",
        choices=["json", "markdown", "html", "all"],
        default="all",
        help="Output format (default: all)",
    )

    parser.add_argument("--search", "-s", help="Search query to test documentation search functionality")

    parser.add_argument("--config", help="Path to configuration file (optional)")

    parser.add_argument("--verbose", "-v", action="store_true", help="Enable verbose output")

    parser.add_argument("--quick", action="store_true", help="Run quick analysis (skip some time-consuming tasks)")

    parser.add_argument(
        "--docs-only", action="store_true", help="Analyze only documentation files (skip code analysis)"
    )

    parser.add_argument("--code-only", action="store_true", help="Analyze only code files (skip documentation parsing)")

    args = parser.parse_args()

    # Set up logging
    setup_logging(args.verbose)
    logger = logging.getLogger(__name__)

    logger.info("Starting documentation analysis...")
    logger.info(f"Project path: {args.project_path}")
    logger.info(f"Output directory: {args.output_dir}")

    # Validate project path
    project_path = Path(args.project_path)
    if not project_path.exists():
        logger.error(f"Project path does not exist: {project_path}")
        sys.exit(1)

    if not project_path.is_dir():
        logger.error(f"Project path is not a directory: {project_path}")
        sys.exit(1)

    # Create output directory
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    try:
        # Initialize the analysis system
        logger.info("Initializing documentation analysis system...")
        system = DocumentationAnalysisSystem()

        # Run the analysis
        logger.info("Running comprehensive analysis...")
        results = system.analyze_project(str(project_path))

        # Export results
        logger.info("Exporting results...")

        if args.format in ["json", "all"]:
            json_path = output_dir / "documentation_analysis.json"
            system.export_analysis(results, str(json_path))
            logger.info(f"JSON results exported to: {json_path}")

        if args.format in ["markdown", "all"]:
            report_path = output_dir / "documentation_analysis_report.md"
            report_content = system.generate_report(results)
            with open(report_path, "w", encoding="utf-8") as f:
                f.write(report_content)
            logger.info(f"Markdown report exported to: {report_path}")

        if args.format in ["html", "all"]:
            html_path = output_dir / "documentation_analysis_report.html"
            html_content = generate_html_report(results)
            with open(html_path, "w", encoding="utf-8") as f:
                f.write(html_content)
            logger.info(f"HTML report exported to: {html_path}")

        # Search functionality
        if args.search:
            logger.info(f"Searching for: {args.search}")
            search_results = system.search_documentation(args.search)

            if search_results:
                for _i, (_doc, _score) in enumerate(search_results, 1):
                    pass
            else:
                pass

        # Print summary
        metrics = results.get("metrics", {})

        # Format distribution
        formats = metrics.get("formats", {})
        if formats:
            for _fmt, _count in formats.items():
                pass

        # Top issues
        issues = results.get("issues", [])
        if issues:
            for issue in issues[:5]:  # Show top 5
                if issue.get("suggestion"):
                    pass

        # Recommendations
        recommendations = results.get("recommendations", {})
        if recommendations:
            for category in ["high_priority", "medium_priority"]:
                if recommendations.get(category):
                    for _rec in recommendations[category][:3]:  # Show top 3
                        pass

        logger.info("Analysis completed successfully!")

    except KeyboardInterrupt:
        logger.info("Analysis interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Analysis failed: {e}")
        if args.verbose:
            import traceback

            traceback.print_exc()
        sys.exit(1)


def generate_html_report(results: dict) -> str:
    """Generate a simple HTML report."""
    metrics = results.get("metrics", {})
    issues = results.get("issues", [])

    html = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Documentation Analysis Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        .header {{ background-color: #f8f9fa; padding: 20px; border-radius: 5px; }}
        .metric {{ display: inline-block; margin: 10px; padding: 10px; background-color: #e9ecef; border-radius: 3px; }}
        .issue {{ margin: 10px 0; padding: 10px; border-left: 4px solid #dc3545; background-color: #f8d7da; }}
        .issue.warning {{ border-left-color: #ffc107; background-color: #fff3cd; }}
        .issue.info {{ border-left-color: #17a2b8; background-color: #d1ecf1; }}
        table {{ border-collapse: collapse; width: 100%; margin: 20px 0; }}
        th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
        th {{ background-color: #f2f2f2; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>Documentation Analysis Report</h1>
        <p>Generated: {results.get("timestamp", "Unknown")}</p>
        <p>Project: {results.get("project_path", "Unknown")}</p>
    </div>

    <h2>Summary Metrics</h2>
    <div class="metric">Total Files: {metrics.get("total_files", 0)}</div>
    <div class="metric">Total Words: {metrics.get("total_words", 0):,}</div>
    <div class="metric">Quality Score: {metrics.get("quality_score", 0):.1f}/100</div>
    <div class="metric">Coverage: {metrics.get("coverage_percentage", 0):.1f}%</div>

    <h2>Issues Found ({len(issues)})</h2>
    """

    for issue in issues:
        severity_class = issue.get("severity", "info")
        html += f"""
        <div class="issue {severity_class}">
            <strong>{issue.get("severity", "info").upper()}</strong>: {issue.get("message", "No message")}
            <br>File: {issue.get("file_path", "Unknown")}
            {f"<br>Suggestion: {issue['suggestion']}" if issue.get("suggestion") else ""}
        </div>
        """

    # Format distribution
    formats = metrics.get("formats", {})
    if formats:
        html += "<h2>Format Distribution</h2><table><tr><th>Format</th><th>Count</th></tr>"
        for fmt, count in formats.items():
            html += f"<tr><td>{fmt.title()}</td><td>{count}</td></tr>"
        html += "</table>"

    html += """
</body>
</html>
    """

    return html


if __name__ == "__main__":
    main()
