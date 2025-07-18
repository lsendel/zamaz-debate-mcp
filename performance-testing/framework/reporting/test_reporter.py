"""
Test reporting and analysis tools for performance testing.

This module provides comprehensive reporting capabilities including:
- HTML report generation
- JSON data export
- Performance trend analysis
- Comparison reports
- Grafana dashboard integration
"""

import json
import logging
import statistics
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import matplotlib.pyplot as plt
import seaborn as sns
from jinja2 import Template
from matplotlib.backends.backend_pdf import PdfPages


@dataclass
class TestResult:
    """Container for test results."""

    test_name: str
    test_type: str
    start_time: datetime
    end_time: datetime
    duration_seconds: float
    success: bool
    metrics: dict[str, Any]
    operation_stats: dict[str, Any]
    error_summary: dict[str, Any]
    configuration: dict[str, Any]
    metadata: dict[str, Any]


class PerformanceReporter:
    """Generates performance test reports."""

    def __init__(self, output_dir: str = "performance_reports"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        self.logger = logging.getLogger(self.__class__.__name__)

        # Setup matplotlib style
        plt.style.use("seaborn-v0_8")
        sns.set_palette("husl")

    def generate_html_report(self, test_results: list[TestResult], report_name: str | None = None) -> str:
        """Generate comprehensive HTML report."""
        if report_name is None:
            report_name = f"performance_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        # Prepare report data
        report_data = {
            "title": "Performance Test Report",
            "generated_at": datetime.now().isoformat(),
            "test_results": test_results,
            "summary": self._generate_summary(test_results),
            "charts": self._generate_charts(test_results),
        }

        # Generate HTML
        html_content = self._render_html_template(report_data)

        # Save report
        html_path = self.output_dir / f"{report_name}.html"
        with open(html_path, "w") as f:
            f.write(html_content)

        self.logger.info(f"HTML report generated: {html_path}")
        return str(html_path)

    def generate_json_report(self, test_results: list[TestResult], report_name: str | None = None) -> str:
        """Generate JSON report for programmatic access."""
        if report_name is None:
            report_name = f"performance_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        # Convert to JSON-serializable format
        json_data = {
            "generated_at": datetime.now().isoformat(),
            "test_results": [asdict(result) for result in test_results],
            "summary": self._generate_summary(test_results),
        }

        # Save JSON
        json_path = self.output_dir / f"{report_name}.json"
        with open(json_path, "w") as f:
            json.dump(json_data, f, indent=2, default=str)

        self.logger.info(f"JSON report generated: {json_path}")
        return str(json_path)

    def generate_pdf_report(self, test_results: list[TestResult], report_name: str | None = None) -> str:
        """Generate PDF report with charts."""
        if report_name is None:
            report_name = f"performance_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        pdf_path = self.output_dir / f"{report_name}.pdf"

        with PdfPages(pdf_path) as pdf:
            # Title page
            fig, ax = plt.subplots(figsize=(8, 6))
            ax.text(0.5, 0.7, "Performance Test Report", horizontalalignment="center", fontsize=20, weight="bold")
            ax.text(
                0.5,
                0.5,
                f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
                horizontalalignment="center",
                fontsize=14,
            )
            ax.text(0.5, 0.3, f"Total Tests: {len(test_results)}", horizontalalignment="center", fontsize=12)
            ax.axis("off")
            pdf.savefig(fig, bbox_inches="tight")
            plt.close(fig)

            # Generate charts
            charts = self._generate_pdf_charts(test_results)
            for chart in charts:
                pdf.savefig(chart, bbox_inches="tight")
                plt.close(chart)

        self.logger.info(f"PDF report generated: {pdf_path}")
        return str(pdf_path)

    def generate_comparison_report(
        self, baseline_results: list[TestResult], current_results: list[TestResult], report_name: str | None = None
    ) -> str:
        """Generate comparison report between test runs."""
        if report_name is None:
            report_name = f"performance_comparison_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        comparison_data = self._compare_results(baseline_results, current_results)

        # Generate HTML comparison report
        html_content = self._render_comparison_template(comparison_data)

        html_path = self.output_dir / f"{report_name}.html"
        with open(html_path, "w") as f:
            f.write(html_content)

        self.logger.info(f"Comparison report generated: {html_path}")
        return str(html_path)

    def generate_trend_analysis(
        self, historical_results: list[list[TestResult]], report_name: str | None = None
    ) -> str:
        """Generate trend analysis report."""
        if report_name is None:
            report_name = f"performance_trend_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        trend_data = self._analyze_trends(historical_results)

        # Generate charts
        charts_dir = self.output_dir / f"{report_name}_charts"
        charts_dir.mkdir(exist_ok=True)

        self._generate_trend_charts(trend_data, charts_dir)

        # Generate HTML report
        html_content = self._render_trend_template(trend_data, charts_dir)

        html_path = self.output_dir / f"{report_name}.html"
        with open(html_path, "w") as f:
            f.write(html_content)

        self.logger.info(f"Trend analysis report generated: {html_path}")
        return str(html_path)

    def export_to_grafana(self, test_results: list[TestResult], datasource_url: str, api_key: str) -> bool:
        """Export results to Grafana dashboard."""
        try:
            import requests

            # Convert results to Grafana-compatible format
            grafana_data = self._convert_to_grafana_format(test_results)

            headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}

            response = requests.post(
                f"{datasource_url}/api/datasources/proxy/1/write", headers=headers, json=grafana_data
            )

            if response.status_code == 200:
                self.logger.info("Successfully exported to Grafana")
                return True
            else:
                self.logger.error(f"Failed to export to Grafana: {response.status_code}")
                return False

        except Exception as e:
            self.logger.error(f"Error exporting to Grafana: {e}")
            return False

    def _generate_summary(self, test_results: list[TestResult]) -> dict[str, Any]:
        """Generate summary statistics."""
        if not test_results:
            return {}

        total_tests = len(test_results)
        successful_tests = sum(1 for r in test_results if r.success)
        failed_tests = total_tests - successful_tests

        # Calculate aggregate metrics
        durations = [r.duration_seconds for r in test_results]
        operations_counts = [r.metrics.get("operations_count", 0) for r in test_results]
        memory_usage = [r.metrics.get("memory_peak_mb", 0) for r in test_results]
        cpu_usage = [r.metrics.get("cpu_peak_percent", 0) for r in test_results]

        return {
            "total_tests": total_tests,
            "successful_tests": successful_tests,
            "failed_tests": failed_tests,
            "success_rate": successful_tests / total_tests if total_tests > 0 else 0,
            "total_duration": sum(durations),
            "avg_duration": statistics.mean(durations) if durations else 0,
            "total_operations": sum(operations_counts),
            "avg_operations": statistics.mean(operations_counts) if operations_counts else 0,
            "peak_memory_mb": max(memory_usage) if memory_usage else 0,
            "avg_memory_mb": statistics.mean(memory_usage) if memory_usage else 0,
            "peak_cpu_percent": max(cpu_usage) if cpu_usage else 0,
            "avg_cpu_percent": statistics.mean(cpu_usage) if cpu_usage else 0,
            "test_types": list({r.test_type for r in test_results}),
        }

    def _generate_charts(self, test_results: list[TestResult]) -> list[str]:
        """Generate charts for HTML report."""
        charts = []

        if not test_results:
            return charts

        # Response time distribution
        response_times = []
        for result in test_results:
            for _op_name, stats in result.operation_stats.items():
                response_times.extend([stats.get("avg_duration_ms", 0)])

        if response_times:
            plt.figure(figsize=(10, 6))
            plt.hist(response_times, bins=20, alpha=0.7, edgecolor="black")
            plt.xlabel("Response Time (ms)")
            plt.ylabel("Frequency")
            plt.title("Response Time Distribution")
            plt.grid(True, alpha=0.3)

            chart_path = self.output_dir / "response_time_distribution.png"
            plt.savefig(chart_path, dpi=300, bbox_inches="tight")
            plt.close()
            charts.append(str(chart_path))

        # Memory usage over time
        memory_data = [(r.start_time, r.metrics.get("memory_peak_mb", 0)) for r in test_results]
        memory_data.sort(key=lambda x: x[0])

        if memory_data:
            plt.figure(figsize=(12, 6))
            times, memory_values = zip(*memory_data, strict=False)
            plt.plot(times, memory_values, marker="o", linewidth=2, markersize=6)
            plt.xlabel("Test Time")
            plt.ylabel("Memory Usage (MB)")
            plt.title("Memory Usage Over Time")
            plt.grid(True, alpha=0.3)
            plt.xticks(rotation=45)

            chart_path = self.output_dir / "memory_usage_trend.png"
            plt.savefig(chart_path, dpi=300, bbox_inches="tight")
            plt.close()
            charts.append(str(chart_path))

        # Operations per second
        ops_data = [(r.start_time, r.metrics.get("operations_per_second", 0)) for r in test_results]
        ops_data.sort(key=lambda x: x[0])

        if ops_data:
            plt.figure(figsize=(12, 6))
            times, ops_values = zip(*ops_data, strict=False)
            plt.plot(times, ops_values, marker="s", linewidth=2, markersize=6, color="green")
            plt.xlabel("Test Time")
            plt.ylabel("Operations per Second")
            plt.title("Throughput Over Time")
            plt.grid(True, alpha=0.3)
            plt.xticks(rotation=45)

            chart_path = self.output_dir / "throughput_trend.png"
            plt.savefig(chart_path, dpi=300, bbox_inches="tight")
            plt.close()
            charts.append(str(chart_path))

        return charts

    def _generate_pdf_charts(self, test_results: list[TestResult]) -> list[plt.Figure]:
        """Generate charts for PDF report."""
        charts = []

        if not test_results:
            return charts

        # Summary statistics chart
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(12, 10))

        # Success rate pie chart
        summary = self._generate_summary(test_results)
        success_data = [summary["successful_tests"], summary["failed_tests"]]
        success_labels = ["Successful", "Failed"]
        ax1.pie(success_data, labels=success_labels, autopct="%1.1f%%", startangle=90)
        ax1.set_title("Test Success Rate")

        # Response time histogram
        response_times = []
        for result in test_results:
            for _op_name, stats in result.operation_stats.items():
                response_times.extend([stats.get("avg_duration_ms", 0)])

        if response_times:
            ax2.hist(response_times, bins=15, alpha=0.7, edgecolor="black")
            ax2.set_xlabel("Response Time (ms)")
            ax2.set_ylabel("Frequency")
            ax2.set_title("Response Time Distribution")
            ax2.grid(True, alpha=0.3)

        # Memory usage bar chart
        memory_data = [r.metrics.get("memory_peak_mb", 0) for r in test_results]
        test_names = [r.test_name[:15] + "..." if len(r.test_name) > 15 else r.test_name for r in test_results]

        if memory_data:
            ax3.bar(range(len(memory_data)), memory_data)
            ax3.set_xlabel("Test")
            ax3.set_ylabel("Memory Usage (MB)")
            ax3.set_title("Peak Memory Usage by Test")
            ax3.set_xticks(range(len(test_names)))
            ax3.set_xticklabels(test_names, rotation=45, ha="right")

        # Operations per second bar chart
        ops_data = [r.metrics.get("operations_per_second", 0) for r in test_results]

        if ops_data:
            ax4.bar(range(len(ops_data)), ops_data, color="green")
            ax4.set_xlabel("Test")
            ax4.set_ylabel("Operations per Second")
            ax4.set_title("Throughput by Test")
            ax4.set_xticks(range(len(test_names)))
            ax4.set_xticklabels(test_names, rotation=45, ha="right")

        plt.tight_layout()
        charts.append(fig)

        return charts

    def _compare_results(self, baseline: list[TestResult], current: list[TestResult]) -> dict[str, Any]:
        """Compare two sets of test results."""
        baseline_summary = self._generate_summary(baseline)
        current_summary = self._generate_summary(current)

        comparison = {}

        # Compare key metrics
        for key in ["avg_duration", "avg_operations", "avg_memory_mb", "avg_cpu_percent", "success_rate"]:
            baseline_value = baseline_summary.get(key, 0)
            current_value = current_summary.get(key, 0)

            change_percent = (current_value - baseline_value) / baseline_value * 100 if baseline_value > 0 else 0

            comparison[key] = {
                "baseline": baseline_value,
                "current": current_value,
                "change_percent": change_percent,
                "improved": self._is_improvement(key, change_percent),
            }

        return {
            "baseline_summary": baseline_summary,
            "current_summary": current_summary,
            "comparison": comparison,
            "recommendations": self._generate_recommendations(comparison),
        }

    def _is_improvement(self, metric: str, change_percent: float) -> bool:
        """Determine if a metric change is an improvement."""
        # For these metrics, lower is better
        lower_is_better = ["avg_duration", "avg_memory_mb", "avg_cpu_percent"]

        # For these metrics, higher is better
        higher_is_better = ["avg_operations", "success_rate"]

        if metric in lower_is_better:
            return change_percent < 0
        elif metric in higher_is_better:
            return change_percent > 0
        else:
            return False

    def _generate_recommendations(self, comparison: dict[str, Any]) -> list[str]:
        """Generate performance recommendations."""
        recommendations = []

        for metric, data in comparison.items():
            if not data["improved"] and abs(data["change_percent"]) > 5:
                if metric == "avg_duration":
                    recommendations.append(
                        f"Response time increased by {data['change_percent']:.1f}%. "
                        "Consider optimizing database queries or adding caching."
                    )
                elif metric == "avg_memory_mb":
                    recommendations.append(
                        f"Memory usage increased by {data['change_percent']:.1f}%. "
                        "Check for memory leaks and optimize data structures."
                    )
                elif metric == "avg_cpu_percent":
                    recommendations.append(
                        f"CPU usage increased by {data['change_percent']:.1f}%. "
                        "Profile CPU-intensive operations and optimize algorithms."
                    )
                elif metric == "success_rate":
                    recommendations.append(
                        f"Success rate decreased by {data['change_percent']:.1f}%. "
                        "Investigate error logs and improve error handling."
                    )

        if not recommendations:
            recommendations.append("Performance appears stable or improved across all metrics.")

        return recommendations

    def _analyze_trends(self, historical_results: list[list[TestResult]]) -> dict[str, Any]:
        """Analyze performance trends over time."""
        if not historical_results:
            return {}

        # Extract metrics over time
        time_series = []
        for i, results in enumerate(historical_results):
            summary = self._generate_summary(results)
            summary["run_index"] = i
            summary["run_date"] = min(r.start_time for r in results) if results else datetime.now()
            time_series.append(summary)

        # Calculate trends
        trends = {}
        metrics = ["avg_duration", "avg_operations", "avg_memory_mb", "avg_cpu_percent", "success_rate"]

        for metric in metrics:
            values = [ts[metric] for ts in time_series if metric in ts]
            if len(values) >= 2:
                # Simple linear trend
                x = list(range(len(values)))
                slope = self._calculate_slope(x, values)
                trends[metric] = {
                    "values": values,
                    "slope": slope,
                    "trending": "up" if slope > 0 else "down" if slope < 0 else "stable",
                }

        return {"time_series": time_series, "trends": trends, "analysis_date": datetime.now().isoformat()}

    def _calculate_slope(self, x: list[float], y: list[float]) -> float:
        """Calculate linear regression slope."""
        if len(x) != len(y) or len(x) < 2:
            return 0.0

        n = len(x)
        sum_x = sum(x)
        sum_y = sum(y)
        sum_xy = sum(x[i] * y[i] for i in range(n))
        sum_x2 = sum(x[i] ** 2 for i in range(n))

        denominator = n * sum_x2 - sum_x**2
        if denominator == 0:
            return 0.0

        return (n * sum_xy - sum_x * sum_y) / denominator

    def _generate_trend_charts(self, trend_data: dict[str, Any], output_dir: Path):
        """Generate trend analysis charts."""
        if not trend_data.get("trends"):
            return

        for metric, data in trend_data["trends"].items():
            plt.figure(figsize=(12, 6))
            values = data["values"]
            x = list(range(len(values)))

            plt.plot(x, values, marker="o", linewidth=2, markersize=6, label=metric)

            # Add trend line
            if len(values) >= 2:
                trend_line = [data["slope"] * i + values[0] for i in x]
                plt.plot(x, trend_line, "--", alpha=0.7, label=f"Trend ({data['trending']})")

            plt.xlabel("Test Run")
            plt.ylabel(metric.replace("_", " ").title())
            plt.title(f"{metric.replace('_', ' ').title()} Trend Over Time")
            plt.grid(True, alpha=0.3)
            plt.legend()

            chart_path = output_dir / f"{metric}_trend.png"
            plt.savefig(chart_path, dpi=300, bbox_inches="tight")
            plt.close()

    def _convert_to_grafana_format(self, test_results: list[TestResult]) -> dict[str, Any]:
        """Convert test results to Grafana-compatible format."""
        grafana_data = []

        for result in test_results:
            timestamp = int(result.start_time.timestamp() * 1000)

            # Add metrics as data points
            for metric_name, value in result.metrics.items():
                grafana_data.append(
                    {
                        "measurement": "performance_test",
                        "tags": {"test_name": result.test_name, "test_type": result.test_type, "metric": metric_name},
                        "fields": {"value": value},
                        "time": timestamp,
                    }
                )

        return {"data": grafana_data}

    def _render_html_template(self, data: dict[str, Any]) -> str:
        """Render HTML report template."""
        template = Template("""
<!DOCTYPE html>
<html>
<head>
    <title>{{ title }}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; margin-bottom: 20px; }
        .summary { background-color: #e8f4f8; padding: 15px; margin-bottom: 20px; }
        .test-result { border: 1px solid #ddd; padding: 15px; margin-bottom: 15px; }
        .success { border-left: 5px solid #4CAF50; }
        .failure { border-left: 5px solid #f44336; }
        .metric { display: inline-block; margin: 5px 10px; padding: 5px; background-color: #f9f9f9; }
        .chart { text-align: center; margin: 20px 0; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{ title }}</h1>
        <p>Generated: {{ generated_at }}</p>
    </div>

    <div class="summary">
        <h2>Summary</h2>
        <div class="metric">Total Tests: {{ summary.total_tests }}</div>
        <div class="metric">Success Rate: {{ "%.1f"|format(summary.success_rate * 100) }}%</div>
        <div class="metric">Avg Duration: {{ "%.2f"|format(summary.avg_duration) }}s</div>
        <div class="metric">Total Operations: {{ summary.total_operations }}</div>
        <div class="metric">Peak Memory: {{ "%.1f"|format(summary.peak_memory_mb) }}MB</div>
        <div class="metric">Peak CPU: {{ "%.1f"|format(summary.peak_cpu_percent) }}%</div>
    </div>

    {% for chart in charts %}
    <div class="chart">
        <img src="{{ chart }}" alt="Performance Chart" style="max-width: 100%;">
    </div>
    {% endfor %}

    <h2>Test Results</h2>
    {% for result in test_results %}
    <div class="test-result {{ 'success' if result.success else 'failure' }}">
        <h3>{{ result.test_name }}</h3>
        <p><strong>Type:</strong> {{ result.test_type }}</p>
        <p><strong>Duration:</strong> {{ "%.2f"|format(result.duration_seconds) }}s</p>
        <p><strong>Status:</strong> {{ "Success" if result.success else "Failed" }}</p>

        <h4>Metrics</h4>
        <table>
            {% for key, value in result.metrics.items() %}
            <tr>
                <td>{{ key }}</td>
                <td>{{ value }}</td>
            </tr>
            {% endfor %}
        </table>

        {% if result.operation_stats %}
        <h4>Operation Statistics</h4>
        <table>
            <tr>
                <th>Operation</th>
                <th>Count</th>
                <th>Avg Duration (ms)</th>
                <th>Min Duration (ms)</th>
                <th>Max Duration (ms)</th>
                <th>Success Rate</th>
            </tr>
            {% for op_name, stats in result.operation_stats.items() %}
            <tr>
                <td>{{ op_name }}</td>
                <td>{{ stats.count }}</td>
                <td>{{ "%.2f"|format(stats.avg_duration_ms) }}</td>
                <td>{{ "%.2f"|format(stats.min_duration_ms) }}</td>
                <td>{{ "%.2f"|format(stats.max_duration_ms) }}</td>
                <td>{{ "%.1f"|format(stats.success_rate * 100) }}%</td>
            </tr>
            {% endfor %}
        </table>
        {% endif %}
    </div>
    {% endfor %}
</body>
</html>
        """)

        return template.render(**data)

    def _render_comparison_template(self, data: dict[str, Any]) -> str:
        """Render comparison report template."""
        template = Template("""
<!DOCTYPE html>
<html>
<head>
    <title>Performance Comparison Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; margin-bottom: 20px; }
        .comparison { background-color: #e8f4f8; padding: 15px; margin-bottom: 20px; }
        .improved { color: #4CAF50; font-weight: bold; }
        .degraded { color: #f44336; font-weight: bold; }
        .stable { color: #666; }
        .recommendations { background-color: #fff3cd; padding: 15px; margin-top: 20px; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Performance Comparison Report</h1>
        <p>Generated: {{ datetime.now().isoformat() }}</p>
    </div>

    <div class="comparison">
        <h2>Comparison Results</h2>
        <table>
            <tr>
                <th>Metric</th>
                <th>Baseline</th>
                <th>Current</th>
                <th>Change</th>
                <th>Status</th>
            </tr>
            {% for metric, values in data.comparison.items() %}
            <tr>
                <td>{{ metric.replace('_', ' ').title() }}</td>
                <td>{{ "%.2f"|format(values.baseline) }}</td>
                <td>{{ "%.2f"|format(values.current) }}</td>
                <td>{{ "%.1f"|format(values.change_percent) }}%</td>
                <td class="{{ 'improved' if values.improved else 'degraded' }}">
                    {{ "Improved" if values.improved else "Degraded" }}
                </td>
            </tr>
            {% endfor %}
        </table>
    </div>

    <div class="recommendations">
        <h2>Recommendations</h2>
        <ul>
            {% for rec in data.recommendations %}
            <li>{{ rec }}</li>
            {% endfor %}
        </ul>
    </div>
</body>
</html>
        """)

        return template.render(data=data, datetime=datetime)

    def _render_trend_template(self, data: dict[str, Any], charts_dir: Path) -> str:
        """Render trend analysis template."""
        template = Template("""
<!DOCTYPE html>
<html>
<head>
    <title>Performance Trend Analysis</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; margin-bottom: 20px; }
        .trend { background-color: #e8f4f8; padding: 15px; margin-bottom: 20px; }
        .trending-up { color: #4CAF50; }
        .trending-down { color: #f44336; }
        .stable { color: #666; }
        .chart { text-align: center; margin: 20px 0; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Performance Trend Analysis</h1>
        <p>Analysis Date: {{ data.analysis_date }}</p>
    </div>

    <div class="trend">
        <h2>Trend Summary</h2>
        <table>
            <tr>
                <th>Metric</th>
                <th>Trend Direction</th>
                <th>Current Value</th>
                <th>Slope</th>
            </tr>
            {% for metric, trend in data.trends.items() %}
            <tr>
                <td>{{ metric.replace('_', ' ').title() }}</td>
                <td class="trending-{{ trend.trending }}">{{ trend.trending.title() }}</td>
                <td>{{ "%.2f"|format(trend.values[-1]) }}</td>
                <td>{{ "%.4f"|format(trend.slope) }}</td>
            </tr>
            {% endfor %}
        </table>
    </div>

    <h2>Trend Charts</h2>
    {% for metric in data.trends.keys() %}
    <div class="chart">
        <h3>{{ metric.replace('_', ' ').title() }}</h3>
        <img src="{{ charts_dir.name }}/{{ metric }}_trend.png" alt="{{ metric }} Trend" style="max-width: 100%;">
    </div>
    {% endfor %}
</body>
</html>
        """)

        return template.render(data=data, charts_dir=charts_dir)


# Export main class
__all__ = ["PerformanceReporter", "TestResult"]
