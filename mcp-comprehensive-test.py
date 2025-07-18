#!/usr/bin/env python3
"""
Comprehensive MCP Testing Framework
Tests all MCP endpoints and generates detailed reports
"""

import concurrent.futures
import json
import sys
import time
import traceback
from datetime import datetime
from pathlib import Path
from typing import Any

import requests


class McpTestResult:
    def __init__(
        self,
        test_name: str,
        service_name: str,
        tool_name: str,
        success: bool,
        execution_time_ms: int,
        error_message: str | None = None,
        response: Any = None,
        metadata: dict | None = None,
    ):
        self.test_name = test_name
        self.service_name = service_name
        self.tool_name = tool_name
        self.success = success
        self.execution_time_ms = execution_time_ms
        self.error_message = error_message
        self.response = response
        self.metadata = metadata or {}

    def __repr__(self):
        return f"McpTestResult(test='{self.test_name}', service='{self.service_name}', tool='{self.tool_name}', success={self.success}, time={self.execution_time_ms}ms)"


class McpComprehensiveTestFramework:
    def __init__(self):
        self.services = {
            "organization": "http://localhost:5005",
            "llm": "http://localhost:5002",
            "controller": "http://localhost:5013",
            "rag": "http://localhost:5018",
            "context": "http://localhost:5007",
        }
        self.results = []

    def test_service_availability(self, service_name: str, base_url: str) -> McpTestResult:
        """Test if service is available"""
        start_time = time.time()
        try:
            response = requests.get(f"{base_url}/mcp", timeout=5)
            execution_time = int((time.time() - start_time) * 1000)

            if response.status_code == 200:
                server_info = response.json()
                return McpTestResult(
                    "service_availability", service_name, "health_check", True, execution_time, response=server_info
                )
            else:
                return McpTestResult(
                    "service_availability",
                    service_name,
                    "health_check",
                    False,
                    execution_time,
                    f"HTTP {response.status_code}",
                )
        except Exception as e:
            execution_time = int((time.time() - start_time) * 1000)
            return McpTestResult("service_availability", service_name, "health_check", False, execution_time, str(e))

    def test_list_tools(self, service_name: str, base_url: str) -> McpTestResult:
        """Test list tools endpoint"""
        start_time = time.time()
        try:
            response = requests.post(f"{base_url}/mcp/list-tools", json={}, timeout=10)
            execution_time = int((time.time() - start_time) * 1000)

            if response.status_code == 200:
                tools_data = response.json()
                tool_count = len(tools_data.get("tools", []))
                return McpTestResult(
                    "list_tools",
                    service_name,
                    "list_tools",
                    True,
                    execution_time,
                    response=tools_data,
                    metadata={"tool_count": tool_count},
                )
            else:
                return McpTestResult(
                    "list_tools", service_name, "list_tools", False, execution_time, f"HTTP {response.status_code}"
                )
        except Exception as e:
            execution_time = int((time.time() - start_time) * 1000)
            return McpTestResult("list_tools", service_name, "list_tools", False, execution_time, str(e))

    def test_call_tool(self, service_name: str, base_url: str, tool_name: str, arguments: dict) -> McpTestResult:
        """Test calling a specific tool"""
        start_time = time.time()
        try:
            payload = {"name": tool_name, "arguments": arguments}
            response = requests.post(f"{base_url}/mcp/call-tool", json=payload, timeout=15)
            execution_time = int((time.time() - start_time) * 1000)

            if response.status_code == 200:
                response_data = response.json()
                return McpTestResult(
                    f"call_tool_{tool_name}", service_name, tool_name, True, execution_time, response=response_data
                )
            else:
                return McpTestResult(
                    f"call_tool_{tool_name}",
                    service_name,
                    tool_name,
                    False,
                    execution_time,
                    f"HTTP {response.status_code}: {response.text}",
                )
        except Exception as e:
            execution_time = int((time.time() - start_time) * 1000)
            return McpTestResult(f"call_tool_{tool_name}", service_name, tool_name, False, execution_time, str(e))

    def test_organization_service(self) -> list[McpTestResult]:
        """Test all Organization service endpoints"""
        results = []
        base_url = self.services["organization"]

        # Test service availability
        results.append(self.test_service_availability("organization", base_url))

        # Test list tools
        results.append(self.test_list_tools("organization", base_url))

        # Test individual tools
        test_cases = [
            ("create_organization", {"name": "Test Organization", "description": "Automated test organization"}),
            ("list_organizations", {}),
            ("get_organization", {"id": "test-org-123"}),
            (
                "update_organization",
                {"id": "test-org-123", "name": "Updated Test Org", "description": "Updated description"},
            ),
            (
                "add_user_to_organization",
                {"organizationId": "test-org-123", "userId": "test-user-456", "role": "member"},
            ),
        ]

        for tool_name, arguments in test_cases:
            results.append(self.test_call_tool("organization", base_url, tool_name, arguments))

        return results

    def test_context_service(self) -> list[McpTestResult]:
        """Test all Context service endpoints"""
        results = []
        base_url = self.services["context"]

        # Test service availability
        results.append(self.test_service_availability("context", base_url))

        # Test list tools
        results.append(self.test_list_tools("context", base_url))

        # Test individual tools
        test_cases = [
            ("create_context", {"name": "Test Context", "description": "Automated test context"}),
            (
                "append_message",
                {
                    "contextId": "test-context-123",
                    "role": "user",
                    "content": "What are the key points in favor of AI regulation?",
                },
            ),
            ("get_context_window", {"contextId": "test-context-123", "maxTokens": 4096, "messageLimit": 50}),
            ("search_contexts", {"query": "AI regulation", "page": 0, "size": 20}),
            (
                "share_context",
                {"contextId": "test-context-123", "targetOrganizationId": "test-org-789", "permission": "read"},
            ),
        ]

        for tool_name, arguments in test_cases:
            results.append(self.test_call_tool("context", base_url, tool_name, arguments))

        return results

    def test_llm_service(self) -> list[McpTestResult]:
        """Test all LLM service endpoints"""
        results = []
        base_url = self.services["llm"]

        # Test service availability
        results.append(self.test_service_availability("llm", base_url))

        # Test list tools
        results.append(self.test_list_tools("llm", base_url))

        # Test individual tools
        test_cases = [
            ("list_providers", {}),
            (
                "generate_completion",
                {
                    "provider": "claude",
                    "prompt": "What is the capital of France?",
                    "maxTokens": 100,
                    "temperature": 0.7,
                },
            ),
            ("get_provider_status", {"provider": "claude"}),
        ]

        for tool_name, arguments in test_cases:
            results.append(self.test_call_tool("llm", base_url, tool_name, arguments))

        return results

    def test_controller_service(self) -> list[McpTestResult]:
        """Test all Controller/Debate service endpoints"""
        results = []
        base_url = self.services["controller"]

        # Test service availability
        results.append(self.test_service_availability("controller", base_url))

        # Test list tools
        results.append(self.test_list_tools("controller", base_url))

        # Test individual tools
        test_cases = [
            (
                "create_debate",
                {
                    "topic": "Should AI be regulated?",
                    "format": "OXFORD",
                    "organizationId": "test-org-123",
                    "participants": ["user_1", "user_2", "ai_claude"],
                    "maxRounds": 3,
                },
            ),
            ("get_debate", {"debateId": "test-debate-789"}),
            ("list_debates", {"organizationId": "test-org-123"}),
            (
                "submit_turn",
                {
                    "debateId": "test-debate-789",
                    "participantId": "user_1",
                    "content": "I believe AI regulation is necessary because...",
                },
            ),
        ]

        for tool_name, arguments in test_cases:
            results.append(self.test_call_tool("controller", base_url, tool_name, arguments))

        return results

    def test_rag_service(self) -> list[McpTestResult]:
        """Test all RAG service endpoints"""
        results = []
        base_url = self.services["rag"]

        # Test service availability
        results.append(self.test_service_availability("rag", base_url))

        # Test list tools
        results.append(self.test_list_tools("rag", base_url))

        # Test individual tools
        test_cases = [
            (
                "index_document",
                {
                    "organizationId": "test-org-123",
                    "documentId": "test-doc-001",
                    "content": "This is a test document about AI ethics...",
                    "metadata": {"title": "AI Ethics Research", "author": "Test Author", "category": "research"},
                },
            ),
            ("search", {"organizationId": "test-org-123", "query": "AI ethics", "limit": 10}),
            (
                "get_context",
                {
                    "organizationId": "test-org-123",
                    "query": "What are the ethical implications of AI?",
                    "maxTokens": 1000,
                },
            ),
        ]

        for tool_name, arguments in test_cases:
            results.append(self.test_call_tool("rag", base_url, tool_name, arguments))

        return results

    def test_performance_parallel(self, concurrency: int = 5) -> list[McpTestResult]:
        """Test performance under parallel load"""
        results = []

        def make_request():
            return self.test_call_tool("llm", self.services["llm"], "list_providers", {})

        start_time = time.time()
        with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [executor.submit(make_request) for _ in range(concurrency)]
            parallel_results = [future.result() for future in concurrent.futures.as_completed(futures)]

        total_time = int((time.time() - start_time) * 1000)

        successful_requests = sum(1 for r in parallel_results if r.success)
        success_rate = (successful_requests / concurrency) * 100

        # Create summary result
        performance_result = McpTestResult(
            "performance_parallel",
            "performance",
            "parallel_load",
            success_rate >= 80,
            total_time,
            metadata={
                "concurrency": concurrency,
                "successful_requests": successful_requests,
                "success_rate": success_rate,
                "average_request_time": total_time / concurrency,
            },
        )

        results.append(performance_result)
        results.extend(parallel_results)
        return results

    def test_integration_workflow(self) -> list[McpTestResult]:
        """Test complete integration workflows"""
        results = []

        # Test organization -> debate creation workflow
        start_time = time.time()
        workflow_success = True
        workflow_steps = []

        try:
            # Step 1: Create organization
            org_result = self.test_call_tool(
                "organization",
                self.services["organization"],
                "create_organization",
                {"name": "Integration Test Org", "description": "Test"},
            )
            workflow_steps.append("create_organization")
            if not org_result.success:
                workflow_success = False

            # Step 2: Create debate
            debate_result = self.test_call_tool(
                "controller",
                self.services["controller"],
                "create_debate",
                {
                    "topic": "Integration Test Topic",
                    "format": "OXFORD",
                    "organizationId": "integration-test-org",
                    "participants": ["user1", "user2"],
                    "maxRounds": 2,
                },
            )
            workflow_steps.append("create_debate")
            if not debate_result.success:
                workflow_success = False

            # Step 3: Test LLM integration
            llm_result = self.test_call_tool("llm", self.services["llm"], "list_providers", {})
            workflow_steps.append("list_providers")
            if not llm_result.success:
                workflow_success = False

        except Exception:
            workflow_success = False

        execution_time = int((time.time() - start_time) * 1000)

        workflow_result = McpTestResult(
            "integration_workflow",
            "integration",
            "workflow",
            workflow_success,
            execution_time,
            metadata={"steps_executed": workflow_steps},
        )

        results.append(workflow_result)
        return results

    def run_comprehensive_test_suite(self) -> dict:
        """Run all tests and return comprehensive results"""

        start_time = time.time()
        all_results = []

        # Test each service
        org_results = self.test_organization_service()
        all_results.extend(org_results)

        context_results = self.test_context_service()
        all_results.extend(context_results)

        llm_results = self.test_llm_service()
        all_results.extend(llm_results)

        controller_results = self.test_controller_service()
        all_results.extend(controller_results)

        rag_results = self.test_rag_service()
        all_results.extend(rag_results)

        performance_results = self.test_performance_parallel()
        all_results.extend(performance_results)

        integration_results = self.test_integration_workflow()
        all_results.extend(integration_results)

        total_execution_time = int((time.time() - start_time) * 1000)

        # Analyze results
        return self.analyze_results(all_results, total_execution_time)

    def analyze_results(self, results: list[McpTestResult], total_time: int) -> dict:
        """Analyze test results and generate summary"""
        total_tests = len(results)
        passed_tests = sum(1 for r in results if r.success)
        failed_tests = total_tests - passed_tests
        success_rate = (passed_tests / total_tests * 100) if total_tests > 0 else 0

        # Group by service
        by_service = {}
        for result in results:
            service = result.service_name
            if service not in by_service:
                by_service[service] = []
            by_service[service].append(result)

        # Service statistics
        service_stats = {}
        for service, service_results in by_service.items():
            service_passed = sum(1 for r in service_results if r.success)
            service_total = len(service_results)
            service_stats[service] = {
                "total": service_total,
                "passed": service_passed,
                "failed": service_total - service_passed,
                "success_rate": (service_passed / service_total * 100) if service_total > 0 else 0,
            }

        # Performance analysis
        execution_times = [r.execution_time_ms for r in results if r.execution_time_ms > 0]
        avg_execution_time = sum(execution_times) / len(execution_times) if execution_times else 0
        max_execution_time = max(execution_times) if execution_times else 0
        min_execution_time = min(execution_times) if execution_times else 0

        # Failed tests details
        failed_tests_details = []
        for result in results:
            if not result.success:
                failed_tests_details.append(
                    {
                        "test_name": result.test_name,
                        "service": result.service_name,
                        "tool": result.tool_name,
                        "error": result.error_message,
                        "execution_time": result.execution_time_ms,
                    }
                )

        return {
            "summary": {
                "total_tests": total_tests,
                "passed_tests": passed_tests,
                "failed_tests": failed_tests,
                "success_rate": round(success_rate, 2),
                "total_execution_time_ms": total_time,
                "average_execution_time_ms": round(avg_execution_time, 2),
                "max_execution_time_ms": max_execution_time,
                "min_execution_time_ms": min_execution_time,
            },
            "service_statistics": service_stats,
            "failed_tests": failed_tests_details,
            "all_results": [
                {
                    "test_name": r.test_name,
                    "service_name": r.service_name,
                    "tool_name": r.tool_name,
                    "success": r.success,
                    "execution_time_ms": r.execution_time_ms,
                    "error_message": r.error_message,
                    "metadata": r.metadata,
                }
                for r in results
            ],
        }

    def generate_report(self, analysis: dict) -> str:
        """Generate comprehensive test report"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        report = f"""
===============================================================================
                    COMPREHENSIVE MCP TEST EXECUTION REPORT
===============================================================================
Generated: {timestamp}

EXECUTIVE SUMMARY
-----------------
Total Tests:           {analysis["summary"]["total_tests"]}
Passed Tests:          {analysis["summary"]["passed_tests"]}
Failed Tests:          {analysis["summary"]["failed_tests"]}
Success Rate:          {analysis["summary"]["success_rate"]}%
Total Execution Time:  {analysis["summary"]["total_execution_time_ms"]}ms
Average Test Time:     {analysis["summary"]["average_execution_time_ms"]}ms

SERVICE BREAKDOWN
-----------------
"""

        for service, stats in analysis["service_statistics"].items():
            report += f"{service.upper():<15}: {stats['passed']}/{stats['total']} ({stats['success_rate']:.1f}%)\n"

        if analysis["failed_tests"]:
            report += f"\nFAILED TESTS ({len(analysis['failed_tests'])})\n"
            report += "-" * 50 + "\n"
            for failed in analysis["failed_tests"]:
                report += f"❌ {failed['test_name']} ({failed['service']}/{failed['tool']})\n"
                report += f"   Error: {failed['error']}\n"
                report += f"   Time: {failed['execution_time']}ms\n\n"

        # Performance analysis
        slow_tests = [r for r in analysis["all_results"] if r["execution_time_ms"] > 1000]
        if slow_tests:
            report += f"\nSLOW TESTS (>1000ms) ({len(slow_tests)})\n"
            report += "-" * 50 + "\n"
            for slow in sorted(slow_tests, key=lambda x: x["execution_time_ms"], reverse=True):
                report += f"⚠️  {slow['test_name']} ({slow['service_name']}): {slow['execution_time_ms']}ms\n"

        # Recommendations
        report += "\nRECOMMENDATIONS\n"
        report += "-" * 50 + "\n"

        success_rate = analysis["summary"]["success_rate"]
        if success_rate >= 95:
            report += "✅ Excellent success rate! System is performing optimally.\n"
        elif success_rate >= 80:
            report += "⚠️  Good success rate, but some issues detected. Review failed tests.\n"
        else:
            report += "🔴 Low success rate. Significant issues require immediate attention.\n"

        if analysis["summary"]["failed_tests"] > 0:
            report += "🔍 Review failed tests and address underlying issues.\n"

        if slow_tests:
            report += "⏰ Performance optimization recommended for slow tests.\n"

        if analysis["summary"]["failed_tests"] == 0:
            report += "🎉 All tests passing! System ready for production.\n"

        report += "\n" + "=" * 80 + "\n"

        return report

    def save_report(self, analysis: dict, filename: str | None = None):
        """Save detailed report to file"""
        if filename is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"mcp-comprehensive-test-report-{timestamp}.txt"

        report = self.generate_report(analysis)

        with Path(filename).open("w") as f:
            f.write(report)

        # Save JSON data
        json_filename = filename.replace(".txt", ".json")
        with Path(json_filename).open("w") as f:
            json.dump(analysis, f, indent=2)


def main():
    """Main execution function"""

    framework = McpComprehensiveTestFramework()

    try:
        # Run comprehensive test suite
        analysis = framework.run_comprehensive_test_suite()

        # Generate and display report
        framework.generate_report(analysis)

        # Save detailed reports
        framework.save_report(analysis)

        # Exit with appropriate code
        success_rate = analysis["summary"]["success_rate"]
        if success_rate >= 95:
            sys.exit(0)
        elif success_rate >= 80:
            sys.exit(1)
        else:
            sys.exit(2)

    except KeyboardInterrupt:
        sys.exit(130)
    except Exception:
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
