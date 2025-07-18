#!/usr/bin/env python3
"""
Configuration manager for Kiro GitHub integration.
This module handles repository-specific settings, custom rules, and configuration validation.
"""

import base64
import json
import logging
import os
from typing import Any

import yaml

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(), logging.FileHandler("kiro_config_manager.log")],
)
logger = logging.getLogger("kiro_config_manager")

# Constants
DEFAULT_CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "..", ".kiro", "config", "github.yml")
CONFIG_SCHEMA_PATH = os.path.join(os.path.dirname(__file__), "..", "..", ".kiro", "config", "github.schema.json")


class ConfigManager:
    """Manages configuration for Kiro GitHub integration."""

    def __init__(self, github_client=None):
        """Initialize the configuration manager."""
        self.github = github_client
        self.default_config = self._load_default_config()
        self.schema = self._load_schema()

    def _load_default_config(self) -> dict[str, Any]:
        """Load the default configuration."""
        try:
            if os.path.exists(DEFAULT_CONFIG_PATH):
                with open(DEFAULT_CONFIG_PATH) as f:
                    return yaml.safe_load(f)
        except Exception as e:
            logger.warning(f"Error loading default config: {e!s}")

        # Return hardcoded default config if file not found or error
        return {
            "review": {
                "depth": "standard",
                "focus_areas": ["security", "performance", "style", "documentation"],
                "auto_fix": True,
                "comment_style": "educational",
            },
            "rules": {
                "custom_rules_enabled": True,
                "rule_sets": [
                    {"name": "Security Rules", "enabled": True},
                    {"name": "Performance Rules", "enabled": True},
                    {"name": "Style Guide", "enabled": True},
                ],
            },
            "notifications": {"channels": ["github"], "events": ["review_complete", "critical_issue"]},
        }

    def _load_schema(self) -> dict[str, Any]:
        """Load the configuration schema."""
        try:
            if os.path.exists(CONFIG_SCHEMA_PATH):
                with open(CONFIG_SCHEMA_PATH) as f:
                    return json.load(f)
        except Exception as e:
            logger.warning(f"Error loading schema: {e!s}")

        # Return a minimal schema if file not found or error
        return {
            "type": "object",
            "properties": {
                "review": {
                    "type": "object",
                    "properties": {
                        "depth": {"type": "string", "enum": ["basic", "standard", "thorough"]},
                        "focus_areas": {"type": "array", "items": {"type": "string"}},
                        "auto_fix": {"type": "boolean"},
                        "comment_style": {"type": "string", "enum": ["concise", "educational", "detailed"]},
                    },
                },
                "rules": {
                    "type": "object",
                    "properties": {
                        "custom_rules_enabled": {"type": "boolean"},
                        "rule_sets": {"type": "array", "items": {"type": "object"}},
                    },
                },
                "notifications": {
                    "type": "object",
                    "properties": {
                        "channels": {"type": "array", "items": {"type": "string"}},
                        "events": {"type": "array", "items": {"type": "string"}},
                    },
                },
            },
        }

    def get_repository_config(self, repo_owner: str, repo_name: str) -> dict[str, Any]:
        """Get configuration for a specific repository."""
        # Try to get repository-specific configuration
        repo_config = self._get_repo_config(repo_owner, repo_name)

        # Merge with default configuration
        config = self._merge_configs(self.default_config, repo_config)

        # Validate the configuration
        self._validate_config(config)

        return config

    def _get_repo_config(self, repo_owner: str, repo_name: str) -> dict[str, Any]:
        """Get repository-specific configuration."""
        if not self.github:
            logger.warning("No GitHub client provided, using default config")
            return {}

        try:
            # Try to get the configuration file
            config_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/.kiro/config/github.yml"
            response = self.github.get(config_url)

            # Decode content
            content = base64.b64decode(response.get("content", "")).decode("utf-8")

            # Parse YAML
            config = yaml.safe_load(content)

            logger.info(f"Loaded configuration for {repo_owner}/{repo_name}")
            return config or {}

        except Exception as e:
            logger.warning(f"Error getting repository config: {e!s}")
            return {}

    def _merge_configs(self, base_config: dict[str, Any], override_config: dict[str, Any]) -> dict[str, Any]:
        """Merge two configurations, with override_config taking precedence."""
        if not override_config:
            return base_config.copy()

        result = base_config.copy()

        # Merge top-level sections
        for section, section_value in override_config.items():
            if section in result and isinstance(result[section], dict) and isinstance(section_value, dict):
                # Merge dictionaries
                result[section] = self._merge_configs(result[section], section_value)
            elif section in result and isinstance(result[section], list) and isinstance(section_value, list):
                # For lists, use the override list
                result[section] = section_value
            else:
                # For other types, use the override value
                result[section] = section_value

        return result

    def _validate_config(self, config: dict[str, Any]) -> bool:
        """Validate a configuration against the schema."""
        try:
            # Basic validation
            if not isinstance(config, dict):
                logger.warning("Configuration must be a dictionary")
                return False

            # Validate review section
            if "review" in config:
                review = config["review"]
                if not isinstance(review, dict):
                    logger.warning("Review section must be a dictionary")
                    return False

                # Validate depth
                if "depth" in review and review["depth"] not in ["basic", "standard", "thorough"]:
                    logger.warning(f"Invalid review depth: {review['depth']}")
                    review["depth"] = "standard"

                # Validate focus_areas
                if "focus_areas" in review and not isinstance(review["focus_areas"], list):
                    logger.warning("Focus areas must be a list")
                    review["focus_areas"] = ["security", "performance", "style", "documentation"]

                # Validate auto_fix
                if "auto_fix" in review and not isinstance(review["auto_fix"], bool):
                    logger.warning("Auto fix must be a boolean")
                    review["auto_fix"] = True

                # Validate comment_style
                if "comment_style" in review and review["comment_style"] not in ["concise", "educational", "detailed"]:
                    logger.warning(f"Invalid comment style: {review['comment_style']}")
                    review["comment_style"] = "educational"

            # Validate rules section
            if "rules" in config:
                rules = config["rules"]
                if not isinstance(rules, dict):
                    logger.warning("Rules section must be a dictionary")
                    return False

                # Validate custom_rules_enabled
                if "custom_rules_enabled" in rules and not isinstance(rules["custom_rules_enabled"], bool):
                    logger.warning("Custom rules enabled must be a boolean")
                    rules["custom_rules_enabled"] = True

                # Validate rule_sets
                if "rule_sets" in rules:
                    if not isinstance(rules["rule_sets"], list):
                        logger.warning("Rule sets must be a list")
                        rules["rule_sets"] = []

                    # Validate each rule set
                    for i, rule_set in enumerate(rules["rule_sets"]):
                        if not isinstance(rule_set, dict):
                            logger.warning(f"Rule set {i} must be a dictionary")
                            continue

                        # Validate name
                        if "name" not in rule_set or not isinstance(rule_set["name"], str):
                            logger.warning(f"Rule set {i} must have a name")
                            rule_set["name"] = f"Rule Set {i}"

                        # Validate enabled
                        if "enabled" in rule_set and not isinstance(rule_set["enabled"], bool):
                            logger.warning(f"Rule set {i} enabled must be a boolean")
                            rule_set["enabled"] = True

                        # Validate rules
                        if "rules" in rule_set:
                            if not isinstance(rule_set["rules"], list):
                                logger.warning(f"Rules in rule set {i} must be a list")
                                rule_set["rules"] = []

                            # Validate each rule
                            for j, rule in enumerate(rule_set["rules"]):
                                if not isinstance(rule, dict):
                                    logger.warning(f"Rule {j} in rule set {i} must be a dictionary")
                                    continue

                                # Validate id
                                if "id" not in rule or not isinstance(rule["id"], str):
                                    logger.warning(f"Rule {j} in rule set {i} must have an id")
                                    rule["id"] = f"rule-{j}"

                                # Validate severity
                                if "severity" in rule and rule["severity"] not in [
                                    "critical",
                                    "major",
                                    "minor",
                                    "suggestion",
                                ]:
                                    logger.warning(f"Invalid severity for rule {j} in rule set {i}: {rule['severity']}")
                                    rule["severity"] = "suggestion"

            # Validate notifications section
            if "notifications" in config:
                notifications = config["notifications"]
                if not isinstance(notifications, dict):
                    logger.warning("Notifications section must be a dictionary")
                    return False

                # Validate channels
                if "channels" in notifications and not isinstance(notifications["channels"], list):
                    logger.warning("Notification channels must be a list")
                    notifications["channels"] = ["github"]

                # Validate events
                if "events" in notifications and not isinstance(notifications["events"], list):
                    logger.warning("Notification events must be a list")
                    notifications["events"] = ["review_complete", "critical_issue"]

            return True

        except Exception as e:
            logger.error(f"Error validating configuration: {e!s}")
            return False

    def create_default_config(self, repo_owner: str, repo_name: str) -> bool:
        """Create a default configuration file in a repository."""
        if not self.github:
            logger.warning("No GitHub client provided, cannot create config")
            return False

        try:
            # Convert default config to YAML
            config_yaml = yaml.dump(self.default_config, default_flow_style=False)

            # Create the file in the repository
            contents_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/.kiro/config/github.yml"

            # Check if file already exists
            try:
                self.github.get(contents_url)
                logger.warning(f"Configuration file already exists in {repo_owner}/{repo_name}")
                return False
            except:
                # File doesn't exist, create it
                pass

            # Create the file
            data = {
                "message": "Create default Kiro configuration",
                "content": base64.b64encode(config_yaml.encode("utf-8")).decode("utf-8"),
                "branch": "main",  # This might need to be configurable
            }

            self.github.put(contents_url, json=data)
            logger.info(f"Created default configuration in {repo_owner}/{repo_name}")
            return True

        except Exception as e:
            logger.error(f"Error creating default config: {e!s}")
            return False

    def update_config(self, repo_owner: str, repo_name: str, config_updates: dict[str, Any]) -> bool:
        """Update configuration in a repository."""
        if not self.github:
            logger.warning("No GitHub client provided, cannot update config")
            return False

        try:
            # Get current configuration
            current_config = self.get_repository_config(repo_owner, repo_name)

            # Merge with updates
            updated_config = self._merge_configs(current_config, config_updates)

            # Validate the updated configuration
            if not self._validate_config(updated_config):
                logger.warning("Invalid configuration updates")
                return False

            # Convert to YAML
            config_yaml = yaml.dump(updated_config, default_flow_style=False)

            # Update the file in the repository
            contents_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/.kiro/config/github.yml"

            # Get current file to get the SHA
            try:
                response = self.github.get(contents_url)
                sha = response.get("sha")
            except:
                # File doesn't exist, create it instead
                return self.create_default_config(repo_owner, repo_name)

            # Update the file
            data = {
                "message": "Update Kiro configuration",
                "content": base64.b64encode(config_yaml.encode("utf-8")).decode("utf-8"),
                "sha": sha,
                "branch": "main",  # This might need to be configurable
            }

            self.github.put(contents_url, json=data)
            logger.info(f"Updated configuration in {repo_owner}/{repo_name}")
            return True

        except Exception as e:
            logger.error(f"Error updating config: {e!s}")
            return False

    def add_custom_rule(self, repo_owner: str, repo_name: str, rule_set_name: str, rule: dict[str, Any]) -> bool:
        """Add a custom rule to a rule set."""
        if not self.github:
            logger.warning("No GitHub client provided, cannot add custom rule")
            return False

        try:
            # Get current configuration
            current_config = self.get_repository_config(repo_owner, repo_name)

            # Ensure rules section exists
            if "rules" not in current_config:
                current_config["rules"] = {"custom_rules_enabled": True, "rule_sets": []}

            # Ensure rule_sets exists
            if "rule_sets" not in current_config["rules"]:
                current_config["rules"]["rule_sets"] = []

            # Find the rule set
            rule_set = None
            for rs in current_config["rules"]["rule_sets"]:
                if rs.get("name") == rule_set_name:
                    rule_set = rs
                    break

            # Create rule set if it doesn't exist
            if not rule_set:
                rule_set = {"name": rule_set_name, "enabled": True, "rules": []}
                current_config["rules"]["rule_sets"].append(rule_set)

            # Ensure rules list exists
            if "rules" not in rule_set:
                rule_set["rules"] = []

            # Check if rule already exists
            for i, r in enumerate(rule_set["rules"]):
                if r.get("id") == rule.get("id"):
                    # Update existing rule
                    rule_set["rules"][i] = rule
                    break
            else:
                # Add new rule
                rule_set["rules"].append(rule)

            # Update the configuration
            return self.update_config(repo_owner, repo_name, current_config)

        except Exception as e:
            logger.error(f"Error adding custom rule: {e!s}")
            return False

    def remove_custom_rule(self, repo_owner: str, repo_name: str, rule_set_name: str, rule_id: str) -> bool:
        """Remove a custom rule from a rule set."""
        if not self.github:
            logger.warning("No GitHub client provided, cannot remove custom rule")
            return False

        try:
            # Get current configuration
            current_config = self.get_repository_config(repo_owner, repo_name)

            # Check if rules section exists
            if "rules" not in current_config or "rule_sets" not in current_config["rules"]:
                logger.warning("No rule sets found in configuration")
                return False

            # Find the rule set
            for rule_set in current_config["rules"]["rule_sets"]:
                if rule_set.get("name") == rule_set_name and "rules" in rule_set:
                    # Find and remove the rule
                    for i, rule in enumerate(rule_set["rules"]):
                        if rule.get("id") == rule_id:
                            rule_set["rules"].pop(i)
                            # Update the configuration
                            return self.update_config(repo_owner, repo_name, current_config)

            logger.warning(f"Rule {rule_id} not found in rule set {rule_set_name}")
            return False

        except Exception as e:
            logger.error(f"Error removing custom rule: {e!s}")
            return False

    def get_team_standards(self, repo_owner: str, repo_name: str) -> dict[str, Any]:
        """Get team coding standards configuration."""
        # Get repository configuration
        config = self.get_repository_config(repo_owner, repo_name)

        # Extract team standards
        team_standards = {}

        # Check for style guide rule set
        if "rules" in config and "rule_sets" in config["rules"]:
            for rule_set in config["rules"]["rule_sets"]:
                if rule_set.get("name") == "Style Guide" and rule_set.get("enabled", True):
                    team_standards["style_guide"] = rule_set.get("rules", [])
                    break

        # Check for other team standards
        if "team_standards" in config:
            team_standards.update(config["team_standards"])

        return team_standards

    def set_team_standards(self, repo_owner: str, repo_name: str, standards: dict[str, Any]) -> bool:
        """Set team coding standards configuration."""
        if not self.github:
            logger.warning("No GitHub client provided, cannot set team standards")
            return False

        try:
            # Get current configuration
            current_config = self.get_repository_config(repo_owner, repo_name)

            # Update style guide rules if provided
            if "style_guide" in standards:
                # Ensure rules section exists
                if "rules" not in current_config:
                    current_config["rules"] = {"custom_rules_enabled": True, "rule_sets": []}

                # Ensure rule_sets exists
                if "rule_sets" not in current_config["rules"]:
                    current_config["rules"]["rule_sets"] = []

                # Find the style guide rule set
                style_guide = None
                for rule_set in current_config["rules"]["rule_sets"]:
                    if rule_set.get("name") == "Style Guide":
                        style_guide = rule_set
                        break

                # Create style guide if it doesn't exist
                if not style_guide:
                    style_guide = {"name": "Style Guide", "enabled": True, "rules": []}
                    current_config["rules"]["rule_sets"].append(style_guide)

                # Update rules
                style_guide["rules"] = standards["style_guide"]

                # Remove from standards to avoid duplication
                del standards["style_guide"]

            # Update other team standards
            if standards:
                current_config["team_standards"] = standards

            # Update the configuration
            return self.update_config(repo_owner, repo_name, current_config)

        except Exception as e:
            logger.error(f"Error setting team standards: {e!s}")
            return False


def get_repository_config(github_client, repo_owner: str, repo_name: str) -> dict[str, Any]:
    """Get configuration for a specific repository."""
    manager = ConfigManager(github_client)
    return manager.get_repository_config(repo_owner, repo_name)


def create_default_config(github_client, repo_owner: str, repo_name: str) -> bool:
    """Create a default configuration file in a repository."""
    manager = ConfigManager(github_client)
    return manager.create_default_config(repo_owner, repo_name)


def update_config(github_client, repo_owner: str, repo_name: str, config_updates: dict[str, Any]) -> bool:
    """Update configuration in a repository."""
    manager = ConfigManager(github_client)
    return manager.update_config(repo_owner, repo_name, config_updates)


def add_custom_rule(github_client, repo_owner: str, repo_name: str, rule_set_name: str, rule: dict[str, Any]) -> bool:
    """Add a custom rule to a rule set."""
    manager = ConfigManager(github_client)
    return manager.add_custom_rule(repo_owner, repo_name, rule_set_name, rule)


def remove_custom_rule(github_client, repo_owner: str, repo_name: str, rule_set_name: str, rule_id: str) -> bool:
    """Remove a custom rule from a rule set."""
    manager = ConfigManager(github_client)
    return manager.remove_custom_rule(repo_owner, repo_name, rule_set_name, rule_id)


def get_team_standards(github_client, repo_owner: str, repo_name: str) -> dict[str, Any]:
    """Get team coding standards configuration."""
    manager = ConfigManager(github_client)
    return manager.get_team_standards(repo_owner, repo_name)


def set_team_standards(github_client, repo_owner: str, repo_name: str, standards: dict[str, Any]) -> bool:
    """Set team coding standards configuration."""
    manager = ConfigManager(github_client)
    return manager.set_team_standards(repo_owner, repo_name, standards)


if __name__ == "__main__":
    # Example usage
    import sys

    if len(sys.argv) < 3:
        sys.exit(1)

    repo_owner = sys.argv[1]
    repo_name = sys.argv[2]

    # Create a mock GitHub client
    class MockGitHubClient:
        def get(self, url, params=None):
            raise Exception("File not found")

        def put(self, url, json=None):
            return {}

    # Get configuration
    manager = ConfigManager(MockGitHubClient())
    config = manager.get_repository_config(repo_owner, repo_name)

    # Create default configuration
    manager.create_default_config(repo_owner, repo_name)
