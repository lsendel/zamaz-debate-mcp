#!/usr/bin/env python3
"""
Enhanced Main Script with Documentation Analysis Integration

This script integrates the comprehensive documentation analysis system with the existing
repository analysis infrastructure, providing a unified analysis platform.
"""

import os
import sys
import json
import logging
import argparse
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, List, Optional

# Add the scripts directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

# Import existing analysis modules
try:
    from analyzers.documentation_analyzer import DocumentationAnalysisSystem
    from core.repository_analyzer import RepositoryAnalyzer
    from api.github_api_client import GitHubAPIClient
    from services.issue_tracker import IssueTracker
except ImportError as e:
    print(f"Warning: Could not import some modules: {e}")
    print("Some features may not be available.")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ComprehensiveAnalysisSystem:
    """
    Comprehensive analysis system that combines repository analysis, 
    documentation analysis, and GitHub integration.
    """
    
    def __init__(self, project_path: str, github_token: Optional[str] = None):
        self.project_path = Path(project_path)
        self.github_token = github_token
        
        # Initialize components
        self.doc_analyzer = DocumentationAnalysisSystem()
        self.repo_analyzer = None
        self.github_client = None
        self.issue_tracker = None
        
        # Initialize optional components
        try:
            self.repo_analyzer = RepositoryAnalyzer(str(self.project_path))
        except:
            logger.warning("Repository analyzer not available")
        
        if github_token:
            try:
                self.github_client = GitHubAPIClient(github_token)
                self.issue_tracker = IssueTracker(github_token)
            except:
                logger.warning("GitHub integration not available")
    
    def run_comprehensive_analysis(self) -> Dict[str, Any]:
        """Run comprehensive analysis including documentation, code, and GitHub data."""
        logger.info("Starting comprehensive analysis...")
        
        analysis_results = {
            'timestamp': datetime.now().isoformat(),
            'project_path': str(self.project_path),
            'documentation_analysis': {},
            'repository_analysis': {},
            'github_analysis': {},
            'integration_insights': {},
            'combined_metrics': {},
            'recommendations': {},
            'issues': []
        }
        
        # Documentation Analysis
        logger.info("Running documentation analysis...")
        try:
            doc_results = self.doc_analyzer.analyze_project(str(self.project_path))
            analysis_results['documentation_analysis'] = doc_results
            logger.info("Documentation analysis completed")
        except Exception as e:
            logger.error(f"Documentation analysis failed: {e}")
            analysis_results['issues'].append({
                'type': 'analysis_error',
                'component': 'documentation',
                'message': str(e)
            })
        
        # Repository Analysis
        if self.repo_analyzer:
            logger.info("Running repository analysis...")
            try:
                repo_results = self.repo_analyzer.analyze()
                analysis_results['repository_analysis'] = repo_results
                logger.info("Repository analysis completed")
            except Exception as e:
                logger.error(f"Repository analysis failed: {e}")
                analysis_results['issues'].append({
                    'type': 'analysis_error',
                    'component': 'repository',
                    'message': str(e)
                })
        
        # GitHub Analysis
        if self.github_client:
            logger.info("Running GitHub analysis...")
            try:
                github_results = self._analyze_github_data()
                analysis_results['github_analysis'] = github_results
                logger.info("GitHub analysis completed")
            except Exception as e:
                logger.error(f"GitHub analysis failed: {e}")
                analysis_results['issues'].append({
                    'type': 'analysis_error',
                    'component': 'github',
                    'message': str(e)
                })
        
        # Integration Analysis
        logger.info("Running integration analysis...")
        try:
            integration_results = self._analyze_integration(analysis_results)
            analysis_results['integration_insights'] = integration_results
            logger.info("Integration analysis completed")
        except Exception as e:
            logger.error(f"Integration analysis failed: {e}")
            analysis_results['issues'].append({
                'type': 'analysis_error',
                'component': 'integration',
                'message': str(e)
            })
        
        # Combined Metrics
        analysis_results['combined_metrics'] = self._calculate_combined_metrics(analysis_results)
        
        # Combined Recommendations
        analysis_results['recommendations'] = self._generate_combined_recommendations(analysis_results)
        
        logger.info("Comprehensive analysis completed")
        return analysis_results
    
    def _analyze_github_data(self) -> Dict[str, Any]:
        """Analyze GitHub-specific data."""
        github_data = {
            'repository_info': {},
            'issues': [],
            'pull_requests': [],
            'contributors': [],
            'activity_metrics': {},
            'documentation_issues': []
        }
        
        try:
            # Get repository information
            repo_info = self.github_client.get_repository_info()
            github_data['repository_info'] = repo_info
            
            # Get issues
            issues = self.github_client.get_issues()
            github_data['issues'] = issues
            
            # Get pull requests
            prs = self.github_client.get_pull_requests()
            github_data['pull_requests'] = prs
            
            # Get contributors
            contributors = self.github_client.get_contributors()
            github_data['contributors'] = contributors
            
            # Calculate activity metrics
            github_data['activity_metrics'] = self._calculate_github_activity(issues, prs)
            
            # Find documentation-related issues
            github_data['documentation_issues'] = self._find_documentation_issues(issues)
            
        except Exception as e:
            logger.error(f"Error analyzing GitHub data: {e}")
        
        return github_data
    
    def _calculate_github_activity(self, issues: List[Dict], prs: List[Dict]) -> Dict[str, Any]:
        """Calculate GitHub activity metrics."""
        metrics = {
            'total_issues': len(issues),
            'open_issues': len([i for i in issues if i['state'] == 'open']),
            'closed_issues': len([i for i in issues if i['state'] == 'closed']),
            'total_prs': len(prs),
            'open_prs': len([pr for pr in prs if pr['state'] == 'open']),
            'closed_prs': len([pr for pr in prs if pr['state'] == 'closed']),
            'merged_prs': len([pr for pr in prs if pr.get('merged_at')]),
            'issue_close_rate': 0.0,
            'pr_merge_rate': 0.0
        }
        
        # Calculate rates
        if metrics['total_issues'] > 0:
            metrics['issue_close_rate'] = metrics['closed_issues'] / metrics['total_issues']
        
        if metrics['total_prs'] > 0:
            metrics['pr_merge_rate'] = metrics['merged_prs'] / metrics['total_prs']
        
        return metrics
    
    def _find_documentation_issues(self, issues: List[Dict]) -> List[Dict]:
        """Find issues related to documentation."""
        doc_keywords = ['documentation', 'docs', 'readme', 'api doc', 'javadoc', 'comment', 'guide']
        doc_issues = []
        
        for issue in issues:
            title = issue.get('title', '').lower()
            body = issue.get('body', '').lower()
            
            for keyword in doc_keywords:
                if keyword in title or keyword in body:
                    doc_issues.append({
                        'number': issue['number'],
                        'title': issue['title'],
                        'state': issue['state'],
                        'labels': [label['name'] for label in issue.get('labels', [])],
                        'created_at': issue['created_at'],
                        'updated_at': issue['updated_at']
                    })
                    break
        
        return doc_issues
    
    def _analyze_integration(self, analysis_results: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze integration between different analysis components."""
        integration_data = {
            'documentation_code_alignment': {},
            'github_documentation_correlation': {},
            'quality_indicators': {},
            'gaps_and_opportunities': [],
            'cross_component_insights': []
        }
        
        # Documentation-Code Alignment
        if analysis_results.get('documentation_analysis') and analysis_results.get('repository_analysis'):
            integration_data['documentation_code_alignment'] = self._analyze_doc_code_alignment(
                analysis_results['documentation_analysis'],
                analysis_results['repository_analysis']
            )
        
        # GitHub-Documentation Correlation
        if analysis_results.get('github_analysis') and analysis_results.get('documentation_analysis'):
            integration_data['github_documentation_correlation'] = self._analyze_github_doc_correlation(
                analysis_results['github_analysis'],
                analysis_results['documentation_analysis']
            )
        
        # Quality Indicators
        integration_data['quality_indicators'] = self._calculate_quality_indicators(analysis_results)
        
        # Gaps and Opportunities
        integration_data['gaps_and_opportunities'] = self._identify_gaps_and_opportunities(analysis_results)
        
        # Cross-Component Insights
        integration_data['cross_component_insights'] = self._generate_cross_component_insights(analysis_results)
        
        return integration_data
    
    def _analyze_doc_code_alignment(self, doc_results: Dict, repo_results: Dict) -> Dict[str, Any]:
        """Analyze alignment between documentation and code."""
        alignment_data = {
            'api_documentation_coverage': 0.0,
            'code_comment_density': 0.0,
            'outdated_documentation_risk': 0.0,
            'documentation_debt': 0.0,
            'alignment_score': 0.0
        }
        
        try:
            # API Documentation Coverage
            doc_metrics = doc_results.get('metrics', {})
            if 'coverage_percentage' in doc_metrics:
                alignment_data['api_documentation_coverage'] = doc_metrics['coverage_percentage']
            
            # Code Comment Density
            code_files = doc_results.get('code_files', [])
            if code_files:
                total_coverage = sum(cf.get('documentation_coverage', 0) for cf in code_files)
                alignment_data['code_comment_density'] = total_coverage / len(code_files)
            
            # Outdated Documentation Risk
            issues = doc_results.get('issues', [])
            outdated_issues = [i for i in issues if i.get('type') == 'outdated']
            if issues:
                alignment_data['outdated_documentation_risk'] = len(outdated_issues) / len(issues) * 100
            
            # Documentation Debt (inverse of quality)
            if 'quality_score' in doc_metrics:
                alignment_data['documentation_debt'] = 100 - doc_metrics['quality_score']
            
            # Overall Alignment Score
            alignment_data['alignment_score'] = (
                alignment_data['api_documentation_coverage'] * 0.4 +
                alignment_data['code_comment_density'] * 0.3 +
                (100 - alignment_data['outdated_documentation_risk']) * 0.2 +
                (100 - alignment_data['documentation_debt']) * 0.1
            )
            
        except Exception as e:
            logger.error(f"Error calculating doc-code alignment: {e}")
        
        return alignment_data
    
    def _analyze_github_doc_correlation(self, github_results: Dict, doc_results: Dict) -> Dict[str, Any]:
        """Analyze correlation between GitHub activity and documentation."""
        correlation_data = {
            'documentation_issues_ratio': 0.0,
            'documentation_pr_ratio': 0.0,
            'community_engagement': 0.0,
            'maintenance_activity': 0.0
        }
        
        try:
            activity_metrics = github_results.get('activity_metrics', {})
            doc_issues = github_results.get('documentation_issues', [])
            
            # Documentation Issues Ratio
            total_issues = activity_metrics.get('total_issues', 0)
            if total_issues > 0:
                correlation_data['documentation_issues_ratio'] = len(doc_issues) / total_issues * 100
            
            # Documentation PR Ratio (estimate based on titles)
            prs = github_results.get('pull_requests', [])
            doc_prs = [pr for pr in prs if any(keyword in pr.get('title', '').lower() 
                                              for keyword in ['doc', 'readme', 'guide'])]
            if prs:
                correlation_data['documentation_pr_ratio'] = len(doc_prs) / len(prs) * 100
            
            # Community Engagement (contributors vs documentation quality)
            contributors = github_results.get('contributors', [])
            doc_quality = doc_results.get('metrics', {}).get('quality_score', 0)
            if contributors:
                correlation_data['community_engagement'] = len(contributors) * (doc_quality / 100)
            
            # Maintenance Activity
            correlation_data['maintenance_activity'] = (
                activity_metrics.get('issue_close_rate', 0) * 50 +
                activity_metrics.get('pr_merge_rate', 0) * 50
            )
            
        except Exception as e:
            logger.error(f"Error calculating GitHub-doc correlation: {e}")
        
        return correlation_data
    
    def _calculate_quality_indicators(self, analysis_results: Dict[str, Any]) -> Dict[str, Any]:
        """Calculate cross-component quality indicators."""
        indicators = {
            'overall_project_health': 0.0,
            'documentation_maturity': 0.0,
            'code_quality_alignment': 0.0,
            'community_health': 0.0,
            'maintenance_score': 0.0
        }
        
        try:
            doc_metrics = analysis_results.get('documentation_analysis', {}).get('metrics', {})
            github_metrics = analysis_results.get('github_analysis', {}).get('activity_metrics', {})
            
            # Documentation Maturity
            if doc_metrics:
                indicators['documentation_maturity'] = (
                    doc_metrics.get('quality_score', 0) * 0.4 +
                    doc_metrics.get('coverage_percentage', 0) * 0.3 +
                    doc_metrics.get('freshness_score', 0) * 0.3
                )
            
            # Community Health
            if github_metrics:
                indicators['community_health'] = (
                    github_metrics.get('issue_close_rate', 0) * 100 * 0.4 +
                    github_metrics.get('pr_merge_rate', 0) * 100 * 0.4 +
                    min(100, len(analysis_results.get('github_analysis', {}).get('contributors', [])) * 10) * 0.2
                )
            
            # Code Quality Alignment
            code_files = analysis_results.get('documentation_analysis', {}).get('code_files', [])
            if code_files:
                avg_coverage = sum(cf.get('documentation_coverage', 0) for cf in code_files) / len(code_files)
                indicators['code_quality_alignment'] = avg_coverage
            
            # Maintenance Score
            integration_data = analysis_results.get('integration_insights', {})
            if integration_data:
                alignment_data = integration_data.get('documentation_code_alignment', {})
                indicators['maintenance_score'] = 100 - alignment_data.get('outdated_documentation_risk', 0)
            
            # Overall Project Health
            indicators['overall_project_health'] = (
                indicators['documentation_maturity'] * 0.3 +
                indicators['community_health'] * 0.3 +
                indicators['code_quality_alignment'] * 0.2 +
                indicators['maintenance_score'] * 0.2
            )
            
        except Exception as e:
            logger.error(f"Error calculating quality indicators: {e}")
        
        return indicators
    
    def _identify_gaps_and_opportunities(self, analysis_results: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Identify gaps and opportunities across all analysis components."""
        gaps = []
        
        try:
            doc_results = analysis_results.get('documentation_analysis', {})
            github_results = analysis_results.get('github_analysis', {})
            
            # Documentation Gaps
            doc_metrics = doc_results.get('metrics', {})
            if doc_metrics.get('coverage_percentage', 0) < 50:
                gaps.append({
                    'type': 'documentation_gap',
                    'severity': 'high',
                    'title': 'Low Documentation Coverage',
                    'description': f'Only {doc_metrics.get("coverage_percentage", 0):.1f}% coverage',
                    'recommendation': 'Prioritize API documentation and code comments'
                })
            
            # GitHub Activity Gaps
            activity_metrics = github_results.get('activity_metrics', {})
            if activity_metrics.get('issue_close_rate', 0) < 0.5:
                gaps.append({
                    'type': 'maintenance_gap',
                    'severity': 'medium',
                    'title': 'Low Issue Resolution Rate',
                    'description': f'Only {activity_metrics.get("issue_close_rate", 0)*100:.1f}% of issues are closed',
                    'recommendation': 'Improve issue triage and resolution processes'
                })
            
            # Community Engagement Gaps
            contributors = github_results.get('contributors', [])
            if len(contributors) < 3:
                gaps.append({
                    'type': 'community_gap',
                    'severity': 'medium',
                    'title': 'Limited Community Engagement',
                    'description': f'Only {len(contributors)} contributors',
                    'recommendation': 'Improve onboarding documentation and contribution guidelines'
                })
            
            # API Documentation Gaps
            doc_issues = github_results.get('documentation_issues', [])
            if len(doc_issues) > 3:
                gaps.append({
                    'type': 'documentation_gap',
                    'severity': 'medium',
                    'title': 'Multiple Documentation Issues',
                    'description': f'{len(doc_issues)} open documentation issues',
                    'recommendation': 'Address documentation issues systematically'
                })
            
        except Exception as e:
            logger.error(f"Error identifying gaps: {e}")
        
        return gaps
    
    def _generate_cross_component_insights(self, analysis_results: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Generate insights that span multiple analysis components."""
        insights = []
        
        try:
            doc_results = analysis_results.get('documentation_analysis', {})
            github_results = analysis_results.get('github_analysis', {})
            
            # Documentation-Community Correlation
            doc_quality = doc_results.get('metrics', {}).get('quality_score', 0)
            contributors = github_results.get('contributors', [])
            
            if doc_quality > 70 and len(contributors) > 5:
                insights.append({
                    'type': 'positive_correlation',
                    'title': 'High Documentation Quality Correlates with Active Community',
                    'description': f'Quality score {doc_quality:.1f} with {len(contributors)} contributors',
                    'implication': 'Good documentation attracts and retains contributors'
                })
            elif doc_quality < 50 and len(contributors) < 3:
                insights.append({
                    'type': 'negative_correlation',
                    'title': 'Low Documentation Quality May Limit Community Growth',
                    'description': f'Quality score {doc_quality:.1f} with {len(contributors)} contributors',
                    'implication': 'Improving documentation could help attract more contributors'
                })
            
            # Issue Resolution vs Documentation
            activity_metrics = github_results.get('activity_metrics', {})
            doc_issues = github_results.get('documentation_issues', [])
            
            if activity_metrics.get('issue_close_rate', 0) > 0.8 and len(doc_issues) > 0:
                insights.append({
                    'type': 'maintenance_insight',
                    'title': 'Good Issue Resolution but Documentation Needs Attention',
                    'description': f'High issue close rate but {len(doc_issues)} documentation issues',
                    'implication': 'Documentation issues may need special attention'
                })
            
            # API Coverage vs Pull Request Activity
            doc_coverage = doc_results.get('metrics', {}).get('coverage_percentage', 0)
            pr_merge_rate = activity_metrics.get('pr_merge_rate', 0)
            
            if doc_coverage > 70 and pr_merge_rate > 0.7:
                insights.append({
                    'type': 'best_practice',
                    'title': 'Well-Documented APIs Support Active Development',
                    'description': f'{doc_coverage:.1f}% API coverage with {pr_merge_rate*100:.1f}% PR merge rate',
                    'implication': 'Good API documentation facilitates smooth development workflow'
                })
            
        except Exception as e:
            logger.error(f"Error generating insights: {e}")
        
        return insights
    
    def _calculate_combined_metrics(self, analysis_results: Dict[str, Any]) -> Dict[str, Any]:
        """Calculate combined metrics from all analysis components."""
        combined_metrics = {
            'overall_health_score': 0.0,
            'documentation_health': 0.0,
            'development_velocity': 0.0,
            'community_engagement': 0.0,
            'maintenance_quality': 0.0,
            'project_maturity': 0.0
        }
        
        try:
            # Get individual metrics
            doc_metrics = analysis_results.get('documentation_analysis', {}).get('metrics', {})
            github_metrics = analysis_results.get('github_analysis', {}).get('activity_metrics', {})
            integration_metrics = analysis_results.get('integration_insights', {}).get('quality_indicators', {})
            
            # Documentation Health
            combined_metrics['documentation_health'] = (
                doc_metrics.get('quality_score', 0) * 0.4 +
                doc_metrics.get('coverage_percentage', 0) * 0.4 +
                doc_metrics.get('freshness_score', 0) * 0.2
            )
            
            # Development Velocity
            combined_metrics['development_velocity'] = (
                github_metrics.get('pr_merge_rate', 0) * 100 * 0.6 +
                github_metrics.get('issue_close_rate', 0) * 100 * 0.4
            )
            
            # Community Engagement
            contributors_count = len(analysis_results.get('github_analysis', {}).get('contributors', []))
            combined_metrics['community_engagement'] = min(100, contributors_count * 15)
            
            # Maintenance Quality
            combined_metrics['maintenance_quality'] = integration_metrics.get('maintenance_score', 0)
            
            # Project Maturity
            combined_metrics['project_maturity'] = (
                combined_metrics['documentation_health'] * 0.3 +
                combined_metrics['development_velocity'] * 0.3 +
                combined_metrics['community_engagement'] * 0.2 +
                combined_metrics['maintenance_quality'] * 0.2
            )
            
            # Overall Health Score
            combined_metrics['overall_health_score'] = (
                combined_metrics['documentation_health'] * 0.25 +
                combined_metrics['development_velocity'] * 0.25 +
                combined_metrics['community_engagement'] * 0.25 +
                combined_metrics['project_maturity'] * 0.25
            )
            
        except Exception as e:
            logger.error(f"Error calculating combined metrics: {e}")
        
        return combined_metrics
    
    def _generate_combined_recommendations(self, analysis_results: Dict[str, Any]) -> Dict[str, Any]:
        """Generate comprehensive recommendations combining all analysis results."""
        recommendations = {
            'critical': [],
            'high_priority': [],
            'medium_priority': [],
            'low_priority': [],
            'strategic': [],
            'tactical': []
        }
        
        try:
            # Get individual recommendations
            doc_recs = analysis_results.get('documentation_analysis', {}).get('recommendations', {})
            gaps = analysis_results.get('integration_insights', {}).get('gaps_and_opportunities', [])
            combined_metrics = analysis_results.get('combined_metrics', {})
            
            # Critical recommendations based on combined metrics
            if combined_metrics.get('overall_health_score', 0) < 40:
                recommendations['critical'].append({
                    'title': 'Critical Project Health Issues',
                    'description': f'Overall health score is {combined_metrics.get("overall_health_score", 0):.1f}',
                    'actions': [
                        'Immediate documentation improvement',
                        'Accelerate issue resolution',
                        'Engage community for contributions'
                    ]
                })
            
            # High priority from gaps
            for gap in gaps:
                if gap.get('severity') == 'high':
                    recommendations['high_priority'].append({
                        'title': gap['title'],
                        'description': gap['description'],
                        'actions': [gap['recommendation']]
                    })
            
            # Strategic recommendations
            if combined_metrics.get('documentation_health', 0) < 60:
                recommendations['strategic'].append({
                    'title': 'Documentation Strategy Overhaul',
                    'description': 'Implement comprehensive documentation strategy',
                    'actions': [
                        'Establish documentation standards',
                        'Create documentation templates',
                        'Implement automated documentation generation',
                        'Set up documentation review process'
                    ]
                })
            
            if combined_metrics.get('community_engagement', 0) < 40:
                recommendations['strategic'].append({
                    'title': 'Community Building Initiative',
                    'description': 'Improve community engagement and contribution',
                    'actions': [
                        'Improve onboarding documentation',
                        'Create contribution guidelines',
                        'Implement mentorship program',
                        'Regular community updates'
                    ]
                })
            
            # Tactical recommendations from documentation analysis
            if doc_recs.get('quick_wins'):
                for quick_win in doc_recs['quick_wins']:
                    recommendations['tactical'].append({
                        'title': quick_win['title'],
                        'description': quick_win['description'],
                        'actions': [quick_win['action']]
                    })
            
        except Exception as e:
            logger.error(f"Error generating combined recommendations: {e}")
        
        return recommendations
    
    def export_results(self, analysis_results: Dict[str, Any], output_dir: str) -> None:
        """Export comprehensive analysis results to multiple formats."""
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # JSON export
        json_path = output_path / 'comprehensive_analysis.json'
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(analysis_results, f, indent=2, default=str)
        
        # Markdown report
        report_path = output_path / 'analysis_report.md'
        report_content = self._generate_markdown_report(analysis_results)
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        # CSV exports for metrics
        self._export_csv_metrics(analysis_results, output_path)
        
        logger.info(f"Analysis results exported to {output_path}")
    
    def _generate_markdown_report(self, analysis_results: Dict[str, Any]) -> str:
        """Generate a comprehensive markdown report."""
        report = []
        
        # Header
        report.append("# Comprehensive Project Analysis Report")
        report.append("")
        report.append(f"**Generated:** {analysis_results['timestamp']}")
        report.append(f"**Project:** {analysis_results['project_path']}")
        report.append("")
        
        # Executive Summary
        combined_metrics = analysis_results.get('combined_metrics', {})
        report.append("## Executive Summary")
        report.append("")
        report.append(f"- **Overall Health Score:** {combined_metrics.get('overall_health_score', 0):.1f}/100")
        report.append(f"- **Documentation Health:** {combined_metrics.get('documentation_health', 0):.1f}/100")
        report.append(f"- **Development Velocity:** {combined_metrics.get('development_velocity', 0):.1f}/100")
        report.append(f"- **Community Engagement:** {combined_metrics.get('community_engagement', 0):.1f}/100")
        report.append(f"- **Project Maturity:** {combined_metrics.get('project_maturity', 0):.1f}/100")
        report.append("")
        
        # Documentation Analysis Summary
        doc_analysis = analysis_results.get('documentation_analysis', {})
        if doc_analysis:
            doc_metrics = doc_analysis.get('metrics', {})
            report.append("## Documentation Analysis")
            report.append("")
            report.append(f"- **Files Analyzed:** {doc_metrics.get('total_files', 0)}")
            report.append(f"- **Quality Score:** {doc_metrics.get('quality_score', 0):.1f}/100")
            report.append(f"- **Coverage:** {doc_metrics.get('coverage_percentage', 0):.1f}%")
            report.append(f"- **Issues Found:** {len(doc_analysis.get('issues', []))}")
            report.append("")
        
        # GitHub Analysis Summary
        github_analysis = analysis_results.get('github_analysis', {})
        if github_analysis:
            activity_metrics = github_analysis.get('activity_metrics', {})
            report.append("## GitHub Analysis")
            report.append("")
            report.append(f"- **Total Issues:** {activity_metrics.get('total_issues', 0)}")
            report.append(f"- **Issue Close Rate:** {activity_metrics.get('issue_close_rate', 0)*100:.1f}%")
            report.append(f"- **PR Merge Rate:** {activity_metrics.get('pr_merge_rate', 0)*100:.1f}%")
            report.append(f"- **Contributors:** {len(github_analysis.get('contributors', []))}")
            report.append("")
        
        # Integration Insights
        integration_insights = analysis_results.get('integration_insights', {})
        if integration_insights:
            report.append("## Integration Insights")
            report.append("")
            
            # Cross-component insights
            cross_insights = integration_insights.get('cross_component_insights', [])
            if cross_insights:
                report.append("### Key Insights")
                report.append("")
                for insight in cross_insights[:5]:  # Top 5 insights
                    report.append(f"- **{insight['title']}**")
                    report.append(f"  - {insight['description']}")
                    report.append(f"  - *{insight['implication']}*")
                    report.append("")
        
        # Critical Recommendations
        recommendations = analysis_results.get('recommendations', {})
        if recommendations:
            report.append("## Critical Recommendations")
            report.append("")
            
            for category in ['critical', 'high_priority', 'strategic']:
                if recommendations.get(category):
                    report.append(f"### {category.replace('_', ' ').title()}")
                    report.append("")
                    for rec in recommendations[category]:
                        report.append(f"- **{rec['title']}**")
                        report.append(f"  - {rec['description']}")
                        for action in rec.get('actions', []):
                            report.append(f"    - {action}")
                        report.append("")
        
        # Issues Summary
        all_issues = analysis_results.get('issues', [])
        if all_issues:
            report.append("## Issues Summary")
            report.append("")
            for issue in all_issues:
                report.append(f"- **{issue['component'].title()} Error:** {issue['message']}")
            report.append("")
        
        return "\n".join(report)
    
    def _export_csv_metrics(self, analysis_results: Dict[str, Any], output_path: Path) -> None:
        """Export metrics to CSV files."""
        import csv
        
        # Combined metrics CSV
        combined_metrics = analysis_results.get('combined_metrics', {})
        if combined_metrics:
            csv_path = output_path / 'combined_metrics.csv'
            with open(csv_path, 'w', newline='', encoding='utf-8') as f:
                writer = csv.writer(f)
                writer.writerow(['Metric', 'Value'])
                for metric, value in combined_metrics.items():
                    writer.writerow([metric, value])
        
        # Documentation metrics CSV
        doc_metrics = analysis_results.get('documentation_analysis', {}).get('metrics', {})
        if doc_metrics:
            csv_path = output_path / 'documentation_metrics.csv'
            with open(csv_path, 'w', newline='', encoding='utf-8') as f:
                writer = csv.writer(f)
                writer.writerow(['Metric', 'Value'])
                for metric, value in doc_metrics.items():
                    if isinstance(value, (int, float)):
                        writer.writerow([metric, value])

def main():
    """Main function for command-line usage."""
    parser = argparse.ArgumentParser(description='Comprehensive Analysis System')
    parser.add_argument('project_path', help='Path to the project to analyze')
    parser.add_argument('--github-token', help='GitHub token for API access')
    parser.add_argument('--output-dir', '-o', default='./analysis_output', help='Output directory')
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize the system
    system = ComprehensiveAnalysisSystem(args.project_path, args.github_token)
    
    # Run analysis
    results = system.run_comprehensive_analysis()
    
    # Export results
    system.export_results(results, args.output_dir)
    
    # Print summary
    combined_metrics = results.get('combined_metrics', {})
    print(f"\nComprehensive Analysis Summary:")
    print(f"- Overall Health Score: {combined_metrics.get('overall_health_score', 0):.1f}/100")
    print(f"- Documentation Health: {combined_metrics.get('documentation_health', 0):.1f}/100")
    print(f"- Development Velocity: {combined_metrics.get('development_velocity', 0):.1f}/100")
    print(f"- Community Engagement: {combined_metrics.get('community_engagement', 0):.1f}/100")
    print(f"- Project Maturity: {combined_metrics.get('project_maturity', 0):.1f}/100")
    
    # Show critical issues
    issues = results.get('issues', [])
    if issues:
        print(f"\nCritical Issues Found: {len(issues)}")
        for issue in issues[:3]:  # Show first 3
            print(f"- {issue['component'].title()}: {issue['message']}")

if __name__ == "__main__":
    main()