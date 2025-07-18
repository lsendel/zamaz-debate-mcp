#!/usr/bin/env python3
"""
Learning system for Kiro GitHub integration.
This module implements machine learning capabilities to improve code review suggestions based on feedback.
"""

import logging
import os
import re
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import Any

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(), logging.FileHandler("kiro_learning.log")],
)
logger = logging.getLogger("kiro_learning")

# Constants
DEFAULT_DB_PATH = str(Path(__file__).parent / ".." / ".." / ".kiro" / "data" / "analytics.db")
LEARNING_DB_PATH = str(Path(__file__).parent / ".." / ".." / ".kiro" / "data" / "learning.db")


class LearningSystem:
    """Machine learning system for improving code review suggestions."""

    def __init__(self, analytics_db_path: str | None = None, learning_db_path: str | None = None):
        """Initialize the learning system."""
        self.analytics_db_path = analytics_db_path or DEFAULT_DB_PATH
        self.learning_db_path = learning_db_path or LEARNING_DB_PATH

        # Ensure directory exists
        os.makedirs(str(Path(self.learning_db_path).parent), exist_ok=True)

        # Initialize learning database
        self._init_learning_db()

    def _init_learning_db(self):
        """Initialize the learning database."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            # Create rule effectiveness table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS rule_effectiveness (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rule_id TEXT NOT NULL,
                category TEXT NOT NULL,
                total_occurrences INTEGER DEFAULT 0,
                positive_feedback INTEGER DEFAULT 0,
                negative_feedback INTEGER DEFAULT 0,
                suggestions_applied INTEGER DEFAULT 0,
                suggestions_rejected INTEGER DEFAULT 0,
                effectiveness_score REAL DEFAULT 0.5,
                last_updated TIMESTAMP NOT NULL,
                UNIQUE(rule_id, category)
            )
            """)

            # Create pattern learning table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS pattern_learning (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pattern_type TEXT NOT NULL,
                pattern_value TEXT NOT NULL,
                file_extension TEXT,
                success_count INTEGER DEFAULT 0,
                failure_count INTEGER DEFAULT 0,
                confidence_score REAL DEFAULT 0.5,
                last_updated TIMESTAMP NOT NULL,
                UNIQUE(pattern_type, pattern_value, file_extension)
            )
            """)

            # Create developer preferences table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS developer_preferences (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                developer TEXT NOT NULL,
                repo_owner TEXT NOT NULL,
                repo_name TEXT NOT NULL,
                rule_id TEXT NOT NULL,
                preference_score REAL DEFAULT 0.5,
                interaction_count INTEGER DEFAULT 0,
                last_updated TIMESTAMP NOT NULL,
                UNIQUE(developer, repo_owner, repo_name, rule_id)
            )
            """)

            # Create suggestion improvements table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS suggestion_improvements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                original_suggestion TEXT NOT NULL,
                improved_suggestion TEXT NOT NULL,
                improvement_type TEXT NOT NULL,
                success_rate REAL DEFAULT 0.0,
                usage_count INTEGER DEFAULT 0,
                last_updated TIMESTAMP NOT NULL
            )
            """)

            conn.commit()
            conn.close()

            logger.info("Learning database initialized")

        except Exception as e:
            logger.error(f"Error initializing learning database: {e!s}")

    def update_rule_effectiveness(
        self, rule_id: str, category: str, feedback_type: str, suggestion_applied: bool = False
    ):
        """Update the effectiveness of a rule based on feedback."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            # Get current effectiveness data
            cursor.execute(
                """
            SELECT total_occurrences, positive_feedback, negative_feedback,
                   suggestions_applied, suggestions_rejected, effectiveness_score
            FROM rule_effectiveness
            WHERE rule_id = ? AND category = ?
            """,
                (rule_id, category),
            )

            result = cursor.fetchone()

            if result:
                # Update existing record
                (
                    total_occurrences,
                    positive_feedback,
                    negative_feedback,
                    suggestions_applied,
                    suggestions_rejected,
                    current_score,
                ) = result

                total_occurrences += 1

                if feedback_type == "positive":
                    positive_feedback += 1
                elif feedback_type == "negative":
                    negative_feedback += 1

                if suggestion_applied:
                    suggestions_applied += 1
                else:
                    suggestions_rejected += 1

                # Calculate new effectiveness score
                effectiveness_score = self._calculate_effectiveness_score(
                    positive_feedback, negative_feedback, suggestions_applied, suggestions_rejected
                )

                cursor.execute(
                    """
                UPDATE rule_effectiveness
                SET total_occurrences = ?, positive_feedback = ?, negative_feedback = ?,
                    suggestions_applied = ?, suggestions_rejected = ?, effectiveness_score = ?,
                    last_updated = ?
                WHERE rule_id = ? AND category = ?
                """,
                    (
                        total_occurrences,
                        positive_feedback,
                        negative_feedback,
                        suggestions_applied,
                        suggestions_rejected,
                        effectiveness_score,
                        datetime.now().isoformat(),
                        rule_id,
                        category,
                    ),
                )
            else:
                # Create new record
                positive_feedback = 1 if feedback_type == "positive" else 0
                negative_feedback = 1 if feedback_type == "negative" else 0
                suggestions_applied = 1 if suggestion_applied else 0
                suggestions_rejected = 0 if suggestion_applied else 1

                effectiveness_score = self._calculate_effectiveness_score(
                    positive_feedback, negative_feedback, suggestions_applied, suggestions_rejected
                )

                cursor.execute(
                    """
                INSERT INTO rule_effectiveness
                (rule_id, category, total_occurrences, positive_feedback, negative_feedback,
                 suggestions_applied, suggestions_rejected, effectiveness_score, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    (
                        rule_id,
                        category,
                        1,
                        positive_feedback,
                        negative_feedback,
                        suggestions_applied,
                        suggestions_rejected,
                        effectiveness_score,
                        datetime.now().isoformat(),
                    ),
                )

            conn.commit()
            conn.close()

            logger.info(f"Updated effectiveness for rule {rule_id} in category {category}")
            return True

        except Exception as e:
            logger.error(f"Error updating rule effectiveness: {e!s}")
            return False

    def _calculate_effectiveness_score(
        self, positive_feedback: int, negative_feedback: int, suggestions_applied: int, suggestions_rejected: int
    ) -> float:
        """Calculate effectiveness score for a rule."""
        # Weight different factors
        feedback_weight = 0.4
        suggestion_weight = 0.6

        # Calculate feedback score
        total_feedback = positive_feedback + negative_feedback
        if total_feedback > 0:
            feedback_score = positive_feedback / total_feedback
        else:
            feedback_score = 0.5  # Neutral if no feedback

        # Calculate suggestion score
        total_suggestions = suggestions_applied + suggestions_rejected
        if total_suggestions > 0:
            suggestion_score = suggestions_applied / total_suggestions
        else:
            suggestion_score = 0.5  # Neutral if no suggestions

        # Combine scores
        effectiveness_score = (feedback_weight * feedback_score) + (suggestion_weight * suggestion_score)

        # Apply confidence adjustment based on sample size
        confidence_factor = min(1.0, (total_feedback + total_suggestions) / 10.0)
        effectiveness_score = 0.5 + (effectiveness_score - 0.5) * confidence_factor

        return max(0.0, min(1.0, effectiveness_score))

    def learn_pattern(self, pattern_type: str, pattern_value: str, file_extension: str, success: bool):
        """Learn from pattern usage."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            # Get current pattern data
            cursor.execute(
                """
            SELECT success_count, failure_count, confidence_score
            FROM pattern_learning
            WHERE pattern_type = ? AND pattern_value = ? AND file_extension = ?
            """,
                (pattern_type, pattern_value, file_extension),
            )

            result = cursor.fetchone()

            if result:
                # Update existing record
                success_count, failure_count, current_score = result

                if success:
                    success_count += 1
                else:
                    failure_count += 1

                # Calculate new confidence score
                total_attempts = success_count + failure_count
                confidence_score = success_count / total_attempts if total_attempts > 0 else 0.5

                cursor.execute(
                    """
                UPDATE pattern_learning
                SET success_count = ?, failure_count = ?, confidence_score = ?, last_updated = ?
                WHERE pattern_type = ? AND pattern_value = ? AND file_extension = ?
                """,
                    (
                        success_count,
                        failure_count,
                        confidence_score,
                        datetime.now().isoformat(),
                        pattern_type,
                        pattern_value,
                        file_extension,
                    ),
                )
            else:
                # Create new record
                success_count = 1 if success else 0
                failure_count = 0 if success else 1
                confidence_score = 1.0 if success else 0.0

                cursor.execute(
                    """
                INSERT INTO pattern_learning
                (pattern_type, pattern_value, file_extension, success_count, failure_count,
                 confidence_score, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                    (
                        pattern_type,
                        pattern_value,
                        file_extension,
                        success_count,
                        failure_count,
                        confidence_score,
                        datetime.now().isoformat(),
                    ),
                )

            conn.commit()
            conn.close()

            logger.info(f"Learned pattern {pattern_type}:{pattern_value} for {file_extension}")
            return True

        except Exception as e:
            logger.error(f"Error learning pattern: {e!s}")
            return False

    def update_developer_preferences(
        self, developer: str, repo_owner: str, repo_name: str, rule_id: str, preference_score: float
    ):
        """Update developer preferences for specific rules."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            # Get current preference data
            cursor.execute(
                """
            SELECT preference_score, interaction_count
            FROM developer_preferences
            WHERE developer = ? AND repo_owner = ? AND repo_name = ? AND rule_id = ?
            """,
                (developer, repo_owner, repo_name, rule_id),
            )

            result = cursor.fetchone()

            if result:
                # Update existing record with exponential moving average
                current_score, interaction_count = result
                interaction_count += 1

                # Use exponential moving average to update preference
                alpha = 0.3  # Learning rate
                new_preference_score = alpha * preference_score + (1 - alpha) * current_score

                cursor.execute(
                    """
                UPDATE developer_preferences
                SET preference_score = ?, interaction_count = ?, last_updated = ?
                WHERE developer = ? AND repo_owner = ? AND repo_name = ? AND rule_id = ?
                """,
                    (
                        new_preference_score,
                        interaction_count,
                        datetime.now().isoformat(),
                        developer,
                        repo_owner,
                        repo_name,
                        rule_id,
                    ),
                )
            else:
                # Create new record
                cursor.execute(
                    """
                INSERT INTO developer_preferences
                (developer, repo_owner, repo_name, rule_id, preference_score, interaction_count, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                    (developer, repo_owner, repo_name, rule_id, preference_score, 1, datetime.now().isoformat()),
                )

            conn.commit()
            conn.close()

            logger.info(f"Updated preferences for {developer} on rule {rule_id}")
            return True

        except Exception as e:
            logger.error(f"Error updating developer preferences: {e!s}")
            return False

    def get_rule_recommendations(
        self, repo_owner: str, repo_name: str, developer: str | None = None
    ) -> list[dict[str, Any]]:
        """Get rule recommendations based on effectiveness and preferences."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            # Get rule effectiveness
            cursor.execute("""
            SELECT rule_id, category, effectiveness_score, total_occurrences
            FROM rule_effectiveness
            WHERE effectiveness_score > 0.6 AND total_occurrences >= 3
            ORDER BY effectiveness_score DESC
            LIMIT 20
            """)

            effectiveness_results = cursor.fetchall()

            recommendations = []

            for rule_id, category, effectiveness_score, total_occurrences in effectiveness_results:
                recommendation = {
                    "rule_id": rule_id,
                    "category": category,
                    "effectiveness_score": effectiveness_score,
                    "total_occurrences": total_occurrences,
                    "developer_preference_score": 0.5,  # Default neutral
                }

                # Get developer preference if available
                if developer:
                    cursor.execute(
                        """
                    SELECT preference_score, interaction_count
                    FROM developer_preferences
                    WHERE developer = ? AND repo_owner = ? AND repo_name = ? AND rule_id = ?
                    """,
                        (developer, repo_owner, repo_name, rule_id),
                    )

                    pref_result = cursor.fetchone()
                    if pref_result:
                        preference_score, interaction_count = pref_result
                        recommendation["developer_preference_score"] = preference_score
                        recommendation["developer_interaction_count"] = interaction_count

                # Calculate combined score
                combined_score = (effectiveness_score + recommendation["developer_preference_score"]) / 2
                recommendation["combined_score"] = combined_score

                recommendations.append(recommendation)

            conn.close()

            # Sort by combined score
            recommendations.sort(key=lambda x: x["combined_score"], reverse=True)

            return recommendations

        except Exception as e:
            logger.error(f"Error getting rule recommendations: {e!s}")
            return []

    def get_pattern_confidence(self, pattern_type: str, pattern_value: str, file_extension: str) -> float:
        """Get confidence score for a pattern."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            cursor.execute(
                """
            SELECT confidence_score, success_count, failure_count
            FROM pattern_learning
            WHERE pattern_type = ? AND pattern_value = ? AND file_extension = ?
            """,
                (pattern_type, pattern_value, file_extension),
            )

            result = cursor.fetchone()
            conn.close()

            if result:
                confidence_score, success_count, failure_count = result

                # Adjust confidence based on sample size
                total_attempts = success_count + failure_count
                if total_attempts < 5:
                    # Reduce confidence for small sample sizes
                    confidence_score *= total_attempts / 5.0

                return confidence_score
            else:
                return 0.5  # Default neutral confidence

        except Exception as e:
            logger.error(f"Error getting pattern confidence: {e!s}")
            return 0.5

    def improve_suggestion(self, original_suggestion: str, context: dict[str, Any]) -> str:
        """Improve a suggestion based on learned patterns."""
        try:
            # Get file extension from context
            file_path = context.get("file_path", "")
            file_extension = os.path.splitext(file_path)[1].lower()

            # Check for learned improvements
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            cursor.execute(
                """
            SELECT improved_suggestion, success_rate, usage_count
            FROM suggestion_improvements
            WHERE original_suggestion = ? AND success_rate > 0.7
            ORDER BY success_rate DESC, usage_count DESC
            LIMIT 1
            """,
                (original_suggestion,),
            )

            result = cursor.fetchone()
            conn.close()

            if result:
                improved_suggestion, success_rate, usage_count = result
                logger.info(f"Using improved suggestion with {success_rate:.2f} success rate")
                return improved_suggestion

            # Apply general improvements based on patterns
            improved_suggestion = self._apply_general_improvements(original_suggestion, file_extension)

            return improved_suggestion

        except Exception as e:
            logger.error(f"Error improving suggestion: {e!s}")
            return original_suggestion

    def _apply_general_improvements(self, suggestion: str, file_extension: str) -> str:
        """Apply general improvements to suggestions."""
        improved = suggestion

        # Language-specific improvements
        if file_extension == ".py":
            # Python-specific improvements
            improved = self._improve_python_suggestion(improved)
        elif file_extension in [".js", ".ts"]:
            # JavaScript/TypeScript-specific improvements
            improved = self._improve_javascript_suggestion(improved)
        elif file_extension == ".java":
            # Java-specific improvements
            improved = self._improve_java_suggestion(improved)

        return improved

    def _improve_python_suggestion(self, suggestion: str) -> str:
        """Apply Python-specific improvements."""
        # Add type hints if missing
        if "def " in suggestion and "->" not in suggestion and ":" in suggestion:
            # Simple heuristic to add return type hint
            suggestion = suggestion.replace(":", " -> None:")

        # Improve variable naming
        suggestion = re.sub(r"\bvar\b", "variable", suggestion)
        suggestion = re.sub(r"\btemp\b", "temporary", suggestion)

        return suggestion

    def _improve_javascript_suggestion(self, suggestion: str) -> str:
        """Apply JavaScript/TypeScript-specific improvements."""
        # Use const instead of let for constants
        if re.search(r'let\s+\w+\s*=\s*["\'\d]', suggestion):
            suggestion = re.sub(r"\blet\b", "const", suggestion)

        # Add semicolons if missing
        lines = suggestion.split("\n")
        improved_lines = []
        for line in lines:
            stripped = line.strip()
            if stripped and not stripped.endswith((";", "{", "}", ",")):
                if not stripped.startswith(("if", "for", "while", "function", "class")):
                    line += ";"
            improved_lines.append(line)

        return "\n".join(improved_lines)

    def _improve_java_suggestion(self, suggestion: str) -> str:
        """Apply Java-specific improvements."""
        # Add proper access modifiers
        if "class " in suggestion and not any(mod in suggestion for mod in ["public", "private", "protected"]):
            suggestion = suggestion.replace("class ", "public class ")

        # Improve method declarations
        if "void " in suggestion and not any(mod in suggestion for mod in ["public", "private", "protected"]):
            suggestion = suggestion.replace("void ", "public void ")

        return suggestion

    def record_suggestion_improvement(
        self, original_suggestion: str, improved_suggestion: str, improvement_type: str, success: bool
    ):
        """Record a suggestion improvement."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            # Get current improvement data
            cursor.execute(
                """
            SELECT success_rate, usage_count
            FROM suggestion_improvements
            WHERE original_suggestion = ? AND improved_suggestion = ?
            """,
                (original_suggestion, improved_suggestion),
            )

            result = cursor.fetchone()

            if result:
                # Update existing record
                current_success_rate, usage_count = result
                usage_count += 1

                # Update success rate using exponential moving average
                alpha = 0.2
                new_success_rate = alpha * (1.0 if success else 0.0) + (1 - alpha) * current_success_rate

                cursor.execute(
                    """
                UPDATE suggestion_improvements
                SET success_rate = ?, usage_count = ?, last_updated = ?
                WHERE original_suggestion = ? AND improved_suggestion = ?
                """,
                    (
                        new_success_rate,
                        usage_count,
                        datetime.now().isoformat(),
                        original_suggestion,
                        improved_suggestion,
                    ),
                )
            else:
                # Create new record
                success_rate = 1.0 if success else 0.0

                cursor.execute(
                    """
                INSERT INTO suggestion_improvements
                (original_suggestion, improved_suggestion, improvement_type, success_rate, usage_count, last_updated)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                    (
                        original_suggestion,
                        improved_suggestion,
                        improvement_type,
                        success_rate,
                        1,
                        datetime.now().isoformat(),
                    ),
                )

            conn.commit()
            conn.close()

            logger.info(f"Recorded suggestion improvement: {improvement_type}")
            return True

        except Exception as e:
            logger.error(f"Error recording suggestion improvement: {e!s}")
            return False

    def get_learning_insights(self, _repo_owner: str | None = None, _repo_name: str | None = None) -> dict[str, Any]:
        """Get insights from the learning system."""
        try:
            conn = sqlite3.connect(self.learning_db_path)
            cursor = conn.cursor()

            insights = {}

            # Get top performing rules
            cursor.execute("""
            SELECT rule_id, category, effectiveness_score, total_occurrences
            FROM rule_effectiveness
            WHERE total_occurrences >= 5
            ORDER BY effectiveness_score DESC
            LIMIT 10
            """)

            top_rules = cursor.fetchall()
            insights["top_performing_rules"] = [
                {
                    "rule_id": rule_id,
                    "category": category,
                    "effectiveness_score": effectiveness_score,
                    "total_occurrences": total_occurrences,
                }
                for rule_id, category, effectiveness_score, total_occurrences in top_rules
            ]

            # Get most confident patterns
            cursor.execute("""
            SELECT pattern_type, pattern_value, file_extension, confidence_score, success_count, failure_count
            FROM pattern_learning
            WHERE (success_count + failure_count) >= 3
            ORDER BY confidence_score DESC
            LIMIT 10
            """)

            confident_patterns = cursor.fetchall()
            insights["most_confident_patterns"] = [
                {
                    "pattern_type": pattern_type,
                    "pattern_value": pattern_value,
                    "file_extension": file_extension,
                    "confidence_score": confidence_score,
                    "success_count": success_count,
                    "failure_count": failure_count,
                }
                for pattern_type, pattern_value, file_extension, confidence_score, success_count, failure_count in confident_patterns
            ]

            # Get suggestion improvements
            cursor.execute("""
            SELECT improvement_type, COUNT(*) as count, AVG(success_rate) as avg_success_rate
            FROM suggestion_improvements
            WHERE usage_count >= 2
            GROUP BY improvement_type
            ORDER BY avg_success_rate DESC
            """)

            improvements = cursor.fetchall()
            insights["suggestion_improvements"] = [
                {"improvement_type": improvement_type, "count": count, "avg_success_rate": avg_success_rate}
                for improvement_type, count, avg_success_rate in improvements
            ]

            conn.close()

            return insights

        except Exception as e:
            logger.error(f"Error getting learning insights: {e!s}")
            return {}


def update_rule_effectiveness(rule_id: str, category: str, feedback_type: str, suggestion_applied: bool = False):
    """Update the effectiveness of a rule based on feedback."""
    system = LearningSystem()
    return system.update_rule_effectiveness(rule_id, category, feedback_type, suggestion_applied)


def learn_pattern(pattern_type: str, pattern_value: str, file_extension: str, success: bool):
    """Learn from pattern usage."""
    system = LearningSystem()
    return system.learn_pattern(pattern_type, pattern_value, file_extension, success)


def update_developer_preferences(
    developer: str, repo_owner: str, repo_name: str, rule_id: str, preference_score: float
):
    """Update developer preferences for specific rules."""
    system = LearningSystem()
    return system.update_developer_preferences(developer, repo_owner, repo_name, rule_id, preference_score)


def get_rule_recommendations(repo_owner: str, repo_name: str, developer: str | None = None) -> list[dict[str, Any]]:
    """Get rule recommendations based on effectiveness and preferences."""
    system = LearningSystem()
    return system.get_rule_recommendations(repo_owner, repo_name, developer)


def improve_suggestion(original_suggestion: str, context: dict[str, Any]) -> str:
    """Improve a suggestion based on learned patterns."""
    system = LearningSystem()
    return system.improve_suggestion(original_suggestion, context)


def get_learning_insights(repo_owner: str | None = None, repo_name: str | None = None) -> dict[str, Any]:
    """Get insights from the learning system."""
    system = LearningSystem()
    return system.get_learning_insights(repo_owner, repo_name)


if __name__ == "__main__":
    # Example usage
    system = LearningSystem()

    # Update rule effectiveness
    system.update_rule_effectiveness("security-hardcoded-password", "security", "positive", True)

    # Learn a pattern
    system.learn_pattern("naming_convention", "camelCase", ".js", True)

    # Update developer preferences
    system.update_developer_preferences("john_doe", "example", "repo", "style-indentation", 0.8)

    # Get recommendations
    recommendations = system.get_rule_recommendations("example", "repo", "john_doe")
    for _rec in recommendations[:5]:
        pass

    # Improve a suggestion
    original = "var x = 5"
    improved = system.improve_suggestion(original, {"file_path": "test.js"})

    # Get insights
    insights = system.get_learning_insights()
