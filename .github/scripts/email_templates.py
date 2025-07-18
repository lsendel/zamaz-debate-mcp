#!/usr/bin/env python3
"""
Email templates for Kiro notifications.
Provides responsive HTML email templates for various notification types.
"""

from datetime import datetime
from typing import Any


class EmailTemplates:
    """Collection of email templates for Kiro notifications."""

    @staticmethod
    def base_template(content: str, footer_content: str = "") -> str:
        """Base HTML template wrapper."""
        return f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kiro Notification</title>
    <!--[if mso]>
    <noscript>
        <xml>
            <o:OfficeDocumentSettings>
                <o:PixelsPerInch>96</o:PixelsPerInch>
            </o:OfficeDocumentSettings>
        </xml>
    </noscript>
    <![endif]-->
    <style>
        @media only screen and (max-width: 600px) {{
            .container {{ width: 100% !important; }}
            .content {{ padding: 20px !important; }}
            .button {{ width: 100% !important; text-align: center !important; }}
            .stats-grid {{ grid-template-columns: 1fr !important; }}
        }}
    </style>
</head>
<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f6f8fa;">
    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="background-color: #f6f8fa;">
        <tr>
            <td align="center" style="padding: 40px 20px;">
                <table class="container" role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="background-color: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #0366d6, #0969da); padding: 30px; border-radius: 8px 8px 0 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%">
                                <tr>
                                    <td style="display: flex; align-items: center;">
                                        <div style="width: 50px; height: 50px; background: white; border-radius: 10px; display: inline-block; text-align: center; line-height: 50px; color: #0366d6; font-weight: bold; font-size: 24px; margin-right: 15px;">K</div>
                                        <h1 style="color: white; margin: 0; font-size: 28px; display: inline-block;">Kiro</h1>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <!-- Content -->
                    <tr>
                        <td class="content" style="padding: 40px;">
                            {content}
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f6f8fa; padding: 30px; border-radius: 0 0 8px 8px; border-top: 1px solid #e1e4e8;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%">
                                <tr>
                                    <td style="text-align: center; color: #656d76; font-size: 14px;">
                                        {footer_content}
                                        <p style="margin: 10px 0 0 0;">
                                            <a href="#" style="color: #0366d6; text-decoration: none;">Manage Preferences</a> |
                                            <a href="#" style="color: #0366d6; text-decoration: none;">Documentation</a> |
                                            <a href="#" style="color: #0366d6; text-decoration: none;">Support</a>
                                        </p>
                                        <p style="margin: 10px 0 0 0; font-size: 12px;">
                                            © {datetime.now().year} Kiro. AI-powered code reviews for better software.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
"""

    @staticmethod
    def review_completed(
        repository: str, pull_request: int, pr_title: str, author: str, summary: dict[str, Any], pr_url: str
    ) -> str:
        """Email template for review completed notification."""
        total_issues = summary.get("total_issues", 0)
        critical_issues = summary.get("critical_issues", 0)
        suggestions = summary.get("suggestions_made", 0)
        time_saved = summary.get("time_saved", "N/A")

        # Determine status
        if critical_issues > 0:
            status_color = "#dc3545"
            status_text = "Critical Issues Found"
            status_emoji = "🔴"
        elif total_issues > 5:
            status_color = "#fd7e14"
            status_text = "Multiple Issues Found"
            status_emoji = "🟡"
        elif total_issues > 0:
            status_color = "#28a745"
            status_text = "Minor Issues Found"
            status_emoji = "🟢"
        else:
            status_color = "#238636"
            status_text = "Looks Good!"
            status_emoji = "✅"

        content = f"""
        <h2 style="color: #1f2328; margin: 0 0 20px 0;">Code Review Complete {status_emoji}</h2>

        <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <p style="margin: 0; color: #24292e;">
                Kiro has completed reviewing pull request <strong>#{pull_request}</strong> in <strong>{repository}</strong>.
            </p>
        </div>

        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="margin-bottom: 30px;">
            <tr>
                <td width="50%" style="padding-right: 10px;">
                    <p style="margin: 0 0 5px 0; color: #656d76; font-size: 14px;">Repository</p>
                    <p style="margin: 0; font-weight: 600;">{repository}</p>
                </td>
                <td width="50%" style="padding-left: 10px;">
                    <p style="margin: 0 0 5px 0; color: #656d76; font-size: 14px;">Pull Request</p>
                    <p style="margin: 0; font-weight: 600;">#{pull_request}: {pr_title}</p>
                </td>
            </tr>
            <tr>
                <td colspan="2" style="padding-top: 15px;">
                    <p style="margin: 0 0 5px 0; color: #656d76; font-size: 14px;">Author</p>
                    <p style="margin: 0; font-weight: 600;">{author}</p>
                </td>
            </tr>
        </table>

        <div style="background-color: {status_color}15; border-left: 4px solid {status_color}; padding: 15px; margin-bottom: 30px;">
            <h3 style="margin: 0 0 10px 0; color: {status_color};">{status_text}</h3>
            <p style="margin: 0; color: #24292e;">Review completed with {total_issues} issue{"s" if total_issues != 1 else ""} found.</p>
        </div>

        <div class="stats-grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px;">
            <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; text-align: center;">
                <p style="margin: 0; color: #656d76; font-size: 14px;">Total Issues</p>
                <p style="margin: 5px 0 0 0; font-size: 32px; font-weight: bold; color: #0366d6;">{total_issues}</p>
            </div>
            <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; text-align: center;">
                <p style="margin: 0; color: #656d76; font-size: 14px;">Critical Issues</p>
                <p style="margin: 5px 0 0 0; font-size: 32px; font-weight: bold; color: #dc3545;">{critical_issues}</p>
            </div>
            <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; text-align: center;">
                <p style="margin: 0; color: #656d76; font-size: 14px;">Suggestions</p>
                <p style="margin: 5px 0 0 0; font-size: 32px; font-weight: bold; color: #0366d6;">{suggestions}</p>
            </div>
            <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; text-align: center;">
                <p style="margin: 0; color: #656d76; font-size: 14px;">Time Saved</p>
                <p style="margin: 5px 0 0 0; font-size: 32px; font-weight: bold; color: #238636;">{time_saved}</p>
            </div>
        </div>
        """

        # Add top issues if present
        if summary.get("top_issues"):
            content += """
        <h3 style="color: #1f2328; margin: 0 0 15px 0;">Top Issues Found</h3>
        <ul style="margin: 0 0 30px 0; padding-left: 20px;">
        """
            for issue in summary["top_issues"][:5]:
                severity_color = {"critical": "#dc3545", "high": "#fd7e14", "medium": "#ffc107", "low": "#28a745"}.get(
                    issue.get("severity", "medium"), "#ffc107"
                )

                content += f"""
            <li style="margin-bottom: 10px;">
                <span style="color: {severity_color}; font-weight: bold;">[{issue.get("severity", "medium").upper()}]</span>
                {issue["message"]}
            </li>
                """

            content += "</ul>"

        content += f"""
        <div style="text-align: center; margin-top: 40px;">
            <a href="{pr_url}" class="button" style="display: inline-block; background-color: #238636; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600;">View on GitHub</a>
        </div>
        """

        footer = f"This review was completed in {summary.get('review_time', 'N/A')}."

        return EmailTemplates.base_template(content, footer)

    @staticmethod
    def weekly_summary(
        recipient_name: str, week_start: str, week_end: str, stats: dict[str, Any], highlights: list[dict[str, Any]]
    ) -> str:
        """Email template for weekly summary."""
        content = f"""
        <h2 style="color: #1f2328; margin: 0 0 20px 0;">Your Weekly Kiro Summary</h2>

        <p style="color: #24292e; margin: 0 0 30px 0;">
            Hi {recipient_name}, here's your code review summary for {week_start} - {week_end}.
        </p>

        <div style="background: linear-gradient(135deg, #f6f8fa, #e1e4e8); padding: 30px; border-radius: 8px; margin-bottom: 30px;">
            <h3 style="color: #1f2328; margin: 0 0 20px 0; text-align: center;">📊 Week at a Glance</h3>

            <div class="stats-grid" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px;">
                <div style="background: white; padding: 20px; border-radius: 6px; text-align: center;">
                    <p style="margin: 0; font-size: 36px; font-weight: bold; color: #0366d6;">{stats["prs_reviewed"]}</p>
                    <p style="margin: 5px 0 0 0; color: #656d76; font-size: 14px;">PRs Reviewed</p>
                </div>
                <div style="background: white; padding: 20px; border-radius: 6px; text-align: center;">
                    <p style="margin: 0; font-size: 36px; font-weight: bold; color: #dc3545;">{stats["issues_found"]}</p>
                    <p style="margin: 5px 0 0 0; color: #656d76; font-size: 14px;">Issues Found</p>
                </div>
                <div style="background: white; padding: 20px; border-radius: 6px; text-align: center;">
                    <p style="margin: 0; font-size: 36px; font-weight: bold; color: #238636;">{stats["time_saved"]}h</p>
                    <p style="margin: 5px 0 0 0; color: #656d76; font-size: 14px;">Time Saved</p>
                </div>
            </div>
        </div>

        <h3 style="color: #1f2328; margin: 0 0 20px 0;">🏆 Top Achievements</h3>

        <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <ul style="margin: 0; padding-left: 20px;">
                <li style="margin-bottom: 10px;">
                    <strong>Fix Rate:</strong> {stats["fix_rate"]}% of identified issues were resolved
                </li>
                <li style="margin-bottom: 10px;">
                    <strong>Security:</strong> {stats["security_issues"]} potential vulnerabilities prevented
                </li>
                <li style="margin-bottom: 10px;">
                    <strong>Code Quality:</strong> Overall score improved by {stats["quality_improvement"]}%
                </li>
            </ul>
        </div>
        """

        if highlights:
            content += """
        <h3 style="color: #1f2328; margin: 0 0 20px 0;">🌟 Weekly Highlights</h3>
        <div style="margin-bottom: 30px;">
        """
            for highlight in highlights[:3]:
                content += f"""
            <div style="border-left: 3px solid #0366d6; padding-left: 15px; margin-bottom: 15px;">
                <p style="margin: 0 0 5px 0; font-weight: 600;">{highlight["title"]}</p>
                <p style="margin: 0; color: #656d76; font-size: 14px;">{highlight["description"]}</p>
            </div>
                """
            content += "</div>"

        content += """
        <h3 style="color: #1f2328; margin: 0 0 20px 0;">💡 Improvement Tips</h3>

        <div style="background-color: #fff8dc; border: 1px solid #ffd700; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <p style="margin: 0 0 10px 0;"><strong>Most Common Issue This Week:</strong></p>
            <p style="margin: 0;">Based on your team's patterns, focus on improving error handling in async functions. This accounted for 23% of issues found.</p>
        </div>

        <div style="text-align: center; margin-top: 40px;">
            <a href="#" class="button" style="display: inline-block; background-color: #0366d6; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600; margin-right: 10px;">View Full Report</a>
            <a href="#" style="display: inline-block; background-color: #f6f8fa; color: #0366d6; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600; border: 1px solid #d1d9e0;">Configure Settings</a>
        </div>
        """

        footer = "You're receiving this because you're subscribed to weekly summaries."

        return EmailTemplates.base_template(content, footer)

    @staticmethod
    def critical_issue_alert(repository: str, pull_request: int, issue: dict[str, Any], pr_url: str) -> str:
        """Email template for critical issue alerts."""
        content = f"""
        <h2 style="color: #dc3545; margin: 0 0 20px 0;">⚠️ Critical Issue Detected</h2>

        <div style="background-color: #fef2f2; border: 1px solid #fecaca; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <p style="margin: 0; color: #dc2626;">
                Kiro has identified a critical issue that requires immediate attention in <strong>{repository}</strong> PR #{pull_request}.
            </p>
        </div>

        <h3 style="color: #1f2328; margin: 0 0 15px 0;">Issue Details</h3>

        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="margin-bottom: 30px;">
            <tr>
                <td style="padding: 10px; background-color: #f6f8fa; border-bottom: 1px solid #e1e4e8;">
                    <strong>Type:</strong>
                </td>
                <td style="padding: 10px; background-color: #f6f8fa; border-bottom: 1px solid #e1e4e8;">
                    {issue.get("type", "Security Vulnerability")}
                </td>
            </tr>
            <tr>
                <td style="padding: 10px; background-color: white; border-bottom: 1px solid #e1e4e8;">
                    <strong>File:</strong>
                </td>
                <td style="padding: 10px; background-color: white; border-bottom: 1px solid #e1e4e8;">
                    <code>{issue.get("file", "Unknown")}</code>
                </td>
            </tr>
            <tr>
                <td style="padding: 10px; background-color: #f6f8fa; border-bottom: 1px solid #e1e4e8;">
                    <strong>Line:</strong>
                </td>
                <td style="padding: 10px; background-color: #f6f8fa; border-bottom: 1px solid #e1e4e8;">
                    {issue.get("line", "N/A")}
                </td>
            </tr>
        </table>

        <h3 style="color: #1f2328; margin: 0 0 15px 0;">Description</h3>
        <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <p style="margin: 0;">{issue.get("description", "No description available")}</p>
        </div>

        <h3 style="color: #1f2328; margin: 0 0 15px 0;">Recommended Action</h3>
        <div style="background-color: #dcfce7; border: 1px solid #bbf7d0; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <p style="margin: 0;">{issue.get("recommendation", "Please review and address this issue immediately.")}</p>
        </div>

        <div style="text-align: center; margin-top: 40px;">
            <a href="{pr_url}" class="button" style="display: inline-block; background-color: #dc3545; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600;">View Issue on GitHub</a>
        </div>
        """

        footer = "This is a critical issue alert. Please address it as soon as possible."

        return EmailTemplates.base_template(content, footer)

    @staticmethod
    def suggestion_accepted(repository: str, pull_request: int, suggestion: dict[str, Any], accepted_by: str) -> str:
        """Email template for accepted suggestion notification."""
        content = f"""
        <h2 style="color: #238636; margin: 0 0 20px 0;">✅ Suggestion Accepted</h2>

        <div style="background-color: #dcfce7; border: 1px solid #bbf7d0; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <p style="margin: 0; color: #166534;">
                Your code suggestion for <strong>{repository}</strong> PR #{pull_request} has been accepted and applied by <strong>{accepted_by}</strong>.
            </p>
        </div>

        <h3 style="color: #1f2328; margin: 0 0 15px 0;">Suggestion Details</h3>

        <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; margin-bottom: 20px;">
            <p style="margin: 0 0 10px 0;"><strong>Original Issue:</strong></p>
            <p style="margin: 0;">{suggestion.get("issue", "Issue description not available")}</p>
        </div>

        <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <p style="margin: 0 0 10px 0;"><strong>Applied Fix:</strong></p>
            <pre style="margin: 0; white-space: pre-wrap; font-family: monospace; font-size: 14px;">{suggestion.get("fix", "Fix details not available")}</pre>
        </div>

        <p style="color: #24292e; margin: 0 0 30px 0;">
            This improvement helps maintain code quality and consistency across your project. Thank you for using Kiro's automated suggestions!
        </p>

        <div style="text-align: center;">
            <a href="#" style="color: #0366d6; text-decoration: none;">View more suggestions</a>
        </div>
        """

        footer = "Your acceptance helps Kiro learn and provide better suggestions."

        return EmailTemplates.base_template(content, footer)

    @staticmethod
    def daily_digest(
        recipient_name: str, date: str, summary: dict[str, Any], pending_reviews: list[dict[str, Any]]
    ) -> str:
        """Email template for daily digest."""
        content = f"""
        <h2 style="color: #1f2328; margin: 0 0 20px 0;">Daily Review Digest</h2>

        <p style="color: #24292e; margin: 0 0 30px 0;">
            Good morning {recipient_name}! Here's your code review summary for {date}.
        </p>

        <div style="background-color: #f6f8fa; padding: 20px; border-radius: 6px; margin-bottom: 30px;">
            <h3 style="color: #1f2328; margin: 0 0 15px 0;">📈 Yesterday's Activity</h3>
            <ul style="margin: 0; padding-left: 20px;">
                <li style="margin-bottom: 8px;">{summary["reviews_completed"]} reviews completed</li>
                <li style="margin-bottom: 8px;">{summary["issues_found"]} issues identified</li>
                <li style="margin-bottom: 8px;">{summary["suggestions_accepted"]} suggestions accepted</li>
                <li style="margin-bottom: 8px;">{summary["time_saved"]} minutes saved</li>
            </ul>
        </div>
        """

        if pending_reviews:
            content += f"""
        <h3 style="color: #1f2328; margin: 0 0 15px 0;">🔍 Pending Reviews ({len(pending_reviews)})</h3>
        <div style="margin-bottom: 30px;">
        """
            for pr in pending_reviews[:5]:
                content += f"""
            <div style="border: 1px solid #e1e4e8; padding: 15px; border-radius: 6px; margin-bottom: 10px;">
                <p style="margin: 0 0 5px 0;">
                    <strong>{pr["repository"]}</strong> - PR #{pr["number"]}
                </p>
                <p style="margin: 0; color: #656d76; font-size: 14px;">
                    {pr["title"]} by {pr["author"]}
                </p>
            </div>
                """

            if len(pending_reviews) > 5:
                content += f"""
            <p style="text-align: center; margin: 15px 0 0 0;">
                <a href="#" style="color: #0366d6; text-decoration: none;">View all {len(pending_reviews)} pending reviews →</a>
            </p>
                """

            content += "</div>"

        content += """
        <div style="text-align: center; margin-top: 40px;">
            <a href="#" class="button" style="display: inline-block; background-color: #0366d6; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600;">Go to Dashboard</a>
        </div>
        """

        footer = "You're receiving this daily digest based on your notification preferences."

        return EmailTemplates.base_template(content, footer)


# Export templates
email_templates = EmailTemplates()
