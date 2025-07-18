#!/usr/bin/env python3
"""
Command-line interface for managing Kiro GitHub integration configurations.
This script provides commands for creating, updating, and validating configurations.
"""

import argparse
import base64
import json
import logging
import sys

import yaml
from config_manager import (
    ConfigManager,
    add_custom_rule,
    create_default_config,
    get_repository_config,
    get_team_standards,
    remove_custom_rule,
    set_team_standards,
    update_config,
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(), logging.FileHandler("kiro_config_cli.log")],
)
logger = logging.getLogger("kiro_config_cli")


class MockGitHubClient:
    """Mock GitHub client for local testing."""

    def __init__(self, config_path=None):
        """Initialize the mock GitHub client."""
        self.config_path = config_path

    def get(self, url, params=None):
        """Mock GET request."""
        if "contents/.kiro/config/github.yml" in url and self.config_path:
            try:
                with open(self.config_path) as f:
                    content = f.read()

                import base64

                encoded_content = base64.b64encode(content.encode("utf-8")).decode("utf-8")

                return {"content": encoded_content, "sha": "mock-sha"}
            except FileNotFoundError:
                raise Exception("File not found")

        raise Exception("Not implemented")

    def put(self, url, json=None):
        """Mock PUT request."""
        if "contents/.kiro/config/github.yml" in url and self.config_path:
            try:
                content = base64.b64decode(json["content"]).decode("utf-8")

                # Write to file
                with open(self.config_path, "w") as f:
                    f.write(content)

                return {"content": {"sha": "new-mock-sha"}}
            except Exception as e:
                raise Exception(f"Error writing file: {e!s}")

        raise Exception("Not implemented")


def create_parser():
    """Create the argument parser."""
    parser = argparse.ArgumentParser(description="Kiro GitHub Integration Configuration CLI")
    subparsers = parser.add_subparsers(dest="command", help="Command to execute")

    # get command
    get_parser = subparsers.add_parser("get", help="Get configuration")
    get_parser.add_argument("--repo", required=True, help="Repository in format owner/name")
    get_parser.add_argument("--token", help="GitHub token")
    get_parser.add_argument("--local", help="Local configuration file path")
    get_parser.add_argument("--output", help="Output file path")
    get_parser.add_argument("--format", choices=["yaml", "json"], default="yaml", help="Output format")

    # create command
    create_parser = subparsers.add_parser("create", help="Create default configuration")
    create_parser.add_argument("--repo", required=True, help="Repository in format owner/name")
    create_parser.add_argument("--token", help="GitHub token")
    create_parser.add_argument("--local", help="Local configuration file path")

    # update command
    update_parser = subparsers.add_parser("update", help="Update configuration")
    update_parser.add_argument("--repo", required=True, help="Repository in format owner/name")
    update_parser.add_argument("--token", help="GitHub token")
    update_parser.add_argument("--local", help="Local configuration file path")
    update_parser.add_argument("--updates", required=True, help="Updates in JSON or YAML format")
    update_parser.add_argument("--updates-file", help="File containing updates in JSON or YAML format")

    # validate command
    validate_parser = subparsers.add_parser("validate", help="Validate configuration")
    validate_parser.add_argument("--config", required=True, help="Configuration file path")

    # add-rule command
    add_rule_parser = subparsers.add_parser("add-rule", help="Add custom rule")
    add_rule_parser.add_argument("--repo", required=True, help="Repository in format owner/name")
    add_rule_parser.add_argument("--token", help="GitHub token")
    add_rule_parser.add_argument("--local", help="Local configuration file path")
    add_rule_parser.add_argument("--rule-set", required=True, help="Rule set name")
    add_rule_parser.add_argument("--rule", required=True, help="Rule in JSON or YAML format")
    add_rule_parser.add_argument("--rule-file", help="File containing rule in JSON or YAML format")

    # remove-rule command
    remove_rule_parser = subparsers.add_parser("remove-rule", help="Remove custom rule")
    remove_rule_parser.add_argument("--repo", required=True, help="Repository in format owner/name")
    remove_rule_parser.add_argument("--token", help="GitHub token")
    remove_rule_parser.add_argument("--local", help="Local configuration file path")
    remove_rule_parser.add_argument("--rule-set", required=True, help="Rule set name")
    remove_rule_parser.add_argument("--rule-id", required=True, help="Rule ID")

    # get-standards command
    get_standards_parser = subparsers.add_parser("get-standards", help="Get team coding standards")
    get_standards_parser.add_argument("--repo", required=True, help="Repository in format owner/name")
    get_standards_parser.add_argument("--token", help="GitHub token")
    get_standards_parser.add_argument("--local", help="Local configuration file path")
    get_standards_parser.add_argument("--output", help="Output file path")
    get_standards_parser.add_argument("--format", choices=["yaml", "json"], default="yaml", help="Output format")

    # set-standards command
    set_standards_parser = subparsers.add_parser("set-standards", help="Set team coding standards")
    set_standards_parser.add_argument("--repo", required=True, help="Repository in format owner/name")
    set_standards_parser.add_argument("--token", help="GitHub token")
    set_standards_parser.add_argument("--local", help="Local configuration file path")
    set_standards_parser.add_argument("--standards", required=True, help="Standards in JSON or YAML format")
    set_standards_parser.add_argument("--standards-file", help="File containing standards in JSON or YAML format")

    return parser


def get_github_client(args):
    """Get a GitHub client based on arguments."""
    if args.local:
        return MockGitHubClient(args.local)

    if args.token:
        # Create a real GitHub client
        class GitHubClient:
            def __init__(self, token):
                self.token = token
                self.headers = {"Authorization": f"token {token}", "Accept": "application/vnd.github.v3+json"}

            def get(self, url, params=None):
                import requests

                response = requests.get(url, headers=self.headers, params=params, timeout=30)
                response.raise_for_status()
                return response.json()

            def put(self, url, json=None):
                import requests

                response = requests.put(url, headers=self.headers, json=json)
                response.raise_for_status()
                return response.json()

        return GitHubClient(args.token)

    logger.error("Either --local or --token must be provided")
    sys.exit(1)


def parse_repo(repo):
    """Parse repository string into owner and name."""
    parts = repo.split("/")
    if len(parts) != 2:
        logger.error("Repository must be in format owner/name")
        sys.exit(1)

    return parts[0], parts[1]


def parse_data(data_str, data_file):
    """Parse data from string or file."""
    if data_file:
        try:
            with open(data_file) as f:
                data_str = f.read()
        except Exception as e:
            logger.error(f"Error reading file: {e!s}")
            sys.exit(1)

    try:
        # Try parsing as JSON
        return json.loads(data_str)
    except json.JSONDecodeError:
        try:
            # Try parsing as YAML
            return yaml.safe_load(data_str)
        except yaml.YAMLError as e:
            logger.error(f"Error parsing data: {e!s}")
            sys.exit(1)


def handle_get(args):
    """Handle get command."""
    github = get_github_client(args)
    repo_owner, repo_name = parse_repo(args.repo)

    try:
        config = get_repository_config(github, repo_owner, repo_name)

        # Format output
        output = json.dumps(config, indent=2) if args.format == "json" else yaml.dump(config, default_flow_style=False)

        # Write to file or print to stdout
        if args.output:
            with open(args.output, "w") as f:
                f.write(output)
            logger.info(f"Configuration written to {args.output}")
        else:
            pass

    except Exception as e:
        logger.error(f"Error getting configuration: {e!s}")
        sys.exit(1)


def handle_create(args):
    """Handle create command."""
    github = get_github_client(args)
    repo_owner, repo_name = parse_repo(args.repo)

    try:
        success = create_default_config(github, repo_owner, repo_name)

        if success:
            logger.info(f"Default configuration created for {args.repo}")
        else:
            logger.warning(f"Failed to create default configuration for {args.repo}")
            sys.exit(1)

    except Exception as e:
        logger.error(f"Error creating configuration: {e!s}")
        sys.exit(1)


def handle_update(args):
    """Handle update command."""
    github = get_github_client(args)
    repo_owner, repo_name = parse_repo(args.repo)

    try:
        updates = parse_data(args.updates, args.updates_file)

        success = update_config(github, repo_owner, repo_name, updates)

        if success:
            logger.info(f"Configuration updated for {args.repo}")
        else:
            logger.warning(f"Failed to update configuration for {args.repo}")
            sys.exit(1)

    except Exception as e:
        logger.error(f"Error updating configuration: {e!s}")
        sys.exit(1)


def handle_validate(args):
    """Handle validate command."""
    try:
        # Load configuration
        with open(args.config) as f:
            config = json.load(f) if args.config.endswith(".json") else yaml.safe_load(f)

        # Validate configuration
        manager = ConfigManager()
        valid = manager._validate_config(config)

        if valid:
            logger.info("Configuration is valid")
        else:
            logger.warning("Configuration is invalid")
            sys.exit(1)

    except Exception as e:
        logger.error(f"Error validating configuration: {e!s}")
        sys.exit(1)


def handle_add_rule(args):
    """Handle add-rule command."""
    github = get_github_client(args)
    repo_owner, repo_name = parse_repo(args.repo)

    try:
        rule = parse_data(args.rule, args.rule_file)

        success = add_custom_rule(github, repo_owner, repo_name, args.rule_set, rule)

        if success:
            logger.info(f"Rule added to {args.rule_set} for {args.repo}")
        else:
            logger.warning(f"Failed to add rule to {args.rule_set} for {args.repo}")
            sys.exit(1)

    except Exception as e:
        logger.error(f"Error adding rule: {e!s}")
        sys.exit(1)


def handle_remove_rule(args):
    """Handle remove-rule command."""
    github = get_github_client(args)
    repo_owner, repo_name = parse_repo(args.repo)

    try:
        success = remove_custom_rule(github, repo_owner, repo_name, args.rule_set, args.rule_id)

        if success:
            logger.info(f"Rule {args.rule_id} removed from {args.rule_set} for {args.repo}")
        else:
            logger.warning(f"Failed to remove rule {args.rule_id} from {args.rule_set} for {args.repo}")
            sys.exit(1)

    except Exception as e:
        logger.error(f"Error removing rule: {e!s}")
        sys.exit(1)


def handle_get_standards(args):
    """Handle get-standards command."""
    github = get_github_client(args)
    repo_owner, repo_name = parse_repo(args.repo)

    try:
        standards = get_team_standards(github, repo_owner, repo_name)

        # Format output
        if args.format == "json":
            output = json.dumps(standards, indent=2)
        else:
            output = yaml.dump(standards, default_flow_style=False)

        # Write to file or print to stdout
        if args.output:
            with open(args.output, "w") as f:
                f.write(output)
            logger.info(f"Team standards written to {args.output}")
        else:
            pass

    except Exception as e:
        logger.error(f"Error getting team standards: {e!s}")
        sys.exit(1)


def handle_set_standards(args):
    """Handle set-standards command."""
    github = get_github_client(args)
    repo_owner, repo_name = parse_repo(args.repo)

    try:
        standards = parse_data(args.standards, args.standards_file)

        success = set_team_standards(github, repo_owner, repo_name, standards)

        if success:
            logger.info(f"Team standards updated for {args.repo}")
        else:
            logger.warning(f"Failed to update team standards for {args.repo}")
            sys.exit(1)

    except Exception as e:
        logger.error(f"Error setting team standards: {e!s}")
        sys.exit(1)


def main():
    """Main entry point."""
    parser = create_parser()
    args = parser.parse_args()

    if args.command == "get":
        handle_get(args)
    elif args.command == "create":
        handle_create(args)
    elif args.command == "update":
        handle_update(args)
    elif args.command == "validate":
        handle_validate(args)
    elif args.command == "add-rule":
        handle_add_rule(args)
    elif args.command == "remove-rule":
        handle_remove_rule(args)
    elif args.command == "get-standards":
        handle_get_standards(args)
    elif args.command == "set-standards":
        handle_set_standards(args)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
