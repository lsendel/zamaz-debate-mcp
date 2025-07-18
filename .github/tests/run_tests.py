#!/usr/bin/env python3
"""
Test runner for the refactored Kiro GitHub Integration.

This script sets up the Python path and runs all tests.
"""

import os
import subprocess
import sys
from pathlib import Path

# Add project root to Python path
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

# Set up test environment
os.environ["PYTHONDONTWRITEBYTECODE"] = "1"
os.environ["TEST_MODE"] = "true"


def run_tests():
    """Run all tests."""

    # Test categories
    test_suites = [
        {"name": "Unit Tests - Core", "pattern": "test_*.py", "directory": ".github/tests"},
        {"name": "Integration Tests", "pattern": "test_*.py", "directory": ".github/tests/integration"},
    ]

    all_passed = True

    for suite in test_suites:
        cmd = [sys.executable, "-m", "pytest", suite["directory"], "-v", "--tb=short", f"-k {suite['pattern']}"]

        result = subprocess.run(cmd, cwd=project_root, check=False)

        if result.returncode != 0:
            all_passed = False
        else:
            pass

    if all_passed:
        return 0
    else:
        return 1


if __name__ == "__main__":
    sys.exit(run_tests())
