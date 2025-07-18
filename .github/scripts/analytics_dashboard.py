#!/usr/bin/env python3
"""
Analytics dashboard for Kiro GitHub integration.
This module provides a web-based dashboard for viewing analytics and insights.
"""

import logging
import os
from datetime import datetime, timedelta
from pathlib import Path

from analytics_collector import get_feedback_stats, get_metrics, get_review_stats, get_top_issues
from flask import Flask, jsonify, render_template, request
from learning_system import get_learning_insights, get_rule_recommendations

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(), logging.FileHandler("kiro_dashboard.log")],
)
logger = logging.getLogger("kiro_dashboard")

# Initialize Flask app
app = Flask(__name__)


@app.route("/")
def dashboard():
    """Main dashboard page."""
    return render_template("dashboard.html")


@app.route("/api/stats")
def get_stats():
    """Get review statistics."""
    repo_owner = request.args.get("repo_owner")
    repo_name = request.args.get("repo_name")
    days = int(request.args.get("days", 30))

    try:
        # Get review stats
        review_stats = get_review_stats(repo_owner, repo_name, days)

        # Get feedback stats
        feedback_stats = get_feedback_stats(repo_owner, repo_name, days)

        # Get top issues
        top_issues = get_top_issues(repo_owner, repo_name, days, 10)

        # Combine stats
        stats = {
            "review_stats": review_stats,
            "feedback_stats": feedback_stats,
            "top_issues": top_issues,
            "period": {
                "days": days,
                "start_date": (datetime.now() - timedelta(days=days)).isoformat(),
                "end_date": datetime.now().isoformat(),
            },
        }

        return jsonify(stats)

    except Exception as e:
        logger.error(f"Error getting stats: {e!s}")
        return jsonify({"error": str(e)}), 500


@app.route("/api/insights")
def get_insights():
    """Get learning insights."""
    repo_owner = request.args.get("repo_owner")
    repo_name = request.args.get("repo_name")

    try:
        insights = get_learning_insights(repo_owner, repo_name)
        return jsonify(insights)

    except Exception as e:
        logger.error(f"Error getting insights: {e!s}")
        return jsonify({"error": str(e)}), 500


@app.route("/api/recommendations")
def get_recommendations():
    """Get rule recommendations."""
    repo_owner = request.args.get("repo_owner")
    repo_name = request.args.get("repo_name")
    developer = request.args.get("developer")

    try:
        recommendations = get_rule_recommendations(repo_owner, repo_name, developer)
        return jsonify({"recommendations": recommendations})

    except Exception as e:
        logger.error(f"Error getting recommendations: {e!s}")
        return jsonify({"error": str(e)}), 500


@app.route("/api/metrics")
def get_metrics_api():
    """Get metrics data."""
    repo_owner = request.args.get("repo_owner")
    repo_name = request.args.get("repo_name")
    metric_name = request.args.get("metric_name")
    days = int(request.args.get("days", 30))

    try:
        metrics = get_metrics(repo_owner, repo_name, metric_name, days)
        return jsonify({"metrics": metrics})

    except Exception as e:
        logger.error(f"Error getting metrics: {e!s}")
        return jsonify({"error": str(e)}), 500


@app.route("/api/repositories")
def get_repositories():
    """Get list of repositories with analytics data."""
    try:
        from analytics_collector import AnalyticsCollector

        collector = AnalyticsCollector()
        conn = collector._get_connection()
        cursor = conn.cursor()

        cursor.execute("""
        SELECT DISTINCT repo_owner, repo_name, COUNT(*) as review_count
        FROM reviews
        GROUP BY repo_owner, repo_name
        ORDER BY review_count DESC
        """)

        results = cursor.fetchall()
        conn.close()

        repositories = [{"repo_owner": row[0], "repo_name": row[1], "review_count": row[2]} for row in results]

        return jsonify({"repositories": repositories})

    except Exception as e:
        logger.error(f"Error getting repositories: {e!s}")
        return jsonify({"error": str(e)}), 500


def create_dashboard_html():
    """Create the dashboard HTML template."""
    html_content = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kiro Analytics Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        .header {
            background: white;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .filters {
            display: flex;
            gap: 15px;
            margin-bottom: 20px;
        }
        .filter-group {
            display: flex;
            flex-direction: column;
        }
        .filter-group label {
            font-weight: 600;
            margin-bottom: 5px;
        }
        .filter-group select, .filter-group input {
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .stat-card h3 {
            margin: 0 0 15px 0;
            color: #333;
        }
        .stat-value {
            font-size: 2em;
            font-weight: bold;
            color: #007acc;
            margin-bottom: 5px;
        }
        .stat-label {
            color: #666;
            font-size: 0.9em;
        }
        .chart-container {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        .loading {
            text-align: center;
            padding: 40px;
            color: #666;
        }
        .error {
            background: #fee;
            color: #c33;
            padding: 15px;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        .recommendations {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .recommendation-item {
            padding: 10px;
            border-left: 4px solid #007acc;
            margin-bottom: 10px;
            background: #f8f9fa;
        }
        .recommendation-score {
            font-weight: bold;
            color: #007acc;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Kiro Analytics Dashboard</h1>
            <div class="filters">
                <div class="filter-group">
                    <label for="repo-select">Repository:</label>
                    <select id="repo-select">
                        <option value="">All Repositories</option>
                    </select>
                </div>
                <div class="filter-group">
                    <label for="days-select">Time Period:</label>
                    <select id="days-select">
                        <option value="7">Last 7 days</option>
                        <option value="30" selected>Last 30 days</option>
                        <option value="90">Last 90 days</option>
                    </select>
                </div>
                <div class="filter-group">
                    <label for="developer-input">Developer (optional):</label>
                    <input type="text" id="developer-input" placeholder="GitHub username">
                </div>
            </div>
        </div>

        <div id="error-container"></div>
        <div id="loading" class="loading">Loading analytics data...</div>

        <div id="stats-container" style="display: none;">
            <div class="stats-grid">
                <div class="stat-card">
                    <h3>Total Reviews</h3>
                    <div class="stat-value" id="total-reviews">-</div>
                    <div class="stat-label">Reviews completed</div>
                </div>
                <div class="stat-card">
                    <h3>Average Review Time</h3>
                    <div class="stat-value" id="avg-review-time">-</div>
                    <div class="stat-label">Minutes per review</div>
                </div>
                <div class="stat-card">
                    <h3>Issues Found</h3>
                    <div class="stat-value" id="total-issues">-</div>
                    <div class="stat-label">Total issues identified</div>
                </div>
                <div class="stat-card">
                    <h3>Suggestion Acceptance</h3>
                    <div class="stat-value" id="suggestion-acceptance">-</div>
                    <div class="stat-label">Percentage of suggestions applied</div>
                </div>
            </div>

            <div class="chart-container">
                <h3>Issues by Severity</h3>
                <canvas id="severity-chart" width="400" height="200"></canvas>
            </div>

            <div class="chart-container">
                <h3>Issues by Category</h3>
                <canvas id="category-chart" width="400" height="200"></canvas>
            </div>

            <div class="recommendations">
                <h3>Rule Recommendations</h3>
                <div id="recommendations-list">
                    <div class="loading">Loading recommendations...</div>
                </div>
            </div>
        </div>
    </div>

    <script>
        let currentRepo = { owner: null, name: null };
        let charts = {};

        // Load repositories on page load
        document.addEventListener('DOMContentLoaded', function() {
            loadRepositories();
            loadData();
        });

        // Add event listeners
        document.getElementById('repo-select').addEventListener('change', function() {
            const value = this.value;
            if (value) {
                const [owner, name] = value.split('/');
                currentRepo = { owner, name };
            } else {
                currentRepo = { owner: null, name: null };
            }
            loadData();
        });

        document.getElementById('days-select').addEventListener('change', loadData);
        document.getElementById('developer-input').addEventListener('input', debounce(loadRecommendations, 500));

        function loadRepositories() {
            fetch('/api/repositories')
                .then(response => response.json())
                .then(data => {
                    const select = document.getElementById('repo-select');
                    data.repositories.forEach(repo => {
                        const option = document.createElement('option');
                        option.value = `${repo.repo_owner}/${repo.repo_name}`;
                        option.textContent = `${repo.repo_owner}/${repo.repo_name} (${repo.review_count} reviews)`;
                        select.appendChild(option);
                    });
                })
                .catch(error => showError('Failed to load repositories: ' + error.message));
        }

        function loadData() {
            showLoading();

            const days = document.getElementById('days-select').value;
            const params = new URLSearchParams({ days });

            if (currentRepo.owner) {
                params.append('repo_owner', currentRepo.owner);
                params.append('repo_name', currentRepo.name);
            }

            Promise.all([
                fetch(`/api/stats?${params}`).then(r => r.json()),
                fetch(`/api/insights?${params}`).then(r => r.json())
            ])
            .then(([stats, insights]) => {
                hideLoading();
                updateStats(stats);
                updateCharts(stats);
                loadRecommendations();
            })
            .catch(error => {
                hideLoading();
                showError('Failed to load data: ' + error.message);
            });
        }

        function loadRecommendations() {
            const developer = document.getElementById('developer-input').value;
            const params = new URLSearchParams();

            if (currentRepo.owner) {
                params.append('repo_owner', currentRepo.owner);
                params.append('repo_name', currentRepo.name);
            }

            if (developer) {
                params.append('developer', developer);
            }

            fetch(`/api/recommendations?${params}`)
                .then(response => response.json())
                .then(data => updateRecommendations(data.recommendations))
                .catch(error => console.error('Failed to load recommendations:', error));
        }

        function updateStats(data) {
            const stats = data.review_stats;

            document.getElementById('total-reviews').textContent = stats.total_reviews || 0;

            const avgTime = stats.avg_review_time_seconds;
            document.getElementById('avg-review-time').textContent =
                avgTime ? Math.round(avgTime / 60) : 0;

            document.getElementById('total-issues').textContent = stats.total_issues || 0;

            const acceptance = stats.suggestion_acceptance_rate;
            document.getElementById('suggestion-acceptance').textContent =
                acceptance ? Math.round(acceptance * 100) + '%' : '0%';
        }

        function updateCharts(data) {
            const stats = data.review_stats;

            // Severity chart
            if (charts.severity) charts.severity.destroy();
            const severityCtx = document.getElementById('severity-chart').getContext('2d');
            charts.severity = new Chart(severityCtx, {
                type: 'doughnut',
                data: {
                    labels: Object.keys(stats.issues_by_severity || {}),
                    datasets: [{
                        data: Object.values(stats.issues_by_severity || {}),
                        backgroundColor: ['#dc3545', '#fd7e14', '#ffc107', '#17a2b8']
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false
                }
            });

            // Category chart
            if (charts.category) charts.category.destroy();
            const categoryCtx = document.getElementById('category-chart').getContext('2d');
            charts.category = new Chart(categoryCtx, {
                type: 'bar',
                data: {
                    labels: Object.keys(stats.issues_by_category || {}),
                    datasets: [{
                        label: 'Issues',
                        data: Object.values(stats.issues_by_category || {}),
                        backgroundColor: '#007acc'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
        }

        function updateRecommendations(recommendations) {
            const container = document.getElementById('recommendations-list');

            if (!recommendations || recommendations.length === 0) {
                container.innerHTML = '<div class="loading">No recommendations available</div>';
                return;
            }

            container.innerHTML = recommendations.slice(0, 5).map(rec => `
                <div class="recommendation-item">
                    <div><strong>${rec.rule_id}</strong> (${rec.category})</div>
                    <div class="recommendation-score">Score: ${(rec.combined_score * 100).toFixed(1)}%</div>
                    <div>Effectiveness: ${(rec.effectiveness_score * 100).toFixed(1)}% |
                         Occurrences: ${rec.total_occurrences}</div>
                </div>
            `).join('');
        }

        function showLoading() {
            document.getElementById('loading').style.display = 'block';
            document.getElementById('stats-container').style.display = 'none';
        }

        function hideLoading() {
            document.getElementById('loading').style.display = 'none';
            document.getElementById('stats-container').style.display = 'block';
        }

        function showError(message) {
            const container = document.getElementById('error-container');
            container.innerHTML = `<div class="error">${message}</div>`;
        }

        function debounce(func, wait) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        }
    </script>
</body>
</html>
    """

    # Create templates directory
    templates_dir = Path(__file__).parent / "templates"
    templates_dir.mkdir(exist_ok=True)

    # Write the HTML template
    dashboard_path = Path(templates_dir) / "dashboard.html"
    dashboard_path.write_text(html_content, encoding="utf-8")


def main():
    """Run the analytics dashboard."""
    # Create the HTML template
    create_dashboard_html()

    # Run the Flask app
    port = int(os.environ.get("DASHBOARD_PORT", 5001))
    app.run(host="0.0.0.0", port=port, debug=True)


if __name__ == "__main__":
    main()
