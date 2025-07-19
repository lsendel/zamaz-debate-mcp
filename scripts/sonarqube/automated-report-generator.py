#!/usr/bin/env python3
"""
Automated SonarQube Report Generator
Generates comprehensive SonarQube reports with scheduling and notifications
"""

import argparse
import json
import logging
import os
import smtplib
import time
from dataclasses import dataclass
from datetime import datetime
from email import encoders
from email.mime.base import MIMEBase
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from pathlib import Path
from typing import Any

import requests
import schedule
import yaml

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.FileHandler("sonarqube_reports.log"), logging.StreamHandler()],
)
logger = logging.getLogger(__name__)


@dataclass
class SonarQubeConfig:
    """Configuration for SonarQube connection"""

    url: str
    token: str
    project_key: str
    organization: str
    branch: str = "main"


@dataclass
class ReportConfig:
    """Configuration for report generation"""

    output_dir: str
    formats: list[str]  # ['markdown', 'html', 'json', 'pdf']
    email_notifications: bool
    slack_notifications: bool
    include_trends: bool
    include_security_analysis: bool
    include_coverage_analysis: bool
    include_detailed_issues: bool = True
    max_issues_per_severity: int = 20
    max_hotspots_per_category: int = 10


@dataclass
class NotificationConfig:
    """Configuration for notifications"""

    email_smtp_server: str
    email_smtp_port: int
    email_username: str
    email_password: str
    email_recipients: list[str]
    slack_webhook_url: str
    slack_channel: str


class SonarQubeReportGenerator:
    """Automated SonarQube report generator"""

    def __init__(self, config_file: str = "sonarqube_config.yaml"):
        self.config = self._load_config(config_file)
        self.sonar_config = SonarQubeConfig(**self.config["sonarqube"])
        self.report_config = ReportConfig(**self.config["reporting"])
        self.notification_config = NotificationConfig(**self.config["notifications"])

        # Create output directory
        Path(self.report_config.output_dir).mkdir(parents=True, exist_ok=True)

        # Historical data storage
        self.historical_data_file = Path(self.report_config.output_dir) / "historical_data.json"
        self.historical_data = self._load_historical_data()

    def _load_config(self, config_file: str) -> dict[str, Any]:
        """Load configuration from YAML file"""
        try:
            with Path(config_file).open() as f:
                return yaml.safe_load(f)
        except FileNotFoundError:
            logger.error(f"Configuration file {config_file} not found")
            return self._get_default_config()

    def _get_default_config(self) -> dict[str, Any]:
        """Get default configuration"""
        return {
            "sonarqube": {
                "url": os.getenv("SONAR_URL", "https://sonarcloud.io"),
                "token": os.getenv("SONAR_TOKEN", ""),
                "project_key": os.getenv("SONAR_PROJECT_KEY", ""),
                "organization": os.getenv("SONAR_ORGANIZATION", ""),
                "branch": os.getenv("SONAR_BRANCH", "main"),
            },
            "reporting": {
                "output_dir": "sonar-reports",
                "formats": ["markdown", "html", "json"],
                "email_notifications": False,
                "slack_notifications": False,
                "include_trends": True,
                "include_security_analysis": True,
                "include_coverage_analysis": True,
            },
            "notifications": {
                "email_smtp_server": "smtp.gmail.com",
                "email_smtp_port": 587,
                "email_username": "",
                "email_password": "",
                "email_recipients": [],
                "slack_webhook_url": "",
                "slack_channel": "#development",
            },
        }

    def _load_historical_data(self) -> dict[str, Any]:
        """Load historical data for trend analysis"""
        if self.historical_data_file.exists():
            try:
                with self.historical_data_file.open() as f:
                    return json.load(f)
            except Exception as e:
                logger.error(f"Error loading historical data: {e}")
        return {"reports": []}

    def _save_historical_data(self, data: dict[str, Any]):
        """Save historical data"""
        self.historical_data["reports"].append(data)
        # Keep only last 100 reports
        self.historical_data["reports"] = self.historical_data["reports"][-100:]

        with self.historical_data_file.open("w") as f:
            json.dump(self.historical_data, f, indent=2)

    def _make_sonar_request(self, endpoint: str, params: dict[str, Any] | None = None) -> dict[str, Any]:
        """Make request to SonarQube API"""
        url = f"{self.sonar_config.url}/api/{endpoint}"
        headers = {"Authorization": f"Bearer {self.sonar_config.token}", "Content-Type": "application/json"}

        default_params = {"component": self.sonar_config.project_key, "branch": self.sonar_config.branch}

        if params:
            default_params.update(params)

        try:
            response = requests.get(url, headers=headers, params=default_params, timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"Error making SonarQube request: {e}")
            return {}

    def collect_project_metrics(self) -> dict[str, Any]:
        """Collect project metrics from SonarQube"""
        logger.info("Collecting project metrics...")

        # Get project measures
        measures = self._make_sonar_request(
            "measures/component",
            {
                "metricKeys": ",".join(
                    [
                        "bugs",
                        "vulnerabilities",
                        "security_hotspots",
                        "code_smells",
                        "coverage",
                        "duplicated_lines_density",
                        "ncloc",
                        "sqale_index",
                        "reliability_rating",
                        "security_rating",
                        "sqale_rating",
                        "alert_status",
                        "quality_gate_details",
                    ]
                )
            },
        )

        # Get issues
        issues = self._make_sonar_request(
            "issues/search", {"componentKeys": self.sonar_config.project_key, "resolved": "false", "ps": 500}
        )

        # Get security hotspots
        hotspots = self._make_sonar_request(
            "hotspots/search", {"projectKey": self.sonar_config.project_key, "status": "TO_REVIEW", "ps": 500}
        )

        # Get quality gate status
        quality_gate = self._make_sonar_request(
            "qualitygates/project_status",
            {"projectKey": self.sonar_config.project_key, "branch": self.sonar_config.branch},
        )

        return {
            "timestamp": datetime.now().isoformat(),
            "measures": measures,
            "issues": issues,
            "hotspots": hotspots,
            "quality_gate": quality_gate,
            "project_key": self.sonar_config.project_key,
            "branch": self.sonar_config.branch,
        }

    def generate_markdown_report(self, data: dict[str, Any]) -> str:
        """Generate markdown report"""
        logger.info("Generating markdown report...")

        measures = data.get("measures", {}).get("component", {}).get("measures", [])
        issues = data.get("issues", {}).get("issues", [])
        hotspots = data.get("hotspots", {}).get("hotspots", [])
        quality_gate = data.get("quality_gate", {}).get("projectStatus", {})

        # Extract key metrics
        metrics = {m["metric"]: m.get("value", "N/A") for m in measures}

        # Generate report
        report = f"""# SonarQube Analysis Report

**Project**: {data["project_key"]}
**Branch**: {data["branch"]}
**Generated**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
**Quality Gate**: {"✅ PASSED" if quality_gate.get("status") == "OK" else "❌ FAILED"}

## 📊 Key Metrics

| Metric | Value | Rating |
|--------|-------|--------|
| **Bugs** | {metrics.get("bugs", "N/A")} | {self._get_rating_emoji(metrics.get("reliability_rating", "N/A"))} |
| **Vulnerabilities** | {metrics.get("vulnerabilities", "N/A")} | {self._get_rating_emoji(metrics.get("security_rating", "N/A"))} |
| **Security Hotspots** | {metrics.get("security_hotspots", "N/A")} | - |
| **Code Smells** | {metrics.get("code_smells", "N/A")} | {self._get_rating_emoji(metrics.get("sqale_rating", "N/A"))} |
| **Coverage** | {metrics.get("coverage", "N/A")}% | - |
| **Duplications** | {metrics.get("duplicated_lines_density", "N/A")}% | - |
| **Technical Debt** | {self._format_technical_debt(metrics.get("sqale_index", "N/A"))} | - |
| **Lines of Code** | {metrics.get("ncloc", "N/A")} | - |

## 🐛 Issues Summary

**Total Issues**: {len(issues)}

### By Severity
{self._generate_issues_by_severity(issues)}

### By Type
{self._generate_issues_by_type(issues)}

## 🔥 Security Hotspots

**Total Hotspots**: {len(hotspots)}

{self._generate_hotspots_summary(hotspots)}

## 🚨 Quality Gate Details

{self._generate_quality_gate_details(quality_gate)}

## 📈 Trends

{self._generate_trends_analysis(data) if self.report_config.include_trends else "Trend analysis disabled"}

## 🔗 Links

- [View Full Analysis on SonarCloud]({self.sonar_config.url}/dashboard?id={self.sonar_config.project_key})
- [Security Hotspots]({self.sonar_config.url}/project/security_hotspots?id={self.sonar_config.project_key})
- [Issues]({self.sonar_config.url}/project/issues?id={self.sonar_config.project_key})

---

*Generated by Automated SonarQube Report Generator*
"""

        return report

    def _get_rating_emoji(self, rating: str) -> str:
        """Get emoji for rating"""
        rating_map = {"1.0": "🟢 A", "2.0": "🟡 B", "3.0": "🟠 C", "4.0": "🔴 D", "5.0": "🔴 E"}
        return rating_map.get(str(rating), "❓")

    def _format_technical_debt(self, minutes: str) -> str:
        """Format technical debt from minutes"""
        if minutes == "N/A":
            return "N/A"

        try:
            total_minutes = int(minutes)
            hours = total_minutes // 60
            days = hours // 8
            remaining_hours = hours % 8

            if days > 0:
                return f"{days}d {remaining_hours}h"
            elif hours > 0:
                return f"{hours}h {total_minutes % 60}m"
            else:
                return f"{total_minutes}m"
        except (ValueError, TypeError) as e:
            logger.warning(f"Failed to format technical debt: {e}")
            return minutes

    def _generate_issues_by_severity(self, issues: list[dict[str, Any]]) -> str:
        """Generate issues breakdown by severity with specific details"""
        severity_groups = {}
        for issue in issues:
            severity = issue.get("severity", "UNKNOWN")
            if severity not in severity_groups:
                severity_groups[severity] = []
            severity_groups[severity].append(issue)

        if not severity_groups:
            return "No issues found"

        result = []
        for severity in ["BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO"]:
            if severity in severity_groups:
                count = len(severity_groups[severity])
                result.append(f"\n### {severity} Issues ({count})")

                for issue in severity_groups[severity][:10]:  # Show first 10 issues
                    component = issue.get("component", "").replace(self.sonar_config.project_key + ":", "")
                    line = issue.get("line", "N/A")
                    message = issue.get("message", "No message")
                    rule = issue.get("rule", "Unknown rule")

                    result.append(f"- **{component}:{line}** - {message}")
                    result.append(f"  - Rule: `{rule}`")
                    result.append(f"  - Type: {issue.get('type', 'N/A')}")

                    # Add link to SonarCloud
                    if "key" in issue:
                        result.append(
                            f"  - [View Issue]({self.sonar_config.url}/project/issues?id={self.sonar_config.project_key}&open={issue['key']})"
                        )
                    result.append("")

                if len(severity_groups[severity]) > 10:
                    result.append(f"... and {len(severity_groups[severity]) - 10} more issues")

        return "\n".join(result) if result else "No issues found"

    def _generate_issues_by_type(self, issues: list[dict[str, Any]]) -> str:
        """Generate issues breakdown by type"""
        type_counts = {}
        for issue in issues:
            issue_type = issue.get("type", "UNKNOWN")
            type_counts[issue_type] = type_counts.get(issue_type, 0) + 1

        if not type_counts:
            return "No issues found"

        result = []
        for issue_type, count in type_counts.items():
            result.append(f"- **{issue_type}**: {count}")

        return "\n".join(result) if result else "No issues found"

    def _generate_hotspots_summary(self, hotspots: list[dict[str, Any]]) -> str:
        """Generate security hotspots summary with detailed information"""
        if not hotspots:
            return "No security hotspots found"

        # Group by category
        category_groups = {}
        for hotspot in hotspots:
            category = hotspot.get("securityCategory", "UNKNOWN")
            if category not in category_groups:
                category_groups[category] = []
            category_groups[category].append(hotspot)

        result = []
        for category, hotspot_list in category_groups.items():
            result.append(f"\n### {category} ({len(hotspot_list)} hotspots)")

            for hotspot in hotspot_list[:5]:  # Show first 5 hotspots per category
                component = hotspot.get("component", "").replace(self.sonar_config.project_key + ":", "")
                line = hotspot.get("line", "N/A")
                message = hotspot.get("message", "No message")
                rule = hotspot.get("ruleKey", "Unknown rule")
                status = hotspot.get("status", "TO_REVIEW")

                result.append(f"- **{component}:{line}** - {message}")
                result.append(f"  - Rule: `{rule}`")
                result.append(f"  - Status: {status}")
                result.append(f"  - Vulnerability Probability: {hotspot.get('vulnerabilityProbability', 'N/A')}")

                # Add link to SonarCloud
                if "key" in hotspot:
                    result.append(
                        f"  - [Review Hotspot]({self.sonar_config.url}/project/security_hotspots?id={self.sonar_config.project_key}&hotspots={hotspot['key']})"
                    )
                result.append("")

            if len(hotspot_list) > 5:
                result.append(f"... and {len(hotspot_list) - 5} more hotspots")

        return "\n".join(result)

    def _generate_quality_gate_details(self, quality_gate: dict[str, Any]) -> str:
        """Generate quality gate details"""
        status = quality_gate.get("status", "UNKNOWN")
        conditions = quality_gate.get("conditions", [])

        if not conditions:
            return f"Status: {status}"

        result = [f"**Status**: {status}\n"]
        result.append("### Conditions")

        for condition in conditions:
            metric = condition.get("metricKey", "Unknown")
            status = condition.get("status", "Unknown")
            threshold = condition.get("errorThreshold", "N/A")
            actual = condition.get("actualValue", "N/A")

            emoji = "✅" if status == "OK" else "❌"
            result.append(f"- {emoji} **{metric}**: {actual} (threshold: {threshold})")

        return "\n".join(result)

    def _generate_trends_analysis(self, current_data: dict[str, Any]) -> str:
        """Generate trends analysis"""
        if len(self.historical_data["reports"]) < 2:
            return "Not enough historical data for trend analysis"

        # Compare with previous report
        previous_data = self.historical_data["reports"][-1]

        # Extract current metrics
        current_metrics = {
            m["metric"]: m.get("value", "N/A")
            for m in current_data.get("measures", {}).get("component", {}).get("measures", [])
        }

        # Extract previous metrics
        previous_metrics = {
            m["metric"]: m.get("value", "N/A")
            for m in previous_data.get("measures", {}).get("component", {}).get("measures", [])
        }

        trends = []

        # Compare key metrics
        for metric in ["bugs", "vulnerabilities", "code_smells", "coverage", "ncloc"]:
            current_val = current_metrics.get(metric, "N/A")
            previous_val = previous_metrics.get(metric, "N/A")

            try:
                curr = float(current_val)
                prev = float(previous_val)

                if curr > prev:
                    trend = f"📈 +{curr - prev:.1f}"
                elif curr < prev:
                    trend = f"📉 {curr - prev:.1f}"
                else:
                    trend = "➡️ No change"

                trends.append(f"- **{metric}**: {curr} {trend}")
            except (ValueError, TypeError):
                trends.append(f"- **{metric}**: {current_val} (no comparison available)")

        return "\n".join(trends)

    def generate_html_report(self, data: dict[str, Any]) -> str:
        """Generate HTML report"""
        logger.info("Generating HTML report...")

        # Convert markdown to HTML (simplified)
        markdown_content = self.generate_markdown_report(data)

        html_template = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>SonarQube Analysis Report</title>
            <style>
                body {{ font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }}
                table {{ border-collapse: collapse; width: 100%; margin: 20px 0; }}
                th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
                th {{ background-color: #f2f2f2; }}
                .passed {{ color: green; }}
                .failed {{ color: red; }}
                .metric {{ font-weight: bold; }}
                .trend-up {{ color: red; }}
                .trend-down {{ color: green; }}
                .trend-stable {{ color: blue; }}
            </style>
        </head>
        <body>
            <div id="report-content">
                {self._markdown_to_html(markdown_content)}
            </div>
            <script>
                // Add interactive features
                document.addEventListener('DOMContentLoaded', function() {{
                    console.log('SonarQube Report loaded');
                }});
            </script>
        </body>
        </html>
        """

        return html_template

    def _markdown_to_html(self, markdown: str) -> str:
        """Simple markdown to HTML conversion"""
        html = markdown

        # Headers
        html = html.replace("# ", "<h1>").replace("\n", "</h1>\n", 1)
        html = html.replace("## ", "<h2>").replace("\n", "</h2>\n", 1)
        html = html.replace("### ", "<h3>").replace("\n", "</h3>\n", 1)

        # Bold
        html = html.replace("**", "<strong>", 1).replace("**", "</strong>", 1)

        # Links
        import re

        html = re.sub(r"\\[([^\\]]+)\\]\\(([^)]+)\\)", r'<a href="\\2">\\1</a>', html)

        # Lists
        html = re.sub(r"^- (.+)$", r"<li>\\1</li>", html, flags=re.MULTILINE)
        html = re.sub(r"(<li>.*</li>)", r"<ul>\\1</ul>", html, flags=re.DOTALL)

        # Tables (simplified)
        lines = html.split("\n")
        in_table = False
        table_html = []

        for line in lines:
            if "|" in line and not line.startswith("|--"):
                if not in_table:
                    table_html.append("<table>")
                    in_table = True

                cells = [cell.strip() for cell in line.split("|")[1:-1]]
                row_html = "<tr>" + "".join(f"<td>{cell}</td>" for cell in cells) + "</tr>"
                table_html.append(row_html)
            elif in_table:
                table_html.append("</table>")
                in_table = False

        return "\n".join(table_html) if table_html else html

    def generate_json_report(self, data: dict[str, Any]) -> str:
        """Generate JSON report"""
        logger.info("Generating JSON report...")

        # Structure the data for JSON output
        json_data = {
            "metadata": {
                "timestamp": data["timestamp"],
                "project_key": data["project_key"],
                "branch": data["branch"],
                "generator": "Automated SonarQube Report Generator",
            },
            "quality_gate": data.get("quality_gate", {}),
            "metrics": {},
            "issues": {"total": len(data.get("issues", {}).get("issues", [])), "by_severity": {}, "by_type": {}},
            "security_hotspots": {"total": len(data.get("hotspots", {}).get("hotspots", [])), "by_category": {}},
            "trends": self._calculate_trends(data) if self.report_config.include_trends else None,
        }

        # Process metrics
        measures = data.get("measures", {}).get("component", {}).get("measures", [])
        for measure in measures:
            json_data["metrics"][measure["metric"]] = {
                "value": measure.get("value", "N/A"),
                "best_value": measure.get("bestValue", False),
            }

        # Process issues
        issues = data.get("issues", {}).get("issues", [])
        for issue in issues:
            severity = issue.get("severity", "UNKNOWN")
            issue_type = issue.get("type", "UNKNOWN")

            json_data["issues"]["by_severity"][severity] = json_data["issues"]["by_severity"].get(severity, 0) + 1
            json_data["issues"]["by_type"][issue_type] = json_data["issues"]["by_type"].get(issue_type, 0) + 1

        # Process hotspots
        hotspots = data.get("hotspots", {}).get("hotspots", [])
        for hotspot in hotspots:
            category = hotspot.get("securityCategory", "UNKNOWN")
            json_data["security_hotspots"]["by_category"][category] = (
                json_data["security_hotspots"]["by_category"].get(category, 0) + 1
            )

        return json.dumps(json_data, indent=2)

    def _calculate_trends(self, current_data: dict[str, Any]) -> dict[str, Any]:
        """Calculate trends for JSON report"""
        if len(self.historical_data["reports"]) < 2:
            return {"available": False, "reason": "Not enough historical data"}

        previous_data = self.historical_data["reports"][-1]

        # Extract metrics
        current_metrics = {
            m["metric"]: m.get("value", "N/A")
            for m in current_data.get("measures", {}).get("component", {}).get("measures", [])
        }
        previous_metrics = {
            m["metric"]: m.get("value", "N/A")
            for m in previous_data.get("measures", {}).get("component", {}).get("measures", [])
        }

        trends = {"available": True, "comparison_date": previous_data.get("timestamp", "Unknown"), "metrics": {}}

        for metric in ["bugs", "vulnerabilities", "code_smells", "coverage", "ncloc"]:
            current_val = current_metrics.get(metric, "N/A")
            previous_val = previous_metrics.get(metric, "N/A")

            try:
                curr = float(current_val)
                prev = float(previous_val)

                change = curr - prev
                change_percent = (change / prev) * 100 if prev != 0 else 0

                trends["metrics"][metric] = {
                    "current": curr,
                    "previous": prev,
                    "change": change,
                    "change_percent": change_percent,
                    "direction": "up" if change > 0 else "down" if change < 0 else "stable",
                }
            except (ValueError, TypeError):
                trends["metrics"][metric] = {
                    "current": current_val,
                    "previous": previous_val,
                    "change": "N/A",
                    "change_percent": "N/A",
                    "direction": "unknown",
                }

        return trends

    def save_report(self, content: str, format_type: str, timestamp: str | None = None) -> str:
        """Save report to file"""
        if not timestamp:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        filename = f"sonarqube_report_{timestamp}.{format_type}"
        filepath = Path(self.report_config.output_dir) / filename

        with filepath.open("w", encoding="utf-8") as f:
            f.write(content)

        logger.info(f"Report saved: {filepath}")
        return str(filepath)

    def send_email_notification(self, report_files: list[str]):
        """Send email notification with report attachments"""
        if not self.report_config.email_notifications:
            return

        try:
            msg = MIMEMultipart()
            msg["From"] = self.notification_config.email_username
            msg["To"] = ", ".join(self.notification_config.email_recipients)
            msg["Subject"] = (
                f"SonarQube Report - {self.sonar_config.project_key} - {datetime.now().strftime('%Y-%m-%d')}"
            )

            body = f"""
            SonarQube analysis report for project {self.sonar_config.project_key} has been generated.

            Branch: {self.sonar_config.branch}
            Generated: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

            Please find the attached reports.

            Best regards,
            Automated SonarQube Report Generator
            """

            msg.attach(MIMEText(body, "plain"))

            # Attach report files
            for file_path in report_files:
                with Path(file_path).open("rb") as attachment:
                    part = MIMEBase("application", "octet-stream")
                    part.set_payload(attachment.read())
                    encoders.encode_base64(part)
                    part.add_header("Content-Disposition", f"attachment; filename= {Path(file_path).name}")
                    msg.attach(part)

            # Send email
            server = smtplib.SMTP(self.notification_config.email_smtp_server, self.notification_config.email_smtp_port)
            server.starttls()
            server.login(self.notification_config.email_username, self.notification_config.email_password)
            text = msg.as_string()
            server.sendmail(self.notification_config.email_username, self.notification_config.email_recipients, text)
            server.quit()

            logger.info("Email notification sent successfully")
        except Exception as e:
            logger.error(f"Failed to send email notification: {e}")

    def send_slack_notification(self, report_data: dict[str, Any]):
        """Send Slack notification"""
        if not self.report_config.slack_notifications:
            return

        try:
            # Extract key metrics
            measures = report_data.get("measures", {}).get("component", {}).get("measures", [])
            metrics = {m["metric"]: m.get("value", "N/A") for m in measures}

            quality_gate = report_data.get("quality_gate", {}).get("projectStatus", {})
            status = quality_gate.get("status", "UNKNOWN")

            # Create Slack message
            message = {
                "channel": self.notification_config.slack_channel,
                "username": "SonarQube Bot",
                "icon_emoji": ":chart_with_upwards_trend:",
                "text": f"SonarQube Analysis Report - {self.sonar_config.project_key}",
                "attachments": [
                    {
                        "color": "good" if status == "OK" else "danger",
                        "fields": [
                            {
                                "title": "Quality Gate",
                                "value": f"{'✅ PASSED' if status == 'OK' else '❌ FAILED'}",
                                "short": True,
                            },
                            {"title": "Bugs", "value": metrics.get("bugs", "N/A"), "short": True},
                            {"title": "Vulnerabilities", "value": metrics.get("vulnerabilities", "N/A"), "short": True},
                            {"title": "Code Smells", "value": metrics.get("code_smells", "N/A"), "short": True},
                            {"title": "Coverage", "value": f"{metrics.get('coverage', 'N/A')}%", "short": True},
                            {
                                "title": "Technical Debt",
                                "value": self._format_technical_debt(metrics.get("sqale_index", "N/A")),
                                "short": True,
                            },
                        ],
                        "footer": "SonarQube Report Generator",
                        "ts": int(time.time()),
                    }
                ],
            }

            response = requests.post(self.notification_config.slack_webhook_url, json=message, timeout=30)
            response.raise_for_status()

            logger.info("Slack notification sent successfully")
        except Exception as e:
            logger.error(f"Failed to send Slack notification: {e}")

    def generate_full_report(self) -> dict[str, str]:
        """Generate complete report in all requested formats"""
        logger.info("Starting full report generation...")

        # Collect data
        data = self.collect_project_metrics()

        # Save to historical data
        self._save_historical_data(data)

        # Generate reports
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_files = {}

        for format_type in self.report_config.formats:
            if format_type == "markdown":
                content = self.generate_markdown_report(data)
                report_files[format_type] = self.save_report(content, "md", timestamp)
            elif format_type == "html":
                content = self.generate_html_report(data)
                report_files[format_type] = self.save_report(content, "html", timestamp)
            elif format_type == "json":
                content = self.generate_json_report(data)
                report_files[format_type] = self.save_report(content, "json", timestamp)

        # Send notifications
        if self.report_config.email_notifications:
            self.send_email_notification(list(report_files.values()))

        if self.report_config.slack_notifications:
            self.send_slack_notification(data)

        # Create latest report symlinks
        self._create_latest_symlinks(report_files)

        logger.info("Full report generation completed")
        return report_files

    def _create_latest_symlinks(self, report_files: dict[str, str]):
        """Create symlinks for latest reports"""
        for format_type, file_path in report_files.items():
            latest_path = Path(self.report_config.output_dir) / f"latest_report.{format_type}"

            # Remove existing symlink
            if latest_path.exists():
                latest_path.unlink()

            # Create new symlink
            latest_path.symlink_to(Path(file_path).name)
            logger.info(f"Created latest report symlink: {latest_path}")

    def setup_scheduler(self):
        """Set up scheduled report generation"""
        # Daily reports at 9 AM
        schedule.every().day.at("09:00").do(self.generate_full_report)

        # Weekly detailed reports on Monday at 8 AM
        schedule.every().monday.at("08:00").do(self.generate_full_report)

        # Monthly reports on the 1st at 7 AM
        schedule.every().month.at("07:00").do(self.generate_full_report)

        logger.info("Scheduler set up for automated report generation")

    def run_scheduler(self):
        """Run the scheduler"""
        logger.info("Starting report scheduler...")

        while True:
            schedule.run_pending()
            time.sleep(60)  # Check every minute


def main():
    """Main function"""
    parser = argparse.ArgumentParser(description="Automated SonarQube Report Generator")
    parser.add_argument("--config", default="sonarqube_config.yaml", help="Configuration file")
    parser.add_argument("--generate", action="store_true", help="Generate report immediately")
    parser.add_argument("--schedule", action="store_true", help="Run scheduler")
    parser.add_argument("--format", choices=["markdown", "html", "json"], help="Generate specific format only")

    args = parser.parse_args()

    # Initialize generator
    generator = SonarQubeReportGenerator(args.config)

    if args.generate:
        if args.format:
            # Generate specific format
            data = generator.collect_project_metrics()
            generator._save_historical_data(data)

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

            if args.format == "markdown":
                content = generator.generate_markdown_report(data)
                generator.save_report(content, "md", timestamp)
            elif args.format == "html":
                content = generator.generate_html_report(data)
                generator.save_report(content, "html", timestamp)
            elif args.format == "json":
                content = generator.generate_json_report(data)
                generator.save_report(content, "json", timestamp)
        else:
            # Generate full report
            generator.generate_full_report()

    elif args.schedule:
        generator.setup_scheduler()
        generator.run_scheduler()

    else:
        print("Use --generate to generate reports or --schedule to run scheduler")
        parser.print_help()


if __name__ == "__main__":
    main()
