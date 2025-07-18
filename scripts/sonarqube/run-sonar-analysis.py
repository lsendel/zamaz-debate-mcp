#!/usr/bin/env python3
"""
Comprehensive SonarQube Analysis Runner
Generates detailed reports and automatically fixes issues
"""

import argparse
import json
import logging
import sys
from datetime import datetime
from pathlib import Path

# Add the current directory to path to import our modules
sys.path.append(str(Path(__file__).parent))

import importlib.util


# Load modules with hyphenated names
def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


# Load our modules
automated_report_generator = load_module(
    "automated_report_generator", str(Path(__file__).parent / "automated-report-generator.py")
)
issue_resolver = load_module("issue_resolver", str(Path(__file__).parent / "issue-resolver.py"))

SonarQubeReportGenerator = automated_report_generator.SonarQubeReportGenerator
IssueResolver = issue_resolver.IssueResolver
Issue = issue_resolver.Issue

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


class SonarQubeAnalysisRunner:
    """Complete SonarQube analysis with reporting and issue resolution"""

    def __init__(self, config_file: str = "sonarqube_config.yaml"):
        self.report_generator = SonarQubeReportGenerator(config_file)
        self.issue_resolver = IssueResolver()
        self.config = self.report_generator.config

    def run_full_analysis(self, fix_issues: bool = True) -> dict:
        """Run complete SonarQube analysis with reporting and fixing"""
        logger.info("Starting comprehensive SonarQube analysis...")

        results = {
            "timestamp": datetime.now().isoformat(),
            "project_key": self.report_generator.sonar_config.project_key,
            "reports_generated": {},
            "issues_analyzed": 0,
            "issues_fixed": 0,
            "fix_results": {},
            "summary": {},
        }

        # Step 1: Generate comprehensive reports
        logger.info("Generating SonarQube reports...")
        report_files = self.report_generator.generate_full_report()
        results["reports_generated"] = report_files

        # Step 2: Collect detailed issue information
        logger.info("Collecting detailed issue information...")
        sonar_data = self.report_generator.collect_project_metrics()

        # Step 3: Parse issues for resolution
        issues = self._parse_issues_from_data(sonar_data)
        results["issues_analyzed"] = len(issues)

        # Step 4: Attempt to fix issues automatically
        if fix_issues and issues:
            logger.info(f"Attempting to fix {len(issues)} issues...")
            fix_results = self.issue_resolver.resolve_issues(issues)
            results["fix_results"] = fix_results
            results["issues_fixed"] = fix_results["summary"]["fixed_count"]

            # Step 5: Generate post-fix report if fixes were applied
            if fix_results["summary"]["fixed_count"] > 0:
                logger.info("Generating post-fix report...")
                post_fix_data = self.report_generator.collect_project_metrics()
                post_fix_report = self.report_generator.generate_markdown_report(post_fix_data)

                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                post_fix_file = self.report_generator.save_report(post_fix_report, "md", f"post_fix_{timestamp}")
                results["reports_generated"]["post_fix_markdown"] = post_fix_file

        # Step 6: Generate summary
        results["summary"] = self._generate_analysis_summary(results)

        logger.info("SonarQube analysis completed")
        return results

    def _parse_issues_from_data(self, sonar_data: dict) -> list:
        """Parse issues from SonarQube data"""
        issues = []

        # Parse regular issues
        for issue_data in sonar_data.get("issues", {}).get("issues", []):
            issue = Issue.from_sonar_data(issue_data, self.report_generator.sonar_config.project_key)
            issues.append(issue)

        # Parse security hotspots as issues
        for hotspot_data in sonar_data.get("hotspots", {}).get("hotspots", []):
            issue = Issue(
                key=hotspot_data.get("key", ""),
                component=hotspot_data.get("component", "").replace(
                    self.report_generator.sonar_config.project_key + ":", ""
                ),
                line=hotspot_data.get("line", 0),
                message=hotspot_data.get("message", ""),
                rule=hotspot_data.get("ruleKey", ""),
                severity="MAJOR",  # Default severity for hotspots
                type="SECURITY_HOTSPOT",
                file_path=hotspot_data.get("component", "").replace(
                    self.report_generator.sonar_config.project_key + ":", ""
                ),
            )
            issues.append(issue)

        return issues

    def _generate_analysis_summary(self, results: dict) -> dict:
        """Generate analysis summary"""
        return {
            "total_issues_found": results["issues_analyzed"],
            "issues_fixed_automatically": results["issues_fixed"],
            "fix_success_rate": results["issues_fixed"] / results["issues_analyzed"]
            if results["issues_analyzed"] > 0
            else 0,
            "reports_generated": len(results["reports_generated"]),
            "analysis_duration": "N/A",  # Could be calculated
            "recommendations": self._generate_recommendations(results),
        }

    def _generate_recommendations(self, results: dict) -> list:
        """Generate recommendations based on analysis results"""
        recommendations = []

        fix_results = results.get("fix_results", {})

        if fix_results.get("summary", {}).get("fixed_count", 0) > 0:
            recommendations.append("✅ Some issues were automatically fixed. Please review the changes.")

        if fix_results.get("summary", {}).get("skipped_count", 0) > 0:
            recommendations.append("⚠️ Some issues require manual attention. See the detailed report for specifics.")

        if fix_results.get("summary", {}).get("error_count", 0) > 0:
            recommendations.append("❌ Some issues couldn't be processed due to errors. Check the logs.")

        if results["issues_analyzed"] == 0:
            recommendations.append("🎉 No issues found! Your code quality is excellent.")

        return recommendations

    def generate_detailed_report(self) -> str:
        """Generate a detailed analysis report"""
        logger.info("Generating detailed SonarQube report...")

        # Get the latest report data
        sonar_data = self.report_generator.collect_project_metrics()

        # Generate enhanced markdown report
        report = self.report_generator.generate_markdown_report(sonar_data)

        # Add additional sections
        report += "\n\n## 🔧 Automated Issue Resolution\n\n"

        if self.config.get("issue_resolution", {}).get("enabled", False):
            report += "Automated issue resolution is **enabled**. The following rules can be automatically fixed:\n\n"

            auto_fix_rules = self.config.get("issue_resolution", {}).get("auto_fix_rules", [])
            for rule in auto_fix_rules:
                report += f"- `{rule}`\n"

            report += "\n**Note**: Issues are automatically fixed when running with `--fix-issues` flag.\n"
        else:
            report += "Automated issue resolution is **disabled**. Enable it in the configuration to automatically fix common issues.\n"

        # Add configuration summary
        report += "\n\n## ⚙️ Configuration Summary\n\n"
        report += f"- **Project**: {self.report_generator.sonar_config.project_key}\n"
        report += f"- **Organization**: {self.report_generator.sonar_config.organization}\n"
        report += f"- **Branch**: {self.report_generator.sonar_config.branch}\n"
        report += f"- **Report Formats**: {', '.join(self.report_generator.report_config.formats)}\n"
        report += (
            f"- **Trend Analysis**: {'Enabled' if self.report_generator.report_config.include_trends else 'Disabled'}\n"
        )
        report += f"- **Security Analysis**: {'Enabled' if self.report_generator.report_config.include_security_analysis else 'Disabled'}\n"

        return report

    def save_analysis_results(self, results: dict, output_file: str | None = None) -> str:
        """Save analysis results to file"""
        if not output_file:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"sonar_analysis_results_{timestamp}.json"

        output_path = Path(self.report_generator.report_config.output_dir) / output_file

        with open(output_path, "w") as f:
            json.dump(results, f, indent=2, default=str)

        logger.info(f"Analysis results saved to: {output_path}")
        return str(output_path)

    def print_summary(self, results: dict):
        """Print analysis summary to console"""
        summary = results["summary"]

        print("\n" + "=" * 60)
        print("📊 SONARQUBE ANALYSIS SUMMARY")
        print("=" * 60)
        print(f"🎯 Project: {results['project_key']}")
        print(f"📅 Timestamp: {results['timestamp']}")
        print(f"🔍 Issues Found: {summary['total_issues_found']}")
        print(f"✅ Issues Fixed: {summary['issues_fixed_automatically']}")
        print(f"📈 Fix Success Rate: {summary['fix_success_rate']:.1%}")
        print(f"📋 Reports Generated: {summary['reports_generated']}")

        print("\n📝 RECOMMENDATIONS:")
        for rec in summary["recommendations"]:
            print(f"  {rec}")

        print("\n📁 GENERATED REPORTS:")
        for format_type, file_path in results["reports_generated"].items():
            print(f"  {format_type}: {file_path}")

        print("\n" + "=" * 60)


def main():
    """Main function"""
    parser = argparse.ArgumentParser(description="Comprehensive SonarQube Analysis")
    parser.add_argument("--config", default="sonarqube_config.yaml", help="Configuration file path")
    parser.add_argument("--fix-issues", action="store_true", help="Automatically fix issues when possible")
    parser.add_argument("--output", help="Output file for analysis results")
    parser.add_argument("--detailed-report", action="store_true", help="Generate detailed report only")
    parser.add_argument("--quiet", action="store_true", help="Suppress console output")

    args = parser.parse_args()

    if args.quiet:
        logging.getLogger().setLevel(logging.WARNING)

    try:
        runner = SonarQubeAnalysisRunner(args.config)

        if args.detailed_report:
            # Generate detailed report only
            report = runner.generate_detailed_report()
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"detailed_sonar_report_{timestamp}.md"
            output_path = Path(runner.report_generator.report_config.output_dir) / output_file

            with open(output_path, "w") as f:
                f.write(report)

            print(f"Detailed report generated: {output_path}")
        else:
            # Run full analysis
            results = runner.run_full_analysis(args.fix_issues)

            # Save results
            results_file = runner.save_analysis_results(results, args.output)

            # Print summary
            if not args.quiet:
                runner.print_summary(results)

            print(f"\nAnalysis complete! Results saved to: {results_file}")

    except Exception as e:
        logger.error(f"Analysis failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
