"""
JMeter integration for HTTP load testing.

This module provides integration with Apache JMeter for advanced HTTP load testing
including test plan generation, execution, and result analysis.
"""

import logging
import os
import subprocess
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any
from xml.dom import minidom

import matplotlib.pyplot as plt
import pandas as pd


@dataclass
class JMeterTestPlan:
    """Configuration for JMeter test plan."""

    name: str
    description: str

    # Test parameters
    thread_groups: list[dict[str, Any]] = field(default_factory=list)
    ramp_up_time: int = 60
    duration: int = 300

    # HTTP parameters
    base_url: str = "http://localhost:8080"
    default_headers: dict[str, str] = field(default_factory=dict)

    # Test data
    csv_data_files: list[str] = field(default_factory=list)

    # Reporting
    output_dir: str = "jmeter_results"
    generate_dashboard: bool = True

    # Advanced settings
    assertions: list[dict[str, Any]] = field(default_factory=list)
    timers: list[dict[str, Any]] = field(default_factory=list)
    listeners: list[dict[str, Any]] = field(default_factory=list)


class JMeterTestGenerator:
    """Generates JMeter test plans (.jmx files)."""

    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)

    def generate_test_plan(self, config: JMeterTestPlan, output_path: str) -> str:
        """Generate JMeter test plan XML."""
        self.logger.info(f"Generating JMeter test plan: {config.name}")

        # Create root element
        root = ET.Element("jmeterTestPlan", {"version": "1.2", "properties": "5.0", "jmeter": "5.4.1"})

        # Add hash tree
        hash_tree = ET.SubElement(root, "hashTree")

        # Add test plan element
        test_plan = ET.SubElement(
            hash_tree,
            "TestPlan",
            {"guiclass": "TestPlanGui", "testclass": "TestPlan", "testname": config.name, "enabled": "true"},
        )

        # Add test plan properties
        self._add_string_prop(test_plan, "TestPlan.comments", config.description)
        self._add_bool_prop(test_plan, "TestPlan.functional_mode", False)
        self._add_bool_prop(test_plan, "TestPlan.tearDown_on_shutdown", True)
        self._add_bool_prop(test_plan, "TestPlan.serialize_threadgroups", False)

        # Add arguments
        ET.SubElement(
            test_plan,
            "elementProp",
            {
                "name": "TestPlan.arguments",
                "elementType": "Arguments",
                "guiclass": "ArgumentsPanel",
                "testclass": "Arguments",
                "testname": "User Defined Variables",
                "enabled": "true",
            },
        )

        # Add test plan hash tree
        test_plan_hash_tree = ET.SubElement(hash_tree, "hashTree")

        # Add thread groups
        for thread_group_config in config.thread_groups:
            self._add_thread_group(test_plan_hash_tree, thread_group_config, config)

        # Add global listeners
        self._add_global_listeners(test_plan_hash_tree, config)

        # Write to file
        self._write_xml_file(root, output_path)

        self.logger.info(f"Test plan generated: {output_path}")
        return output_path

    def _add_thread_group(self, parent: ET.Element, thread_config: dict[str, Any], test_config: JMeterTestPlan):
        """Add thread group to test plan."""
        thread_group = ET.SubElement(
            parent,
            "ThreadGroup",
            {
                "guiclass": "ThreadGroupGui",
                "testclass": "ThreadGroup",
                "testname": thread_config.get("name", "Thread Group"),
                "enabled": "true",
            },
        )

        # Thread group properties
        self._add_string_prop(thread_group, "ThreadGroup.on_sample_error", "continue")

        # Thread properties
        thread_props = ET.SubElement(
            thread_group,
            "elementProp",
            {
                "name": "ThreadGroup.main_controller",
                "elementType": "LoopController",
                "guiclass": "LoopControlPanel",
                "testclass": "LoopController",
                "testname": "Loop Controller",
                "enabled": "true",
            },
        )

        self._add_bool_prop(thread_props, "LoopController.continue_forever", False)
        self._add_string_prop(thread_props, "LoopController.loops", str(thread_config.get("loops", -1)))

        # Thread counts
        self._add_string_prop(thread_group, "ThreadGroup.num_threads", str(thread_config.get("threads", 10)))
        self._add_string_prop(
            thread_group, "ThreadGroup.ramp_time", str(thread_config.get("ramp_time", test_config.ramp_up_time))
        )
        self._add_string_prop(thread_group, "ThreadGroup.start_time", "")
        self._add_string_prop(thread_group, "ThreadGroup.end_time", "")
        self._add_bool_prop(thread_group, "ThreadGroup.scheduler", thread_config.get("scheduler", True))
        self._add_string_prop(
            thread_group, "ThreadGroup.duration", str(thread_config.get("duration", test_config.duration))
        )
        self._add_string_prop(thread_group, "ThreadGroup.delay", "0")

        # Add thread group hash tree
        thread_hash_tree = ET.SubElement(parent, "hashTree")

        # Add HTTP requests
        if "requests" in thread_config:
            for request_config in thread_config["requests"]:
                self._add_http_request(thread_hash_tree, request_config, test_config)

        # Add timers
        if "timers" in thread_config:
            for timer_config in thread_config["timers"]:
                self._add_timer(thread_hash_tree, timer_config)

        # Add assertions
        if "assertions" in thread_config:
            for assertion_config in thread_config["assertions"]:
                self._add_assertion(thread_hash_tree, assertion_config)

    def _add_http_request(self, parent: ET.Element, request_config: dict[str, Any], test_config: JMeterTestPlan):
        """Add HTTP request sampler."""
        http_request = ET.SubElement(
            parent,
            "HTTPSamplerProxy",
            {
                "guiclass": "HttpTestSampleGui",
                "testclass": "HTTPSamplerProxy",
                "testname": request_config.get("name", "HTTP Request"),
                "enabled": "true",
            },
        )

        # Parse base URL
        from urllib.parse import urlparse

        parsed_url = urlparse(test_config.base_url)

        # HTTP request properties
        self._add_string_prop(http_request, "HTTPSampler.domain", parsed_url.hostname)
        self._add_string_prop(http_request, "HTTPSampler.port", str(parsed_url.port or 80))
        self._add_string_prop(http_request, "HTTPSampler.protocol", parsed_url.scheme)
        self._add_string_prop(http_request, "HTTPSampler.contentEncoding", "")
        self._add_string_prop(http_request, "HTTPSampler.path", request_config.get("path", "/"))
        self._add_string_prop(http_request, "HTTPSampler.method", request_config.get("method", "GET"))
        self._add_bool_prop(http_request, "HTTPSampler.follow_redirects", True)
        self._add_bool_prop(http_request, "HTTPSampler.auto_redirects", False)
        self._add_bool_prop(http_request, "HTTPSampler.use_keepalive", True)
        self._add_bool_prop(http_request, "HTTPSampler.DO_MULTIPART_POST", False)

        # Add request body if present
        if "body" in request_config:
            self._add_bool_prop(http_request, "HTTPSampler.postBodyRaw", True)

            # Add arguments for body
            arguments = ET.SubElement(
                http_request, "elementProp", {"name": "HTTPsampler.Arguments", "elementType": "Arguments"}
            )

            argument = ET.SubElement(arguments, "elementProp", {"name": "", "elementType": "HTTPArgument"})

            self._add_string_prop(argument, "HTTPArgument.value", request_config["body"])
            self._add_string_prop(argument, "HTTPArgument.metadata", "=")

        # Add headers
        headers = {**test_config.default_headers, **request_config.get("headers", {})}
        if headers:
            self._add_header_manager(parent, headers)

        # Add request hash tree
        ET.SubElement(parent, "hashTree")

    def _add_header_manager(self, parent: ET.Element, headers: dict[str, str]):
        """Add HTTP header manager."""
        header_manager = ET.SubElement(
            parent,
            "HeaderManager",
            {
                "guiclass": "HeaderPanel",
                "testclass": "HeaderManager",
                "testname": "HTTP Header Manager",
                "enabled": "true",
            },
        )

        # Headers collection
        headers_prop = ET.SubElement(header_manager, "collectionProp", {"name": "HeaderManager.headers"})

        for name, value in headers.items():
            header = ET.SubElement(headers_prop, "elementProp", {"name": "", "elementType": "Header"})
            self._add_string_prop(header, "Header.name", name)
            self._add_string_prop(header, "Header.value", value)

    def _add_timer(self, parent: ET.Element, timer_config: dict[str, Any]):
        """Add timer element."""
        timer_type = timer_config.get("type", "constant")

        if timer_type == "constant":
            timer = ET.SubElement(
                parent,
                "ConstantTimer",
                {
                    "guiclass": "ConstantTimerGui",
                    "testclass": "ConstantTimer",
                    "testname": timer_config.get("name", "Constant Timer"),
                    "enabled": "true",
                },
            )
            self._add_string_prop(timer, "ConstantTimer.delay", str(timer_config.get("delay", 1000)))

        elif timer_type == "uniform":
            timer = ET.SubElement(
                parent,
                "UniformRandomTimer",
                {
                    "guiclass": "UniformRandomTimerGui",
                    "testclass": "UniformRandomTimer",
                    "testname": timer_config.get("name", "Uniform Random Timer"),
                    "enabled": "true",
                },
            )
            self._add_string_prop(timer, "ConstantTimer.delay", str(timer_config.get("delay", 1000)))
            self._add_string_prop(timer, "RandomTimer.range", str(timer_config.get("range", 1000)))

        # Add timer hash tree
        ET.SubElement(parent, "hashTree")

    def _add_assertion(self, parent: ET.Element, assertion_config: dict[str, Any]):
        """Add assertion element."""
        assertion_type = assertion_config.get("type", "response")

        if assertion_type == "response":
            assertion = ET.SubElement(
                parent,
                "ResponseAssertion",
                {
                    "guiclass": "AssertionGui",
                    "testclass": "ResponseAssertion",
                    "testname": assertion_config.get("name", "Response Assertion"),
                    "enabled": "true",
                },
            )

            # Assertion properties
            self._add_string_prop(assertion, "Assertion.test_field", "Assertion.response_data")
            self._add_bool_prop(assertion, "Assertion.assume_success", False)
            self._add_int_prop(assertion, "Assertion.test_type", 2)  # Contains

            # Test strings
            test_strings = ET.SubElement(assertion, "collectionProp", {"name": "Asserion.test_strings"})

            for pattern in assertion_config.get("patterns", []):
                self._add_string_prop(test_strings, "test_string", pattern)

        elif assertion_type == "duration":
            assertion = ET.SubElement(
                parent,
                "DurationAssertion",
                {
                    "guiclass": "DurationAssertionGui",
                    "testclass": "DurationAssertion",
                    "testname": assertion_config.get("name", "Duration Assertion"),
                    "enabled": "true",
                },
            )

            self._add_string_prop(assertion, "DurationAssertion.duration", str(assertion_config.get("duration", 5000)))

        # Add assertion hash tree
        ET.SubElement(parent, "hashTree")

    def _add_global_listeners(self, parent: ET.Element, config: JMeterTestPlan):
        """Add global result listeners."""
        # Summary Report
        summary_report = ET.SubElement(
            parent,
            "SummaryReport",
            {
                "guiclass": "SummaryReport",
                "testclass": "SummaryReport",
                "testname": "Summary Report",
                "enabled": "true",
            },
        )

        # Results file
        results_file = Path(config.output_dir) / "results.jtl"
        self._add_string_prop(summary_report, "filename", str(results_file))

        ET.SubElement(parent, "hashTree")

        # Aggregate Report
        aggregate_report = ET.SubElement(
            parent,
            "StatVisualizer",
            {
                "guiclass": "StatVisualizer",
                "testclass": "StatVisualizer",
                "testname": "Aggregate Report",
                "enabled": "true",
            },
        )

        self._add_string_prop(aggregate_report, "filename", str(results_file))

        ET.SubElement(parent, "hashTree")

    def _add_string_prop(self, parent: ET.Element, name: str, value: str):
        """Add string property."""
        prop = ET.SubElement(parent, "stringProp", {"name": name})
        prop.text = value

    def _add_bool_prop(self, parent: ET.Element, name: str, value: bool):
        """Add boolean property."""
        prop = ET.SubElement(parent, "boolProp", {"name": name})
        prop.text = str(value).lower()

    def _add_int_prop(self, parent: ET.Element, name: str, value: int):
        """Add integer property."""
        prop = ET.SubElement(parent, "intProp", {"name": name})
        prop.text = str(value)

    def _write_xml_file(self, root: ET.Element, output_path: str):
        """Write XML to file with proper formatting."""
        # Convert to string
        xml_str = ET.tostring(root, encoding="unicode")

        # Pretty print
        dom = minidom.parseString(xml_str)
        pretty_xml = dom.toprettyxml(indent="  ")

        # Remove extra newlines
        pretty_xml = "\n".join([line for line in pretty_xml.split("\n") if line.strip()])

        # Write to file
        with Path(output_path).open("w", encoding="utf-8") as f:
            f.write(pretty_xml)


class JMeterExecutor:
    """Executes JMeter tests and manages results."""

    def __init__(self, jmeter_path: str | None = None):
        self.jmeter_path = jmeter_path or self._find_jmeter_path()
        self.logger = logging.getLogger(self.__class__.__name__)

        if not self.jmeter_path:
            raise ValueError("JMeter installation not found. Please install JMeter or specify path.")

    def execute_test(
        self, test_plan_path: str, output_dir: str, additional_args: list[str] | None = None
    ) -> dict[str, Any]:
        """Execute JMeter test plan."""
        self.logger.info(f"Executing JMeter test: {test_plan_path}")

        # Create output directory
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)

        # Prepare command
        cmd = [
            self.jmeter_path,
            "-n",  # Non-GUI mode
            "-t",
            test_plan_path,  # Test plan
            "-l",
            str(output_path / "results.jtl"),  # Results file
            "-e",  # Generate HTML dashboard
            "-o",
            str(output_path / "dashboard"),  # Dashboard output
        ]

        # Add additional arguments
        if additional_args:
            cmd.extend(additional_args)

        # Execute
        start_time = time.time()

        try:
            result = subprocess.run(cmd, capture_output=True, text=True, cwd=output_path, check=False)  # noqa: S603, S607 (calling known development tool)

            execution_time = time.time() - start_time

            if result.returncode == 0:
                self.logger.info(f"JMeter test completed successfully in {execution_time:.2f}s")
                return {
                    "success": True,
                    "execution_time": execution_time,
                    "output_dir": str(output_path),
                    "results_file": str(output_path / "results.jtl"),
                    "dashboard_dir": str(output_path / "dashboard"),
                    "stdout": result.stdout,
                    "stderr": result.stderr,
                }
            else:
                self.logger.error(f"JMeter test failed: {result.stderr}")
                return {
                    "success": False,
                    "execution_time": execution_time,
                    "error": result.stderr,
                    "stdout": result.stdout,
                }

        except Exception as e:
            self.logger.error(f"Error executing JMeter test: {e}")
            return {"success": False, "execution_time": time.time() - start_time, "error": str(e)}

    def _find_jmeter_path(self) -> str | None:
        """Find JMeter installation path."""
        # Common JMeter locations
        possible_paths = [
            "/usr/local/bin/jmeter",
            "/opt/jmeter/bin/jmeter",
            "C:\\apache-jmeter\\bin\\jmeter.bat",
            "C:\\Program Files\\apache-jmeter\\bin\\jmeter.bat",
        ]

        # Check PATH
        try:
            result = subprocess.run(["which", "jmeter"], capture_output=True, text=True, check=False)  # noqa: S603, S607 (calling known development tool)
            if result.returncode == 0:
                return result.stdout.strip()
        except:
            pass

        # Check common locations
        for path in possible_paths:
            if Path(path).exists():
                return path

        return None


class JMeterResultAnalyzer:
    """Analyzes JMeter test results."""

    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)

    def analyze_results(self, results_file: str) -> dict[str, Any]:
        """Analyze JMeter results file."""
        self.logger.info(f"Analyzing JMeter results: {results_file}")

        try:
            # Read results
            df = pd.read_csv(results_file)

            # Basic statistics
            stats = self._calculate_basic_stats(df)

            # Error analysis
            error_analysis = self._analyze_errors(df)

            # Throughput analysis
            throughput_analysis = self._analyze_throughput(df)

            # Response time analysis
            response_time_analysis = self._analyze_response_times(df)

            return {
                "summary": stats,
                "errors": error_analysis,
                "throughput": throughput_analysis,
                "response_times": response_time_analysis,
                "total_samples": len(df),
            }

        except Exception as e:
            self.logger.error(f"Error analyzing results: {e}")
            return {"error": str(e)}

    def _calculate_basic_stats(self, df: pd.DataFrame) -> dict[str, Any]:
        """Calculate basic statistics."""
        success_count = len(df[df["success"]])
        total_count = len(df)

        return {
            "total_samples": total_count,
            "success_count": success_count,
            "error_count": total_count - success_count,
            "success_rate": success_count / total_count if total_count > 0 else 0,
            "error_rate": (total_count - success_count) / total_count if total_count > 0 else 0,
            "avg_response_time": df["elapsed"].mean(),
            "min_response_time": df["elapsed"].min(),
            "max_response_time": df["elapsed"].max(),
            "median_response_time": df["elapsed"].median(),
            "p95_response_time": df["elapsed"].quantile(0.95),
            "p99_response_time": df["elapsed"].quantile(0.99),
        }

    def _analyze_errors(self, df: pd.DataFrame) -> dict[str, Any]:
        """Analyze error patterns."""
        error_df = df[not df["success"]]

        if len(error_df) == 0:
            return {"total_errors": 0, "error_types": {}}

        # Group by response message
        error_types = error_df["responseMessage"].value_counts().to_dict()

        # Group by response code
        error_codes = error_df["responseCode"].value_counts().to_dict()

        return {
            "total_errors": len(error_df),
            "error_types": error_types,
            "error_codes": error_codes,
            "error_rate_over_time": self._calculate_error_rate_over_time(df),
        }

    def _analyze_throughput(self, df: pd.DataFrame) -> dict[str, Any]:
        """Analyze throughput patterns."""
        # Convert timestamp to datetime
        df["timestamp"] = pd.to_datetime(df["timeStamp"], unit="ms")

        # Calculate throughput per second
        throughput_per_second = df.groupby(df["timestamp"].dt.floor("S")).size()

        return {
            "max_throughput": throughput_per_second.max(),
            "min_throughput": throughput_per_second.min(),
            "avg_throughput": throughput_per_second.mean(),
            "throughput_std": throughput_per_second.std(),
            "throughput_over_time": throughput_per_second.to_dict(),
        }

    def _analyze_response_times(self, df: pd.DataFrame) -> dict[str, Any]:
        """Analyze response time patterns."""
        # Group by label (request name)
        response_times_by_label = {}

        for label in df["label"].unique():
            label_df = df[df["label"] == label]
            response_times_by_label[label] = {
                "count": len(label_df),
                "avg": label_df["elapsed"].mean(),
                "min": label_df["elapsed"].min(),
                "max": label_df["elapsed"].max(),
                "median": label_df["elapsed"].median(),
                "p95": label_df["elapsed"].quantile(0.95),
                "p99": label_df["elapsed"].quantile(0.99),
            }

        return {"by_request_type": response_times_by_label, "overall_distribution": df["elapsed"].describe().to_dict()}

    def _calculate_error_rate_over_time(self, df: pd.DataFrame) -> dict[str, float]:
        """Calculate error rate over time."""
        df["timestamp"] = pd.to_datetime(df["timeStamp"], unit="ms")

        # Group by minute
        time_groups = df.groupby(df["timestamp"].dt.floor("min"))

        error_rates = {}
        for timestamp, group in time_groups:
            error_count = len(group[not group["success"]])
            total_count = len(group)
            error_rate = error_count / total_count if total_count > 0 else 0
            error_rates[timestamp.isoformat()] = error_rate

        return error_rates

    def generate_charts(self, results_file: str, output_dir: str):
        """Generate analysis charts."""
        self.logger.info(f"Generating charts for: {results_file}")

        try:
            df = pd.read_csv(results_file)
            df["timestamp"] = pd.to_datetime(df["timeStamp"], unit="ms")

            output_path = Path(output_dir)
            output_path.mkdir(parents=True, exist_ok=True)

            # Response time over time
            plt.figure(figsize=(12, 6))
            df_success = df[df["success"]]
            plt.plot(df_success["timestamp"], df_success["elapsed"], alpha=0.6)
            plt.title("Response Time Over Time")
            plt.xlabel("Time")
            plt.ylabel("Response Time (ms)")
            plt.xticks(rotation=45)
            plt.tight_layout()
            plt.savefig(output_path / "response_time_over_time.png", dpi=300)
            plt.close()

            # Response time histogram
            plt.figure(figsize=(10, 6))
            plt.hist(df_success["elapsed"], bins=50, alpha=0.7, edgecolor="black")
            plt.title("Response Time Distribution")
            plt.xlabel("Response Time (ms)")
            plt.ylabel("Frequency")
            plt.grid(True, alpha=0.3)
            plt.tight_layout()
            plt.savefig(output_path / "response_time_distribution.png", dpi=300)
            plt.close()

            # Throughput over time
            plt.figure(figsize=(12, 6))
            throughput_per_second = df.groupby(df["timestamp"].dt.floor("S")).size()
            plt.plot(throughput_per_second.index, throughput_per_second.values, linewidth=2)
            plt.title("Throughput Over Time")
            plt.xlabel("Time")
            plt.ylabel("Requests per Second")
            plt.xticks(rotation=45)
            plt.grid(True, alpha=0.3)
            plt.tight_layout()
            plt.savefig(output_path / "throughput_over_time.png", dpi=300)
            plt.close()

            # Error rate over time
            if len(df[not df["success"]]) > 0:
                plt.figure(figsize=(12, 6))
                error_rates = self._calculate_error_rate_over_time(df)
                times = [pd.to_datetime(t) for t in error_rates]
                rates = list(error_rates.values())
                plt.plot(times, rates, linewidth=2, color="red")
                plt.title("Error Rate Over Time")
                plt.xlabel("Time")
                plt.ylabel("Error Rate")
                plt.xticks(rotation=45)
                plt.grid(True, alpha=0.3)
                plt.tight_layout()
                plt.savefig(output_path / "error_rate_over_time.png", dpi=300)
                plt.close()

            self.logger.info(f"Charts generated in: {output_path}")

        except Exception as e:
            self.logger.error(f"Error generating charts: {e}")


# Export main classes
__all__ = ["JMeterExecutor", "JMeterResultAnalyzer", "JMeterTestGenerator", "JMeterTestPlan"]
